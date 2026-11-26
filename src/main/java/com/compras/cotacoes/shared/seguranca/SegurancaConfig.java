package com.compras.cotacoes.shared.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * API stateless com JWT no header {@code Authorization: Bearer}. Sem sessão e sem cookie, portanto sem CSRF.
 * Os cabeçalhos de segurança vêm do {@link CabecalhosSegurancaFiltro} (o padrão do Spring Security é desligado
 * para não duplicar valores).
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(PropriedadesSeguranca.class)
public class SegurancaConfig {

    public static final String CLAIM_PERFIL = "perfil";
    public static final String CLAIM_NOME = "nome";

    @Bean
    SecurityFilterChain segurancaApi(HttpSecurity http, RespostasSeguranca respostas, Environment ambiente) throws Exception {
        RequestMatcher portaDeGerencia = portaDeGerencia(ambiente);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        // Porta de gerência separada (ex.: 8081 no Docker), não publicada: o Prometheus coleta sem token
                        .requestMatchers(portaDeGerencia).permitAll()
                        .requestMatchers("/", "/index.html", "/app.js", "/app.css", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/cotacoes/*/decidir").hasRole("APROVADOR")
                        .requestMatchers(HttpMethod.POST, "/api/pedidos-compra/*/receber").hasRole("APROVADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/pedidos-compra/*/cancelar").hasRole("APROVADOR")
                        .requestMatchers("/api/produtos/**", "/api/fornecedores/**", "/api/cotacoes/**").hasRole("COMPRADOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(conversorPerfil()))
                        .authenticationEntryPoint(respostas)
                        .accessDeniedHandler(respostas))
                .exceptionHandling(e -> e.authenticationEntryPoint(respostas).accessDeniedHandler(respostas));
        return http.build();
    }

    /** ADMIN herda as permissões de COMPRADOR e APROVADOR. */
    @Bean
    static RoleHierarchy hierarquiaDePerfis() {
        return RoleHierarchyImpl.withDefaultRolePrefix().role("ADMIN").implies("COMPRADOR", "APROVADOR").build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecretKey chaveJwt(PropriedadesSeguranca propriedades) {
        String segredo = propriedades.jwtSegredo();
        byte[] bytes;
        if (segredo == null || segredo.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("APP_JWT_SEGREDO não definido: usando chave aleatória (tokens deixam de valer a cada reinício)");
        } else {
            bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("app.seguranca.jwt-segredo precisa de pelo menos 32 bytes (HS256)");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static JwtAuthenticationConverter conversorPerfil() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {
            String perfil = jwt.getClaimAsString(CLAIM_PERFIL);
            return perfil == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + perfil));
        });
        return conversor;
    }

    private static RequestMatcher portaDeGerencia(Environment ambiente) {
        Integer porta = ambiente.getProperty("management.server.port", Integer.class);
        Integer portaApp = ambiente.getProperty("server.port", Integer.class, 8080);
        if (porta == null || porta.equals(portaApp) || porta <= 0) {
            return request -> false;
        }
        return request -> request.getLocalPort() == porta;
    }
}
