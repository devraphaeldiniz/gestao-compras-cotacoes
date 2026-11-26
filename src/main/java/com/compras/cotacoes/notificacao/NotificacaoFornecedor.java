package com.compras.cotacoes.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Registro de e-mail enviado; a chave única (cotação, fornecedor, tipo) impede convite ou lembrete repetido. */
@Entity
@Table(name = "notificacoes_fornecedor")
@Getter
@NoArgsConstructor
public class NotificacaoFornecedor {

    public enum Tipo { CONVITE, LEMBRETE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cotacao_id", nullable = false)
    private Long cotacaoId;

    @Column(name = "fornecedor_id", nullable = false)
    private Long fornecedorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(name = "enviado_em", nullable = false)
    private LocalDateTime enviadoEm;

    public NotificacaoFornecedor(Long cotacaoId, Long fornecedorId, Tipo tipo, LocalDateTime enviadoEm) {
        this.cotacaoId = cotacaoId;
        this.fornecedorId = fornecedorId;
        this.tipo = tipo;
        this.enviadoEm = enviadoEm;
    }
}
