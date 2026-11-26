package com.compras.cotacoes.proposta;

import static org.assertj.core.api.Assertions.assertThat;

import com.compras.cotacoes.proposta.CalculadoraPropostaService.ResultadoCalculo;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalculadoraPropostaServiceTest {

    private final CalculadoraPropostaService calculadora = new CalculadoraPropostaService();

    @ParameterizedTest(name = "{0} x {1} + frete {2} + {3}% + fixo {4} = {7}")
    @DisplayName("Regra C: mercadoria + frete + (imposto percentual + fixo), com arredondamento HALF_UP")
    @CsvSource({
            // quantidade, preço,  frete,  %,     fixo,  mercadoria, imposto, total
            "32,         24.90,  35.00,  12,    0,     796.80,     95.62,   927.42",
            "32,         23.50,  80.00,  12,    10.00, 752.00,     100.24,  932.24",
            "32,         22.90,  120.00, 12,    0,     732.80,     87.94,   940.74",
            "1.5,        10.0050, 0,     10,    0.50,  15.01,      2.00,    17.01",
            "3,          3.3333, 0,      0,     0,     10.00,      0.00,    10.00",
            "10,         7.25,   12.50,  17.5,  3.20,  72.50,      15.89,   100.89"
    })
    void calculaValorTotal(BigDecimal quantidade, BigDecimal preco, BigDecimal frete, BigDecimal percentual,
                           BigDecimal fixo, BigDecimal mercadoria, BigDecimal imposto, BigDecimal total) {
        ResultadoCalculo resultado = calculadora.calcular(quantidade, preco, frete, percentual, fixo);

        assertThat(resultado.valorMercadoria()).isEqualByComparingTo(mercadoria);
        assertThat(resultado.valorImposto()).isEqualByComparingTo(imposto);
        assertThat(resultado.valorTotal()).isEqualByComparingTo(total);
        assertThat(resultado.valorTotal().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Frete e impostos ausentes contam como zero")
    void valoresOpcionaisNulosValemZero() {
        ResultadoCalculo resultado = calculadora.calcular(new BigDecimal("4"), new BigDecimal("2.50"), null, null, null);

        assertThat(resultado.valorMercadoria()).isEqualByComparingTo("10.00");
        assertThat(resultado.valorImposto()).isEqualByComparingTo("0.00");
        assertThat(resultado.valorTotal()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("O percentual só é arredondado depois de somado ao imposto fixo")
    void arredondaSomenteNoFinal() {
        // mercadoria 33.33; 1.5% = 0.49995; + fixo 0.005 = 0.50495 -> 0.50 (arredondar antes daria 0.51)
        ResultadoCalculo resultado = calculadora.calcular(BigDecimal.ONE, new BigDecimal("33.33"),
                BigDecimal.ZERO, new BigDecimal("1.5"), new BigDecimal("0.005"));

        assertThat(resultado.valorImposto()).isEqualByComparingTo("0.50");
        assertThat(resultado.valorTotal()).isEqualByComparingTo("33.83");
    }
}
