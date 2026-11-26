package com.compras.cotacoes.pedido.dto;

import com.compras.cotacoes.pedido.StatusPedidoCompra;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoCompraResponse(
        Long id,
        String numeroPedido,
        Long cotacaoId,
        String cotacaoCodigo,
        Long propostaId,
        Long fornecedorId,
        String fornecedorRazaoSocial,
        List<PedidoItemResponse> itens,
        BigDecimal valorMercadoria,
        BigDecimal valorFrete,
        BigDecimal valorImposto,
        BigDecimal valorTotal,
        Integer prazoEntregaDias,
        StatusPedidoCompra status,
        LocalDateTime dataRecebimento,
        String emitidoPor,
        String motivoCancelamento,
        String canceladoPor,
        LocalDateTime canceladoEm,
        LocalDateTime criadoEm) {
}
