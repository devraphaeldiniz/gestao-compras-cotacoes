package com.compras.cotacoes.cotacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record AbrirCotacaoRequest(
        @Schema(description = "Fornecedores convidados; vazio ou ausente = cotação aberta ao mercado", example = "[1, 2, 3]")
        List<Long> fornecedoresIds,

        @Schema(description = "Último dia para receber propostas", example = "2026-12-15")
        LocalDate dataLimite) {
}
