package com.compras.cotacoes.support;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.produto.UnidadeMedida;
import com.compras.cotacoes.proposta.Proposta;
import com.compras.cotacoes.proposta.StatusProposta;
import com.compras.cotacoes.shared.i18n.MensagemService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.context.support.ResourceBundleMessageSource;

/** Objetos de domínio prontos para os testes unitários. */
public final class Fabrica {

    /** "Hoje" nos testes unitários: 26/11/2026. */
    public static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC);
    public static final LocalDate HOJE = LocalDate.now(RELOGIO);

    private Fabrica() {
    }

    public static MensagemService mensagens() {
        ResourceBundleMessageSource fonte = new ResourceBundleMessageSource();
        fonte.setBasename("messages");
        fonte.setDefaultEncoding("UTF-8");
        fonte.setFallbackToSystemLocale(false);
        return new MensagemService(fonte);
    }

    public static Produto produto(long id, String sku, String saldo, String minimo) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setSku(sku);
        produto.setNome("Produto " + sku);
        produto.setUnidade(UnidadeMedida.UN);
        produto.setSaldoAtual(new BigDecimal(saldo));
        produto.setEstoqueMinimo(new BigDecimal(minimo));
        produto.setAtivo(true);
        return produto;
    }

    public static Fornecedor fornecedor(long id, String razaoSocial, boolean ativo) {
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setId(id);
        fornecedor.setRazaoSocial(razaoSocial);
        fornecedor.setDocumento(String.format("%014d", id));
        fornecedor.setEmail("f" + id + "@teste.exemplo");
        fornecedor.setPrazoMedioDias(5);
        fornecedor.setAtivo(ativo);
        return fornecedor;
    }

    public static Cotacao cotacaoAberta(long id, Produto produto, String quantidade) {
        Cotacao cotacao = new Cotacao();
        cotacao.setId(id);
        cotacao.setCodigo(Cotacao.gerarCodigo(2026, id));
        cotacao.setProduto(produto);
        cotacao.setQuantidade(new BigDecimal(quantidade));
        cotacao.setStatus(StatusCotacao.ABERTA);
        return cotacao;
    }

    /** Proposta já calculada: só o que importa para o ranking (total, prazo, validade). */
    public static Proposta proposta(long id, Fornecedor fornecedor, String valorTotal, int prazoDias, LocalDate validade) {
        Proposta proposta = new Proposta();
        proposta.setId(id);
        proposta.setFornecedor(fornecedor);
        proposta.setPrecoUnitario(new BigDecimal(valorTotal).divide(BigDecimal.TEN));
        proposta.setValorFrete(BigDecimal.ZERO);
        proposta.setImpostoPercentual(BigDecimal.ZERO);
        proposta.setValorImpostoFixo(BigDecimal.ZERO);
        proposta.setValorImpostoCalculado(BigDecimal.ZERO);
        proposta.setValorTotal(new BigDecimal(valorTotal));
        proposta.setPrazoEntregaDias(prazoDias);
        proposta.setDataValidade(validade);
        proposta.setStatus(StatusProposta.PENDENTE);
        return proposta;
    }

    public static Proposta proposta(long id, Fornecedor fornecedor, String valorTotal, int prazoDias) {
        return proposta(id, fornecedor, valorTotal, prazoDias, HOJE.plusDays(30));
    }
}
