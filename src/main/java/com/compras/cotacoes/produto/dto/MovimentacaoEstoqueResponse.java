package com.compras.cotacoes.produto.dto;

import com.compras.cotacoes.produto.TipoMovimentacaoEstoque;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimentacaoEstoqueResponse(
        Long id,
        TipoMovimentacaoEstoque tipo,
        BigDecimal quantidade,
        BigDecimal saldoAnterior,
        BigDecimal saldoPosterior,
        BigDecimal custoUnitario,
        String referencia,
        String observacao,
        String usuario,
        LocalDateTime criadoEm) {
}
