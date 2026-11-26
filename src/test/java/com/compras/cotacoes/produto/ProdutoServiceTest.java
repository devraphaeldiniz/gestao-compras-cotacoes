package com.compras.cotacoes.produto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.cotacao.CotacaoItemRepository;
import com.compras.cotacoes.cotacao.CotacaoRepository;
import com.compras.cotacoes.cotacao.OrigemCotacao;
import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.historico.AcaoHistorico;
import com.compras.cotacoes.historico.EntidadeHistorico;
import com.compras.cotacoes.historico.HistoricoService;
import com.compras.cotacoes.produto.dto.AtualizarProdutoRequest;
import com.compras.cotacoes.produto.dto.CriarProdutoRequest;
import com.compras.cotacoes.produto.dto.MovimentarEstoqueRequest;
import com.compras.cotacoes.produto.dto.ProdutoResponse;
import com.compras.cotacoes.shared.exception.ConflitoException;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.shared.seguranca.UsuarioAtual;
import com.compras.cotacoes.support.Fabrica;
import java.math.BigDecimal;
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
    @Mock
    private CotacaoItemRepository cotacaoItemRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;
    @Mock
    private HistoricoService historico;

    @Captor
    private ArgumentCaptor<Cotacao> cotacaoCaptor;
    @Captor
    private ArgumentCaptor<MovimentacaoEstoque> movimentacaoCaptor;

    private ProdutoService service;

    @BeforeEach
    void configurar() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pt-BR"));
        UsuarioAtual usuarioAtual = new UsuarioAtual();
        ExtratoEstoqueService extrato = new ExtratoEstoqueService(movimentacaoRepository, produtoRepository, usuarioAtual, Fabrica.RELOGIO);
        service = new ProdutoService(produtoRepository, cotacaoRepository, cotacaoItemRepository,
                Mappers.getMapper(ProdutoMapper.class), extrato, historico, Fabrica.metricas(), Fabrica.mensagens(),
                usuarioAtual, Fabrica.RELOGIO);

        lenient().when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> {
            Produto produto = inv.getArgument(0);
            if (produto.getId() == null) {
                produto.setId(10L);
            }
            return produto;
        });
        lenient().when(cotacaoRepository.save(any(Cotacao.class))).thenAnswer(inv -> {
            Cotacao cotacao = inv.getArgument(0);
            if (cotacao.getId() == null) {
                cotacao.setId(70L);
            }
            return cotacao;
        });
        lenient().when(cotacaoRepository.proximoNumeroCodigo()).thenReturn(7L);
    }

    @AfterEach
    void limpar() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Regra A: produto criado abaixo do mínimo gera cotação RASCUNHO automática com (mínimo - saldo) + mínimo")
    void criarComEstoqueBaixoGeraCotacaoRascunho() {
        ProdutoResponse resposta = service.criar(requisicao("pap-a4", "4", "10"));

        verify(cotacaoRepository).save(cotacaoCaptor.capture());
        Cotacao gerada = cotacaoCaptor.getValue();
        assertThat(gerada.getStatus()).isEqualTo(StatusCotacao.RASCUNHO);
        assertThat(gerada.getOrigem()).isEqualTo(OrigemCotacao.AUTOMATICA);
        assertThat(gerada.getCodigo()).isEqualTo("COT-2026-00007");
        assertThat(gerada.getItens()).singleElement().satisfies(item -> {
            assertThat(item.getProduto().getSku()).isEqualTo("PAP-A4");
            assertThat(item.getQuantidade()).isEqualByComparingTo("16");
        });
        assertThat(gerada.getObservacao()).contains("Reposição automática");
        assertThat(resposta.necessitaCompra()).isTrue();
        verify(historico).registrar(eq(EntidadeHistorico.COTACAO), eq(70L), eq(AcaoHistorico.COTACAO_CRIADA), any());
    }

    @Test
    @DisplayName("Extrato: o saldo informado no cadastro vira a movimentação SALDO_INICIAL")
    void cadastroRegistraSaldoInicial() {
        ProdutoResponse resposta = service.criar(requisicao("CAN-02", "50", "10"));

        verify(movimentacaoRepository).save(movimentacaoCaptor.capture());
        MovimentacaoEstoque inicial = movimentacaoCaptor.getValue();
        assertThat(inicial.getTipo()).isEqualTo(TipoMovimentacaoEstoque.SALDO_INICIAL);
        assertThat(inicial.getSaldoAnterior()).isEqualByComparingTo("0");
        assertThat(inicial.getSaldoPosterior()).isEqualByComparingTo("50");
        assertThat(inicial.getUsuario()).isEqualTo(UsuarioAtual.SISTEMA);
        assertThat(resposta.saldoAtual()).isEqualByComparingTo("50");
        assertThat(resposta.necessitaCompra()).isFalse();
        verify(cotacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Regra A: saldo exatamente no mínimo sugere comprar um estoque mínimo")
    void criarComSaldoIgualAoMinimoSugereEstoqueMinimo() {
        service.criar(requisicao("CAN-01", "10", "10"));

        verify(cotacaoRepository).save(cotacaoCaptor.capture());
        assertThat(cotacaoCaptor.getValue().getItens().getFirst().getQuantidade()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Regra A: SAIDA abaixo do mínimo gera reposição e fica no extrato com saldo antes e depois")
    void saidaAbaixoDoMinimoGeraCotacao() {
        Produto produto = Fabrica.produto(1L, "LUV-M", "50", "20");
        when(produtoRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(produto));

        ProdutoResponse resposta = service.movimentarEstoque(1L,
                new MovimentarEstoqueRequest(TipoMovimentacaoEstoque.SAIDA, new BigDecimal("35"), "Requisição 12"));

        assertThat(resposta.saldoAtual()).isEqualByComparingTo("15");
        verify(movimentacaoRepository).save(movimentacaoCaptor.capture());
        assertThat(movimentacaoCaptor.getValue().getSaldoAnterior()).isEqualByComparingTo("50");
        assertThat(movimentacaoCaptor.getValue().getSaldoPosterior()).isEqualByComparingTo("15");
        assertThat(movimentacaoCaptor.getValue().getObservacao()).isEqualTo("Requisição 12");
        verify(cotacaoRepository).save(cotacaoCaptor.capture());
        assertThat(cotacaoCaptor.getValue().getItens().getFirst().getQuantidade()).isEqualByComparingTo("25");
    }

    @Test
    @DisplayName("Regra A: AJUSTE de inventário que leva ao mínimo também gera cotação")
    void ajusteAbaixoDoMinimoGeraCotacao() {
        Produto produto = Fabrica.produto(1L, "LUV-M", "50", "20");
        when(produtoRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(produto));

        service.movimentarEstoque(1L, movimento(TipoMovimentacaoEstoque.AJUSTE, "0"));

        verify(cotacaoRepository).save(cotacaoCaptor.capture());
        assertThat(cotacaoCaptor.getValue().getItens().getFirst().getQuantidade()).isEqualByComparingTo("40");
    }

    @ParameterizedTest(name = "produto já em cotação {0}")
    @EnumSource(value = StatusCotacao.class, names = {"RASCUNHO", "ABERTA"})
    @DisplayName("Regra A: não duplica quando o produto já está em cotação RASCUNHO ou ABERTA")
    void naoDuplicaCotacaoEmAndamento(StatusCotacao statusExistente) {
        Produto produto = Fabrica.produto(1L, "LUV-M", "50", "20");
        Cotacao existente = Fabrica.cotacaoAberta(3L, produto, "30");
        existente.setStatus(statusExistente);
        when(produtoRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(produto));
        when(cotacaoItemRepository.cotacaoEmAndamentoDoProduto(eq(1L), anyCollection())).thenReturn(Optional.of(existente));

        service.movimentarEstoque(1L, movimento(TipoMovimentacaoEstoque.SAIDA, "40"));

        verify(cotacaoRepository, never()).save(any());
        verify(cotacaoRepository, never()).proximoNumeroCodigo();
    }

    @Test
    @DisplayName("Regra A: consolida no rascunho automático existente em vez de abrir outra cotação")
    void consolidaNoRascunhoAutomatico() {
        Produto toner = Fabrica.produto(2L, "TON-85A", "20", "5");
        Cotacao rascunho = Fabrica.cotacaoAberta(9L, toner, "10");
        rascunho.setStatus(StatusCotacao.RASCUNHO);
        rascunho.setOrigem(OrigemCotacao.AUTOMATICA);
        Produto luva = Fabrica.produto(1L, "LUV-M", "50", "20");
        when(produtoRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(luva));
        when(cotacaoRepository.findFirstByStatusAndOrigemOrderByIdDesc(StatusCotacao.RASCUNHO, OrigemCotacao.AUTOMATICA))
                .thenReturn(Optional.of(rascunho));
        when(cotacaoRepository.buscarComBloqueio(9L)).thenReturn(Optional.of(rascunho));

        service.movimentarEstoque(1L, movimento(TipoMovimentacaoEstoque.SAIDA, "35"));

        assertThat(rascunho.getItens()).extracting(i -> i.getProduto().getSku()).containsExactly("TON-85A", "LUV-M");
        verify(cotacaoRepository, never()).proximoNumeroCodigo();
        verify(historico).registrar(eq(EntidadeHistorico.COTACAO), eq(9L), eq(AcaoHistorico.COTACAO_ITEM_ADICIONADO), any());
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
        verify(movimentacaoRepository, never()).save(any());
        verify(produtoRepository, never()).save(any());
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
    @DisplayName("RECEBIMENTO e SALDO_INICIAL não podem ser lançados manualmente")
    void tipoDoSistemaNaoPodeSerManual() {
        assertThatThrownBy(() -> service.movimentarEstoque(1L, movimento(TipoMovimentacaoEstoque.RECEBIMENTO, "5")))
                .isInstanceOf(NegocioException.class)
                .extracting("chave").isEqualTo("estoque.tipo.invalido");
        verify(produtoRepository, never()).buscarComBloqueio(1L);
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
    @DisplayName("Subir o estoque mínimo acima do saldo dispara a reposição e fica no histórico")
    void atualizarMinimoDisparaReposicao() {
        Produto produto = Fabrica.produto(1L, "LUV-M", "15", "10");
        when(produtoRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(produto));

        ProdutoResponse resposta = service.atualizar(1L, new AtualizarProdutoRequest("Luva M", UnidadeMedida.CX, new BigDecimal("20")));

        assertThat(resposta.estoqueMinimo()).isEqualByComparingTo("20");
        assertThat(resposta.necessitaCompra()).isTrue();
        verify(historico).registrar(eq(EntidadeHistorico.PRODUTO), eq(1L), eq(AcaoHistorico.PRODUTO_ATUALIZADO), any());
        verify(cotacaoRepository).save(cotacaoCaptor.capture());
        assertThat(cotacaoCaptor.getValue().getItens().getFirst().getQuantidade()).isEqualByComparingTo("25");
    }

    @Test
    @DisplayName("Quantidade sugerida nunca é zero, mesmo com estoque mínimo zero")
    void quantidadeSugeridaNuncaZero() {
        assertThat(ProdutoService.quantidadeSugerida(BigDecimal.ZERO, BigDecimal.ZERO)).isEqualByComparingTo("1");
        assertThat(ProdutoService.quantidadeSugerida(new BigDecimal("2.5"), new BigDecimal("10"))).isEqualByComparingTo("17.5");
    }

    private static CriarProdutoRequest requisicao(String sku, String saldo, String minimo) {
        return new CriarProdutoRequest(sku, "Produto " + sku, UnidadeMedida.CX, new BigDecimal(saldo), new BigDecimal(minimo));
    }

    private static MovimentarEstoqueRequest movimento(TipoMovimentacaoEstoque tipo, String quantidade) {
        return new MovimentarEstoqueRequest(tipo, new BigDecimal(quantidade));
    }
}
