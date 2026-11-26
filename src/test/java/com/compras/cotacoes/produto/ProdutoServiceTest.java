package com.compras.cotacoes.produto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.cotacao.CotacaoRepository;
import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.produto.dto.CriarProdutoRequest;
import com.compras.cotacoes.produto.dto.MovimentarEstoqueRequest;
import com.compras.cotacoes.produto.dto.ProdutoResponse;
import com.compras.cotacoes.shared.exception.ConflitoException;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.support.Fabrica;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private CotacaoRepository cotacaoRepository;

    @Captor
    private ArgumentCaptor<Cotacao> cotacaoCaptor;

    @Captor
    private ArgumentCaptor<Collection<StatusCotacao>> statusCaptor;

    private ProdutoService service;

    @BeforeEach
    void configurar() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pt-BR"));
        service = new ProdutoService(produtoRepository, cotacaoRepository,
                Mappers.getMapper(ProdutoMapper.class), Fabrica.mensagens(), Fabrica.RELOGIO);

        lenient().when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> {
            Produto produto = inv.getArgument(0);
            if (produto.getId() == null) {
                produto.setId(10L);
            }
            return produto;
        });
        lenient().when(cotacaoRepository.save(any(Cotacao.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(cotacaoRepository.proximoNumeroCodigo()).thenReturn(7L);
    }

    @AfterEach
    void limpar() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Regra A: produto criado abaixo do mínimo gera cotação RASCUNHO com (mínimo - saldo) + mínimo")
    void criarComEstoqueBaixoGeraCotacaoRascunho() {
        ProdutoResponse resposta = service.criar(requisicao("pap-a4", "4", "10"));

        verify(cotacaoRepository).save(cotacaoCaptor.capture());
        Cotacao gerada = cotacaoCaptor.getValue();
        assertThat(gerada.getStatus()).isEqualTo(StatusCotacao.RASCUNHO);
        assertThat(gerada.getQuantidade()).isEqualByComparingTo("16");
        assertThat(gerada.getCodigo()).isEqualTo("COT-2026-00007");
        assertThat(gerada.getProduto().getSku()).isEqualTo("PAP-A4");
        assertThat(gerada.getObservacao()).contains("Reposição automática").contains("PAP-A4");
        assertThat(resposta.necessitaCompra()).isTrue();
        assertThat(resposta.sku()).isEqualTo("PAP-A4");
    }

    @Test
    @DisplayName("Regra A: saldo exatamente no mínimo sugere comprar um estoque mínimo")
    void criarComSaldoIgualAoMinimoSugereEstoqueMinimo() {
        service.criar(requisicao("CAN-01", "10", "10"));

        verify(cotacaoRepository).save(cotacaoCaptor.capture());
        assertThat(cotacaoCaptor.getValue().getQuantidade()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Produto criado acima do mínimo não gera cotação")
    void criarComEstoqueSuficienteNaoGeraCotacao() {
        ProdutoResponse resposta = service.criar(requisicao("CAN-02", "50", "10"));

        verify(cotacaoRepository, never()).save(any());
        assertThat(resposta.necessitaCompra()).isFalse();
    }

    @Test
    @DisplayName("Regra A: SAIDA que deixa o saldo abaixo do mínimo gera cotação de reposição")
    void saidaAbaixoDoMinimoGeraCotacao() {
        Produto produto = Fabrica.produto(1L, "LUV-M", "50", "20");
        when(produtoRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(produto));

        ProdutoResponse resposta = service.movimentarEstoque(1L, movimento(TipoMovimentacaoEstoque.SAIDA, "35"));

        assertThat(resposta.saldoAtual()).isEqualByComparingTo("15");
        verify(cotacaoRepository).save(cotacaoCaptor.capture());
        assertThat(cotacaoCaptor.getValue().getQuantidade()).isEqualByComparingTo("25");
        assertThat(cotacaoCaptor.getValue().getStatus()).isEqualTo(StatusCotacao.RASCUNHO);
    }

    @Test
    @DisplayName("Regra A: AJUSTE de inventário que leva ao mínimo também gera cotação")
    void ajusteAbaixoDoMinimoGeraCotacao() {
        Produto produto = Fabrica.produto(1L, "LUV-M", "50", "20");
        when(produtoRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(produto));

        service.movimentarEstoque(1L, movimento(TipoMovimentacaoEstoque.AJUSTE, "0"));

        verify(cotacaoRepository).save(cotacaoCaptor.capture());
        assertThat(cotacaoCaptor.getValue().getQuantidade()).isEqualByComparingTo("40");
    }

    @ParameterizedTest(name = "cotação existente em {0}")
    @EnumSource(value = StatusCotacao.class, names = {"RASCUNHO", "ABERTA"})
    @DisplayName("Regra A: não duplica cotação quando já existe uma RASCUNHO ou ABERTA para o produto")
    void naoDuplicaCotacaoEmAndamento(StatusCotacao statusExistente) {
        Produto produto = Fabrica.produto(1L, "LUV-M", "50", "20");
        Cotacao existente = Fabrica.cotacaoAberta(3L, produto, "30");
        existente.setStatus(statusExistente);
        when(produtoRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(produto));
        when(cotacaoRepository.findFirstByProdutoIdAndStatusInOrderByIdDesc(eq(1L), anyCollection()))
                .thenReturn(Optional.of(existente));

        service.movimentarEstoque(1L, movimento(TipoMovimentacaoEstoque.SAIDA, "40"));

        verify(cotacaoRepository).findFirstByProdutoIdAndStatusInOrderByIdDesc(eq(1L), statusCaptor.capture());
        assertThat(statusCaptor.getValue()).containsExactlyInAnyOrder(StatusCotacao.RASCUNHO, StatusCotacao.ABERTA);
        verify(cotacaoRepository, never()).save(any());
        verify(cotacaoRepository, never()).proximoNumeroCodigo();
    }

    @Test
    @DisplayName("SAIDA maior que o saldo é bloqueada e não altera o estoque")
    void saidaMaiorQueSaldoLancaErro() {
        Produto produto = Fabrica.produto(1L, "LUV-M", "5", "2");
        when(produtoRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.movimentarEstoque(1L, movimento(TipoMovimentacaoEstoque.SAIDA, "6")))
                .isInstanceOf(NegocioException.class)
                .extracting("chave").isEqualTo("estoque.saldo.insuficiente");

        assertThat(produto.getSaldoAtual()).isEqualByComparingTo("5");
        verify(produtoRepository, never()).save(any());
        verify(cotacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("ENTRADA soma ao saldo e não dispara a Regra A")
    void entradaNaoGeraCotacao() {
        Produto produto = Fabrica.produto(1L, "LUV-M", "5", "20");
        when(produtoRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(produto));

        ProdutoResponse resposta = service.movimentarEstoque(1L, movimento(TipoMovimentacaoEstoque.ENTRADA, "3"));

        assertThat(resposta.saldoAtual()).isEqualByComparingTo("8");
        verify(cotacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("ENTRADA e SAIDA exigem quantidade maior que zero")
    void movimentoComQuantidadeZeroLancaErro() {
        Produto produto = Fabrica.produto(1L, "LUV-M", "5", "2");
        when(produtoRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> service.movimentarEstoque(1L, movimento(TipoMovimentacaoEstoque.SAIDA, "0")))
                .isInstanceOf(NegocioException.class)
                .extracting("chave").isEqualTo("estoque.quantidade.positiva");
    }

    @Test
    @DisplayName("SKU duplicado retorna conflito")
    void skuDuplicadoLancaConflito() {
        when(produtoRepository.existsBySkuIgnoreCase("PAP-A4")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(requisicao("pap-a4", "1", "1")))
                .isInstanceOf(ConflitoException.class)
                .extracting("chave").isEqualTo("produto.sku.duplicado");
        verify(produtoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Quantidade sugerida nunca é zero, mesmo com estoque mínimo zero")
    void quantidadeSugeridaNuncaZero() {
        assertThat(ProdutoService.quantidadeSugerida(BigDecimal.ZERO, BigDecimal.ZERO)).isEqualByComparingTo("1");
        assertThat(ProdutoService.quantidadeSugerida(new BigDecimal("2.5"), new BigDecimal("10")))
                .isEqualByComparingTo("17.5");
        verify(cotacaoRepository, never()).proximoNumeroCodigo();
        verify(produtoRepository, never()).findById(anyLong());
    }

    private static CriarProdutoRequest requisicao(String sku, String saldo, String minimo) {
        return new CriarProdutoRequest(sku, "Produto " + sku, UnidadeMedida.CX, new BigDecimal(saldo), new BigDecimal(minimo));
    }

    private static MovimentarEstoqueRequest movimento(TipoMovimentacaoEstoque tipo, String quantidade) {
        return new MovimentarEstoqueRequest(tipo, new BigDecimal(quantidade));
    }
}
