package com.compras.cotacoes.shared.seguranca;

import com.compras.cotacoes.shared.config.I18nConfig;
import com.compras.cotacoes.shared.exception.ErroRespostaDTO;
import com.compras.cotacoes.shared.i18n.MensagemService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 401 e 403 no mesmo formato de erro do resto da API, com mensagem no idioma da requisição. */
@Component
@RequiredArgsConstructor
public class RespostasSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final MensagemService mensagens;
    private final ObjectMapper objectMapper;
    private final LocaleResolver localeResolver;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        escrever(request, response, HttpStatus.UNAUTHORIZED, mensagem(request, "auth.nao.autenticado"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN, mensagem(request, "auth.acesso.negado"));
    }

    /** Roda antes do DispatcherServlet: aplica o mesmo critério de idioma da API (?lang=, Accept-Language, pt-BR). */
    private String mensagem(HttpServletRequest request, String chave) {
        String lang = request.getParameter("lang");
        Locale idioma = lang != null && lang.toLowerCase(Locale.ROOT).startsWith("en") ? I18nConfig.INGLES
                : lang != null ? I18nConfig.PORTUGUES : localeResolver.resolveLocale(request);
        return mensagens.obterNoIdioma(idioma, chave);
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String mensagem)
            throws IOException {
        ErroRespostaDTO corpo = new ErroRespostaDTO(OffsetDateTime.now(), status.value(), status.getReasonPhrase(),
                mensagem, request.getRequestURI(), List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
