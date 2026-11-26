package com.compras.cotacoes.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.pedido.dto.PedidoCompraResponse;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.shared.exception.RecursoNaoEncontradoException;
import com.compras.cotacoes.support.Fabrica;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PedidoCompraServiceTest {

    @Mock
    private PedidoCompraRepository pedidoCompraRepository;

    private PedidoCompraService service;

    @BeforeEach
    void configurar() {
        service = new PedidoCompraService(pedidoCompraRepository,
                Mappers.getMapper(PedidoCompraMapper.class), Fabrica.RELOGIO);
        lenient().when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Regra E: recebimento soma ao saldo e recalcula último preço e preço médio ponderado")
    void recebimentoAtualizaEstoqueECusto() {
        Produto produto = Fabrica.produto(1L, "PAP-A4", "10", "20");
        produto.setPrecoMedioCompra(new BigDecimal("20.0000"));
        PedidoCompra pedido = pedido(produto, "30", "750.00", StatusPedidoCompra.EMITIDO);
        when(pedidoCompraRepository.findComDetalhesById(50L)).thenReturn(Optional.of(pedido));

        PedidoCompraResponse resposta = service.receber(50L);

        // custo efetivo = 750 / 30 = 25; médio = (10 x 20 + 30 x 25) / 40 = 23.75
        assertThat(produto.getSaldoAtual()).isEqualByComparingTo("40");
        assertThat(produto.getUltimoPrecoCompra()).isEqualByComparingTo("25.0000");
        assertThat(produto.getPrecoMedioCompra()).isEqualByComparingTo("23.7500");
        assertThat(resposta.status()).isEqualTo(StatusPedidoCompra.RECEBIDO);
        assertThat(resposta.dataRecebimento()).isEqualTo(LocalDateTime.now(Fabrica.RELOGIO));
        verify(pedidoCompraRepository).save(pedido);
    }

    @Test
    @DisplayName("Primeira compra (sem preço médio): o médio passa a ser o custo efetivo")
    void primeiraCompraUsaCustoEfetivo() {
        Produto produto = Fabrica.produto(1L, "PAP-A4", "4", "20");
        PedidoCompra pedido = pedido(produto, "32", "927.42", StatusPedidoCompra.EMITIDO);
        when(pedidoCompraRepository.findComDetalhesById(50L)).thenReturn(Optional.of(pedido));

        service.receber(50L);

        // 927.42 / 32 = 28.981875 -> 28.9819
        assertThat(produto.getSaldoAtual()).isEqualByComparingTo("36");
        assertThat(produto.getUltimoPrecoCompra()).isEqualByComparingTo("28.9819");
        assertThat(produto.getPrecoMedioCompra()).isEqualByComparingTo("28.9819");
    }

    @Test
    @DisplayName("Saldo anterior zero: o preço médio antigo é descartado")
    void saldoZeroDescartaMedioAntigo() {
        Produto produto = Fabrica.produto(1L, "PAP-A4", "0", "20");
        produto.setPrecoMedioCompra(new BigDecimal("99.0000"));
        PedidoCompra pedido = pedido(produto, "3", "100.00", StatusPedidoCompra.EMITIDO);
        when(pedidoCompraRepository.findComDetalhesById(50L)).thenReturn(Optional.of(pedido));

        service.receber(50L);

        assertThat(produto.getPrecoMedioCompra()).isEqualByComparingTo("33.3333");
        assertThat(produto.getUltimoPrecoCompra()).isEqualByComparingTo("33.3333");
    }

    @Test
    @DisplayName("Só pedidos EMITIDOS podem ser recebidos")
    void naoRecebeDuasVezes() {
        Produto produto = Fabrica.produto(1L, "PAP-A4", "10", "20");
        PedidoCompra pedido = pedido(produto, "30", "750.00", StatusPedidoCompra.RECEBIDO);
        when(pedidoCompraRepository.findComDetalhesById(50L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.receber(50L))
                .isInstanceOf(NegocioException.class)
                .extracting("chave").isEqualTo("pedido.status.invalido.receber");
        assertThat(produto.getSaldoAtual()).isEqualByComparingTo("10");
        verify(pedidoCompraRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pedido inexistente retorna 404")
    void pedidoInexistente() {
        when(pedidoCompraRepository.findComDetalhesById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.receber(1L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private static PedidoCompra pedido(Produto produto, String quantidade, String total, StatusPedidoCompra status) {
        Cotacao cotacao = Fabrica.cotacaoAberta(5L, produto, quantidade);
        Fornecedor fornecedor = Fabrica.fornecedor(1L, "Alfa", true);
        PedidoCompra pedido = new PedidoCompra();
        pedido.setId(50L);
        pedido.setNumeroPedido("PC-2026-00050");
        pedido.setCotacao(cotacao);
        pedido.setFornecedor(fornecedor);
        pedido.setProduto(produto);
        pedido.setQuantidade(new BigDecimal(quantidade));
        pedido.setValorUnitario(new BigDecimal("1"));
        pedido.setValorFrete(BigDecimal.ZERO);
        pedido.setValorImposto(BigDecimal.ZERO);
        pedido.setValorTotal(new BigDecimal(total));
        pedido.setPrazoEntregaDias(5);
        pedido.setStatus(status);
        return pedido;
    }
}
