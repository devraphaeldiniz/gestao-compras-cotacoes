package com.compras.cotacoes.notificacao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envio de e-mail em texto simples. Com {@code app.email.habilitado=false} (padrão local e nos testes)
 * só registra no log o que seria enviado. Falha de SMTP é logada e não derruba o fluxo de negócio.
 */
@Slf4j
@Component
public class EnvioEmail {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean habilitado;
    private final String remetente;

    public EnvioEmail(ObjectProvider<JavaMailSender> mailSender,
                      @Value("${app.email.habilitado:false}") boolean habilitado,
                      @Value("${app.email.remetente:compras@empresa.exemplo}") String remetente) {
        this.mailSender = mailSender;
        this.habilitado = habilitado;
        this.remetente = remetente;
    }

    /** Devolve true se o e-mail foi entregue ao servidor SMTP (ou simulado, com o envio desabilitado). */
    public boolean enviar(String para, String assunto, String corpo) {
        if (para == null || para.isBlank()) {
            return false;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!habilitado || sender == null) {
            log.info("[e-mail desabilitado] para={} assunto=\"{}\"", para, assunto);
            return true;
        }
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(para);
            mensagem.setSubject(assunto);
            mensagem.setText(corpo);
            sender.send(mensagem);
            log.info("E-mail enviado para {}: \"{}\"", para, assunto);
            return true;
        } catch (MailException ex) {
            log.error("Falha ao enviar e-mail para {}: {}", para, ex.getMessage());
            return false;
        }
    }
}
