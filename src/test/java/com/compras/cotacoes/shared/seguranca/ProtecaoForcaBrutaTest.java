package com.compras.cotacoes.shared.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProtecaoForcaBrutaTest {

    private Instant agora = Instant.parse("2026-11-26T12:00:00Z");
    private final Clock relogio = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    };
    private final ProtecaoForcaBruta protecao = new ProtecaoForcaBruta(
            new PropriedadesSeguranca(null, null, null, null, 3, Duration.ofMinutes(15)), relogio);

    @Test
    @DisplayName("Bloqueia após N falhas seguidas e libera quando o tempo passa")
    void bloqueiaEDesbloqueia() {
        protecao.registrarFalha("Maria");
        protecao.registrarFalha("maria");
        assertThat(protecao.bloqueio("maria")).isEmpty();

        protecao.registrarFalha("MARIA ");
        assertThat(protecao.bloqueio("maria")).hasValue(Duration.ofMinutes(15));

        agora = agora.plus(Duration.ofMinutes(15));
        assertThat(protecao.bloqueio("maria")).isEmpty();
    }

    @Test
    @DisplayName("Login certo zera o contador de falhas")
    void sucessoZeraContador() {
        protecao.registrarFalha("joao");
        protecao.registrarFalha("joao");
        protecao.registrarSucesso("joao");
        protecao.registrarFalha("joao");
        protecao.registrarFalha("joao");

        assertThat(protecao.bloqueio("joao")).isEmpty();
    }
}
