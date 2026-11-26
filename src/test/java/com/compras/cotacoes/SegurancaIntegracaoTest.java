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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Regressão dos achados do teste de vulnerabilidade (docs/relatorio-seguranca.md).
 * Os testes de concorrência disparam requisições em paralelo, em threads reais, contra o PostgreSQL.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration")
class SegurancaIntegracaoTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("SEC-01: 20 saídas simultâneas não perdem atualização de saldo")
    void saidasConcorrentesNaoPerdemAtualizacao() throws Exception {
        long produtoId = criarProduto("CONC-SAIDA", 40, 5);

        List<Integer> status = emParalelo(20, () -> saida(produtoId));

        assertThat(status).containsOnly(200);
        assertThat(saldo(produtoId)).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("SEC-01: saídas simultâneas abaixo do mínimo geram uma única cotação de reposição")
    void reposicaoNaoDuplicaSobConcorrencia() throws Exception {
        long produtoId = criarProduto("CONC-REPO", 12, 10);

        emParalelo(12, () -> saida(produtoId));

        String cotacoes = mockMvc.perform(get("/api/cotacoes")).andReturn().getResponse().getContentAsString();
        List<Object> emAndamento = JsonPath.read(cotacoes,
                "$[?(@.produtoId == " + produtoId + " && (@.status == 'RASCUNHO' || @.status == 'ABERTA'))]");
        assertThat(emAndamento).hasSize(1);
        assertThat(saldo(produtoId)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("SEC-02: o mesmo pedido recebido 10 vezes em paralelo só é aceito uma vez")
    void recebimentoConcorrenteSoUmaVez() throws Exception {
        PedidoEmitido pedido = pedidoEmitido("CONC-PED");
        BigDecimal saldoAntes = saldo(pedido.produtoId());

        List<Integer> status = emParalelo(10, () -> mockMvc.perform(
                post("/api/pedidos-compra/{id}/receber", pedido.id())).andReturn().getResponse().getStatus());

        assertThat(status).filteredOn(s -> s == 200).hasSize(1);
        assertThat(status).filteredOn(s -> s == 422).hasSize(9);
        assertThat(saldo(pedido.produtoId())).isEqualByComparingTo(saldoAntes.add(new BigDecimal("15")));
    }

    @Test
    @DisplayName("SEC-03: método e content-type errados retornam 405 e 415, não 500")
    void metodoEMidiaInvalidos() throws Exception {
        mockMvc.perform(delete("/api/produtos"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.mensagem").value(containsString("DELETE")));
        mockMvc.perform(get("/api/produtos/1/estoque")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/produtos").contentType(MediaType.TEXT_PLAIN).content("oi"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("SEC-04: valores acima da precisão das colunas NUMERIC são recusados com 400")
    void limitesNumericos() throws Exception {
        mockMvc.perform(json(post("/api/produtos"),
                        "{\"sku\":\"OVF\",\"nome\":\"x\",\"unidade\":\"UN\",\"saldoAtual\":1e20,\"estoqueMinimo\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("saldoAtual"));
        mockMvc.perform(json(post("/api/produtos"),
                        "{\"sku\":\"FRAC\",\"nome\":\"x\",\"unidade\":\"UN\",\"saldoAtual\":1.123456,\"estoqueMinimo\":1}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post("/api/fornecedores"),
                        "{\"razaoSocial\":\"x\",\"documento\":\"12312312000112\",\"email\":\"a@b.co\",\"prazoMedioDias\":99999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SEC-05: respostas trazem os cabeçalhos de segurança e a API não é cacheável")
    void cabecalhosDeSeguranca() throws Exception {
        mockMvc.perform(get("/api/produtos"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cross-Origin-Resource-Policy", "same-origin"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")));
    }

    @Test
    @DisplayName("SEC-06: corpo acima de 1 MB é recusado com 413 antes de chegar ao controller")
    void corpoMuitoGrande() throws Exception {
        String grande = "{\"sku\":\"BIG\",\"nome\":\"" + "A".repeat(1_100_000)
                + "\",\"unidade\":\"UN\",\"saldoAtual\":1,\"estoqueMinimo\":1}";
        mockMvc.perform(json(post("/api/produtos"), grande))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    @DisplayName("SEC-07: actuator expõe só o health, sem detalhes")
    void actuatorRestrito() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ utilitários

    private record PedidoEmitido(long id, long produtoId) {
    }

    private int saida(long produtoId) throws Exception {
        return mockMvc.perform(json(patch("/api/produtos/{id}/estoque", produtoId),
                "{\"tipo\":\"SAIDA\",\"quantidade\":1}")).andReturn().getResponse().getStatus();
    }

    private long criarProduto(String sku, int saldo, int minimo) throws Exception {
        String corpo = "{\"sku\":\"%s\",\"nome\":\"Teste\",\"unidade\":\"UN\",\"saldoAtual\":%d,\"estoqueMinimo\":%d}"
                .formatted(sku, saldo, minimo);
        return id(mockMvc.perform(json(post("/api/produtos"), corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    /** Fluxo mínimo até um pedido EMITIDO: saldo 5 e mínimo 10 geram cotação de (10 - 5) + 10 = 15 unidades. */
    private PedidoEmitido pedidoEmitido(String sku) throws Exception {
        long produtoId = criarProduto(sku, 5, 10);
        String rascunhos = mockMvc.perform(get("/api/cotacoes?status=RASCUNHO")).andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(rascunhos, "$[?(@.produtoId == " + produtoId + ")].id");
        long cotacaoId = ids.getFirst().longValue();
        mockMvc.perform(json(patch("/api/cotacoes/{id}/abrir", cotacaoId), "{}")).andExpect(status().isOk());

        String validade = LocalDate.now().plusDays(10).toString();
        for (int i = 1; i <= 3; i++) {
            String fornecedor = "{\"razaoSocial\":\"F%d %s\",\"documento\":\"%014d\",\"email\":\"f%d@t.co\",\"prazoMedioDias\":%d}"
                    .formatted(i, sku, 90_000_000_000_000L + sku.hashCode() % 1000 * 10 + i, i, i);
            long fornecedorId = id(mockMvc.perform(json(post("/api/fornecedores"), fornecedor))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
            String proposta = "{\"fornecedorId\":%d,\"precoUnitario\":%d,\"prazoEntregaDias\":%d,\"dataValidade\":\"%s\"}"
                    .formatted(fornecedorId, 10 + i, i, validade);
            mockMvc.perform(json(post("/api/cotacoes/{id}/propostas", cotacaoId), proposta))
                    .andExpect(status().isCreated());
        }

        String decisao = mockMvc.perform(json(post("/api/cotacoes/{id}/decidir", cotacaoId), "{}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new PedidoEmitido(((Number) JsonPath.read(decisao, "$.pedidoCompra.id")).longValue(), produtoId);
    }

    private BigDecimal saldo(long produtoId) throws Exception {
        String produtos = mockMvc.perform(get("/api/produtos")).andReturn().getResponse().getContentAsString();
        List<Object> saldos = JsonPath.read(produtos, "$[?(@.id == " + produtoId + ")].saldoAtual");
        return new BigDecimal(saldos.getFirst().toString());
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

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, String corpo) {
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
