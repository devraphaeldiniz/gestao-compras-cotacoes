package com.compras.cotacoes.usuario.dto;

import java.time.Instant;

public record TokenResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioResponse usuario) {
}
