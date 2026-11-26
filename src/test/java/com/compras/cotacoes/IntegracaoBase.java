package com.compras.cotacoes;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.compras.cotacoes.support.Fabrica;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integração: um único PostgreSQL 16 (Testcontainers) e um único contexto Spring para todas
 * as classes. Só as migrações de db/migration (sem dados de demonstração), job de prazos e e-mail desligados.
 */
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration",
        "app.prazos.habilitado=false",
        "app.email.habilitado=false"})
abstract class IntegracaoBase {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    private static final AtomicLong SEQUENCIA = new AtomicLong(1000);

    @Autowired
    protected MockMvc mockMvc;

    // ------------------------------------------------------------------ autenticação (JWT simulado por perfil)

    protected static RequestPostProcessor comprador() {
        return comoPerfil("comprador", "COMPRADOR");
    }

    protected static RequestPostProcessor aprovador() {
        return comoPerfil("aprovador", "APROVADOR");
    }

    protected static RequestPostProcessor admin() {
        return comoPerfil("admin", "ADMIN");
    }

    private static RequestPostProcessor comoPerfil(String usuario, String perfil) {
        return jwt().jwt(j -> j.subject(usuario).claim("perfil", perfil))
                .authorities(new SimpleGrantedAuthority("ROLE_" + perfil));
    }

    // ------------------------------------------------------------------ atalhos de cenário

    protected static long proximo() {
        return SEQUENCIA.incrementAndGet();
    }

    protected long criarProduto(String sku, String saldo, String minimo) throws Exception {
        String corpo = "{\"sku\":\"%s\",\"nome\":\"Produto %s\",\"unidade\":\"UN\",\"saldoAtual\":%s,\"estoqueMinimo\":%s}"
                .formatted(sku, sku, saldo, minimo);
        return id(mockMvc.perform(json(post("/api/produtos"), corpo).with(comprador()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    protected long criarFornecedor(String nome) throws Exception {
        String corpo = "{\"razaoSocial\":\"%s\",\"documento\":\"%s\",\"email\":\"%s@teste.exemplo\",\"prazoMedioDias\":5}"
                .formatted(nome, Fabrica.cnpj(proximo()), nome.toLowerCase().replaceAll("[^a-z0-9]", ""));
        return id(mockMvc.perform(json(post("/api/fornecedores"), corpo).with(comprador()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    /** Cotação ABERTA (manual, aberta ao mercado) com os pares produtoId, quantidade. */
    protected long criarCotacaoAberta(long... produtoEQuantidade) throws Exception {
        StringBuilder itens = new StringBuilder();
        for (int i = 0; i < produtoEQuantidade.length; i += 2) {
            itens.append(i == 0 ? "" : ",").append("{\"produtoId\":%d,\"quantidade\":%d}"
                    .formatted(produtoEQuantidade[i], produtoEQuantidade[i + 1]));
        }
        return id(mockMvc.perform(json(post("/api/cotacoes"), "{\"status\":\"ABERTA\",\"itens\":[" + itens + "]}")
                        .with(comprador()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    /** Proposta com o mesmo preço unitário para todos os produtos informados. */
    protected static String proposta(long fornecedorId, List<Long> produtos, String preco, int prazo, String frete) {
        String itens = produtos.stream().map(p -> "{\"produtoId\":%d,\"precoUnitario\":%s}".formatted(p, preco))
                .reduce((a, b) -> a + "," + b).orElseThrow();
        return ("{\"fornecedorId\":%d,\"itens\":[%s],\"valorFrete\":%s,\"impostoPercentual\":12,\"valorImpostoFixo\":0,"
                + "\"prazoEntregaDias\":%d,\"dataValidade\":\"%s\"}")
                .formatted(fornecedorId, itens, frete, prazo, LocalDate.now().plusDays(20));
    }

    protected BigDecimal saldo(long produtoId) throws Exception {
        String json = mockMvc.perform(get("/api/produtos/{id}", produtoId).with(comprador()))
                .andReturn().getResponse().getContentAsString();
        return new BigDecimal(JsonPath.read(json, "$.saldoAtual").toString());
    }

    protected static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, String corpo) {
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    protected static MockHttpServletRequestBuilder cancelamento(String rota, long id, String motivo) {
        return json(patch(rota, id), "{\"motivo\":\"" + motivo + "\"}");
    }

    protected static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    protected static BigDecimal decimal(String json, String caminho) {
        Object valor = JsonPath.read(json, caminho);
        if (valor instanceof List<?> lista) {
            valor = lista.getFirst();
        }
        return new BigDecimal(valor.toString());
    }
}
