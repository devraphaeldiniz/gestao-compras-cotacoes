package com.compras.cotacoes.cotacao;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CotacaoRepository extends JpaRepository<Cotacao, Long> {

    Optional<Cotacao> findFirstByProdutoIdAndStatusInOrderByIdDesc(Long produtoId, Collection<StatusCotacao> status);

    @EntityGraph(attributePaths = "produto")
    List<Cotacao> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = "produto")
    List<Cotacao> findByStatusOrderByIdDesc(StatusCotacao status);

    /** Carrega produto e convidados numa única ida ao banco (evita N+1 no detalhe). */
    @EntityGraph(attributePaths = {"produto", "fornecedoresConvidados"})
    Optional<Cotacao> findComDetalhesById(Long id);

    /**
     * Bloqueia a linha da cotação (sem joins: o PostgreSQL não aceita FOR UPDATE no lado opcional de um
     * outer join). Serializa abertura, registro de propostas e decisão: duas decisões simultâneas não
     * emitem dois pedidos e nenhuma proposta entra no meio de uma decisão.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cotacao c join fetch c.produto where c.id = :id")
    Optional<Cotacao> buscarComBloqueio(@Param("id") Long id);

    @Query(value = "select nextval('seq_codigo_cotacao')", nativeQuery = true)
    long proximoNumeroCodigo();
}
