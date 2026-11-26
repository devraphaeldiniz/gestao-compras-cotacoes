package com.compras.cotacoes.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.historico.HistoricoService;
import com.compras.cotacoes.pedido.dto.PedidoCompraResponse;
import com.compras.cotacoes.pedido.dto.ReceberPedidoRequest;
import com.compras.cotacoes.produto.ExtratoEstoqueService;
import com.compras.cotacoes.produto.MovimentacaoEstoque;
import com.compras.cotacoes.produto.MovimentacaoEstoqueRepository;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.produto.ProdutoRepository;
import com.compras.cotacoes.produto.TipoMovimentacaoEstoque;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import com.compras.cotacoes.shared.seguranca.UsuarioAtual;
import com.compras.cotacoes.support.Fabrica;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PedidoCompraServiceTest {

    @Mock
    private PedidoCompraRepository pedidoCompraRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;
    @Mock
    private HistoricoService historico;

    private PedidoCompraService service;
    private final List<Produto> produtos = new ArrayList<>();

    @BeforeEach
    void configurar() {
        UsuarioAtual usuarioAtual = new UsuarioAtual();
        service = new PedidoCompraService(pedidoCompraRepository, produtoRepository,
                Mappers.getMapper(PedidoCompraMapper.class),
                new ExtratoEstoqueService(movimentacaoRepository, produtoRepository, usuarioAtual, Fabrica.RELOGIO),
                historico, Fabrica.metricas(), usuarioAtual, Fabrica.RELOGIO);
        lenient().when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(produtoRepository.buscarComBloqueio(anyCollection())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return produtos.stream().filter(p -> ids.contains(p.getId())).toList();
        });
    }

    @Test
    @DisplayName("Regra E: recebimento total soma ao saldo, recalcula custos, registra no extrato e fecha o pedido")
    void recebimentoTotal() {
        Produto produto = produto(1L, "PAP-A4", "10", "20.0000");
        PedidoCompra pedido = pedido(StatusPedidoCompra.EMITIDO);
        pedido.adicionarItem(produto, new BigDecimal("30"), new BigDecimal("24"), new BigDecimal("25.0000"));
        when(pedidoCompraRepository.buscarComBloqueio(50L)).thenReturn(Optional.of(pedido));

        PedidoCompraResponse resposta = service.receber(50L, null);

        // médio = (10 x 20 + 30 x 25) / 40 = 23.75
        assertThat(produto.getSaldoAtual()).isEqualByComparingTo("40");
        assertThat(produto.getUltimoPrecoCompra()).isEqualByComparingTo("25.0000");
        assertThat(produto.getPrecoMedioCompra()).isEqualByComparingTo("23.7500");
        assertThat(resposta.status()).isEqualTo(StatusPedidoCompra.RECEBIDO);
        assertThat(resposta.dataRecebimento()).isEqualTo(LocalDateTime.now(Fabrica.RELOGIO));
        assertThat(resposta.itens().getFirst().quantidadePendente()).isEqualByComparingTo("0");

        ArgumentCaptor<MovimentacaoEstoque> movimento = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).save(movimento.capture());
        assertThat(movimento.getValue().getTipo()).isEqualTo(TipoMovimentacaoEstoque.RECEBIMENTO);
        assertThat(movimento.getValue().getReferencia()).isEqualTo("PC-2026-00050");
        assertThat(movimento.getValue().getCustoUnitario()).isEqualByComparingTo("25.0000");
    }

    @Test
    @DisplayName("Recebimento parcial: entra só o que chegou e o pedido fica PARCIALMENTE_RECEBIDO até completar")
    void recebimentoParcial() {
        Produto produto = produto(1L, "PAP-A4", "0", null);
        PedidoCompra pedido = pedido(StatusPedidoCompra.EMITIDO);
        pedido.adicionarItem(produto, new BigDecimal("30"), new BigDecimal("10"), new BigDecimal("10.0000"));
        when(pedidoCompraRepository.buscarComBloqueio(50L)).thenReturn(Optional.of(pedido));

        PedidoCompraResponse parcial = service.receber(50L, receber(1L, "12"));

        assertThat(parcial.status()).isEqualTo(StatusPedidoCompra.PARCIALMENTE_RECEBIDO);
        assertThat(parcial.dataRecebimento()).isNull();
        assertThat(parcial.itens().getFirst().quantidadeRecebida()).isEqualByComparingTo("12");
        assertThat(parcial.itens().getFirst().quantidadePendente()).isEqualByComparingTo("18");
        assertThat(produto.getSaldoAtual()).isEqualByComparingTo("12");

        PedidoCompraResponse resto = service.receber(50L, null);

        assertThat(resto.status()).isEqualTo(StatusPedidoCompra.RECEBIDO);
        assertThat(produto.getSaldoAtual()).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("Receber mais que o pendente ou produto fora do pedido é recusado")
    void recebimentoInvalido() {
        PedidoCompra pedido = pedido(StatusPedidoCompra.EMITIDO);
        pedido.adicionarItem(produto(1L, "PAP-A4", "0", null), new BigDecimal("30"), BigDecimal.TEN, BigDecimal.TEN);
        when(pedidoCompraRepository.buscarComBloqueio(50L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.receber(50L, receber(1L, "31")))
                .isInstanceOf(NegocioException.class).extracting("chave").isEqualTo("pedido.quantidade.excede");
        assertThatThrownBy(() -> service.receber(50L, receber(99L, "1")))
                .isInstanceOf(NegocioException.class).extracting("chave").isEqualTo("pedido.item.invalido");
        verify(movimentacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Primeira compra (sem preço médio) e saldo zero: o médio passa a ser o custo do recebimento")
    void semMedioAnteriorUsaCusto() {
        Produto semMedio = produto(1L, "A", "4", null);
        Produto saldoZero = produto(2L, "B", "0", "99.0000");
        PedidoCompra pedido = pedido(StatusPedidoCompra.EMITIDO);
        pedido.adicionarItem(semMedio, new BigDecimal("32"), BigDecimal.ONE, new BigDecimal("28.9819"));
        pedido.adicionarItem(saldoZero, new BigDecimal("3"), BigDecimal.ONE, new BigDecimal("33.3333"));
        when(pedidoCompraRepository.buscarComBloqueio(50L)).thenReturn(Optional.of(pedido));

        service.receber(50L, null);

        assertThat(semMedio.getPrecoMedioCompra()).isEqualByComparingTo("28.9819");
        assertThat(saldoZero.getPrecoMedioCompra()).isEqualByComparingTo("33.3333");
    }

    @Test
    @DisplayName("Rateio: frete e impostos entram no custo de cada item na proporção do seu valor")
    void custoEfetivoRateado() {
        // item de 100,00 num pedido de 400,00 com 40,00 de frete+imposto recebe 10,00: (100 + 10) / 4 = 27,5
        assertThat(PedidoCompraService.custoUnitarioEfetivo(new BigDecimal("100.00"), new BigDecimal("400.00"),
                new BigDecimal("40.00"), new BigDecimal("4"))).isEqualByComparingTo("27.5000");
        // (300 + 30) / 3 = 110
        assertThat(PedidoCompraService.custoUnitarioEfetivo(new BigDecimal("300.00"), new BigDecimal("400.00"),
                new BigDecimal("40.00"), new BigDecimal("3"))).isEqualByComparingTo("110.0000");
    }

    @Test
    @DisplayName("Cancelamento vale para EMITIDO e PARCIALMENTE_RECEBIDO; pedido RECEBIDO não pode ser cancelado nem recebido")
    void cancelamento() {
        PedidoCompra parcial = pedido(StatusPedidoCompra.PARCIALMENTE_RECEBIDO);
        when(pedidoCompraRepository.buscarComBloqueio(50L)).thenReturn(Optional.of(parcial));

        PedidoCompraResponse cancelado = service.cancelar(50L, "  Fornecedor sem estoque  ");

        assertThat(cancelado.status()).isEqualTo(StatusPedidoCompra.CANCELADO);
        assertThat(cancelado.motivoCancelamento()).isEqualTo("Fornecedor sem estoque");
        assertThat(cancelado.canceladoPor()).isEqualTo(UsuarioAtual.SISTEMA);

        PedidoCompra recebido = pedido(StatusPedidoCompra.RECEBIDO);
        when(pedidoCompraRepository.buscarComBloqueio(51L)).thenReturn(Optional.of(recebido));
        assertThatThrownBy(() -> service.cancelar(51L, "x"))
                .isInstanceOf(NegocioException.class).extracting("chave").isEqualTo("pedido.status.invalido.cancelar");
        assertThatThrownBy(() -> service.receber(51L, null))
                .isInstanceOf(NegocioException.class).extracting("chave").isEqualTo("pedido.status.invalido.receber");
    }

    @Test
    @DisplayName("Pedido inexistente retorna 404")
    void pedidoInexistente() {
        when(pedidoCompraRepository.buscarComBloqueio(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.receber(1L, null)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Produto produto(long id, String sku, String saldo, String medio) {
        Produto produto = Fabrica.produto(id, sku, saldo, "1");
        produto.setPrecoMedioCompra(medio == null ? null : new BigDecimal(medio));
        produtos.add(produto);
        return produto;
    }

    private static PedidoCompra pedido(StatusPedidoCompra status) {
        PedidoCompra pedido = new PedidoCompra();
        pedido.setId(50L);
        pedido.setNumeroPedido("PC-2026-00050");
        Cotacao cotacao = new Cotacao();
        cotacao.setId(5L);
        cotacao.setCodigo("COT-2026-00005");
        pedido.setCotacao(cotacao);
        pedido.setFornecedor(Fabrica.fornecedor(1L, "Alfa", true));
        pedido.setValorMercadoria(BigDecimal.TEN);
        pedido.setValorFrete(BigDecimal.ZERO);
        pedido.setValorImposto(BigDecimal.ZERO);
        pedido.setValorTotal(BigDecimal.TEN);
        pedido.setPrazoEntregaDias(5);
        pedido.setStatus(status);
        pedido.setEmitidoPor("aprovador");
        return pedido;
    }

    private static ReceberPedidoRequest receber(long produtoId, String quantidade) {
        return new ReceberPedidoRequest(List.of(new ReceberPedidoRequest.Item(produtoId, new BigDecimal(quantidade))));
    }
}
