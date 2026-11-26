package com.compras.cotacoes.proposta;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.cotacao.CotacaoItem;
import com.compras.cotacoes.fornecedor.Fornecedor;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "propostas")
@Getter
@Setter
@NoArgsConstructor
public class Proposta extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotacao_id", nullable = false)
    private Cotacao cotacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @OneToMany(mappedBy = "proposta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PropostaItem> itens = new ArrayList<>();

    @Column(name = "valor_mercadoria", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorMercadoria;

    @Column(name = "valor_frete", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorFrete = BigDecimal.ZERO;

    @Column(name = "imposto_percentual", nullable = false, precision = 7, scale = 4)
    private BigDecimal impostoPercentual = BigDecimal.ZERO;

    @Column(name = "valor_imposto_fixo", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorImpostoFixo = BigDecimal.ZERO;

    @Column(name = "valor_imposto_calculado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorImpostoCalculado = BigDecimal.ZERO;

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "prazo_entrega_dias", nullable = false)
    private Integer prazoEntregaDias;

    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProposta status = StatusProposta.PENDENTE;

    @Column(name = "pontuacao_calculada", precision = 10, scale = 4)
    private BigDecimal pontuacaoCalculada;

    @Column(name = "justificativa_escolha", length = 500)
    private String justificativaEscolha;

    public PropostaItem adicionarItem(CotacaoItem cotacaoItem, BigDecimal precoUnitario, BigDecimal valorMercadoriaItem) {
        PropostaItem item = new PropostaItem(this, cotacaoItem, precoUnitario, valorMercadoriaItem);
        itens.add(item);
        return item;
    }
}
