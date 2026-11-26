package com.compras.cotacoes.shared.seguranca;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Bloqueia temporariamente o usuário após N falhas seguidas de login. Em memória: suficiente para uma instância;
 * com várias réplicas, o estado iria para um cache compartilhado (Redis) ou o limite ficaria no gateway.
 */
@Component
public class ProtecaoForcaBruta {

    private record Tentativas(int falhas, Instant bloqueadoAte) {
    }

    private final Map<String, Tentativas> porUsuario = new ConcurrentHashMap<>();
    private final PropriedadesSeguranca propriedades;
    private final Clock clock;

    public ProtecaoForcaBruta(PropriedadesSeguranca propriedades, Clock clock) {
        this.propriedades = propriedades;
        this.clock = clock;
    }

    /** Tempo restante de bloqueio, se o usuário estiver bloqueado. */
    public Optional<Duration> bloqueio(String username) {
        Tentativas t = porUsuario.get(chave(username));
        if (t == null || t.bloqueadoAte() == null) {
            return Optional.empty();
        }
        Duration restante = Duration.between(Instant.now(clock), t.bloqueadoAte());
        if (restante.isNegative() || restante.isZero()) {
            porUsuario.remove(chave(username));
            return Optional.empty();
        }
        return Optional.of(restante);
    }

    public void registrarFalha(String username) {
        porUsuario.compute(chave(username), (k, atual) -> {
            int falhas = (atual == null ? 0 : atual.falhas()) + 1;
            Instant bloqueadoAte = falhas >= propriedades.maxTentativas() ? Instant.now(clock).plus(propriedades.bloqueio()) : null;
            return new Tentativas(falhas, bloqueadoAte);
        });
    }

    public void registrarSucesso(String username) {
        porUsuario.remove(chave(username));
    }

    private static String chave(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
