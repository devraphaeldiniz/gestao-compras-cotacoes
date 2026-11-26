package com.compras.cotacoes.historico;

import com.compras.cotacoes.historico.dto.EventoHistoricoResponse;
import com.compras.cotacoes.shared.i18n.MensagemService;
import com.compras.cotacoes.shared.seguranca.UsuarioAtual;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HistoricoService {

    private final HistoricoRepository historicoRepository;
    private final UsuarioAtual usuarioAtual;
    private final MensagemService mensagens;
    private final Clock clock;

    /** Grava na mesma transação da operação: se ela falhar, o evento também não fica. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(EntidadeHistorico entidade, Long entidadeId, AcaoHistorico acao, String detalhe) {
        historicoRepository.save(new EventoHistorico(entidade, entidadeId, acao, detalhe,
                usuarioAtual.username(), LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<EventoHistoricoResponse> listar(EntidadeHistorico entidade, Long entidadeId) {
        return historicoRepository.findByEntidadeAndEntidadeIdOrderByCriadoEmAscIdAsc(entidade, entidadeId).stream()
                .map(e -> new EventoHistoricoResponse(e.getAcao(), mensagens.obter("historico." + e.getAcao()),
                        e.getDetalhe(), e.getUsuario(), e.getCriadoEm()))
                .toList();
    }
}
