package com.compras.cotacoes.shared.validacao;

import static org.assertj.core.api.Assertions.assertThat;

import com.compras.cotacoes.support.Fabrica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentoValidoValidatorTest {

    private final DocumentoValidoValidator validador = new DocumentoValidoValidator();

    @ParameterizedTest
    @DisplayName("Aceita CPF e CNPJ com dígitos verificadores corretos, com ou sem pontuação")
    @ValueSource(strings = {"11.222.333/0001-81", "11222333000181", "529.982.247-25", "52998224725"})
    void aceitaDocumentosValidos(String documento) {
        assertThat(validador.isValid(documento, null)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Recusa dígito verificador errado, sequência repetida e formato inválido")
    @ValueSource(strings = {"11.222.333/0001-82", "52998224724", "11111111111", "00000000000000", "1234", "11.222.333-0001/81"})
    void recusaDocumentosInvalidos(String documento) {
        assertThat(validador.isValid(documento, null)).isFalse();
    }

    @Test
    @DisplayName("Vazio fica a cargo do @NotBlank; a fábrica de testes gera CNPJs válidos")
    void vazioEFabrica() {
        assertThat(validador.isValid(null, null)).isTrue();
        assertThat(validador.isValid(Fabrica.cnpj(42), null)).isTrue();
        assertThat(validador.isValid(Fabrica.cnpj(43), null)).isTrue();
    }
}
