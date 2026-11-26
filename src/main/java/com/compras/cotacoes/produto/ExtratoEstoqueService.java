package com.compras.cotacoes.produto;

import com.compras.cotacoes.produto.dto.MovimentacaoEstoqueResponse;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import com.compras.cotacoes.shared.seguranca.UsuarioAtual;
import com.compras.cotacoes.shared.web.PaginaResponse;
import com.compras.cotacoes.shared.web.Paginacao;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Toda alteração de saldo passa por aqui e vira uma linha do extrato, com o autor e o saldo antes/depois. */
@Service
@RequiredArgsConstructor
public class ExtratoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    /** Aplica o novo saldo no produto e registra a movimentação (a transação é a da operação que chamou). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Produto produto, TipoMovimentacaoEstoque tipo, BigDecimal quantidade, BigDecimal novoSaldo,
                          BigDecimal custoUnitario, String referencia, String observacao) {
        BigDecimal saldoAnterior = produto.getSaldoAtual();
        produto.setSaldoAtual(novoSaldo);
        movimentacaoRepository.save(MovimentacaoEstoque.builder()
                .produto(produto)
                .tipo(tipo)
                .quantidade(quantidade)
                .saldoAnterior(saldoAnterior)
                .saldoPosterior(novoSaldo)
                .custoUnitario(custoUnitario)
                .referencia(referencia)
                .observacao(observacao)
                .usuario(usuarioAtual.username())
                .criadoEm(LocalDateTime.now(clock))
                .build());
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MovimentacaoEstoqueResponse> extrato(Long produtoId, Integer pagina, Integer tamanho) {
        if (!produtoRepository.existsById(produtoId)) {
            throw new RecursoNaoEncontradoException("produto.nao.encontrado", produtoId);
        }
        Sort maisRecentes = Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id"));
        return PaginaResponse.de(movimentacaoRepository.findByProdutoId(produtoId, Paginacao.de(pagina, tamanho, maisRecentes)),
                m -> new MovimentacaoEstoqueResponse(m.getId(), m.getTipo(), m.getQuantidade(), m.getSaldoAnterior(),
                        m.getSaldoPosterior(), m.getCustoUnitario(), m.getReferencia(), m.getObservacao(),
                        m.getUsuario(), m.getCriadoEm()));
    }
}
