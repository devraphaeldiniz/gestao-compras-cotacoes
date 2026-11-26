package com.compras.cotacoes.fornecedor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CriarFornecedorRequest(
        @Schema(example = "Papelaria Central Ltda")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Size(max = 180, message = "{validacao.tamanho.maximo}")
        String razaoSocial,

        @Schema(description = "CPF ou CNPJ, com ou sem pontuação", example = "11.222.333/0001-81")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Pattern(regexp = "^(\\d{11}|\\d{14}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})$",
                message = "{validacao.documento}")
        String documento,

        @Schema(example = "vendas@papelaria.exemplo")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Email(message = "{validacao.email}")
        @Size(max = 150, message = "{validacao.tamanho.maximo}")
        String email,

        @Schema(example = "5")
        @NotNull(message = "{validacao.obrigatorio}")
        @PositiveOrZero(message = "{validacao.nao.negativo}")
        Integer prazoMedioDias) {
}
