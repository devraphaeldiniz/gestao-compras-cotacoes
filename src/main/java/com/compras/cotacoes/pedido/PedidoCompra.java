package com.compras.cotacoes.pedido;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.proposta.Proposta;
import com.compras.cotacoes.shared.persistencia.EntidadeAuditavel;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 19, scale = 4)
    private BigDecimal valorUnitario;

    @Column(name = "valor_frete", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorFrete;

    @Column(name = "valor_imposto", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorImposto;

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "prazo_entrega_dias", nullable = false)
    private Integer prazoEntregaDias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedidoCompra status;

    @Column(name = "data_recebimento")
    private LocalDateTime dataRecebimento;

    /** Ex.: PC-2026-00001. O sequencial vem da sequence {@code seq_numero_pedido}. */
    public static String gerarNumero(int ano, long sequencial) {
        return "PC-%d-%05d".formatted(ano, sequencial);
    }
}
