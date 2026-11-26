package com.compras.cotacoes.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearer-jwt";

    @Bean
    public OpenAPI comprasCotacoesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Gestão de Compras e Cotações")
                        .version("2.0.0")
                        .description("""
                                Ciclo completo de compras: estoque mínimo gera cotação (com vários itens), fornecedores \
                                enviam propostas, o sistema recomenda a vencedora (menor preço ou pontuação ponderada 70/30), \
                                emite o pedido e atualiza estoque e preço médio no recebimento, inclusive parcial.

                                **Autenticação:** faça `POST /api/auth/login`, copie o `token` e use o botão *Authorize*. \
                                Perfis: COMPRADOR (cadastros, estoque, cotações, propostas), APROVADOR (decisão, \
                                recebimento, cancelamento de pedido) e ADMIN (tudo + usuários).

                                Envie `Accept-Language: en` (ou `?lang=en`) para mensagens em inglês.""")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
