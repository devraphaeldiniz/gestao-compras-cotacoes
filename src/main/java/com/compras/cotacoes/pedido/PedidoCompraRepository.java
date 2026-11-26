package com.compras.cotacoes.pedido;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long> {

    @EntityGraph(attributePaths = {"cotacao", "fornecedor", "produto"})
    List<PedidoCompra> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"cotacao", "fornecedor", "produto"})
    List<PedidoCompra> findByStatusOrderByIdDesc(StatusPedidoCompra status);

    /** Bloqueia o pedido: dois recebimentos simultâneos não passam juntos pela checagem de status EMITIDO. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PedidoCompra p where p.id = :id")
    Optional<PedidoCompra> buscarComBloqueio(@Param("id") Long id);

    @Query(value = "select nextval('seq_numero_pedido')", nativeQuery = true)
    long proximoNumeroPedido();
}
