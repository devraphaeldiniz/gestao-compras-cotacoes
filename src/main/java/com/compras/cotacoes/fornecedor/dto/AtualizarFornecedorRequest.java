package com.compras.cotacoes.fornecedor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AtualizarFornecedorRequest(
        @Schema(example = "Papelaria Central Ltda")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Size(max = 180, message = "{validacao.tamanho.maximo}")
        String razaoSocial,

        @Schema(example = "vendas@papelaria.exemplo")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Email(message = "{validacao.email}")
        @Size(max = 150, message = "{validacao.tamanho.maximo}")
        String email,

        @Schema(example = "5")
        @NotNull(message = "{validacao.obrigatorio}")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        @Max(value = 3650, message = "{validacao.maximo}")
        Integer prazoMedioDias) {
}
