package com.compras.cotacoes.pedido;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.historico.AcaoHistorico;
import com.compras.cotacoes.historico.EntidadeHistorico;
import com.compras.cotacoes.historico.HistoricoService;
import com.compras.cotacoes.historico.dto.EventoHistoricoResponse;
import com.compras.cotacoes.pedido.dto.PedidoCompraResponse;
import com.compras.cotacoes.pedido.dto.ReceberPedidoRequest;
import com.compras.cotacoes.produto.ExtratoEstoqueService;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.produto.ProdutoRepository;
import com.compras.cotacoes.produto.TipoMovimentacaoEstoque;
import com.compras.cotacoes.proposta.Proposta;
import com.compras.cotacoes.proposta.PropostaItem;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import com.compras.cotacoes.shared.metricas.MetricasCompras;
import com.compras.cotacoes.shared.seguranca.UsuarioAtual;
import com.compras.cotacoes.shared.web.PaginaResponse;
import com.compras.cotacoes.shared.web.Paginacao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoCompraService {

    private static final int ESCALA_CUSTO = 4;
    private static final int ESCALA_INTERMEDIARIA = 10;
    private static final Set<StatusPedidoCompra> RECEBIVEIS =
            EnumSet.of(StatusPedidoCompra.EMITIDO, StatusPedidoCompra.PARCIALMENTE_RECEBIDO);

    private final PedidoCompraRepository pedidoCompraRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoCompraMapper pedidoCompraMapper;
    private final ExtratoEstoqueService extratoEstoque;
    private final HistoricoService historico;
    private final MetricasCompras metricas;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    /**
     * Chamado pela decisão da cotação, na mesma transação. Copia os valores da vencedora e calcula o custo
     * efetivo de cada item: preço + rateio de frete e impostos proporcional ao valor da mercadoria do item.
     */
    @Transactional
    public PedidoCompra emitir(Cotacao cotacao, Proposta vencedora) {
        PedidoCompra pedido = new PedidoCompra();
        pedido.setNumeroPedido(PedidoCompra.gerarNumero(Year.now(clock).getValue(), pedidoCompraRepository.proximoNumeroPedido()));
        pedido.setCotacao(cotacao);
        pedido.setProposta(vencedora);
        pedido.setFornecedor(vencedora.getFornecedor());
        pedido.setValorMercadoria(vencedora.getValorMercadoria());
        pedido.setValorFrete(vencedora.getValorFrete());
        pedido.setValorImposto(vencedora.getValorImpostoCalculado());
        pedido.setValorTotal(vencedora.getValorTotal());
        pedido.setPrazoEntregaDias(vencedora.getPrazoEntregaDias());
        pedido.setStatus(StatusPedidoCompra.EMITIDO);
        pedido.setEmitidoPor(usuarioAtual.username());

        BigDecimal custosAdicionais = vencedora.getValorFrete().add(vencedora.getValorImpostoCalculado());
        for (PropostaItem item : vencedora.getItens()) {
            BigDecimal quantidade = item.getCotacaoItem().getQuantidade();
            BigDecimal custoEfetivo = custoUnitarioEfetivo(item.getValorMercadoria(), vencedora.getValorMercadoria(),
                    custosAdicionais, quantidade);
            pedido.adicionarItem(item.getCotacaoItem().getProduto(), quantidade, item.getPrecoUnitario(), custoEfetivo);
        }

        PedidoCompra salvo = pedidoCompraRepository.save(pedido);
        historico.registrar(EntidadeHistorico.PEDIDO, salvo.getId(), AcaoHistorico.PEDIDO_EMITIDO,
                "%s %s total=%s".formatted(cotacao.getCodigo(), vencedora.getFornecedor().getRazaoSocial(),
                        salvo.getValorTotal().toPlainString()));
        metricas.pedidoEmitido(salvo.getValorTotal());
        return salvo;
    }

    /** (mercadoria do item + parcela proporcional de frete e impostos) / quantidade, 4 casas HALF_UP. */
    static BigDecimal custoUnitarioEfetivo(BigDecimal mercadoriaItem, BigDecimal mercadoriaTotal,
                                           BigDecimal custosAdicionais, BigDecimal quantidade) {
        BigDecimal rateio = mercadoriaTotal.signum() == 0 ? BigDecimal.ZERO
                : custosAdicionais.multiply(mercadoriaItem).divide(mercadoriaTotal, ESCALA_INTERMEDIARIA, RoundingMode.HALF_UP);
        return mercadoriaItem.add(rateio).divide(quantidade, ESCALA_CUSTO, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PedidoCompraResponse> listar(StatusPedidoCompra status, Integer pagina, Integer tamanho) {
        Page<PedidoCompra> paginaPedidos = pedidoCompraRepository.buscar(status,
                Paginacao.de(pagina, tamanho, Sort.by(Sort.Order.desc("id"))));
        List<Long> ids = paginaPedidos.getContent().stream().map(PedidoCompra::getId).toList();
        Map<Long, PedidoCompra> completos = ids.isEmpty() ? Map.of() : pedidoCompraRepository.carregarCompletos(ids).stream()
                .collect(Collectors.toMap(PedidoCompra::getId, Function.identity()));
        return PaginaResponse.de(paginaPedidos, p -> pedidoCompraMapper.paraResposta(completos.getOrDefault(p.getId(), p)));
    }

    @Transactional(readOnly = true)
    public PedidoCompraResponse buscar(Long id) {
        return pedidoCompraMapper.paraResposta(pedidoCompraRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("pedido.nao.encontrado", id)));
    }

    /**
     * Regra E com recebimento parcial: soma ao estoque o que chegou, recalcula último custo e preço médio
     * ponderado (com o custo efetivo do item) e só marca RECEBIDO quando todos os itens chegaram por completo.
     */
    @Transactional
    public PedidoCompraResponse receber(Long pedidoId, ReceberPedidoRequest request) {
        // Ordem dos bloqueios: pedido, depois produtos por id (a mesma de qualquer outro fluxo que toque o produto)
        PedidoCompra pedido = pedidoCompraRepository.buscarComBloqueio(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("pedido.nao.encontrado", pedidoId));
        if (!RECEBIVEIS.contains(pedido.getStatus())) {
            throw new NegocioException("pedido.status.invalido.receber", pedido.getNumeroPedido(), pedido.getStatus());
        }

        Map<PedidoItem, BigDecimal> recebimento = quantidadesAReceber(pedido, request);
        Map<Long, Produto> produtos = produtoRepository.buscarComBloqueio(
                        recebimento.keySet().stream().map(i -> i.getProduto().getId()).toList()).stream()
                .collect(Collectors.toMap(Produto::getId, Function.identity()));

        StringBuilder detalhe = new StringBuilder();
        recebimento.forEach((item, quantidade) -> {
            Produto produto = produtos.get(item.getProduto().getId());
            BigDecimal novoSaldo = atualizarCusto(produto, quantidade, item.getCustoUnitarioEfetivo());
            extratoEstoque.registrar(produto, TipoMovimentacaoEstoque.RECEBIMENTO, quantidade, novoSaldo,
                    item.getCustoUnitarioEfetivo(), pedido.getNumeroPedido(), null);
            item.registrarRecebimento(quantidade);
            detalhe.append(produto.getSku()).append(" x ").append(quantidade.stripTrailingZeros().toPlainString()).append("; ");
        });

        boolean completo = pedido.totalmenteRecebido();
        pedido.setStatus(completo ? StatusPedidoCompra.RECEBIDO : StatusPedidoCompra.PARCIALMENTE_RECEBIDO);
        if (completo) {
            pedido.setDataRecebimento(LocalDateTime.now(clock));
        }
        PedidoCompra salvo = pedidoCompraRepository.save(pedido);
        historico.registrar(EntidadeHistorico.PEDIDO, pedidoId, AcaoHistorico.PEDIDO_RECEBIMENTO,
                detalhe.toString().replaceAll("; $", "") + (completo ? " (completo)" : " (parcial)"));
        metricas.recebimento(completo);
        log.info("Pedido {} recebido por {} ({})", salvo.getNumeroPedido(), usuarioAtual.username(), salvo.getStatus());
        return pedidoCompraMapper.paraResposta(salvo);
    }

    /** Cancela o que ainda não foi recebido; o que já entrou no estoque permanece. */
    @Transactional
    public PedidoCompraResponse cancelar(Long pedidoId, String motivo) {
        PedidoCompra pedido = pedidoCompraRepository.buscarComBloqueio(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("pedido.nao.encontrado", pedidoId));
        if (!RECEBIVEIS.contains(pedido.getStatus())) {
            throw new NegocioException("pedido.status.invalido.cancelar", pedido.getNumeroPedido(), pedido.getStatus());
        }
        pedido.setStatus(StatusPedidoCompra.CANCELADO);
        pedido.setMotivoCancelamento(motivo.trim());
        pedido.setCanceladoPor(usuarioAtual.username());
        pedido.setCanceladoEm(LocalDateTime.now(clock));
        PedidoCompra salvo = pedidoCompraRepository.save(pedido);
        historico.registrar(EntidadeHistorico.PEDIDO, pedidoId, AcaoHistorico.PEDIDO_CANCELADO, motivo.trim());
        metricas.pedidoCancelado();
        return pedidoCompraMapper.paraResposta(salvo);
    }

    @Transactional(readOnly = true)
    public List<EventoHistoricoResponse> historico(Long pedidoId) {
        if (!pedidoCompraRepository.existsById(pedidoId)) {
            throw new RecursoNaoEncontradoException("pedido.nao.encontrado", pedidoId);
        }
        return historico.listar(EntidadeHistorico.PEDIDO, pedidoId);
    }

    private static Map<PedidoItem, BigDecimal> quantidadesAReceber(PedidoCompra pedido, ReceberPedidoRequest request) {
        Map<PedidoItem, BigDecimal> resultado = new LinkedHashMap<>();
        if (request == null || request.itens() == null || request.itens().isEmpty()) {
            pedido.getItens().stream()
                    .filter(i -> !i.totalmenteRecebido())
                    .forEach(i -> resultado.put(i, i.quantidadePendente()));
            return resultado;
        }
        Map<Long, PedidoItem> porProduto = pedido.getItens().stream()
                .collect(Collectors.toMap(i -> i.getProduto().getId(), Function.identity()));
        for (ReceberPedidoRequest.Item item : request.itens()) {
            PedidoItem pedidoItem = porProduto.get(item.produtoId());
            if (pedidoItem == null) {
                throw new NegocioException("pedido.item.invalido", item.produtoId(), pedido.getNumeroPedido());
            }
            BigDecimal acumulado = resultado.getOrDefault(pedidoItem, BigDecimal.ZERO).add(item.quantidade());
            if (acumulado.compareTo(pedidoItem.quantidadePendente()) > 0) {
                throw new NegocioException("pedido.quantidade.excede", pedidoItem.getProduto().getSku(),
                        pedidoItem.quantidadePendente(), acumulado);
            }
            resultado.put(pedidoItem, acumulado);
        }
        return resultado;
    }

    /**
     * Atualiza último custo e preço médio ponderado; devolve o novo saldo (aplicado pelo extrato).
     * Sem preço médio anterior ou com saldo zero, o médio passa a ser o custo do recebimento.
     */
    static BigDecimal atualizarCusto(Produto produto, BigDecimal quantidade, BigDecimal custoUnitario) {
        BigDecimal saldoAnterior = produto.getSaldoAtual();
        BigDecimal novoSaldo = saldoAnterior.add(quantidade);
        BigDecimal medioAnterior = produto.getPrecoMedioCompra();
        BigDecimal novoMedio;
        if (medioAnterior == null || saldoAnterior.signum() == 0) {
            novoMedio = custoUnitario;
        } else {
            novoMedio = saldoAnterior.multiply(medioAnterior).add(quantidade.multiply(custoUnitario))
                    .divide(novoSaldo, ESCALA_CUSTO, RoundingMode.HALF_UP);
        }
        produto.setUltimoPrecoCompra(custoUnitario);
        produto.setPrecoMedioCompra(novoMedio);
        return novoSaldo;
    }
}
