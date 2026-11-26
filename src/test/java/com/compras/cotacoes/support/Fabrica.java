package com.compras.cotacoes.support;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.cotacao.OrigemCotacao;
import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.produto.UnidadeMedida;
import com.compras.cotacoes.proposta.Proposta;
import com.compras.cotacoes.proposta.StatusProposta;
import com.compras.cotacoes.shared.i18n.MensagemService;
import com.compras.cotacoes.shared.metricas.MetricasCompras;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.context.support.ResourceBundleMessageSource;

/** Objetos de domínio prontos para os testes. */
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

    public static MetricasCompras metricas() {
        return new MetricasCompras(new SimpleMeterRegistry());
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
        fornecedor.setDocumento(cnpj(id));
        fornecedor.setEmail("f" + id + "@teste.exemplo");
        fornecedor.setPrazoMedioDias(5);
        fornecedor.setAtivo(ativo);
        return fornecedor;
    }

    /** Cotação ABERTA com um item do produto informado. */
    public static Cotacao cotacaoAberta(long id, Produto produto, String quantidade) {
        Cotacao cotacao = new Cotacao();
        cotacao.setId(id);
        cotacao.setCodigo(Cotacao.gerarCodigo(2026, id));
        cotacao.setStatus(StatusCotacao.ABERTA);
        cotacao.setOrigem(OrigemCotacao.MANUAL);
        cotacao.setCriadoPor("teste");
        cotacao.adicionarItem(produto, new BigDecimal(quantidade)).setId(id * 100);
        return cotacao;
    }

    /** Proposta já calculada: só o que importa para o ranking (total, prazo, validade). */
    public static Proposta proposta(long id, Fornecedor fornecedor, String valorTotal, int prazoDias, LocalDate validade) {
        Proposta proposta = new Proposta();
        proposta.setId(id);
        proposta.setFornecedor(fornecedor);
        proposta.setValorMercadoria(new BigDecimal(valorTotal));
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

    /** CNPJ válido (dígitos verificadores corretos) gerado a partir de um número qualquer. */
    public static String cnpj(long semente) {
        String base = String.format("%012d", Math.floorMod(semente * 7919L + 11L, 1_000_000_000_000L));
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        String com1 = base + digito(base, pesos1);
        return com1 + digito(com1, pesos2);
    }

    private static int digito(String numeros, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (numeros.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
