package com.compras.cotacoes.shared.seguranca;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Quem está executando a operação, para auditoria (decidido_por, cancelado_por, extrato, histórico).
 * Fora de uma requisição autenticada (jobs agendados, migrações) o autor é {@value #SISTEMA}.
 */
@Component
public class UsuarioAtual {

    public static final String SISTEMA = "sistema";

    public String username() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated() || autenticacao instanceof AnonymousAuthenticationToken) {
            return SISTEMA;
        }
        return autenticacao.getName();
    }
}
