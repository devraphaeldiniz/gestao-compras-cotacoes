package com.compras.cotacoes.pedido;

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

@Entity
@Table(name = "pedido_itens")
@Getter
@NoArgsConstructor
public class PedidoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoCompra pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantidade;

    @Column(name = "quantidade_recebida", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantidadeRecebida = BigDecimal.ZERO;

    @Column(name = "valor_unitario", nullable = false, precision = 19, scale = 4)
    private BigDecimal valorUnitario;

    /** Preço unitário + rateio de frete e impostos proporcional ao valor do item: é o custo que entra no estoque. */
    @Column(name = "custo_unitario_efetivo", nullable = false, precision = 19, scale = 4)
    private BigDecimal custoUnitarioEfetivo;

    PedidoItem(PedidoCompra pedido, Produto produto, BigDecimal quantidade, BigDecimal valorUnitario,
               BigDecimal custoUnitarioEfetivo) {
        this.pedido = pedido;
        this.produto = produto;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
        this.custoUnitarioEfetivo = custoUnitarioEfetivo;
    }

    public BigDecimal quantidadePendente() {
        return quantidade.subtract(quantidadeRecebida);
    }

    public boolean totalmenteRecebido() {
        return quantidadePendente().signum() == 0;
    }

    void registrarRecebimento(BigDecimal recebida) {
        this.quantidadeRecebida = quantidadeRecebida.add(recebida);
    }
}
