package com.compras.cotacoes.fornecedor;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    boolean existsByDocumento(String documento);

    List<Fornecedor> findAllByOrderByRazaoSocialAsc();

    List<Fornecedor> findByAtivoOrderByRazaoSocialAsc(boolean ativo);
}
