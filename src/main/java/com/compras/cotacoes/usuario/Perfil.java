package com.compras.cotacoes.usuario;

/**
 * COMPRADOR: cadastros, estoque, cotações e propostas. APROVADOR: decide cotações, recebe e cancela pedidos.
 * ADMIN: tudo, mais a gestão de usuários. Comprar e aprovar em perfis diferentes é segregação de funções:
 * quem registra as propostas não é quem escolhe a vencedora.
 */
public enum Perfil {
    ADMIN, COMPRADOR, APROVADOR;

    public String role() {
        return "ROLE_" + name();
    }
}
