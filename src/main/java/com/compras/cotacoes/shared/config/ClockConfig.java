package com.compras.cotacoes.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio injetável: as regras de validade e prazo usam {@code LocalDate.now(clock)},
 * o que deixa os testes determinísticos (basta passar um {@link Clock#fixed}).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
