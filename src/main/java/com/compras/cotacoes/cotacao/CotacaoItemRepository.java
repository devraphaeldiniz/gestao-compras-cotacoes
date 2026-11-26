package com.compras.cotacoes.cotacao;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CotacaoItemRepository extends JpaRepository<CotacaoItem, Long> {

    /** Cotação em andamento (RASCUNHO/ABERTA) que já contém o produto, se houver. */
    @Query("""
            select i.cotacao from CotacaoItem i
            where i.produto.id = :produtoId and i.cotacao.status in :status
            order by i.cotacao.id desc
            limit 1
            """)
    Optional<Cotacao> cotacaoEmAndamentoDoProduto(@Param("produtoId") Long produtoId,
                                                  @Param("status") Collection<StatusCotacao> status);
}
