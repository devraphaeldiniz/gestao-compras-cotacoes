package com.compras.cotacoes.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelamentoRequest(
        @Schema(example = "Demanda suspensa pela diretoria")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Size(max = 500, message = "{validacao.tamanho.maximo}")
        String motivo) {
}
