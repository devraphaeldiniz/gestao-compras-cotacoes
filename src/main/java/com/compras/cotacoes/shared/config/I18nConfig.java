package com.compras.cotacoes.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Idioma da requisição: {@code ?lang=pt|en} tem prioridade; senão vale o header {@code Accept-Language}.
 * Português (pt-BR) é o padrão quando nada é informado ou o idioma não é suportado.
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    public static final Locale PORTUGUES = Locale.forLanguageTag("pt-BR");
    public static final Locale INGLES = Locale.ENGLISH;

    @Bean
    public LocaleResolver localeResolver() {
        ResolvedorIdioma resolvedor = new ResolvedorIdioma();
        resolvedor.setSupportedLocales(List.of(PORTUGUES, Locale.forLanguageTag("pt"), INGLES));
        resolvedor.setDefaultLocale(PORTUGUES);
        return resolvedor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        interceptor.setIgnoreInvalidLocale(true);
        registry.addInterceptor(interceptor);
    }

    /**
     * O {@link AcceptHeaderLocaleResolver} não aceita troca de idioma; esta extensão guarda o idioma
     * vindo do {@code ?lang=} como atributo da própria requisição (sem sessão nem cookie, API stateless).
     */
    static class ResolvedorIdioma extends AcceptHeaderLocaleResolver {

        private static final String ATRIBUTO = ResolvedorIdioma.class.getName() + ".LOCALE";

        @Override
        public Locale resolveLocale(HttpServletRequest request) {
            Object escolhido = request.getAttribute(ATRIBUTO);
            if (escolhido instanceof Locale locale) {
                return locale;
            }
            return super.resolveLocale(request);
        }

        @Override
        public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
            if (locale == null) {
                request.removeAttribute(ATRIBUTO);
                return;
            }
            Locale suportado = "en".equalsIgnoreCase(locale.getLanguage()) ? INGLES : PORTUGUES;
            request.setAttribute(ATRIBUTO, suportado);
        }
    }
}
