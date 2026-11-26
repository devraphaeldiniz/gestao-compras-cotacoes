package com.compras.cotacoes.usuario.dto;

import com.compras.cotacoes.usuario.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
        @Schema(example = "maria.silva")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Pattern(regexp = "^[a-z0-9._-]{3,60}$", message = "{validacao.username}")
        String username,

        @Schema(example = "Maria Silva")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Size(max = 120, message = "{validacao.tamanho.maximo}")
        String nome,

        @Schema(description = "Recebe os avisos de prazo das cotações (aprovadores)", example = "maria@empresa.exemplo")
        @Email(message = "{validacao.email}")
        @Size(max = 150, message = "{validacao.tamanho.maximo}")
        String email,

        @Schema(description = "Entre 10 e 72 caracteres (limite do BCrypt)")
        @NotBlank(message = "{validacao.obrigatorio}")
        @Size(min = 10, max = 72, message = "{validacao.senha.tamanho}")
        String senha,

        @NotNull(message = "{validacao.obrigatorio}")
        Perfil perfil) {

    @Override
    public String toString() {
        return "CriarUsuarioRequest[username=" + username + ", perfil=" + perfil + ", senha=***]";
    }
}
