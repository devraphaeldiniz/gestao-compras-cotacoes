package com.compras.cotacoes.proposta;

import com.compras.cotacoes.cotacao.CotacaoItem;
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

/** Preço ofertado pelo fornecedor para um item da cotação. */
@Entity
@Table(name = "proposta_itens")
@Getter
@NoArgsConstructor
public class PropostaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposta_id", nullable = false)
    private Proposta proposta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotacao_item_id", nullable = false)
    private CotacaoItem cotacaoItem;

    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 4)
    private BigDecimal precoUnitario;

    /** Quantidade do item x preço unitário, 2 casas HALF_UP. */
    @Column(name = "valor_mercadoria", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorMercadoria;

    PropostaItem(Proposta proposta, CotacaoItem cotacaoItem, BigDecimal precoUnitario, BigDecimal valorMercadoria) {
        this.proposta = proposta;
        this.cotacaoItem = cotacaoItem;
        this.precoUnitario = precoUnitario;
        this.valorMercadoria = valorMercadoria;
    }
}
