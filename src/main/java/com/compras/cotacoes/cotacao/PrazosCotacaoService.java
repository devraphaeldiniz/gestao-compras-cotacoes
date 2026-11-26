package com.compras.cotacoes.cotacao;

import com.compras.cotacoes.historico.AcaoHistorico;
import com.compras.cotacoes.historico.EntidadeHistorico;
import com.compras.cotacoes.historico.HistoricoService;
import com.compras.cotacoes.notificacao.NotificacaoService;
import com.compras.cotacoes.proposta.AvaliadorPropostasService;
import com.compras.cotacoes.proposta.PropostaRepository;
import com.compras.cotacoes.proposta.PropostaService;
import com.compras.cotacoes.shared.i18n.MensagemService;
import com.compras.cotacoes.shared.metricas.MetricasCompras;
import com.compras.cotacoes.shared.seguranca.UsuarioAtual;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Prazos automáticos das cotações ABERTAS (executado pelo {@link PrazosCotacaoAgendador}):
 * <ul>
 *   <li>prazo vencido e menos de 3 propostas válidas: cancela automaticamente;</li>
 *   <li>prazo vencido com 3+ propostas válidas: marca "prazo encerrado" e avisa os aprovadores;</li>
 *   <li>prazo nos próximos 2 dias: lembra os fornecedores que ainda não responderam.</li>
 * </ul>
 * Cada cotação é processada na própria transação e com bloqueio de linha: uma falha não afeta as outras
 * e a decisão manual concorrente espera o job terminar.
 */
@Slf4j
@Service
public class PrazosCotacaoService {

    static final int DIAS_ANTECEDENCIA_LEMBRETE = 2;

    private final CotacaoRepository cotacaoRepository;
    private final PropostaRepository propostaRepository;
    private final CotacaoService cotacaoService;
    private final AvaliadorPropostasService avaliador;
    private final NotificacaoService notificacoes;
    private final HistoricoService historico;
    private final MetricasCompras metricas;
    private final MensagemService mensagens;
    private final TransactionTemplate transacao;
    private final Clock clock;

    public PrazosCotacaoService(CotacaoRepository cotacaoRepository, PropostaRepository propostaRepository,
                                CotacaoService cotacaoService, AvaliadorPropostasService avaliador,
                                NotificacaoService notificacoes, HistoricoService historico, MetricasCompras metricas,
                                MensagemService mensagens, PlatformTransactionManager transactionManager, Clock clock) {
        this.cotacaoRepository = cotacaoRepository;
        this.propostaRepository = propostaRepository;
        this.cotacaoService = cotacaoService;
        this.avaliador = avaliador;
        this.notificacoes = notificacoes;
        this.historico = historico;
        this.metricas = metricas;
        this.mensagens = mensagens;
        this.transacao = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public record Resultado(int canceladas, int prontasParaDecisao, int lembretesEnviados) {
    }

    public Resultado processar() {
        LocalDate hoje = LocalDate.now(clock);
        int canceladas = 0;
        int prontas = 0;
        for (Long id : cotacaoRepository.idsComPrazoVencido(StatusCotacao.ABERTA, hoje)) {
            try {
                Situacao situacao = transacao.execute(status -> encerrarPrazo(id, hoje));
                if (situacao == Situacao.CANCELADA) {
                    canceladas++;
                } else if (situacao == Situacao.PRONTA) {
                    prontas++;
                }
            } catch (RuntimeException ex) {
                log.error("Falha ao processar o prazo da cotação {}", id, ex);
            }
        }
        int lembretes = 0;
        for (Long id : cotacaoRepository.idsComPrazoProximo(StatusCotacao.ABERTA, hoje, hoje.plusDays(DIAS_ANTECEDENCIA_LEMBRETE))) {
            lembretes += notificacoes.enviarLembretes(id);
        }
        if (canceladas + prontas + lembretes > 0) {
            log.info("Prazos de cotação: {} canceladas, {} prontas para decisão, {} lembretes", canceladas, prontas, lembretes);
        }
        return new Resultado(canceladas, prontas, lembretes);
    }

    private enum Situacao { CANCELADA, PRONTA, IGNORADA }

    private Situacao encerrarPrazo(Long id, LocalDate hoje) {
        Cotacao cotacao = cotacaoRepository.buscarComBloqueio(id).orElse(null);
        // Revalida depois do bloqueio: a cotação pode ter sido decidida ou cancelada enquanto o job esperava
        if (cotacao == null || cotacao.getStatus() != StatusCotacao.ABERTA || cotacao.getDataLimite() == null
                || !cotacao.getDataLimite().isBefore(hoje) || cotacao.getPrazoEncerradoEm() != null) {
            return Situacao.IGNORADA;
        }
        int validas = avaliador.filtrarElegiveis(propostaRepository.findByCotacaoIdOrderByIdAsc(id), hoje).size();
        if (validas < PropostaService.MINIMO_PROPOSTAS_VALIDAS) {
            cotacaoService.aplicarCancelamento(cotacao,
                    mensagens.obter("cotacao.cancelamento.automatico", validas, PropostaService.MINIMO_PROPOSTAS_VALIDAS),
                    UsuarioAtual.SISTEMA);
            metricas.cotacaoCancelada("prazo");
            return Situacao.CANCELADA;
        }
        cotacao.setPrazoEncerradoEm(LocalDateTime.now(clock));
        cotacaoRepository.save(cotacao);
        historico.registrar(EntidadeHistorico.COTACAO, id, AcaoHistorico.PRAZO_ENCERRADO, "propostasValidas=" + validas);
        notificacoes.avisarProntaParaDecisao(cotacao.getCodigo(), validas);
        return Situacao.PRONTA;
    }
}
