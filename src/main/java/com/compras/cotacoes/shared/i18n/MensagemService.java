package com.compras.cotacoes.shared.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/** Resolve chaves de mensagem no idioma da requisição atual (Accept-Language ou ?lang=). */
@Service
@RequiredArgsConstructor
public class MensagemService {

    private final MessageSource messageSource;

    public String obter(String chave, Object... argumentos) {
        return messageSource.getMessage(chave, argumentos, chave, LocaleContextHolder.getLocale());
    }
}
