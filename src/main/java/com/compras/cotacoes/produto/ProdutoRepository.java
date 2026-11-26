package com.compras.cotacoes.produto;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    List<Produto> findAllByOrderByNomeAsc();

    @Query("select p from Produto p where p.saldoAtual <= p.estoqueMinimo order by p.nome")
    List<Produto> buscarComNecessidadeCompra();
}
