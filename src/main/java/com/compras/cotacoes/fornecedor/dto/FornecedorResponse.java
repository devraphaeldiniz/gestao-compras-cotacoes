package com.compras.cotacoes.fornecedor.dto;

import java.time.LocalDateTime;

public record FornecedorResponse(
        Long id,
        String razaoSocial,
        String documento,
        String email,
        Integer prazoMedioDias,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
