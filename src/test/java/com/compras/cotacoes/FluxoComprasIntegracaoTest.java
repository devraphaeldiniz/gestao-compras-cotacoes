package com.compras.cotacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Ciclo completo contra um PostgreSQL 16 real (Testcontainers), com as migrações Flyway de verdade
 * e só via HTTP: estoque mínimo -> cotação -> propostas -> decisão -> pedido -> recebimento.
 * Usa apenas db/migration (sem os dados de demonstração de db/seed).
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration")
class FluxoComprasIntegracaoTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final LocalDate HOJE = LocalDate.now();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Fluxo completo: estoque mínimo, cotação, 3 propostas, decisão, pedido e recebimento")
    void cicloCompletoDeCompra() throws Exception {
        // 1. Fornecedores: 3 ativos e 1 inativado após o cadastro
        long alfa = criarFornecedor("Alfa Suprimentos Ltda", "10.000.000/0001-01", 5);
        long beta = criarFornecedor("Beta Distribuidora S.A.", "20000000000102", 2);
        long gama = criarFornecedor("Gama Atacado ME", "30000000000103", 10);
        long omega = criarFornecedor("Omega Comercial Eireli", "40000000000104", 1);
        enviar(patch("/api/fornecedores/{id}/status", omega), Map.of("ativo", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        String ativos = mockMvc.perform(get("/api/fornecedores?ativo=true")).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Object>>read(ativos, "$[?(@.id == " + omega + ")]")).isEmpty();

        // 2. Produto acima do mínimo não gera cotação; a baixa até 15 (mínimo 20) gera uma RASCUNHO
        long produtoId = id(enviar(post("/api/produtos"), Map.of(
                "sku", "pap-a4-int", "nome", "Papel A4 integração", "unidade", "CX",
                "saldoAtual", 50, "estoqueMinimo", 20))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("PAP-A4-INT"))
                .andExpect(jsonPath("$.necessitaCompra").value(false)));
        assertThat(cotacoesDoProduto(produtoId)).isEmpty();

        enviar(patch("/api/produtos/{id}/estoque", produtoId), Map.of("tipo", "SAIDA", "quantidade", 35))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.necessitaCompra").value(true));

        List<Map<String, Object>> cotacoes = cotacoesDoProduto(produtoId);
        assertThat(cotacoes).hasSize(1);
        Map<String, Object> rascunho = cotacoes.getFirst();
        assertThat(rascunho).containsEntry("status", "RASCUNHO");
        assertThat(rascunho.get("codigo").toString()).matches("COT-\\d{4}-\\d{5}");
        assertThat(decimal(rascunho.get("quantidade"))).isEqualByComparingTo("25"); // (20 - 15) + 20
        long cotacaoId = ((Number) rascunho.get("id")).longValue();

        // Nova baixa não duplica a cotação em andamento
        enviar(patch("/api/produtos/{id}/estoque", produtoId), Map.of("tipo", "SAIDA", "quantidade", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoAtual").value(10.0));
        assertThat(cotacoesDoProduto(produtoId)).hasSize(1);

        // Proposta em cotação RASCUNHO é recusada
        registrarProposta(cotacaoId, alfa, "24.90", "35.00", "12", "0", 5)
                .andExpect(status().isUnprocessableEntity());

        // 3. Abre a cotação convidando os 3 fornecedores ativos
        enviar(patch("/api/cotacoes/{id}/abrir", cotacaoId), Map.of(
                "fornecedoresIds", List.of(alfa, beta, gama), "dataLimite", HOJE.plusDays(7).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.fornecedoresConvidados.length()").value(3))
                .andExpect(jsonPath("$.podeDecidir").value(false));

        // Fornecedor inativo não pode enviar proposta
        registrarProposta(cotacaoId, omega, "20.00", "0", "0", "0", 1)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("inativo")));

        // 4. Duas propostas: a decisão ainda é bloqueada com 422
        //    Alfa: 25 x 24,90 = 622,50 + frete 35,00 + 12% (74,70) = 732,20
        long propostaAlfa = id(registrarProposta(cotacaoId, alfa, "24.90", "35.00", "12", "0", 5)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorMercadoria").value(622.50))
                .andExpect(jsonPath("$.valorImpostoCalculado").value(74.70))
                .andExpect(jsonPath("$.valorTotal").value(732.20)));
        //    Beta: 587,50 + frete 80,00 + (70,50 + fixo 10,00) = 748,00
        registrarProposta(cotacaoId, beta, "23.50", "80.00", "12", "10.00", 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(748.00));

        // Mesmo fornecedor não envia duas propostas para a mesma cotação
        registrarProposta(cotacaoId, beta, "23.00", "80.00", "12", "10.00", 2)
                .andExpect(status().isConflict());

        enviar(post("/api/cotacoes/{id}/decidir", cotacaoId), Map.of("criterio", "MENOR_PRECO"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.mensagem").value(containsString("no mínimo 3 propostas válidas")));

        // 5. Terceira proposta. Gama: 572,50 + frete 120,00 + 68,70 = 761,20
        registrarProposta(cotacaoId, gama, "22.90", "120.00", "12", "0", 10)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(761.20));

        mockMvc.perform(get("/api/cotacoes/{id}", cotacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propostas.length()").value(3))
                .andExpect(jsonPath("$.propostasElegiveis").value(3))
                .andExpect(jsonPath("$.podeDecidir").value(true))
                .andExpect(jsonPath("$.recomendacaoMenorPreco.propostaRecomendadaId").value(propostaAlfa))
                .andExpect(jsonPath("$.recomendacaoPontuacaoPonderada.ranking.length()").value(3));

        String decisao = enviar(post("/api/cotacoes/{id}/decidir", cotacaoId), Map.of("criterio", "MENOR_PRECO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCotacao").value("FECHADA"))
                .andExpect(jsonPath("$.propostaVencedora.id").value(propostaAlfa))
                .andExpect(jsonPath("$.propostaVencedora.status").value("VENCEDORA"))
                .andExpect(jsonPath("$.escolhaManual").value(false))
                .andExpect(jsonPath("$.pedidoCompra.status").value("EMITIDO"))
                .andExpect(jsonPath("$.pedidoCompra.valorTotal").value(732.20))
                .andExpect(jsonPath("$.pedidoCompra.fornecedorId").value(alfa))
                .andReturn().getResponse().getContentAsString();
        long pedidoId = ((Number) JsonPath.read(decisao, "$.pedidoCompra.id")).longValue();
        assertThat(JsonPath.<String>read(decisao, "$.pedidoCompra.numeroPedido")).matches("PC-\\d{4}-\\d{5}");

        mockMvc.perform(get("/api/cotacoes/{id}", cotacaoId))
                .andExpect(jsonPath("$.status").value("FECHADA"))
                .andExpect(jsonPath("$.propostaVencedoraId").value(propostaAlfa))
                .andExpect(jsonPath("$.propostas[?(@.status == 'REJEITADA')]", hasSize(2)));

        // 6. Recebimento: saldo 10 + 25 = 35; custo efetivo = 732,20 / 25 = 29,2880
        mockMvc.perform(post("/api/pedidos-compra/{id}/receber", pedidoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEBIDO"))
                .andExpect(jsonPath("$.dataRecebimento").isNotEmpty());

        String produtos = mockMvc.perform(get("/api/produtos")).andReturn().getResponse().getContentAsString();
        Map<String, Object> produto = JsonPath.<List<Map<String, Object>>>read(
                produtos, "$[?(@.id == " + produtoId + ")]").getFirst();
        assertThat(decimal(produto.get("saldoAtual"))).isEqualByComparingTo("35");
        assertThat(decimal(produto.get("ultimoPrecoCompra"))).isEqualByComparingTo("29.2880");
        assertThat(decimal(produto.get("precoMedioCompra"))).isEqualByComparingTo("29.2880");
        assertThat(produto).containsEntry("necessitaCompra", false);

        // Não é possível receber o mesmo pedido duas vezes
        mockMvc.perform(post("/api/pedidos-compra/{id}/receber", pedidoId))
                .andExpect(status().isUnprocessableEntity());

        // 7. Mensagens de erro localizadas pelo Accept-Language
        mockMvc.perform(post("/api/cotacoes/{id}/decidir", cotacaoId)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("is not OPEN")));

        mockMvc.perform(post("/api/cotacoes/{id}/decidir", cotacaoId)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("não está ABERTA")));
    }

    @Test
    @DisplayName("Erros de validação e de recurso inexistente saem no idioma pedido (header ou ?lang=)")
    void mensagensLocalizadas() throws Exception {
        String invalido = "{\"sku\":\"\",\"nome\":\"x\",\"unidade\":\"UN\",\"saldoAtual\":-1,\"estoqueMinimo\":1}";

        mockMvc.perform(post("/api/produtos").header(HttpHeaders.ACCEPT_LANGUAGE, "en-US")
                        .contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("The request contains invalid fields"))
                .andExpect(jsonPath("$.campos[?(@.campo == 'sku')].mensagem").value("is required"))
                .andExpect(jsonPath("$.campos[?(@.campo == 'saldoAtual')].mensagem").value("must not be negative"));

        mockMvc.perform(post("/api/produtos").header(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR")
                        .contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Há campos inválidos na requisição"))
                .andExpect(jsonPath("$.campos[?(@.campo == 'sku')].mensagem").value("é obrigatório"));

        // ?lang= tem prioridade sobre o header
        mockMvc.perform(get("/api/cotacoes/{id}?lang=en", 987654).header(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Quotation 987654 not found"));

        // Sem header: português é o padrão
        mockMvc.perform(get("/api/cotacoes/{id}", 987654))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cotação 987654 não encontrada"));
    }

    @Test
    @DisplayName("SKU e documento duplicados retornam 409; produto criado já no mínimo gera cotação na hora")
    void conflitosERegraANoCadastro() throws Exception {
        Map<String, Object> produto = Map.of("sku", "TON-DUP", "nome", "Toner", "unidade", "UN",
                "saldoAtual", 2, "estoqueMinimo", 5);
        long produtoId = id(enviar(post("/api/produtos"), produto)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.necessitaCompra").value(true)));
        enviar(post("/api/produtos"), produto)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("TON-DUP")));

        List<Map<String, Object>> cotacoes = cotacoesDoProduto(produtoId);
        assertThat(cotacoes).hasSize(1);
        assertThat(decimal(cotacoes.getFirst().get("quantidade"))).isEqualByComparingTo("8"); // (5 - 2) + 5

        criarFornecedor("Duplicado Ltda", "55555555000155", 3);
        enviar(post("/api/fornecedores"), Map.of("razaoSocial", "Outro", "documento", "55.555.555/0001-55",
                "email", "outro@teste.exemplo", "prazoMedioDias", 1))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ utilitários

    private long criarFornecedor(String razaoSocial, String documento, int prazo) throws Exception {
        String email = razaoSocial.split(" ")[0].toLowerCase() + "@teste.exemplo";
        return id(enviar(post("/api/fornecedores"), Map.of(
                "razaoSocial", razaoSocial, "documento", documento, "email", email, "prazoMedioDias", prazo))
                .andExpect(status().isCreated()));
    }

    private ResultActions registrarProposta(long cotacaoId, long fornecedorId, String preco, String frete,
                                            String percentual, String fixo, int prazo) throws Exception {
        return enviar(post("/api/cotacoes/{id}/propostas", cotacaoId), Map.of(
                "fornecedorId", fornecedorId,
                "precoUnitario", new BigDecimal(preco),
                "valorFrete", new BigDecimal(frete),
                "impostoPercentual", new BigDecimal(percentual),
                "valorImpostoFixo", new BigDecimal(fixo),
                "prazoEntregaDias", prazo,
                "dataValidade", HOJE.plusDays(30).toString()));
    }

    private List<Map<String, Object>> cotacoesDoProduto(long produtoId) throws Exception {
        String json = mockMvc.perform(get("/api/cotacoes")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[?(@.produtoId == " + produtoId + ")]");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder requisicao, Object corpo) throws Exception {
        return mockMvc.perform(requisicao
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));
    }

    private static long id(ResultActions resultado) throws Exception {
        String json = resultado.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private static BigDecimal decimal(Object valor) {
        return new BigDecimal(valor.toString());
    }
}
