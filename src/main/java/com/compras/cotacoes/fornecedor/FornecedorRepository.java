package com.compras.cotacoes.fornecedor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    boolean existsByDocumento(String documento);

    /** {@code busca} vazia = sem filtro de texto; casa razão social ou documento. */
    @Query("""
            select f from Fornecedor f
            where (:ativo is null or f.ativo = :ativo)
              and (:busca = '' or lower(f.razaoSocial) like concat('%', :busca, '%') or f.documento like concat('%', :busca, '%'))
            """)
    Page<Fornecedor> buscar(@Param("ativo") Boolean ativo, @Param("busca") String busca, Pageable pagina);
}
