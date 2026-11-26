package com.compras.cotacoes.cotacao;

import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.proposta.CriterioDecisao;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cotacoes")
@Getter
@Setter
@NoArgsConstructor
public class Cotacao extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCotacao status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemCotacao origem = OrigemCotacao.MANUAL;

    @OneToMany(mappedBy = "cotacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CotacaoItem> itens = new ArrayList<>();

    @Column(name = "data_limite")
    private LocalDate dataLimite;

    @Column(length = 500)
    private String observacao;

    @Column(name = "proposta_vencedora_id")
    private Long propostaVencedoraId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cotacao_fornecedores",
            joinColumns = @JoinColumn(name = "cotacao_id"),
            inverseJoinColumns = @JoinColumn(name = "fornecedor_id"))
    private Set<Fornecedor> fornecedoresConvidados = new LinkedHashSet<>();

    // ---- auditoria do ciclo de vida
    @Column(name = "criado_por", nullable = false, length = 60, updatable = false)
    private String criadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterio_decisao", length = 30)
    private CriterioDecisao criterioDecisao;

    @Column(name = "decidido_por", length = 60)
    private String decididoPor;

    @Column(name = "decidido_em")
    private LocalDateTime decididoEm;

    @Column(name = "prazo_encerrado_em")
    private LocalDateTime prazoEncerradoEm;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "cancelado_por", length = 60)
    private String canceladoPor;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    /** Ex.: COT-2026-00001. O sequencial vem da sequence {@code seq_codigo_cotacao}. */
    public static String gerarCodigo(int ano, long sequencial) {
        return "COT-%d-%05d".formatted(ano, sequencial);
    }

    public CotacaoItem adicionarItem(Produto produto, BigDecimal quantidade) {
        CotacaoItem item = new CotacaoItem(this, produto, quantidade);
        itens.add(item);
        return item;
    }

    public Optional<CotacaoItem> itemDoProduto(Long produtoId) {
        return itens.stream().filter(i -> Objects.equals(i.getProduto().getId(), produtoId)).findFirst();
    }

    public boolean emAndamento() {
        return status == StatusCotacao.RASCUNHO || status == StatusCotacao.ABERTA;
    }

    /** Sem convidados, qualquer fornecedor ativo pode enviar proposta. */
    public boolean abertaAoMercado() {
        return fornecedoresConvidados.isEmpty();
    }

    public boolean convidou(Fornecedor fornecedor) {
        return fornecedoresConvidados.stream().anyMatch(f -> Objects.equals(f.getId(), fornecedor.getId()));
    }
}
