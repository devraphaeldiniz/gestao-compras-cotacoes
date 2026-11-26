package com.compras.cotacoes.pedido.dto;

import com.compras.cotacoes.pedido.StatusPedidoCompra;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PedidoCompraResponse(
        Long id,
        String numeroPedido,
        Long cotacaoId,
        String cotacaoCodigo,
        Long propostaId,
        Long fornecedorId,
        String fornecedorRazaoSocial,
        Long produtoId,
        String produtoSku,
        String produtoNome,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorFrete,
        BigDecimal valorImposto,
        BigDecimal valorTotal,
        Integer prazoEntregaDias,
        StatusPedidoCompra status,
        LocalDateTime dataRecebimento,
        LocalDateTime criadoEm) {
}
