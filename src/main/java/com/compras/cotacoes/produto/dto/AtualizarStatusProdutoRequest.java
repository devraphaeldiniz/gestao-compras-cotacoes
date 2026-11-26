package com.compras.cotacoes.produto.dto;

import jakarta.validation.constraints.NotNull;

public record AtualizarStatusProdutoRequest(
        @NotNull(message = "{validacao.obrigatorio}")
        Boolean ativo) {
}
