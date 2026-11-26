package com.compras.cotacoes.cotacao;

import com.compras.cotacoes.produto.Produto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cotacao_itens")
@Getter
@Setter
@NoArgsConstructor
public class CotacaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotacao_id", nullable = false)
    private Cotacao cotacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantidade;

    CotacaoItem(Cotacao cotacao, Produto produto, BigDecimal quantidade) {
        this.cotacao = cotacao;
        this.produto = produto;
        this.quantidade = quantidade;
    }
}
