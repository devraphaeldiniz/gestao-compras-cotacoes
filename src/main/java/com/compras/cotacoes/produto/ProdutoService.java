package com.compras.cotacoes.produto;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.cotacao.CotacaoItemRepository;
import com.compras.cotacoes.cotacao.CotacaoRepository;
import com.compras.cotacoes.cotacao.OrigemCotacao;
import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.historico.AcaoHistorico;
import com.compras.cotacoes.historico.EntidadeHistorico;
import com.compras.cotacoes.historico.HistoricoService;
import com.compras.cotacoes.produto.dto.AtualizarProdutoRequest;
import com.compras.cotacoes.produto.dto.CriarProdutoRequest;
import com.compras.cotacoes.produto.dto.MovimentarEstoqueRequest;
import com.compras.cotacoes.produto.dto.ProdutoResponse;
import com.compras.cotacoes.shared.exception.ConflitoException;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import com.compras.cotacoes.shared.i18n.MensagemService;
import com.compras.cotacoes.shared.metricas.MetricasCompras;
import com.compras.cotacoes.shared.seguranca.UsuarioAtual;
import com.compras.cotacoes.shared.web.PaginaResponse;
import com.compras.cotacoes.shared.web.Paginacao;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdutoService {

    static final List<StatusCotacao> STATUS_EM_ANDAMENTO = List.of(StatusCotacao.RASCUNHO, StatusCotacao.ABERTA);

    private final ProdutoRepository produtoRepository;
    private final CotacaoRepository cotacaoRepository;
    private final CotacaoItemRepository cotacaoItemRepository;
    private final ProdutoMapper produtoMapper;
    private final ExtratoEstoqueService extratoEstoque;
    private final HistoricoService historico;
    private final MetricasCompras metricas;
    private final MensagemService mensagens;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    @Transactional
    public ProdutoResponse criar(CriarProdutoRequest request) {
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (produtoRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflitoException("produto.sku.duplicado", sku);
        }
        Produto produto = produtoMapper.paraEntidade(request);
        produto.setSku(sku);
        produto.setNome(request.nome().trim());
        produto.setSaldoAtual(BigDecimal.ZERO);

        Produto salvo = produtoRepository.save(produto);
        extratoEstoque.registrar(salvo, TipoMovimentacaoEstoque.SALDO_INICIAL, request.saldoAtual(), request.saldoAtual(),
                null, null, null);
        historico.registrar(EntidadeHistorico.PRODUTO, salvo.getId(), AcaoHistorico.PRODUTO_CRIADO, sku);
        verificarNecessidadeCompra(salvo);
        return produtoMapper.paraResposta(salvo);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscar(Long id) {
        return produtoMapper.paraResposta(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProdutoResponse> listar(boolean apenasNecessidadeCompra, Boolean ativo, String busca,
                                                  Integer pagina, Integer tamanho) {
        String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
        return PaginaResponse.de(produtoRepository.buscar(apenasNecessidadeCompra, ativo, termo,
                Paginacao.de(pagina, tamanho, Sort.by("nome", "id"))), produtoMapper::paraResposta);
    }

    /** Nome, unidade e estoque mínimo. Subir o mínimo acima do saldo dispara a reposição na hora. */
    @Transactional
    public ProdutoResponse atualizar(Long id, AtualizarProdutoRequest request) {
        Produto produto = produtoRepository.buscarComBloqueio(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("produto.nao.encontrado", id));
        String alteracoes = descreverAlteracoes(produto, request);
        produto.setNome(request.nome().trim());
        produto.setUnidade(request.unidade());
        produto.setEstoqueMinimo(request.estoqueMinimo());
        Produto salvo = produtoRepository.save(produto);
        if (!alteracoes.isEmpty()) {
            historico.registrar(EntidadeHistorico.PRODUTO, id, AcaoHistorico.PRODUTO_ATUALIZADO, alteracoes);
        }
        verificarNecessidadeCompra(salvo);
        return produtoMapper.paraResposta(salvo);
    }

    @Transactional
    public ProdutoResponse alterarStatus(Long id, boolean ativo) {
        Produto produto = buscarEntidade(id);
        if (produto.isAtivo() != ativo) {
            produto.setAtivo(ativo);
            historico.registrar(EntidadeHistorico.PRODUTO, id,
                    ativo ? AcaoHistorico.PRODUTO_ATIVADO : AcaoHistorico.PRODUTO_INATIVADO, produto.getSku());
        }
        return produtoMapper.paraResposta(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponse movimentarEstoque(Long produtoId, MovimentarEstoqueRequest request) {
        if (!request.tipo().manual()) {
            throw new NegocioException("estoque.tipo.invalido", request.tipo());
        }
        Produto produto = produtoRepository.buscarComBloqueio(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("produto.nao.encontrado", produtoId));
        BigDecimal quantidade = request.quantidade();
        BigDecimal saldo = produto.getSaldoAtual();

        BigDecimal novoSaldo = switch (request.tipo()) {
            case ENTRADA -> {
                exigirQuantidadePositiva(request);
                yield saldo.add(quantidade);
            }
            case SAIDA -> {
                exigirQuantidadePositiva(request);
                if (quantidade.compareTo(saldo) > 0) {
                    throw new NegocioException("estoque.saldo.insuficiente", saldo, quantidade);
                }
                yield saldo.subtract(quantidade);
            }
            case AJUSTE -> quantidade;
            default -> throw new NegocioException("estoque.tipo.invalido", request.tipo());
        };

        extratoEstoque.registrar(produto, request.tipo(), quantidade, novoSaldo, null, null, request.observacao());
        Produto salvo = produtoRepository.save(produto);
        if (request.tipo() != TipoMovimentacaoEstoque.ENTRADA) {
            verificarNecessidadeCompra(salvo);
        }
        return produtoMapper.paraResposta(salvo);
    }

    /**
     * Regra A: se o saldo chegou ao estoque mínimo e o produto não está em nenhuma cotação RASCUNHO/ABERTA,
     * inclui o produto no rascunho de reposição automática mais recente ou, se não houver, cria um novo.
     * Assim várias necessidades de compra viram uma única cotação com vários itens.
     */
    Optional<Cotacao> verificarNecessidadeCompra(Produto produto) {
        if (!produto.isAtivo() || !produto.precisaReposicao()) {
            return Optional.empty();
        }
        Optional<Cotacao> emAndamento = cotacaoItemRepository.cotacaoEmAndamentoDoProduto(produto.getId(), STATUS_EM_ANDAMENTO);
        if (emAndamento.isPresent()) {
            log.debug("Produto {} já está na cotação {} em andamento", produto.getSku(), emAndamento.get().getCodigo());
            return Optional.empty();
        }

        BigDecimal quantidade = quantidadeSugerida(produto.getSaldoAtual(), produto.getEstoqueMinimo());
        String detalhe = "%s saldo=%s minimo=%s quantidade=%s".formatted(produto.getSku(),
                produto.getSaldoAtual().stripTrailingZeros().toPlainString(),
                produto.getEstoqueMinimo().stripTrailingZeros().toPlainString(),
                quantidade.stripTrailingZeros().toPlainString());

        Optional<Cotacao> rascunho = cotacaoRepository
                .findFirstByStatusAndOrigemOrderByIdDesc(StatusCotacao.RASCUNHO, OrigemCotacao.AUTOMATICA)
                .flatMap(c -> cotacaoRepository.buscarComBloqueio(c.getId()))
                .filter(c -> c.getStatus() == StatusCotacao.RASCUNHO);
        if (rascunho.isPresent()) {
            Cotacao cotacao = rascunho.get();
            cotacao.adicionarItem(produto, quantidade);
            historico.registrar(EntidadeHistorico.COTACAO, cotacao.getId(), AcaoHistorico.COTACAO_ITEM_ADICIONADO, detalhe);
            log.info("Produto {} incluído no rascunho de reposição {}", produto.getSku(), cotacao.getCodigo());
            return Optional.of(cotacaoRepository.save(cotacao));
        }

        Cotacao cotacao = new Cotacao();
        cotacao.setCodigo(Cotacao.gerarCodigo(Year.now(clock).getValue(), cotacaoRepository.proximoNumeroCodigo()));
        cotacao.setStatus(StatusCotacao.RASCUNHO);
        cotacao.setOrigem(OrigemCotacao.AUTOMATICA);
        cotacao.setCriadoPor(usuarioAtual.username());
        cotacao.setObservacao(mensagens.obter("cotacao.observacao.reposicao.automatica"));
        cotacao.adicionarItem(produto, quantidade);
        Cotacao salva = cotacaoRepository.save(cotacao);
        historico.registrar(EntidadeHistorico.COTACAO, salva.getId(), AcaoHistorico.COTACAO_CRIADA, detalhe);
        metricas.cotacaoCriada(OrigemCotacao.AUTOMATICA.name());
        log.info("Cotação {} de reposição criada para o produto {}", salva.getCodigo(), produto.getSku());
        return Optional.of(salva);
    }

    /**
     * Abaixo do mínimo: repõe o que falta e mais um estoque mínimo de folga, (mínimo - saldo) + mínimo.
     * Exatamente no mínimo: pede um estoque mínimo. Nunca sugere zero.
     */
    static BigDecimal quantidadeSugerida(BigDecimal saldoAtual, BigDecimal estoqueMinimo) {
        BigDecimal quantidade = saldoAtual.compareTo(estoqueMinimo) < 0
                ? estoqueMinimo.subtract(saldoAtual).add(estoqueMinimo)
                : estoqueMinimo;
        return quantidade.signum() > 0 ? quantidade : BigDecimal.ONE;
    }

    private static String descreverAlteracoes(Produto atual, AtualizarProdutoRequest novo) {
        StringBuilder sb = new StringBuilder();
        if (!atual.getNome().equals(novo.nome().trim())) {
            sb.append("nome: ").append(atual.getNome()).append(" -> ").append(novo.nome().trim()).append("; ");
        }
        if (atual.getUnidade() != novo.unidade()) {
            sb.append("unidade: ").append(atual.getUnidade()).append(" -> ").append(novo.unidade()).append("; ");
        }
        if (atual.getEstoqueMinimo().compareTo(novo.estoqueMinimo()) != 0) {
            sb.append("estoqueMinimo: ").append(atual.getEstoqueMinimo().stripTrailingZeros().toPlainString())
                    .append(" -> ").append(novo.estoqueMinimo().stripTrailingZeros().toPlainString());
        }
        return sb.toString().replaceAll("; $", "");
    }

    private Produto buscarEntidade(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("produto.nao.encontrado", id));
    }

    private static void exigirQuantidadePositiva(MovimentarEstoqueRequest request) {
        if (request.quantidade().signum() <= 0) {
            throw new NegocioException("estoque.quantidade.positiva", request.tipo());
        }
    }
}
