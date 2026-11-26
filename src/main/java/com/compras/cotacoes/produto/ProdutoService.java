package com.compras.cotacoes.produto;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.cotacao.CotacaoRepository;
import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.produto.dto.CriarProdutoRequest;
import com.compras.cotacoes.produto.dto.MovimentarEstoqueRequest;
import com.compras.cotacoes.produto.dto.ProdutoResponse;
import com.compras.cotacoes.shared.exception.ConflitoException;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import com.compras.cotacoes.shared.i18n.MensagemService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdutoService {

    static final List<StatusCotacao> STATUS_EM_ANDAMENTO = List.of(StatusCotacao.RASCUNHO, StatusCotacao.ABERTA);

    private final ProdutoRepository produtoRepository;
    private final CotacaoRepository cotacaoRepository;
    private final ProdutoMapper produtoMapper;
    private final MensagemService mensagens;
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

        Produto salvo = produtoRepository.save(produto);
        verificarNecessidadeCompra(salvo);
        return produtoMapper.paraResposta(salvo);
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar(boolean apenasNecessidadeCompra) {
        List<Produto> produtos = apenasNecessidadeCompra
                ? produtoRepository.buscarComNecessidadeCompra()
                : produtoRepository.findAllByOrderByNomeAsc();
        return produtoMapper.paraRespostas(produtos);
    }

    @Transactional
    public ProdutoResponse movimentarEstoque(Long produtoId, MovimentarEstoqueRequest request) {
        Produto produto = produtoRepository.buscarComBloqueio(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("produto.nao.encontrado", produtoId));
        BigDecimal quantidade = request.quantidade();
        BigDecimal saldo = produto.getSaldoAtual();

        switch (request.tipo()) {
            case ENTRADA -> {
                exigirQuantidadePositiva(request);
                produto.setSaldoAtual(saldo.add(quantidade));
            }
            case SAIDA -> {
                exigirQuantidadePositiva(request);
                if (quantidade.compareTo(saldo) > 0) {
                    throw new NegocioException("estoque.saldo.insuficiente", saldo, quantidade);
                }
                produto.setSaldoAtual(saldo.subtract(quantidade));
            }
            case AJUSTE -> produto.setSaldoAtual(quantidade);
        }

        Produto salvo = produtoRepository.save(produto);
        if (request.tipo() != TipoMovimentacaoEstoque.ENTRADA) {
            verificarNecessidadeCompra(salvo);
        }
        return produtoMapper.paraResposta(salvo);
    }

    /**
     * Regra A: se o saldo chegou ao estoque mínimo e não há cotação RASCUNHO/ABERTA para o produto,
     * gera uma cotação RASCUNHO de reposição. Nunca duplica uma cotação em andamento.
     */
    Optional<Cotacao> verificarNecessidadeCompra(Produto produto) {
        if (!produto.isAtivo() || !produto.precisaReposicao()) {
            return Optional.empty();
        }
        Optional<Cotacao> emAndamento = cotacaoRepository
                .findFirstByProdutoIdAndStatusInOrderByIdDesc(produto.getId(), STATUS_EM_ANDAMENTO);
        if (emAndamento.isPresent()) {
            log.debug("Produto {} já possui a cotação {} em andamento", produto.getSku(), emAndamento.get().getCodigo());
            return Optional.empty();
        }

        Cotacao cotacao = new Cotacao();
        cotacao.setCodigo(Cotacao.gerarCodigo(Year.now(clock).getValue(), cotacaoRepository.proximoNumeroCodigo()));
        cotacao.setProduto(produto);
        cotacao.setQuantidade(quantidadeSugerida(produto.getSaldoAtual(), produto.getEstoqueMinimo()));
        cotacao.setStatus(StatusCotacao.RASCUNHO);
        cotacao.setObservacao(mensagens.obter("cotacao.observacao.reposicao",
                produto.getSaldoAtual(), produto.getEstoqueMinimo(), produto.getSku()));

        Cotacao salva = cotacaoRepository.save(cotacao);
        log.info("Cotação {} gerada automaticamente para o produto {}", salva.getCodigo(), produto.getSku());
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

    private static void exigirQuantidadePositiva(MovimentarEstoqueRequest request) {
        if (request.quantidade().signum() <= 0) {
            throw new NegocioException("estoque.quantidade.positiva", request.tipo());
        }
    }
}
