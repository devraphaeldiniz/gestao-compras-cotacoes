package com.compras.cotacoes.cotacao.dto;

import com.compras.cotacoes.cotacao.StatusCotacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CriarCotacaoRequest(
        @Schema(description = "Produtos e quantidades a cotar (um item por produto)")
        @NotEmpty(message = "{validacao.obrigatorio}")
        @Size(max = 50, message = "{validacao.itens.maximo}")
        List<@Valid ItemCotacaoRequest> itens,

        @Schema(description = "RASCUNHO (padrão) ou ABERTA", example = "RASCUNHO")
        StatusCotacao status,

        @Schema(example = "2026-12-15")
        LocalDate dataLimite,

        @Size(max = 500, message = "{validacao.tamanho.maximo}")
        String observacao,

        @Schema(description = "Fornecedores convidados; vazio = aberta ao mercado")
        List<Long> fornecedoresIds) {
}
