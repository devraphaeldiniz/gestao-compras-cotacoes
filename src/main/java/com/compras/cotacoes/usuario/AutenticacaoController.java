package com.compras.cotacoes.usuario;

import com.compras.cotacoes.usuario.dto.LoginRequest;
import com.compras.cotacoes.usuario.dto.TokenResponse;
import com.compras.cotacoes.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Login com usuário e senha; devolve um JWT para o header Authorization")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;
    private final UsuarioService usuarioService;

    @Operation(summary = "Login", description = "Após 5 falhas seguidas o usuário fica bloqueado por 15 minutos (429).")
    @SecurityRequirements
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacaoService.login(request);
    }

    @Operation(summary = "Usuário autenticado")
    @GetMapping("/eu")
    public UsuarioResponse eu() {
        return usuarioService.atual();
    }
}
