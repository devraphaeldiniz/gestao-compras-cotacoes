package com.compras.cotacoes.pedido;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.pedido.dto.PedidoCompraResponse;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.proposta.Proposta;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoCompraService {

    private static final int ESCALA_CUSTO = 4;

    private final PedidoCompraRepository pedidoCompraRepository;
    private final PedidoCompraMapper pedidoCompraMapper;
    private final Clock clock;

    /** Chamado pela decisão da cotação, dentro da mesma transação: copia os valores consolidados da vencedora. */
    @Transactional
    public PedidoCompra emitir(Cotacao cotacao, Proposta vencedora) {
        PedidoCompra pedido = new PedidoCompra();
        pedido.setNumeroPedido(PedidoCompra.gerarNumero(
                Year.now(clock).getValue(), pedidoCompraRepository.proximoNumeroPedido()));
        pedido.setCotacao(cotacao);
        pedido.setProposta(vencedora);
        pedido.setFornecedor(vencedora.getFornecedor());
        pedido.setProduto(cotacao.getProduto());
        pedido.setQuantidade(cotacao.getQuantidade());
        pedido.setValorUnitario(vencedora.getPrecoUnitario());
        pedido.setValorFrete(vencedora.getValorFrete());
        pedido.setValorImposto(vencedora.getValorImpostoCalculado());
        pedido.setValorTotal(vencedora.getValorTotal());
        pedido.setPrazoEntregaDias(vencedora.getPrazoEntregaDias());
        pedido.setStatus(StatusPedidoCompra.EMITIDO);
        return pedidoCompraRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoCompraResponse> listar(StatusPedidoCompra status) {
        List<PedidoCompra> pedidos = status == null
                ? pedidoCompraRepository.findAllByOrderByIdDesc()
                : pedidoCompraRepository.findByStatusOrderByIdDesc(status);
        return pedidoCompraMapper.paraRespostas(pedidos);
    }

    /**
     * Regra E: recebe o pedido e, na mesma transação, dá entrada no estoque e recalcula o custo.
     * O custo unitário efetivo inclui frete e impostos (valor total / quantidade).
     */
    @Transactional
    public PedidoCompraResponse receber(Long pedidoId) {
        PedidoCompra pedido = pedidoCompraRepository.findComDetalhesById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("pedido.nao.encontrado", pedidoId));
        if (pedido.getStatus() != StatusPedidoCompra.EMITIDO) {
            throw new NegocioException("pedido.status.invalido.receber", pedido.getNumeroPedido(), pedido.getStatus());
        }

        pedido.setStatus(StatusPedidoCompra.RECEBIDO);
        pedido.setDataRecebimento(LocalDateTime.now(clock));
        atualizarEstoqueECusto(pedido.getProduto(), pedido.getQuantidade(), pedido.getValorTotal());

        PedidoCompra salvo = pedidoCompraRepository.save(pedido);
        log.info("Pedido {} recebido; saldo do produto {} agora é {}",
                salvo.getNumeroPedido(), salvo.getProduto().getSku(), salvo.getProduto().getSaldoAtual());
        return pedidoCompraMapper.paraResposta(salvo);
    }

    static void atualizarEstoqueECusto(Produto produto, BigDecimal quantidade, BigDecimal valorTotal) {
        BigDecimal saldoAnterior = produto.getSaldoAtual();
        BigDecimal novoSaldo = saldoAnterior.add(quantidade);
        BigDecimal custoUnitarioEfetivo = valorTotal.divide(quantidade, ESCALA_CUSTO, RoundingMode.HALF_UP);

        BigDecimal precoMedioAnterior = produto.getPrecoMedioCompra();
        BigDecimal novoPrecoMedio;
        if (precoMedioAnterior == null || saldoAnterior.signum() == 0) {
            novoPrecoMedio = custoUnitarioEfetivo;
        } else {
            BigDecimal valorEstoqueAnterior = saldoAnterior.multiply(precoMedioAnterior);
            BigDecimal valorEntrada = quantidade.multiply(custoUnitarioEfetivo);
            novoPrecoMedio = valorEstoqueAnterior.add(valorEntrada).divide(novoSaldo, ESCALA_CUSTO, RoundingMode.HALF_UP);
        }

        produto.setSaldoAtual(novoSaldo);
        produto.setUltimoPrecoCompra(custoUnitarioEfetivo);
        produto.setPrecoMedioCompra(novoPrecoMedio);
    }
}
