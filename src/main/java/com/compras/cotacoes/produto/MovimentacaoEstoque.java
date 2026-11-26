package com.compras.cotacoes.produto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Linha do extrato de estoque: imutável, guarda o saldo antes e depois para reconstruir a história do produto. */
@Entity
@Table(name = "movimentacoes_estoque")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TipoMovimentacaoEstoque tipo;

    @Column(nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal quantidade;

    @Column(name = "saldo_anterior", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal saldoPosterior;

    @Column(name = "custo_unitario", precision = 19, scale = 4, updatable = false)
    private BigDecimal custoUnitario;

    @Column(length = 60, updatable = false)
    private String referencia;

    @Column(length = 500, updatable = false)
    private String observacao;

    @Column(nullable = false, length = 60, updatable = false)
    private String usuario;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
