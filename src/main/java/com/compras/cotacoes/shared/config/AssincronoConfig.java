package com.compras.cotacoes.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita o envio assíncrono de e-mails e o job de prazos das cotações. */
@Configuration
@EnableAsync
@EnableScheduling
public class AssincronoConfig {
}
