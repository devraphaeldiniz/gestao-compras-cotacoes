package com.compras.cotacoes.notificacao;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.cotacao.CotacaoRepository;
import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.fornecedor.FornecedorRepository;
import com.compras.cotacoes.historico.AcaoHistorico;
import com.compras.cotacoes.historico.EntidadeHistorico;
import com.compras.cotacoes.historico.HistoricoService;
import com.compras.cotacoes.proposta.PropostaRepository;
import com.compras.cotacoes.usuario.Perfil;
import com.compras.cotacoes.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * E-mails da cotação: convite ao abrir, lembrete perto do prazo e aviso aos aprovadores quando o prazo encerra.
 * Fornecedores são brasileiros: os textos saem em português, independentemente do idioma de quem operou.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private static final Locale IDIOMA_EMAIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CotacaoRepository cotacaoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final PropostaRepository propostaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoFornecedorRepository notificacaoRepository;
    private final HistoricoService historico;
    private final EnvioEmail envioEmail;
    private final MessageSource messageSource;
    private final Clock clock;

    /** Depois do commit da abertura, fora da thread da requisição: e-mail lento não segura a resposta. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aoAbrirCotacao(CotacaoAbertaEvento evento) {
        enviarConvites(evento.cotacaoId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int enviarConvites(Long cotacaoId) {
        Cotacao cotacao = cotacaoRepository.findComDetalhesById(cotacaoId).orElse(null);
        if (cotacao == null || cotacao.getStatus() != StatusCotacao.ABERTA) {
            return 0;
        }
        int enviados = 0;
        for (Fornecedor fornecedor : destinatarios(cotacao)) {
            if (notificacaoRepository.existsByCotacaoIdAndFornecedorIdAndTipo(cotacaoId, fornecedor.getId(),
                    NotificacaoFornecedor.Tipo.CONVITE)) {
                continue;
            }
            boolean ok = envioEmail.enviar(fornecedor.getEmail(),
                    texto("email.convite.assunto", cotacao.getCodigo()),
                    texto("email.convite.corpo", fornecedor.getRazaoSocial(), cotacao.getCodigo(), listaItens(cotacao),
                            prazo(cotacao)));
            if (ok) {
                registrar(cotacao, fornecedor, NotificacaoFornecedor.Tipo.CONVITE, AcaoHistorico.CONVITE_ENVIADO);
                enviados++;
            }
        }
        return enviados;
    }

    /** Lembra apenas quem ainda não enviou proposta, uma vez por cotação. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int enviarLembretes(Long cotacaoId) {
        Cotacao cotacao = cotacaoRepository.findComDetalhesById(cotacaoId).orElse(null);
        if (cotacao == null || cotacao.getStatus() != StatusCotacao.ABERTA) {
            return 0;
        }
        Set<Long> jaResponderam = propostaRepository.findByCotacaoIdOrderByIdAsc(cotacaoId).stream()
                .map(p -> p.getFornecedor().getId())
                .collect(Collectors.toSet());
        int enviados = 0;
        for (Fornecedor fornecedor : destinatarios(cotacao)) {
            if (jaResponderam.contains(fornecedor.getId()) || notificacaoRepository.existsByCotacaoIdAndFornecedorIdAndTipo(
                    cotacaoId, fornecedor.getId(), NotificacaoFornecedor.Tipo.LEMBRETE)) {
                continue;
            }
            boolean ok = envioEmail.enviar(fornecedor.getEmail(),
                    texto("email.lembrete.assunto", cotacao.getCodigo(), prazo(cotacao)),
                    texto("email.lembrete.corpo", fornecedor.getRazaoSocial(), cotacao.getCodigo(), listaItens(cotacao),
                            prazo(cotacao)));
            if (ok) {
                registrar(cotacao, fornecedor, NotificacaoFornecedor.Tipo.LEMBRETE, AcaoHistorico.LEMBRETE_ENVIADO);
                enviados++;
            }
        }
        return enviados;
    }

    /** Avisa aprovadores (e administradores) com e-mail cadastrado que a cotação pode ser decidida. */
    public void avisarProntaParaDecisao(String codigo, int propostasValidas) {
        List<String> destinatarios = usuarioRepository
                .findByPerfilInAndAtivoTrueAndEmailIsNotNull(List.of(Perfil.APROVADOR, Perfil.ADMIN)).stream()
                .map(u -> u.getEmail())
                .toList();
        destinatarios.forEach(email -> envioEmail.enviar(email,
                texto("email.prazo.assunto", codigo),
                texto("email.prazo.corpo", codigo, propostasValidas)));
    }

    /** Convidados; se a cotação é aberta ao mercado, todos os fornecedores ativos. */
    private Collection<Fornecedor> destinatarios(Cotacao cotacao) {
        if (!cotacao.abertaAoMercado()) {
            return cotacao.getFornecedoresConvidados().stream().filter(Fornecedor::isAtivo).toList();
        }
        return fornecedorRepository.findAll().stream().filter(Fornecedor::isAtivo).toList();
    }

    private void registrar(Cotacao cotacao, Fornecedor fornecedor, NotificacaoFornecedor.Tipo tipo, AcaoHistorico acao) {
        notificacaoRepository.save(new NotificacaoFornecedor(cotacao.getId(), fornecedor.getId(), tipo, LocalDateTime.now(clock)));
        historico.registrar(EntidadeHistorico.COTACAO, cotacao.getId(), acao, fornecedor.getRazaoSocial());
    }

    private static String listaItens(Cotacao cotacao) {
        return cotacao.getItens().stream()
                .map(i -> " - %s %s: %s %s".formatted(i.getProduto().getSku(), i.getProduto().getNome(),
                        i.getQuantidade().stripTrailingZeros().toPlainString(), i.getProduto().getUnidade()))
                .collect(Collectors.joining("\n"));
    }

    private String prazo(Cotacao cotacao) {
        return cotacao.getDataLimite() == null ? texto("email.sem.prazo") : DATA.format(cotacao.getDataLimite());
    }

    private String texto(String chave, Object... argumentos) {
        return messageSource.getMessage(chave, argumentos, chave, IDIOMA_EMAIL);
    }
}
