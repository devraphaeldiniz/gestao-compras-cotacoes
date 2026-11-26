package com.compras.cotacoes.shared.validacao;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class DocumentoValidoValidator implements ConstraintValidator<DocumentoValido, String> {

    private static final Pattern FORMATO = Pattern.compile(
            "^(\\d{11}|\\d{14}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})$");

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null || valor.isBlank()) {
            return true; // obrigatoriedade é papel do @NotBlank
        }
        if (!FORMATO.matcher(valor).matches()) {
            return false;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.length() == 11 ? cpfValido(digitos) : cnpjValido(digitos);
    }

    static boolean cpfValido(String cpf) {
        if (cpf.chars().distinct().count() == 1) {
            return false; // 111.111.111-11 passa no cálculo, mas não é um CPF real
        }
        return digito(cpf, 9, 10) == cpf.charAt(9) - '0' && digito(cpf, 10, 11) == cpf.charAt(10) - '0';
    }

    static boolean cnpjValido(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return digitoCnpj(cnpj, pesos1) == cnpj.charAt(12) - '0' && digitoCnpj(cnpj, pesos2) == cnpj.charAt(13) - '0';
    }

    private static int digito(String cpf, int tamanho, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (cpf.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    private static int digitoCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
