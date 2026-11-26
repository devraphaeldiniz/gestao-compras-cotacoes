package com.compras.cotacoes.cotacao;

import com.compras.cotacoes.cotacao.dto.AbrirCotacaoRequest;
import com.compras.cotacoes.cotacao.dto.CotacaoDetalheResponse;
import com.compras.cotacoes.cotacao.dto.CotacaoResumoResponse;
import com.compras.cotacoes.cotacao.dto.CriarCotacaoRequest;
import com.compras.cotacoes.cotacao.dto.ItemCotacaoRequest;
import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.fornecedor.FornecedorMapper;
import com.compras.cotacoes.fornecedor.FornecedorRepository;
import com.compras.cotacoes.historico.AcaoHistorico;
import com.compras.cotacoes.historico.EntidadeHistorico;
import com.compras.cotacoes.historico.HistoricoService;
import com.compras.cotacoes.historico.dto.EventoHistoricoResponse;
import com.compras.cotacoes.notificacao.CotacaoAbertaEvento;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.produto.ProdutoRepository;
import com.compras.cotacoes.proposta.AvaliadorPropostasService;
import com.compras.cotacoes.proposta.CriterioDecisao;
import com.compras.cotacoes.proposta.Proposta;
import com.compras.cotacoes.proposta.PropostaMapper;
import com.compras.cotacoes.proposta.PropostaRepository;
import com.compras.cotacoes.proposta.PropostaService;
import com.compras.cotacoes.proposta.StatusProposta;
import com.compras.cotacoes.proposta.dto.RecomendacaoResponse;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import com.compras.cotacoes.shared.metricas.MetricasCompras;
import com.compras.cotacoes.shared.seguranca.UsuarioAtual;
import com.compras.cotacoes.shared.web.PaginaResponse;
import com.compras.cotacoes.shared.web.Paginacao;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CotacaoService {

    private static final List<StatusCotacao> STATUS_EM_ANDAMENTO = List.of(StatusCotacao.RASCUNHO, StatusCotacao.ABERTA);

    private final CotacaoRepository cotacaoRepository;
    private final CotacaoItemRepository cotacaoItemRepository;
    private final ProdutoRepository produtoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final PropostaRepository propostaRepository;
    private final CotacaoMapper cotacaoMapper;
    private final FornecedorMapper fornecedorMapper;
    private final PropostaMapper propostaMapper;
    private final AvaliadorPropostasService avaliador;
    private final HistoricoService historico;
    private final MetricasCompras metricas;
    private final UsuarioAtual usuarioAtual;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    /**
     * Regra B: criação manual com um ou mais itens, em RASCUNHO (padrão) ou já ABERTA. Um produto não pode
     * estar em duas cotações em andamento. Os produtos são bloqueados para não concorrer com a reposição automática.
     */
    @Transactional
    public CotacaoDetalheResponse criar(CriarCotacaoRequest request) {
        StatusCotacao status = request.status() == null ? StatusCotacao.RASCUNHO : request.status();
        if (!STATUS_EM_ANDAMENTO.contains(status)) {
            throw new NegocioException("cotacao.status.inicial.invalido", status);
        }
        Map<Long, ItemCotacaoRequest> itensPorProduto = new HashMap<>();
        for (ItemCotacaoRequest item : request.itens()) {
            if (itensPorProduto.put(item.produtoId(), item) != null) {
                throw new NegocioException("cotacao.item.duplicado", item.produtoId());
            }
        }
        validarDataLimite(request.dataLimite());

        Map<Long, Produto> produtos = produtoRepository.buscarComBloqueio(itensPorProduto.keySet()).stream()
                .collect(Collectors.toMap(Produto::getId, Function.identity()));
        Cotacao cotacao = new Cotacao();
        for (ItemCotacaoRequest item : request.itens()) {
            Produto produto = produtos.get(item.produtoId());
            if (produto == null) {
                throw new RecursoNaoEncontradoException("produto.nao.encontrado", item.produtoId());
            }
            if (!produto.isAtivo()) {
                throw new NegocioException("produto.inativo", produto.getSku());
            }
            cotacaoItemRepository.cotacaoEmAndamentoDoProduto(produto.getId(), STATUS_EM_ANDAMENTO).ifPresent(outra -> {
                throw new NegocioException("cotacao.produto.em.andamento", produto.getSku(), outra.getCodigo(), outra.getStatus());
            });
            cotacao.adicionarItem(produto, item.quantidade());
        }

        cotacao.setCodigo(Cotacao.gerarCodigo(Year.now(clock).getValue(), cotacaoRepository.proximoNumeroCodigo()));
        cotacao.setStatus(status);
        cotacao.setOrigem(OrigemCotacao.MANUAL);
        cotacao.setCriadoPor(usuarioAtual.username());
        cotacao.setDataLimite(request.dataLimite());
        cotacao.setObservacao(request.observacao());
        cotacao.setFornecedoresConvidados(carregarFornecedoresAtivos(request.fornecedoresIds()));

        Cotacao salva = cotacaoRepository.save(cotacao);
        historico.registrar(EntidadeHistorico.COTACAO, salva.getId(), AcaoHistorico.COTACAO_CRIADA, resumoItens(salva));
        metricas.cotacaoCriada(OrigemCotacao.MANUAL.name());
        if (status == StatusCotacao.ABERTA) {
            historico.registrar(EntidadeHistorico.COTACAO, salva.getId(), AcaoHistorico.COTACAO_ABERTA, null);
            eventos.publishEvent(new CotacaoAbertaEvento(salva.getId()));
        }
        return montarDetalhe(salva, List.of());
    }

    @Transactional(readOnly = true)
    public PaginaResponse<CotacaoResumoResponse> listar(StatusCotacao status, Long produtoId, Integer pagina, Integer tamanho) {
        Page<Cotacao> paginaCotacoes = cotacaoRepository.buscar(status, produtoId,
                Paginacao.de(pagina, tamanho, Sort.by(Sort.Order.desc("id"))));
        List<Long> ids = paginaCotacoes.getContent().stream().map(Cotacao::getId).toList();
        // Itens e contagem de propostas em consultas separadas: paginar com fetch de coleção faria a paginação em memória
        Map<Long, Cotacao> comItens = ids.isEmpty() ? Map.of() : cotacaoRepository.carregarComItens(ids).stream()
                .collect(Collectors.toMap(Cotacao::getId, Function.identity()));
        Map<Long, Long> propostas = contarPropostas(ids);
        return PaginaResponse.de(paginaCotacoes, c -> {
            Cotacao completa = comItens.getOrDefault(c.getId(), c);
            return new CotacaoResumoResponse(c.getId(), c.getCodigo(), c.getOrigem(), c.getStatus(),
                    cotacaoMapper.paraItens(completa.getItens()), c.getDataLimite(),
                    propostas.getOrDefault(c.getId(), 0L), c.getPropostaVencedoraId(), c.getCriadoPor(), c.getCriadoEm());
        });
    }

    @Transactional(readOnly = true)
    public CotacaoDetalheResponse detalhar(Long id) {
        Cotacao cotacao = cotacaoRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("cotacao.nao.encontrada", id));
        return montarDetalhe(cotacao, propostaRepository.findByCotacaoIdOrderByIdAsc(id));
    }

    /** Regra B: RASCUNHO -> ABERTA, com convidados e data limite opcionais. Os convidados recebem e-mail. */
    @Transactional
    public CotacaoDetalheResponse abrir(Long id, AbrirCotacaoRequest request) {
        Cotacao cotacao = buscarComBloqueio(id);
        if (cotacao.getStatus() != StatusCotacao.RASCUNHO) {
            throw new NegocioException("cotacao.status.invalido.abrir", cotacao.getCodigo(), cotacao.getStatus());
        }
        LocalDate dataLimite = request == null ? null : request.dataLimite();
        validarDataLimite(dataLimite);

        cotacao.setStatus(StatusCotacao.ABERTA);
        if (dataLimite != null) {
            cotacao.setDataLimite(dataLimite);
        }
        if (request != null && request.fornecedoresIds() != null && !request.fornecedoresIds().isEmpty()) {
            cotacao.getFornecedoresConvidados().clear();
            cotacao.getFornecedoresConvidados().addAll(carregarFornecedoresAtivos(request.fornecedoresIds()));
        }
        Cotacao salva = cotacaoRepository.save(cotacao);
        historico.registrar(EntidadeHistorico.COTACAO, id, AcaoHistorico.COTACAO_ABERTA,
                salva.abertaAoMercado() ? null : "convidados=" + salva.getFornecedoresConvidados().size());
        eventos.publishEvent(new CotacaoAbertaEvento(id));
        return montarDetalhe(salva, propostaRepository.findByCotacaoIdOrderByIdAsc(id));
    }

    /** Cancela uma cotação em andamento; as propostas pendentes são rejeitadas. */
    @Transactional
    public CotacaoDetalheResponse cancelar(Long id, String motivo) {
        Cotacao cotacao = buscarComBloqueio(id);
        if (!cotacao.emAndamento()) {
            throw new NegocioException("cotacao.status.invalido.cancelar", cotacao.getCodigo(), cotacao.getStatus());
        }
        aplicarCancelamento(cotacao, motivo.trim(), usuarioAtual.username());
        metricas.cotacaoCancelada("manual");
        return montarDetalhe(cotacao, propostaRepository.findByCotacaoIdOrderByIdAsc(id));
    }

    /** Usado também pelo job de prazos (cancelamento automático). */
    void aplicarCancelamento(Cotacao cotacao, String motivo, String autor) {
        cotacao.setStatus(StatusCotacao.CANCELADA);
        cotacao.setMotivoCancelamento(motivo);
        cotacao.setCanceladoPor(autor);
        cotacao.setCanceladoEm(LocalDateTime.now(clock));
        List<Proposta> propostas = propostaRepository.findByCotacaoIdOrderByIdAsc(cotacao.getId());
        propostas.stream().filter(p -> p.getStatus() == StatusProposta.PENDENTE)
                .forEach(p -> p.setStatus(StatusProposta.REJEITADA));
        propostaRepository.saveAll(propostas);
        cotacaoRepository.save(cotacao);
        historico.registrar(EntidadeHistorico.COTACAO, cotacao.getId(), AcaoHistorico.COTACAO_CANCELADA, motivo);
    }

    @Transactional(readOnly = true)
    public List<EventoHistoricoResponse> historico(Long id) {
        if (!cotacaoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("cotacao.nao.encontrada", id);
        }
        return historico.listar(EntidadeHistorico.COTACAO, id);
    }

    private CotacaoDetalheResponse montarDetalhe(Cotacao cotacao, List<Proposta> propostas) {
        LocalDate hoje = LocalDate.now(clock);
        List<Proposta> elegiveis = avaliador.filtrarElegiveis(propostas, hoje);
        boolean podeDecidir = cotacao.getStatus() == StatusCotacao.ABERTA
                && elegiveis.size() >= PropostaService.MINIMO_PROPOSTAS_VALIDAS;
        List<Fornecedor> convidados = cotacao.getFornecedoresConvidados().stream()
                .sorted(Comparator.comparing(Fornecedor::getRazaoSocial))
                .toList();

        return new CotacaoDetalheResponse(
                cotacao.getId(),
                cotacao.getCodigo(),
                cotacao.getOrigem(),
                cotacao.getStatus(),
                cotacaoMapper.paraItens(cotacao.getItens()),
                cotacao.getDataLimite(),
                cotacao.getObservacao(),
                cotacao.getPropostaVencedoraId(),
                cotacao.abertaAoMercado(),
                fornecedorMapper.paraRespostas(convidados),
                propostaMapper.paraRespostas(propostas),
                elegiveis.size(),
                PropostaService.MINIMO_PROPOSTAS_VALIDAS,
                podeDecidir,
                recomendar(elegiveis, CriterioDecisao.MENOR_PRECO),
                recomendar(elegiveis, CriterioDecisao.PONTUACAO_PONDERADA),
                cotacao.getCriadoPor(),
                cotacao.getCriterioDecisao(),
                cotacao.getDecididoPor(),
                cotacao.getDecididoEm(),
                cotacao.getPrazoEncerradoEm(),
                cotacao.getMotivoCancelamento(),
                cotacao.getCanceladoPor(),
                cotacao.getCanceladoEm(),
                cotacao.getCriadoEm(),
                cotacao.getAtualizadoEm());
    }

    /** Prévia: o ranking aparece mesmo antes de haver 3 propostas, mas a decisão continua bloqueada. */
    private RecomendacaoResponse recomendar(List<Proposta> elegiveis, CriterioDecisao criterio) {
        List<AvaliadorPropostasService.PropostaAvaliada> ranking = avaliador.classificar(elegiveis, criterio);
        if (ranking.isEmpty()) {
            return new RecomendacaoResponse(criterio, null, null, List.of());
        }
        Proposta melhor = ranking.getFirst().proposta();
        return new RecomendacaoResponse(criterio, melhor.getId(), melhor.getFornecedor().getRazaoSocial(),
                ranking.stream().map(propostaMapper::paraItemRanking).toList());
    }

    private Set<Fornecedor> carregarFornecedoresAtivos(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<Long> idsUnicos = new LinkedHashSet<>(ids);
        Map<Long, Fornecedor> encontrados = fornecedorRepository.findAllById(idsUnicos).stream()
                .collect(Collectors.toMap(Fornecedor::getId, f -> f));

        Set<Fornecedor> convidados = new LinkedHashSet<>();
        for (Long id : idsUnicos) {
            Fornecedor fornecedor = encontrados.get(id);
            if (fornecedor == null) {
                throw new RecursoNaoEncontradoException("fornecedor.nao.encontrado", id);
            }
            if (!fornecedor.isAtivo()) {
                throw new NegocioException("fornecedor.inativo", fornecedor.getRazaoSocial());
            }
            convidados.add(fornecedor);
        }
        return convidados;
    }

    private void validarDataLimite(LocalDate dataLimite) {
        if (dataLimite != null && dataLimite.isBefore(LocalDate.now(clock))) {
            throw new NegocioException("cotacao.data.limite.passada");
        }
    }

    private Map<Long, Long> contarPropostas(List<Long> cotacaoIds) {
        Map<Long, Long> contagem = new HashMap<>();
        if (cotacaoIds.isEmpty()) {
            return contagem;
        }
        for (Object[] linha : propostaRepository.contarPorCotacoes(cotacaoIds)) {
            contagem.put((Long) linha[0], (Long) linha[1]);
        }
        return contagem;
    }

    private Cotacao buscarComBloqueio(Long id) {
        return cotacaoRepository.buscarComBloqueio(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("cotacao.nao.encontrada", id));
    }

    static String resumoItens(Cotacao cotacao) {
        return cotacao.getItens().stream()
                .map(i -> i.getProduto().getSku() + " x " + i.getQuantidade().stripTrailingZeros().toPlainString())
                .collect(Collectors.joining("; "));
    }
}
