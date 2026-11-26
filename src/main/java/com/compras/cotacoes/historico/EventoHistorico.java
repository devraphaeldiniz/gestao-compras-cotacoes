package com.compras.cotacoes.historico;

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

/** Registro imutável de "quem fez o quê e quando". Só é inserido, nunca alterado. */
@Entity
@Table(name = "historico_eventos")
@Getter
@NoArgsConstructor
public class EventoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private EntidadeHistorico entidade;

    @Column(name = "entidade_id", nullable = false, updatable = false)
    private Long entidadeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private AcaoHistorico acao;

    @Column(length = 1000, updatable = false)
    private String detalhe;

    @Column(nullable = false, length = 60, updatable = false)
    private String usuario;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public EventoHistorico(EntidadeHistorico entidade, Long entidadeId, AcaoHistorico acao, String detalhe,
                           String usuario, LocalDateTime criadoEm) {
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.acao = acao;
        this.detalhe = detalhe == null || detalhe.length() <= 1000 ? detalhe : detalhe.substring(0, 1000);
        this.usuario = usuario;
        this.criadoEm = criadoEm;
    }
}
