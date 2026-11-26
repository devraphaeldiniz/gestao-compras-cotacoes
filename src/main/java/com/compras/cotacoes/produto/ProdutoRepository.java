package com.compras.cotacoes.produto;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    /**
     * SELECT ... FOR UPDATE: serializa movimentações e recebimentos do mesmo produto.
     * Sem o bloqueio, duas baixas simultâneas leem o mesmo saldo e uma delas se perde.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    Optional<Produto> buscarComBloqueio(@Param("id") Long id);

    boolean existsBySkuIgnoreCase(String sku);

    List<Produto> findAllByOrderByNomeAsc();

    @Query("select p from Produto p where p.saldoAtual <= p.estoqueMinimo order by p.nome")
    List<Produto> buscarComNecessidadeCompra();
}
