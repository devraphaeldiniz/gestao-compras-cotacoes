package com.compras.cotacoes.shared.seguranca;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração de autenticação ({@code app.seguranca.*}).
 *
 * @param jwtSegredo     chave HMAC do JWT (mín. 32 bytes). Vazia = gera uma aleatória a cada subida (tokens não
 *                       sobrevivem a reinício); em produção, defina {@code APP_JWT_SEGREDO}.
 * @param jwtExpiracao   validade do token
 * @param adminUsername  usuário ADMIN criado na primeira subida, se não houver nenhum usuário
 * @param adminSenha     senha desse ADMIN; vazia = não cria
 * @param maxTentativas  falhas de login seguidas antes do bloqueio temporário
 * @param bloqueio       duração do bloqueio
 */
@ConfigurationProperties(prefix = "app.seguranca")
public record PropriedadesSeguranca(
        String jwtSegredo,
        Duration jwtExpiracao,
        String adminUsername,
        String adminSenha,
        int maxTentativas,
        Duration bloqueio) {

    public PropriedadesSeguranca {
        jwtExpiracao = jwtExpiracao == null ? Duration.ofHours(8) : jwtExpiracao;
        adminUsername = adminUsername == null || adminUsername.isBlank() ? "admin" : adminUsername;
        maxTentativas = maxTentativas <= 0 ? 5 : maxTentativas;
        bloqueio = bloqueio == null ? Duration.ofMinutes(15) : bloqueio;
    }
}
