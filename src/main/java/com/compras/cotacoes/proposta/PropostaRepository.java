package com.compras.cotacoes.proposta;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    /** Fornecedor e itens (com produto) numa consulta: o detalhe da cotação não dispara N+1. */
    @EntityGraph(attributePaths = {"fornecedor", "itens", "itens.cotacaoItem", "itens.cotacaoItem.produto"})
    List<Proposta> findByCotacaoIdOrderByIdAsc(Long cotacaoId);

    boolean existsByCotacaoIdAndFornecedorId(Long cotacaoId, Long fornecedorId);

    /** Cada linha: [cotacaoId (Long), quantidade de propostas (Long)]. */
    @Query("select p.cotacao.id, count(p) from Proposta p where p.cotacao.id in :ids group by p.cotacao.id")
    List<Object[]> contarPorCotacoes(@Param("ids") Collection<Long> cotacaoIds);
}
