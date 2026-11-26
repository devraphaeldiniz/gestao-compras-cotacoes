package com.compras.cotacoes.pedido;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.proposta.Proposta;
import com.compras.cotacoes.shared.persistencia.EntidadeAuditavel;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pedidos_compra")
@Getter
@Setter
@NoArgsConstructor
public class PedidoCompra extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_pedido", nullable = false, unique = true, length = 40)
    private String numeroPedido;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotacao_id", nullable = false, unique = true)
    private Cotacao cotacao;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposta_id", nullable = false, unique = true)
    private Proposta proposta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PedidoItem> itens = new ArrayList<>();

    @Column(name = "valor_mercadoria", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorMercadoria;

    @Column(name = "valor_frete", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorFrete;

    @Column(name = "valor_imposto", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorImposto;

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "prazo_entrega_dias", nullable = false)
    private Integer prazoEntregaDias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedidoCompra status;

    /** Data do recebimento que completou o pedido. */
    @Column(name = "data_recebimento")
    private LocalDateTime dataRecebimento;

    @Column(name = "emitido_por", nullable = false, length = 60, updatable = false)
    private String emitidoPor;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "cancelado_por", length = 60)
    private String canceladoPor;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    public PedidoItem adicionarItem(Produto produto, BigDecimal quantidade, BigDecimal valorUnitario,
                                    BigDecimal custoUnitarioEfetivo) {
        PedidoItem item = new PedidoItem(this, produto, quantidade, valorUnitario, custoUnitarioEfetivo);
        itens.add(item);
        return item;
    }

    public boolean totalmenteRecebido() {
        return itens.stream().allMatch(PedidoItem::totalmenteRecebido);
    }

    /** Ex.: PC-2026-00001. O sequencial vem da sequence {@code seq_numero_pedido}. */
    public static String gerarNumero(int ano, long sequencial) {
        return "PC-%d-%05d".formatted(ano, sequencial);
    }
}
