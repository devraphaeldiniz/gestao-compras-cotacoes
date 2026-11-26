package com.compras.cotacoes.produto;

import com.compras.cotacoes.shared.persistencia.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
public class Produto extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UnidadeMedida unidade;

    @Column(name = "saldo_atual", nullable = false, precision = 19, scale = 4)
    private BigDecimal saldoAtual = BigDecimal.ZERO;

    @Column(name = "estoque_minimo", nullable = false, precision = 19, scale = 4)
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;

    @Column(name = "ultimo_preco_compra", precision = 19, scale = 4)
    private BigDecimal ultimoPrecoCompra;

    @Column(name = "preco_medio_compra", precision = 19, scale = 4)
    private BigDecimal precoMedioCompra;

    @Column(nullable = false)
    private boolean ativo = true;

    /** Regra A: o produto precisa de reposição quando o saldo chega ao estoque mínimo. */
    public boolean precisaReposicao() {
        return saldoAtual.compareTo(estoqueMinimo) <= 0;
    }
}
