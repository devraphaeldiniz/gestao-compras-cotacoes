package com.compras.cotacoes.proposta.dto;

import com.compras.cotacoes.proposta.StatusProposta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PropostaResponse(
        Long id,
        Long cotacaoId,
        Long fornecedorId,
        String fornecedorRazaoSocial,
        boolean fornecedorAtivo,
        List<PropostaItemResponse> itens,
        BigDecimal valorMercadoria,
        BigDecimal valorFrete,
        BigDecimal impostoPercentual,
        BigDecimal valorImpostoFixo,
        BigDecimal valorImpostoCalculado,
        BigDecimal valorTotal,
        Integer prazoEntregaDias,
        LocalDate dataValidade,
        StatusProposta status,
        BigDecimal pontuacaoCalculada,
        String justificativaEscolha,
        LocalDateTime criadoEm) {
}
