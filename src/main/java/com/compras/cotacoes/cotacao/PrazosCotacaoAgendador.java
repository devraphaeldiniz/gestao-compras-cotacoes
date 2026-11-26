package com.compras.cotacoes.cotacao;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara o processamento de prazos (padrão: de hora em hora). Com várias réplicas, só uma deve rodar o job:
 * use {@code APP_PRAZOS_HABILITADO=false} nas demais ou um lock distribuído (ex.: ShedLock).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.prazos.habilitado", havingValue = "true", matchIfMissing = true)
public class PrazosCotacaoAgendador {

    private final PrazosCotacaoService prazosCotacaoService;

    @Scheduled(cron = "${app.prazos.cron:0 0 * * * *}")
    public void executar() {
        prazosCotacaoService.processar();
    }
}
