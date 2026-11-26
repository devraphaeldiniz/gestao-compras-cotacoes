package com.compras.cotacoes.shared.metricas;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Métricas de negócio expostas no /actuator/prometheus (além das técnicas que o Spring já publica). */
@Component
public class MetricasCompras {

    private final MeterRegistry registry;
    private final DistributionSummary valorPedidos;

    public MetricasCompras(MeterRegistry registry) {
        this.registry = registry;
        this.valorPedidos = DistributionSummary.builder("compras.pedidos.valor")
                .description("Valor total dos pedidos de compra emitidos")
                .baseUnit("BRL")
                .register(registry);
    }

    public void cotacaoCriada(String origem) {
        contador("compras.cotacoes.criadas", "Cotações criadas", "origem", origem).increment();
    }

    public void cotacaoDecidida(String criterio, boolean escolhaManual) {
        Counter.builder("compras.cotacoes.decididas")
                .description("Cotações decididas")
                .tag("criterio", criterio)
                .tag("escolha_manual", String.valueOf(escolhaManual))
                .register(registry)
                .increment();
    }

    public void cotacaoCancelada(String motivo) {
        contador("compras.cotacoes.canceladas", "Cotações canceladas", "motivo", motivo).increment();
    }

    public void pedidoEmitido(BigDecimal valorTotal) {
        valorPedidos.record(valorTotal.doubleValue());
    }

    public void recebimento(boolean completo) {
        contador("compras.pedidos.recebimentos", "Recebimentos registrados", "completo", String.valueOf(completo)).increment();
    }

    public void pedidoCancelado() {
        contador("compras.pedidos.cancelados", "Pedidos cancelados", "origem", "manual").increment();
    }

    public void loginFalho() {
        contador("compras.autenticacao.falhas", "Tentativas de login recusadas", "tipo", "credencial").increment();
    }

    private Counter contador(String nome, String descricao, String tag, String valor) {
        return Counter.builder(nome).description(descricao).tag(tag, valor).register(registry);
    }
}
