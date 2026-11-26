package com.compras.cotacoes.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "comprador")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Size(max = 60, message = "{validacao.tamanho.maximo}")
        String username,

        @Schema(example = "comprador123")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Size(max = 72, message = "{validacao.tamanho.maximo}")
        String senha) {

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", senha=***]";
    }
}
