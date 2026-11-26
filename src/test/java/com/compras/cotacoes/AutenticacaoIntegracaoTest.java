package com.compras.cotacoes;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** Login real (usuário no banco, senha BCrypt, JWT assinado) e as regras de acesso por perfil. */
class AutenticacaoIntegracaoTest extends IntegracaoBase {

    @Test
    @DisplayName("Sem token: 401 no formato padrão; tela e Swagger continuam abertos")
    void semToken() throws Exception {
        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(containsString("Autenticação necessária")));
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/api/produtos").header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Perfis: comprador não decide nem gerencia usuários; aprovador não cadastra; admin faz tudo")
    void autorizacaoPorPerfil() throws Exception {
        String produto = "{\"sku\":\"AUT-%s\",\"nome\":\"x\",\"unidade\":\"UN\",\"saldoAtual\":5,\"estoqueMinimo\":1}";
        mockMvc.perform(json(post("/api/produtos"), produto.formatted("A")).with(aprovador())).andExpect(status().isForbidden());
        mockMvc.perform(json(post("/api/produtos"), produto.formatted("B")).with(admin())).andExpect(status().isCreated());
        mockMvc.perform(json(post("/api/cotacoes/1/decidir"), "{}").with(comprador()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(containsString("não tem permissão")));
        mockMvc.perform(get("/api/usuarios").with(comprador())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/usuarios").with(admin())).andExpect(status().isOk());
        mockMvc.perform(get("/api/produtos").with(aprovador())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login real: admin cria usuário, ele entra e usa o token; a senha nunca é devolvida")
    void loginReal() throws Exception {
        mockMvc.perform(json(post("/api/usuarios"),
                        "{\"username\":\"maria.compras\",\"nome\":\"Maria\",\"email\":\"maria@t.co\",\"senha\":\"senha-forte-123\",\"perfil\":\"COMPRADOR\"}")
                        .with(admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("COMPRADOR"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
        mockMvc.perform(json(post("/api/usuarios"),
                        "{\"username\":\"maria.compras\",\"nome\":\"Outra\",\"senha\":\"senha-forte-123\",\"perfil\":\"COMPRADOR\"}")
                        .with(admin()))
                .andExpect(status().isConflict());

        String login = mockMvc.perform(json(post("/api/auth/login"), "{\"username\":\"Maria.Compras\",\"senha\":\"senha-forte-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.username").value("maria.compras"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.token");

        mockMvc.perform(get("/api/auth/eu").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("COMPRADOR"));
        mockMvc.perform(json(post("/api/cotacoes/1/decidir"), "{}").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Força bruta: senha errada dá 401 genérico; após 5 falhas o login fica bloqueado com 429")
    void forcaBruta() throws Exception {
        mockMvc.perform(json(post("/api/usuarios"),
                        "{\"username\":\"alvo\",\"nome\":\"Alvo\",\"senha\":\"senha-correta-1\",\"perfil\":\"APROVADOR\"}").with(admin()))
                .andExpect(status().isCreated());
        String errado = "{\"username\":\"alvo\",\"senha\":\"chute-errado\"}";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(json(post("/api/auth/login"), errado))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensagem").value("Usuário ou senha inválidos"));
        }
        mockMvc.perform(json(post("/api/auth/login"), "{\"username\":\"alvo\",\"senha\":\"senha-correta-1\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        // Usuário inexistente recebe a mesma resposta (não revela quem existe)
        mockMvc.perform(json(post("/api/auth/login"), "{\"username\":\"ninguem\",\"senha\":\"qualquer\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuário ou senha inválidos"));
    }

    @Test
    @DisplayName("Usuário inativado não consegue mais entrar")
    void usuarioInativo() throws Exception {
        String criado = mockMvc.perform(json(post("/api/usuarios"),
                        "{\"username\":\"temporario\",\"nome\":\"Temp\",\"senha\":\"senha-temp-123\",\"perfil\":\"COMPRADOR\"}").with(admin()))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(json(patch("/api/usuarios/{id}/status", id(criado)), "{\"ativo\":false}").with(admin()))
                .andExpect(status().isOk());
        mockMvc.perform(json(post("/api/auth/login"), "{\"username\":\"temporario\",\"senha\":\"senha-temp-123\"}"))
                .andExpect(status().isUnauthorized());
    }
}
