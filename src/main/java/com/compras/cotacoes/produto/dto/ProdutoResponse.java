package com.compras.cotacoes.produto.dto;

import com.compras.cotacoes.produto.UnidadeMedida;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProdutoResponse(
        Long id,
        String sku,
        String nome,
        UnidadeMedida unidade,
        BigDecimal saldoAtual,
        BigDecimal estoqueMinimo,
        BigDecimal ultimoPrecoCompra,
        BigDecimal precoMedioCompra,
        boolean ativo,
        boolean necessitaCompra,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
