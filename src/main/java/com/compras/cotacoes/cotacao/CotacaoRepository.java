package com.compras.cotacoes.cotacao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CotacaoRepository extends JpaRepository<Cotacao, Long> {

    Optional<Cotacao> findFirstByProdutoIdAndStatusInOrderByIdDesc(Long produtoId, Collection<StatusCotacao> status);

    @EntityGraph(attributePaths = "produto")
    List<Cotacao> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = "produto")
    List<Cotacao> findByStatusOrderByIdDesc(StatusCotacao status);

    /** Carrega produto e convidados numa única ida ao banco (evita N+1 no detalhe). */
    @EntityGraph(attributePaths = {"produto", "fornecedoresConvidados"})
    Optional<Cotacao> findComDetalhesById(Long id);

    @Query(value = "select nextval('seq_codigo_cotacao')", nativeQuery = true)
    long proximoNumeroCodigo();
}
