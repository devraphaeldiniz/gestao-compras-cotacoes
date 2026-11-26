package com.compras.cotacoes.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI comprasCotacoesOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Sistema de Gestão de Compras e Cotações")
                .version("1.0.0")
                .description("""
                        Ciclo completo de compras: estoque mínimo gera cotação, fornecedores enviam propostas, \
                        o sistema recomenda a vencedora (menor preço ou pontuação ponderada 70/30), emite o pedido \
                        de compra e atualiza estoque e preço médio no recebimento.

                        Envie o header `Accept-Language: en` (ou `?lang=en`) para receber as mensagens em inglês.""")
                .license(new License().name("MIT")));
    }
}
