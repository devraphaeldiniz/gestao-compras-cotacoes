package com.compras.cotacoes.cotacao;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CotacaoRepository extends JpaRepository<Cotacao, Long> {

    @Query("""
            select c from Cotacao c
            where (:status is null or c.status = :status)
              and (:produtoId is null or exists (select 1 from CotacaoItem i where i.cotacao = c and i.produto.id = :produtoId))
            """)
    Page<Cotacao> buscar(@Param("status") StatusCotacao status, @Param("produtoId") Long produtoId, Pageable pagina);

    /** Itens e produtos de várias cotações numa consulta (lista paginada sem N+1). */
    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    @Query("select c from Cotacao c where c.id in :ids")
    List<Cotacao> carregarComItens(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    Optional<Cotacao> findComDetalhesById(Long id);

    /**
     * Bloqueia a linha da cotação (sem joins: o PostgreSQL não aceita FOR UPDATE no lado opcional de um
     * outer join). Serializa abertura, registro de propostas, decisão e cancelamento.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cotacao c where c.id = :id")
    Optional<Cotacao> buscarComBloqueio(@Param("id") Long id);

    /** Rascunho automático mais recente: a reposição consolida novos itens nele em vez de abrir outra cotação. */
    Optional<Cotacao> findFirstByStatusAndOrigemOrderByIdDesc(StatusCotacao status, OrigemCotacao origem);

    @Query("select c.id from Cotacao c where c.status = :status and c.dataLimite < :hoje and c.prazoEncerradoEm is null")
    List<Long> idsComPrazoVencido(@Param("status") StatusCotacao status, @Param("hoje") LocalDate hoje);

    @Query("select c.id from Cotacao c where c.status = :status and c.dataLimite between :hoje and :ate")
    List<Long> idsComPrazoProximo(@Param("status") StatusCotacao status, @Param("hoje") LocalDate hoje,
                                  @Param("ate") LocalDate ate);

    @Query(value = "select nextval('seq_codigo_cotacao')", nativeQuery = true)
    long proximoNumeroCodigo();
}
