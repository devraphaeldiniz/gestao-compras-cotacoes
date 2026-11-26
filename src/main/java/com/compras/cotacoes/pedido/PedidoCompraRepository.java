package com.compras.cotacoes.pedido;

import jakarta.persistence.LockModeType;
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

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long> {

    @Query("select p from PedidoCompra p where (:status is null or p.status = :status)")
    Page<PedidoCompra> buscar(@Param("status") StatusPedidoCompra status, Pageable pagina);

    /** Itens, produtos, fornecedor e cotação de vários pedidos numa consulta (lista paginada sem N+1). */
    @EntityGraph(attributePaths = {"cotacao", "fornecedor", "itens", "itens.produto"})
    @Query("select p from PedidoCompra p where p.id in :ids")
    List<PedidoCompra> carregarCompletos(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"cotacao", "fornecedor", "itens", "itens.produto"})
    Optional<PedidoCompra> findComDetalhesById(Long id);

    /** Bloqueia o pedido: dois recebimentos simultâneos não passam juntos pela checagem de status. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PedidoCompra p where p.id = :id")
    Optional<PedidoCompra> buscarComBloqueio(@Param("id") Long id);

    @Query(value = "select nextval('seq_numero_pedido')", nativeQuery = true)
    long proximoNumeroPedido();
}
