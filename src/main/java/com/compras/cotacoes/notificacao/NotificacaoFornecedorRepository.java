package com.compras.cotacoes.notificacao;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoFornecedorRepository extends JpaRepository<NotificacaoFornecedor, Long> {

    boolean existsByCotacaoIdAndFornecedorIdAndTipo(Long cotacaoId, Long fornecedorId, NotificacaoFornecedor.Tipo tipo);
}
