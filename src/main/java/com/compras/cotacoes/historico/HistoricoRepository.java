package com.compras.cotacoes.historico;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoRepository extends JpaRepository<EventoHistorico, Long> {

    List<EventoHistorico> findByEntidadeAndEntidadeIdOrderByCriadoEmAscIdAsc(EntidadeHistorico entidade, Long entidadeId);
}
