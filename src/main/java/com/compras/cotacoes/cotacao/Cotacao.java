package com.compras.cotacoes.cotacao;

import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.produto.Produto;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCotacao status;

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

    /** Ex.: COT-2026-00001. O sequencial vem da sequence {@code seq_codigo_cotacao}. */
    public static String gerarCodigo(int ano, long sequencial) {
        return "COT-%d-%05d".formatted(ano, sequencial);
    }

    /** Sem convidados, qualquer fornecedor ativo pode enviar proposta. */
    public boolean abertaAoMercado() {
        return fornecedoresConvidados.isEmpty();
    }

    public boolean convidou(Fornecedor fornecedor) {
        return fornecedoresConvidados.stream().anyMatch(f -> Objects.equals(f.getId(), fornecedor.getId()));
    }
}
