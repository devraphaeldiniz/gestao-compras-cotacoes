package com.compras.cotacoes.fornecedor;

import com.compras.cotacoes.shared.persistencia.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fornecedores")
@Getter
@Setter
@NoArgsConstructor
public class Fornecedor extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razao_social", nullable = false, length = 180)
    private String razaoSocial;

    /** CPF ou CNPJ, armazenado só com dígitos. */
    @Column(nullable = false, unique = true, length = 20)
    private String documento;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "prazo_medio_dias", nullable = false)
    private Integer prazoMedioDias;

    @Column(nullable = false)
    private boolean ativo = true;
}
