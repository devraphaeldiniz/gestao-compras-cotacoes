package com.compras.cotacoes.shared.seguranca;

import com.compras.cotacoes.shared.exception.ErroRespostaDTO;
import com.compras.cotacoes.shared.i18n.MensagemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cabeçalhos de segurança em todas as respostas e limite de tamanho do corpo nas rotas da API.
 *
 * <p>A interface web não usa script nem estilo inline, então a CSP padrão aceita só recursos do próprio
 * domínio ({@code 'self'}). O Swagger UI usa estilos inline e imagens em data URI e recebe uma CSP
 * um pouco mais permissiva, restrita às suas rotas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class CabecalhosSegurancaFiltro extends OncePerRequestFilter {

    static final long LIMITE_CORPO_BYTES = 1024L * 1024L;

    static final String CSP_PADRAO = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data:",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    static final String CSP_SWAGGER = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "connect-src 'self'",
            "object-src 'none'",
            "frame-ancestors 'none'");

    private final MensagemService mensagens;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String caminho = request.getRequestURI();

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        boolean swagger = caminho.startsWith("/swagger-ui") || caminho.startsWith("/v3/api-docs");
        response.setHeader("Content-Security-Policy", swagger ? CSP_SWAGGER : CSP_PADRAO);

        if (caminho.startsWith("/api/")) {
            // Respostas da API refletem estado de negócio: nada de cache em navegador ou proxy
            response.setHeader("Cache-Control", "no-store");
            if (request.getContentLengthLong() > LIMITE_CORPO_BYTES) {
                responderCorpoMuitoGrande(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void responderCorpoMuitoGrande(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpStatus status = HttpStatus.PAYLOAD_TOO_LARGE;
        ErroRespostaDTO corpo = new ErroRespostaDTO(OffsetDateTime.now(), status.value(), status.getReasonPhrase(),
                mensagens.obter("erro.corpo.muito.grande", LIMITE_CORPO_BYTES / 1024), request.getRequestURI(), List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
