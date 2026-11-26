package com.compras.cotacoes.pedido.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Corpo opcional: sem ele (ou com lista vazia) o recebimento é de todo o saldo pendente. */
public record ReceberPedidoRequest(
        @Schema(description = "Quantidade recebida agora, por produto; omitir = receber tudo o que falta")
        @Size(max = 50, message = "{validacao.itens.maximo}")
        List<@Valid Item> itens) {

    public record Item(
            @Schema(example = "1")
            @NotNull(message = "{validacao.obrigatorio}")
            Long produtoId,

            @Schema(example = "20")
            @NotNull(message = "{validacao.obrigatorio}")
            @Positive(message = "{validacao.positivo}")
            @Digits(integer = 15, fraction = 4, message = "{validacao.digitos}")
            BigDecimal quantidade) {
    }
}
