package com.compras.cotacoes.historico.dto;

import com.compras.cotacoes.historico.AcaoHistorico;
import java.time.LocalDateTime;

public record EventoHistoricoResponse(
        AcaoHistorico acao,
        String descricao,
        String detalhe,
        String usuario,
        LocalDateTime criadoEm) {
}
