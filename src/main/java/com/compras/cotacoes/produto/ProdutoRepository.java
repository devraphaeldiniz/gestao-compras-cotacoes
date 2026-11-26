package com.compras.cotacoes.produto;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    /** Filtros opcionais; {@code busca} vazia = sem filtro de texto (evita parâmetro nulo tipado no PostgreSQL). */
    @Query("""
            select p from Produto p
            where (:apenasNecessidade = false or p.saldoAtual <= p.estoqueMinimo)
              and (:ativo is null or p.ativo = :ativo)
              and (:busca = '' or lower(p.sku) like concat('%', :busca, '%') or lower(p.nome) like concat('%', :busca, '%'))
            """)
    Page<Produto> buscar(@Param("apenasNecessidade") boolean apenasNecessidade, @Param("ativo") Boolean ativo,
                         @Param("busca") String busca, Pageable pagina);

    /**
     * SELECT ... FOR UPDATE: serializa movimentações e recebimentos do mesmo produto.
     * Sem o bloqueio, duas baixas simultâneas leem o mesmo saldo e uma delas se perde.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    Optional<Produto> buscarComBloqueio(@Param("id") Long id);

    /** Bloqueia vários produtos sempre na mesma ordem (por id), o que evita deadlock entre transações. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id in :ids order by p.id")
    List<Produto> buscarComBloqueio(@Param("ids") Collection<Long> ids);
}
