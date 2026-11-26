package com.compras.cotacoes.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ID de correlação por requisição: reaproveita o {@code X-Request-Id} do cliente/proxy (se tiver formato
 * seguro) ou gera um UUID. Vai para o MDC (aparece em todo log, inclusive no formato JSON) e volta no header
 * da resposta, para cruzar o erro que o usuário viu com a linha de log.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CorrelacaoFiltro extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String CHAVE_MDC = "requestId";
    private static final Pattern FORMATO_SEGURO = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recebido = request.getHeader(HEADER);
        // Valor arbitrário do cliente nunca vai direto para o log (evita injeção de linhas de log)
        String id = recebido != null && FORMATO_SEGURO.matcher(recebido).matches() ? recebido : UUID.randomUUID().toString();
        MDC.put(CHAVE_MDC, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CHAVE_MDC);
        }
    }
}
