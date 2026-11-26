package com.compras.cotacoes.usuario.dto;

import com.compras.cotacoes.usuario.Perfil;
import com.compras.cotacoes.usuario.Usuario;
import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String username,
        String nome,
        String email,
        Perfil perfil,
        boolean ativo,
        LocalDateTime criadoEm) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil(), usuario.isAtivo(), usuario.getCriadoEm());
    }
}
