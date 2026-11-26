package com.compras.cotacoes.proposta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Regra C, fórmula determinística do valor total da proposta:
 * <pre>
 *   mercadoria(item) = quantidade x preço unitário                  (2 casas, HALF_UP)
 *   mercadoria       = soma das mercadorias dos itens
 *   imposto          = mercadoria x (percentual / 100) + imposto fixo (2 casas, HALF_UP)
 *   total            = mercadoria + frete + imposto
 * </pre>
 * O percentual não é arredondado antes da soma com o fixo, para não acumular erro de arredondamento.
 */
@Service
public class CalculadoraPropostaService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final int ESCALA_MONETARIA = 2;

    public record Linha(BigDecimal quantidade, BigDecimal precoUnitario) {
    }

    public record ResultadoCalculo(List<BigDecimal> mercadoriaPorItem, BigDecimal valorMercadoria,
                                   BigDecimal valorImposto, BigDecimal valorTotal) {
    }

    public ResultadoCalculo calcular(List<Linha> linhas, BigDecimal valorFrete, BigDecimal impostoPercentual,
                                     BigDecimal valorImpostoFixo) {
        List<BigDecimal> porItem = linhas.stream()
                .map(l -> l.quantidade().multiply(l.precoUnitario()).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP))
                .toList();
        BigDecimal valorMercadoria = porItem.stream().reduce(BigDecimal.ZERO.setScale(ESCALA_MONETARIA), BigDecimal::add);
        BigDecimal frete = zeroSeNulo(valorFrete).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal impostoPercentualCalculado = valorMercadoria.multiply(zeroSeNulo(impostoPercentual)).divide(CEM);
        BigDecimal valorImposto = impostoPercentualCalculado.add(zeroSeNulo(valorImpostoFixo))
                .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal valorTotal = valorMercadoria.add(frete).add(valorImposto);
        return new ResultadoCalculo(porItem, valorMercadoria, valorImposto, valorTotal);
    }

    /** Atalho para proposta de um único item. */
    public ResultadoCalculo calcular(BigDecimal quantidade, BigDecimal precoUnitario, BigDecimal valorFrete,
                                     BigDecimal impostoPercentual, BigDecimal valorImpostoFixo) {
        return calcular(List.of(new Linha(quantidade, precoUnitario)), valorFrete, impostoPercentual, valorImpostoFixo);
    }

    private static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
