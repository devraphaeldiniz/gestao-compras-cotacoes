package com.compras.cotacoes.usuario;

import com.compras.cotacoes.usuario.dto.CriarUsuarioRequest;
import com.compras.cotacoes.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuários", description = "Gestão de usuários (somente ADMIN)")
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    public record AtualizarStatusUsuarioRequest(@NotNull(message = "{validacao.obrigatorio}") Boolean ativo) {
    }

    @Operation(summary = "Cria um usuário")
    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        UsuarioResponse criado = usuarioService.criar(request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
    }

    @Operation(summary = "Lista usuários")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @Operation(summary = "Ativa ou inativa um usuário", description = "O administrador não pode inativar a si mesmo.")
    @PatchMapping("/{id}/status")
    public UsuarioResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AtualizarStatusUsuarioRequest request) {
        return usuarioService.alterarStatus(id, request.ativo());
    }
}
