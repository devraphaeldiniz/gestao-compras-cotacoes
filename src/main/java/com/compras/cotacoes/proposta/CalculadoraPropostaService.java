package com.compras.cotacoes.proposta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Regra C, fórmula determinística do valor total:
 * <pre>
 *   mercadoria = quantidade x preço unitário                     (2 casas, HALF_UP)
 *   imposto    = mercadoria x (percentual / 100) + imposto fixo  (2 casas, HALF_UP)
 *   total      = mercadoria + frete + imposto
 * </pre>
 * O percentual não é arredondado antes da soma com o fixo, para não acumular erro de arredondamento.
 */
@Service
public class CalculadoraPropostaService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final int ESCALA_MONETARIA = 2;

    public ResultadoCalculo calcular(BigDecimal quantidade, BigDecimal precoUnitario, BigDecimal valorFrete,
                                     BigDecimal impostoPercentual, BigDecimal valorImpostoFixo) {
        BigDecimal frete = zeroSeNulo(valorFrete).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal percentual = zeroSeNulo(impostoPercentual);
        BigDecimal fixo = zeroSeNulo(valorImpostoFixo);

        BigDecimal valorMercadoria = quantidade.multiply(precoUnitario)
                .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal impostoPercentualCalculado = valorMercadoria.multiply(percentual).divide(CEM);
        BigDecimal valorImposto = impostoPercentualCalculado.add(fixo)
                .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal valorTotal = valorMercadoria.add(frete).add(valorImposto);

        return new ResultadoCalculo(valorMercadoria, valorImposto, valorTotal);
    }

    private static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public record ResultadoCalculo(BigDecimal valorMercadoria, BigDecimal valorImposto, BigDecimal valorTotal) {
    }
}
