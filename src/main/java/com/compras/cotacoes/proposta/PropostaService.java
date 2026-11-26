package com.compras.cotacoes.proposta;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.cotacao.CotacaoRepository;
import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.fornecedor.FornecedorRepository;
import com.compras.cotacoes.pedido.PedidoCompra;
import com.compras.cotacoes.pedido.PedidoCompraMapper;
import com.compras.cotacoes.pedido.PedidoCompraService;
import com.compras.cotacoes.proposta.AvaliadorPropostasService.PropostaAvaliada;
import com.compras.cotacoes.proposta.dto.DecidirCotacaoRequest;
import com.compras.cotacoes.proposta.dto.PropostaResponse;
import com.compras.cotacoes.proposta.dto.RegistrarPropostaRequest;
import com.compras.cotacoes.proposta.dto.ResultadoDecisaoResponse;
import com.compras.cotacoes.shared.exception.ConflitoException;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import com.compras.cotacoes.shared.i18n.MensagemService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropostaService {

    /** Trava da Regra D: a decisão exige pelo menos 3 propostas elegíveis. */
    public static final int MINIMO_PROPOSTAS_VALIDAS = 3;

    /** Limite de NUMERIC(19,2): 17 dígitos inteiros. */
    static final BigDecimal VALOR_TOTAL_MAXIMO = BigDecimal.TEN.pow(17);

    private final PropostaRepository propostaRepository;
    private final CotacaoRepository cotacaoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final CalculadoraPropostaService calculadora;
    private final AvaliadorPropostasService avaliador;
    private final PedidoCompraService pedidoCompraService;
    private final PropostaMapper propostaMapper;
    private final PedidoCompraMapper pedidoCompraMapper;
    private final MensagemService mensagens;
    private final Clock clock;

    /** Regra C: valida a cotação e o fornecedor e grava a proposta com o valor total já calculado. */
    @Transactional
    public PropostaResponse registrar(Long cotacaoId, RegistrarPropostaRequest request) {
        Cotacao cotacao = buscarCotacao(cotacaoId);
        LocalDate hoje = LocalDate.now(clock);

        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new NegocioException("cotacao.nao.aberta", cotacao.getCodigo(), cotacao.getStatus());
        }
        if (cotacao.getDataLimite() != null && hoje.isAfter(cotacao.getDataLimite())) {
            throw new NegocioException("cotacao.prazo.encerrado", cotacao.getCodigo(), cotacao.getDataLimite());
        }

        Fornecedor fornecedor = fornecedorRepository.findById(request.fornecedorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("fornecedor.nao.encontrado", request.fornecedorId()));
        if (!fornecedor.isAtivo()) {
            throw new NegocioException("fornecedor.inativo", fornecedor.getRazaoSocial());
        }
        if (!cotacao.abertaAoMercado() && !cotacao.convidou(fornecedor)) {
            throw new NegocioException("cotacao.fornecedor.nao.convidado", fornecedor.getRazaoSocial(), cotacao.getCodigo());
        }
        if (propostaRepository.existsByCotacaoIdAndFornecedorId(cotacao.getId(), fornecedor.getId())) {
            throw new ConflitoException("proposta.duplicada", fornecedor.getRazaoSocial(), cotacao.getCodigo());
        }
        if (request.dataValidade().isBefore(hoje)) {
            throw new NegocioException("proposta.validade.passada");
        }

        CalculadoraPropostaService.ResultadoCalculo calculo = calculadora.calcular(
                cotacao.getQuantidade(), request.precoUnitario(), request.valorFrete(),
                request.impostoPercentual(), request.valorImpostoFixo());
        // A coluna valor_total é NUMERIC(19,2): sem esta checagem o overflow viraria erro de banco
        if (calculo.valorTotal().compareTo(VALOR_TOTAL_MAXIMO) >= 0) {
            throw new NegocioException("proposta.valor.excede.limite");
        }

        Proposta proposta = new Proposta();
        proposta.setCotacao(cotacao);
        proposta.setFornecedor(fornecedor);
        proposta.setPrecoUnitario(request.precoUnitario());
        proposta.setValorFrete(zeroSeNulo(request.valorFrete()));
        proposta.setImpostoPercentual(zeroSeNulo(request.impostoPercentual()));
        proposta.setValorImpostoFixo(zeroSeNulo(request.valorImpostoFixo()));
        proposta.setValorImpostoCalculado(calculo.valorImposto());
        proposta.setValorTotal(calculo.valorTotal());
        proposta.setPrazoEntregaDias(request.prazoEntregaDias());
        proposta.setDataValidade(request.dataValidade());
        proposta.setStatus(StatusProposta.PENDENTE);

        return propostaMapper.paraResposta(propostaRepository.save(proposta));
    }

    /**
     * Regra D: exige 3+ propostas elegíveis, aplica o critério, aceita a recomendação ou uma escolha manual
     * justificada e, na mesma transação, fecha a cotação, classifica todas as propostas e emite o pedido.
     */
    @Transactional
    public ResultadoDecisaoResponse decidir(Long cotacaoId, DecidirCotacaoRequest request) {
        Cotacao cotacao = buscarCotacao(cotacaoId);
        if (cotacao.getStatus() != StatusCotacao.ABERTA) {
            throw new NegocioException("cotacao.nao.aberta", cotacao.getCodigo(), cotacao.getStatus());
        }
        CriterioDecisao criterio = request.criterio() == null ? CriterioDecisao.MENOR_PRECO : request.criterio();
        LocalDate hoje = LocalDate.now(clock);

        List<Proposta> propostas = propostaRepository.findByCotacaoIdOrderByIdAsc(cotacao.getId());
        List<Proposta> elegiveis = avaliador.filtrarElegiveis(propostas, hoje);
        if (elegiveis.size() < MINIMO_PROPOSTAS_VALIDAS) {
            throw new NegocioException("proposta.minimo.nao.atingido",
                    MINIMO_PROPOSTAS_VALIDAS, cotacao.getCodigo(), elegiveis.size());
        }

        List<PropostaAvaliada> ranking = avaliador.classificar(elegiveis, criterio);
        PropostaAvaliada recomendada = ranking.getFirst();
        PropostaAvaliada vencedora = escolherVencedora(cotacao, propostas, ranking, recomendada, request, criterio, hoje);
        boolean escolhaManual = vencedora != recomendada;

        String justificativa = StringUtils.hasText(request.justificativaEscolha())
                ? request.justificativaEscolha().trim()
                : mensagens.obter("proposta.justificativa.automatica", criterio);

        aplicarResultado(propostas, ranking, vencedora, justificativa);

        cotacao.setStatus(StatusCotacao.FECHADA);
        cotacao.setPropostaVencedoraId(vencedora.proposta().getId());
        propostaRepository.saveAll(propostas);
        cotacaoRepository.save(cotacao);

        PedidoCompra pedido = pedidoCompraService.emitir(cotacao, vencedora.proposta());
        log.info("Cotação {} decidida por {}: proposta {} venceu, pedido {} emitido",
                cotacao.getCodigo(), criterio, vencedora.proposta().getId(), pedido.getNumeroPedido());

        return new ResultadoDecisaoResponse(
                cotacao.getId(),
                cotacao.getCodigo(),
                cotacao.getStatus(),
                criterio,
                recomendada.proposta().getId(),
                escolhaManual,
                justificativa,
                propostaMapper.paraResposta(vencedora.proposta()),
                ranking.stream().map(propostaMapper::paraItemRanking).toList(),
                pedidoCompraMapper.paraResposta(pedido));
    }

    private PropostaAvaliada escolherVencedora(Cotacao cotacao, List<Proposta> propostas, List<PropostaAvaliada> ranking,
                                               PropostaAvaliada recomendada, DecidirCotacaoRequest request,
                                               CriterioDecisao criterio, LocalDate hoje) {
        Long escolhidaId = request.propostaIdEscolhida();
        if (escolhidaId == null) {
            return recomendada;
        }
        Proposta escolhida = propostas.stream()
                .filter(p -> Objects.equals(p.getId(), escolhidaId))
                .findFirst()
                .orElseThrow(() -> new NegocioException("proposta.nao.pertence", escolhidaId, cotacao.getCodigo()));
        avaliador.motivoInelegibilidade(escolhida, hoje).ifPresent(motivo -> {
            throw motivo;
        });

        PropostaAvaliada avaliada = ranking.stream()
                .filter(item -> item.proposta() == escolhida)
                .findFirst()
                .orElseThrow();
        if (avaliada != recomendada && !StringUtils.hasText(request.justificativaEscolha())) {
            throw new NegocioException("proposta.justificativa.obrigatoria",
                    escolhidaId, criterio, recomendada.proposta().getId());
        }
        return avaliada;
    }

    /** Vencedora -> VENCEDORA; demais elegíveis -> REJEITADA; pendentes inelegíveis -> DESQUALIFICADA. */
    private static void aplicarResultado(List<Proposta> propostas, List<PropostaAvaliada> ranking,
                                         PropostaAvaliada vencedora, String justificativa) {
        // Mapa por identidade: as propostas da lista e as do ranking são as mesmas instâncias
        Map<Proposta, BigDecimal> pontuacoes = new IdentityHashMap<>();
        ranking.forEach(item -> pontuacoes.put(item.proposta(), item.pontuacao()));

        for (Proposta proposta : propostas) {
            if (proposta == vencedora.proposta()) {
                proposta.setStatus(StatusProposta.VENCEDORA);
                proposta.setPontuacaoCalculada(vencedora.pontuacao());
                proposta.setJustificativaEscolha(justificativa);
            } else if (pontuacoes.containsKey(proposta)) {
                proposta.setStatus(StatusProposta.REJEITADA);
                proposta.setPontuacaoCalculada(pontuacoes.get(proposta));
            } else if (proposta.getStatus() == StatusProposta.PENDENTE) {
                proposta.setStatus(StatusProposta.DESQUALIFICADA);
            }
        }
    }

    /** Com bloqueio de linha: registro de proposta e decisão da mesma cotação nunca rodam em paralelo. */
    private Cotacao buscarCotacao(Long cotacaoId) {
        return cotacaoRepository.buscarComBloqueio(cotacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("cotacao.nao.encontrada", cotacaoId));
    }

    private static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
