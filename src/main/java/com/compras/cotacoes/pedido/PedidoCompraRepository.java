package com.compras.cotacoes.pedido;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long> {

    @EntityGraph(attributePaths = {"cotacao", "fornecedor", "produto"})
    List<PedidoCompra> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"cotacao", "fornecedor", "produto"})
    List<PedidoCompra> findByStatusOrderByIdDesc(StatusPedidoCompra status);

    @EntityGraph(attributePaths = {"cotacao", "fornecedor", "produto"})
    Optional<PedidoCompra> findComDetalhesById(Long id);

    @Query(value = "select nextval('seq_numero_pedido')", nativeQuery = true)
    long proximoNumeroPedido();
}
