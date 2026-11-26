package com.compras.cotacoes.fornecedor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusFornecedorRequest(
        @Schema(example = "false")
        @NotNull(message = "{validacao.obrigatorio}")
        Boolean ativo) {
}
