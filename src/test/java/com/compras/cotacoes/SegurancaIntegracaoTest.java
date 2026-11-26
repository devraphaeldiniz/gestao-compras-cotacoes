package com.compras.cotacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Regressão dos achados do teste de vulnerabilidade (docs/relatorio-seguranca.md).
 * Os testes de concorrência disparam requisições em paralelo, em threads reais, contra o PostgreSQL.
 */
class SegurancaIntegracaoTest extends IntegracaoBase {

    @Test
    @DisplayName("SEC-01: 20 saídas simultâneas não perdem atualização de saldo")
    void saidasConcorrentesNaoPerdemAtualizacao() throws Exception {
        long produtoId = criarProduto("SEC-SAIDA", "40", "5");

        List<Integer> status = emParalelo(20, () -> saida(produtoId));

        assertThat(status).containsOnly(200);
        assertThat(saldo(produtoId)).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("SEC-01: saídas simultâneas abaixo do mínimo geram uma única entrada de reposição")
    void reposicaoNaoDuplicaSobConcorrencia() throws Exception {
        long produtoId = criarProduto("SEC-REPO", "12", "10");

        emParalelo(12, () -> saida(produtoId));

        String cotacoes = mockMvc.perform(get("/api/cotacoes?produtoId={id}", produtoId).with(comprador()))
                .andReturn().getResponse().getContentAsString();
        List<Object> emAndamento = JsonPath.read(cotacoes, "$.conteudo[?(@.status == 'RASCUNHO' || @.status == 'ABERTA')]");
        assertThat(emAndamento).hasSize(1);
        assertThat(saldo(produtoId)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("SEC-02: o mesmo pedido recebido 10 vezes em paralelo só é aceito uma vez")
    void recebimentoConcorrenteSoUmaVez() throws Exception {
        long produtoId = criarProduto("SEC-PED", "100", "1");
        long pedidoId = pedidoEmitido(produtoId);
        BigDecimal saldoAntes = saldo(produtoId);

        List<Integer> status = emParalelo(10, () -> mockMvc.perform(
                post("/api/pedidos-compra/{id}/receber", pedidoId).with(aprovador())).andReturn().getResponse().getStatus());

        assertThat(status).filteredOn(s -> s == 200).hasSize(1);
        assertThat(status).filteredOn(s -> s == 422).hasSize(9);
        assertThat(saldo(produtoId)).isEqualByComparingTo(saldoAntes.add(new BigDecimal("15")));
    }

    @Test
    @DisplayName("SEC-03: método e content-type errados retornam 405 e 415, não 500")
    void metodoEMidiaInvalidos() throws Exception {
        mockMvc.perform(delete("/api/produtos").with(comprador()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.mensagem").value(containsString("DELETE")));
        mockMvc.perform(get("/api/produtos/1/estoque").with(comprador())).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/produtos").with(comprador()).contentType(MediaType.TEXT_PLAIN).content("oi"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("SEC-04: valores acima da precisão das colunas NUMERIC são recusados com 400")
    void limitesNumericos() throws Exception {
        mockMvc.perform(json(post("/api/produtos"),
                        "{\"sku\":\"OVF\",\"nome\":\"x\",\"unidade\":\"UN\",\"saldoAtual\":1e20,\"estoqueMinimo\":1}").with(comprador()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("saldoAtual"));
        mockMvc.perform(json(post("/api/produtos"),
                        "{\"sku\":\"FRAC\",\"nome\":\"x\",\"unidade\":\"UN\",\"saldoAtual\":1.123456,\"estoqueMinimo\":1}").with(comprador()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SEC-05: respostas trazem os cabeçalhos de segurança e a API não é cacheável")
    void cabecalhosDeSeguranca() throws Exception {
        mockMvc.perform(get("/api/produtos").with(comprador()))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cross-Origin-Resource-Policy", "same-origin"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")));
    }

    @Test
    @DisplayName("SEC-06: corpo acima de 1 MB é recusado com 413 antes de chegar ao controller")
    void corpoMuitoGrande() throws Exception {
        String grande = "{\"sku\":\"BIG\",\"nome\":\"" + "A".repeat(1_100_000) + "\",\"unidade\":\"UN\",\"saldoAtual\":1,\"estoqueMinimo\":1}";
        mockMvc.perform(json(post("/api/produtos"), grande).with(comprador()))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    @DisplayName("SEC-07: actuator público só no health; métricas exigem ADMIN")
    void actuatorRestrito() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus").with(comprador())).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/prometheus").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content -> assertThat(content.getResponse().getContentAsString()).contains("compras_cotacoes_criadas"));
        mockMvc.perform(get("/actuator/env").with(admin())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SEC-08: X-Request-Id do cliente é reaproveitado só se tiver formato seguro")
    void idDeCorrelacao() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "pedido-abc-12345"))
                .andExpect(header().string("X-Request-Id", "pedido-abc-12345"));
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "x\r\nINJETADO"))
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.not(containsString("INJETADO"))));
    }

    // ------------------------------------------------------------------ utilitários

    private int saida(long produtoId) throws Exception {
        return mockMvc.perform(json(patch("/api/produtos/{id}/estoque", produtoId), "{\"tipo\":\"SAIDA\",\"quantidade\":1}")
                .with(comprador())).andReturn().getResponse().getStatus();
    }

    /** Cotação de 15 unidades com 3 propostas, decidida: devolve o id do pedido EMITIDO. */
    private long pedidoEmitido(long produtoId) throws Exception {
        long cotacao = criarCotacaoAberta(produtoId, 15);
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(json(post("/api/cotacoes/{id}/propostas", cotacao),
                            proposta(criarFornecedor("Sec F" + proximo()), List.of(produtoId), String.valueOf(10 + i), 3, "0"))
                            .with(comprador()))
                    .andExpect(status().isCreated());
        }
        String decisao = mockMvc.perform(json(post("/api/cotacoes/{id}/decidir", cotacao), "{}").with(aprovador()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(decisao, "$.pedidoCompra.id")).longValue();
    }

    /** Dispara as tarefas ao mesmo tempo (todas esperam a largada) e devolve os resultados. */
    private static <T> List<T> emParalelo(int vezes, Callable<T> tarefa) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(vezes);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<T>> futuros = new ArrayList<>();
            for (int i = 0; i < vezes; i++) {
                futuros.add(executor.submit(() -> {
                    largada.await();
                    return tarefa.call();
                }));
            }
            largada.countDown();
            List<T> resultados = new ArrayList<>();
            for (Future<T> futuro : futuros) {
                resultados.add(futuro.get());
            }
            return resultados;
        } finally {
            executor.shutdownNow();
        }
    }
}
