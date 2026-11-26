package com.compras.cotacoes.cotacao;

import com.compras.cotacoes.cotacao.dto.AbrirCotacaoRequest;
import com.compras.cotacoes.cotacao.dto.CotacaoDetalheResponse;
import com.compras.cotacoes.cotacao.dto.CotacaoResumoResponse;
import com.compras.cotacoes.cotacao.dto.CriarCotacaoRequest;
import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.fornecedor.FornecedorMapper;
import com.compras.cotacoes.fornecedor.FornecedorRepository;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.produto.ProdutoRepository;
import com.compras.cotacoes.proposta.AvaliadorPropostasService;
import com.compras.cotacoes.proposta.CriterioDecisao;
import com.compras.cotacoes.proposta.Proposta;
import com.compras.cotacoes.proposta.PropostaMapper;
import com.compras.cotacoes.proposta.PropostaRepository;
import com.compras.cotacoes.proposta.PropostaService;
import com.compras.cotacoes.proposta.dto.RecomendacaoResponse;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CotacaoService {

    private static final List<StatusCotacao> STATUS_EM_ANDAMENTO = List.of(StatusCotacao.RASCUNHO, StatusCotacao.ABERTA);

    private final CotacaoRepository cotacaoRepository;
    private final ProdutoRepository produtoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final PropostaRepository propostaRepository;
    private final CotacaoMapper cotacaoMapper;
    private final FornecedorMapper fornecedorMapper;
    private final PropostaMapper propostaMapper;
    private final AvaliadorPropostasService avaliador;
    private final Clock clock;

    /** Regra B: criação manual, em RASCUNHO (padrão) ou já ABERTA. Não permite duas cotações em andamento por produto. */
    @Transactional
    public CotacaoDetalheResponse criar(CriarCotacaoRequest request) {
        Produto produto = produtoRepository.findById(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("produto.nao.encontrado", request.produtoId()));
        if (!produto.isAtivo()) {
            throw new NegocioException("produto.inativo", produto.getSku());
        }

        StatusCotacao status = request.status() == null ? StatusCotacao.RASCUNHO : request.status();
        if (!STATUS_EM_ANDAMENTO.contains(status)) {
            throw new NegocioException("cotacao.status.inicial.invalido", status);
        }
        cotacaoRepository.findFirstByProdutoIdAndStatusInOrderByIdDesc(produto.getId(), STATUS_EM_ANDAMENTO)
                .ifPresent(existente -> {
                    throw new NegocioException("cotacao.em.andamento", existente.getCodigo(), existente.getStatus());
                });
        validarDataLimite(request.dataLimite());

        Cotacao cotacao = new Cotacao();
        cotacao.setCodigo(Cotacao.gerarCodigo(Year.now(clock).getValue(), cotacaoRepository.proximoNumeroCodigo()));
        cotacao.setProduto(produto);
        cotacao.setQuantidade(request.quantidade());
        cotacao.setStatus(status);
        cotacao.setDataLimite(request.dataLimite());
        cotacao.setObservacao(request.observacao());
        cotacao.setFornecedoresConvidados(carregarFornecedoresAtivos(request.fornecedoresIds()));

        return montarDetalhe(cotacaoRepository.save(cotacao), List.of());
    }

    @Transactional(readOnly = true)
    public List<CotacaoResumoResponse> listar(StatusCotacao status) {
        List<Cotacao> cotacoes = status == null
                ? cotacaoRepository.findAllByOrderByIdDesc()
                : cotacaoRepository.findByStatusOrderByIdDesc(status);
        Map<Long, Long> propostasPorCotacao = contarPropostas(cotacoes.stream().map(Cotacao::getId).toList());
        return cotacoes.stream()
                .map(c -> cotacaoMapper.paraResumo(c, propostasPorCotacao.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CotacaoDetalheResponse detalhar(Long id) {
        Cotacao cotacao = buscarComDetalhes(id);
        return montarDetalhe(cotacao, propostaRepository.findByCotacaoIdOrderByIdAsc(id));
    }

    /** Regra B: RASCUNHO -> ABERTA, com convidados e data limite opcionais. */
    @Transactional
    public CotacaoDetalheResponse abrir(Long id, AbrirCotacaoRequest request) {
        Cotacao cotacao = buscarComDetalhes(id);
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
        return montarDetalhe(salva, propostaRepository.findByCotacaoIdOrderByIdAsc(id));
    }

    private CotacaoDetalheResponse montarDetalhe(Cotacao cotacao, List<Proposta> propostas) {
        LocalDate hoje = LocalDate.now(clock);
        List<Proposta> elegiveis = avaliador.filtrarElegiveis(propostas, hoje);
        boolean podeDecidir = cotacao.getStatus() == StatusCotacao.ABERTA
                && elegiveis.size() >= PropostaService.MINIMO_PROPOSTAS_VALIDAS;

        List<Fornecedor> convidados = cotacao.getFornecedoresConvidados().stream()
                .sorted(Comparator.comparing(Fornecedor::getRazaoSocial))
                .toList();
        Produto produto = cotacao.getProduto();

        return new CotacaoDetalheResponse(
                cotacao.getId(),
                cotacao.getCodigo(),
                produto.getId(),
                produto.getSku(),
                produto.getNome(),
                produto.getUnidade(),
                produto.getSaldoAtual(),
                produto.getEstoqueMinimo(),
                cotacao.getQuantidade(),
                cotacao.getStatus(),
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

    private Cotacao buscarComDetalhes(Long id) {
        return cotacaoRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("cotacao.nao.encontrada", id));
    }
}
