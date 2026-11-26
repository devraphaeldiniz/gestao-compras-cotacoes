package com.compras.cotacoes.proposta;

import static com.compras.cotacoes.support.Fabrica.HOJE;
import static com.compras.cotacoes.support.Fabrica.fornecedor;
import static com.compras.cotacoes.support.Fabrica.proposta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.compras.cotacoes.cotacao.Cotacao;
import com.compras.cotacoes.cotacao.CotacaoRepository;
import com.compras.cotacoes.cotacao.StatusCotacao;
import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.fornecedor.FornecedorRepository;
import com.compras.cotacoes.pedido.PedidoCompra;
import com.compras.cotacoes.pedido.PedidoCompraMapper;
import com.compras.cotacoes.pedido.PedidoCompraRepository;
import com.compras.cotacoes.pedido.PedidoCompraService;
import com.compras.cotacoes.pedido.StatusPedidoCompra;
import com.compras.cotacoes.produto.Produto;
import com.compras.cotacoes.produto.ProdutoRepository;
import com.compras.cotacoes.proposta.dto.DecidirCotacaoRequest;
import com.compras.cotacoes.proposta.dto.RegistrarPropostaRequest;
import com.compras.cotacoes.proposta.dto.ResultadoDecisaoResponse;
import com.compras.cotacoes.shared.exception.NegocioException;
import com.compras.cotacoes.support.Fabrica;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

/** Regra D (decisão) e as validações de registro da Regra C, com repositórios simulados. */
@ExtendWith(MockitoExtension.class)
class PropostaDecisaoServiceTest {

    @Mock
    private PropostaRepository propostaRepository;
    @Mock
    private CotacaoRepository cotacaoRepository;
    @Mock
    private FornecedorRepository fornecedorRepository;
    @Mock
    private PedidoCompraRepository pedidoCompraRepository;
    @Mock
    private ProdutoRepository produtoRepository;

    private PropostaService service;
    private Cotacao cotacao;
    private final List<Proposta> propostas = new ArrayList<>();

    private final Fornecedor alfa = fornecedor(1L, "Alfa", true);
    private final Fornecedor beta = fornecedor(2L, "Beta", true);
    private final Fornecedor gama = fornecedor(3L, "Gama", true);
    private final Fornecedor delta = fornecedor(4L, "Delta", true);

    @BeforeEach
    void configurar() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pt-BR"));
        PedidoCompraMapper pedidoMapper = Mappers.getMapper(PedidoCompraMapper.class);
        PedidoCompraService pedidoService = new PedidoCompraService(pedidoCompraRepository, produtoRepository, pedidoMapper, Fabrica.RELOGIO);
        service = new PropostaService(propostaRepository, cotacaoRepository, fornecedorRepository,
                new CalculadoraPropostaService(), new AvaliadorPropostasService(), pedidoService,
                Mappers.getMapper(PropostaMapper.class), pedidoMapper, Fabrica.mensagens(), Fabrica.RELOGIO);

        Produto produto = Fabrica.produto(1L, "PAP-A4", "8", "20");
        cotacao = Fabrica.cotacaoAberta(5L, produto, "32");

        lenient().when(cotacaoRepository.buscarComBloqueio(5L)).thenReturn(Optional.of(cotacao));
        lenient().when(propostaRepository.findByCotacaoIdOrderByIdAsc(5L)).thenReturn(propostas);
        lenient().when(pedidoCompraRepository.proximoNumeroPedido()).thenReturn(1L);
        lenient().when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(inv -> {
            PedidoCompra pedido = inv.getArgument(0);
            pedido.setId(99L);
            return pedido;
        });
    }

    @AfterEach
    void limpar() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Proposta adicionar(Proposta proposta) {
        proposta.setCotacao(cotacao);
        propostas.add(proposta);
        return proposta;
    }

    private static DecidirCotacaoRequest automatica(CriterioDecisao criterio) {
        return new DecidirCotacaoRequest(criterio, null, null);
    }

    @Nested
    @DisplayName("Trava de mínimo de 3 propostas elegíveis")
    class MinimoDePropostas {

        @Test
        @DisplayName("Bloqueia a decisão com apenas 2 propostas")
        void bloqueiaComMenosDeTres() {
            adicionar(proposta(10L, alfa, "900.00", 5));
            adicionar(proposta(11L, beta, "950.00", 3));

            assertThatThrownBy(() -> service.decidir(5L, automatica(CriterioDecisao.MENOR_PRECO)))
                    .isInstanceOf(NegocioException.class)
                    .hasMessageContaining("proposta.minimo.nao.atingido")
                    .extracting("chave").isEqualTo("proposta.minimo.nao.atingido");

            assertThat(cotacao.getStatus()).isEqualTo(StatusCotacao.ABERTA);
            verify(pedidoCompraRepository, never()).save(any());
        }

        @Test
        @DisplayName("Proposta de fornecedor inativo não conta para o mínimo")
        void fornecedorInativoNaoConta() {
            adicionar(proposta(10L, alfa, "900.00", 5));
            adicionar(proposta(11L, beta, "950.00", 3));
            adicionar(proposta(12L, fornecedor(9L, "Inativo", false), "800.00", 1));

            assertThatThrownBy(() -> service.decidir(5L, automatica(CriterioDecisao.MENOR_PRECO)))
                    .isInstanceOf(NegocioException.class)
                    .extracting("chave").isEqualTo("proposta.minimo.nao.atingido");
        }

        @Test
        @DisplayName("Proposta vencida (validade antes de hoje) não conta para o mínimo")
        void propostaVencidaNaoConta() {
            adicionar(proposta(10L, alfa, "900.00", 5));
            adicionar(proposta(11L, beta, "950.00", 3));
            adicionar(proposta(12L, gama, "800.00", 1, HOJE.minusDays(1)));

            assertThatThrownBy(() -> service.decidir(5L, automatica(CriterioDecisao.MENOR_PRECO)))
                    .isInstanceOf(NegocioException.class)
                    .extracting("chave").isEqualTo("proposta.minimo.nao.atingido");
        }

        @Test
        @DisplayName("Cotação que não está ABERTA não pode ser decidida")
        void cotacaoNaoAberta() {
            cotacao.setStatus(StatusCotacao.RASCUNHO);

            assertThatThrownBy(() -> service.decidir(5L, automatica(CriterioDecisao.MENOR_PRECO)))
                    .isInstanceOf(NegocioException.class)
                    .extracting("chave").isEqualTo("cotacao.nao.aberta");
        }
    }

    @Nested
    @DisplayName("Escolha manual")
    class EscolhaManual {

        private Proposta barata;
        private Proposta rapida;

        @BeforeEach
        void cenario() {
            barata = adicionar(proposta(10L, alfa, "900.00", 10));
            rapida = adicionar(proposta(11L, beta, "950.00", 2));
            adicionar(proposta(12L, gama, "1000.00", 5));
        }

        @Test
        @DisplayName("Escolher proposta diferente da recomendada sem justificativa retorna erro")
        void exigeJustificativa() {
            DecidirCotacaoRequest request = new DecidirCotacaoRequest(CriterioDecisao.MENOR_PRECO, rapida.getId(), "  ");

            assertThatThrownBy(() -> service.decidir(5L, request))
                    .isInstanceOf(NegocioException.class)
                    .extracting("chave").isEqualTo("proposta.justificativa.obrigatoria");
            assertThat(cotacao.getStatus()).isEqualTo(StatusCotacao.ABERTA);
        }

        @Test
        @DisplayName("Com justificativa, a escolha manual é aceita e registrada")
        void aceitaEscolhaJustificada() {
            DecidirCotacaoRequest request = new DecidirCotacaoRequest(
                    CriterioDecisao.MENOR_PRECO, rapida.getId(), "Entrega urgente para a filial");

            ResultadoDecisaoResponse resultado = service.decidir(5L, request);

            assertThat(resultado.escolhaManual()).isTrue();
            assertThat(resultado.propostaRecomendadaId()).isEqualTo(barata.getId());
            assertThat(resultado.propostaVencedora().id()).isEqualTo(rapida.getId());
            assertThat(rapida.getStatus()).isEqualTo(StatusProposta.VENCEDORA);
            assertThat(rapida.getJustificativaEscolha()).isEqualTo("Entrega urgente para a filial");
            assertThat(barata.getStatus()).isEqualTo(StatusProposta.REJEITADA);
        }

        @Test
        @DisplayName("Escolher a própria recomendada dispensa justificativa")
        void escolherARecomendadaDispensaJustificativa() {
            ResultadoDecisaoResponse resultado = service.decidir(5L,
                    new DecidirCotacaoRequest(CriterioDecisao.MENOR_PRECO, barata.getId(), null));

            assertThat(resultado.escolhaManual()).isFalse();
            assertThat(resultado.justificativaEscolha()).isEqualTo("Selecionada automaticamente pelo critério MENOR_PRECO");
        }

        @Test
        @DisplayName("Bloqueia a escolha de proposta de fornecedor inativo")
        void bloqueiaFornecedorInativo() {
            Proposta inativa = adicionar(proposta(13L, fornecedor(9L, "Inativo", false), "700.00", 1));

            assertThatThrownBy(() -> service.decidir(5L,
                    new DecidirCotacaoRequest(CriterioDecisao.MENOR_PRECO, inativa.getId(), "Preferência")))
                    .isInstanceOf(NegocioException.class)
                    .extracting("chave").isEqualTo("proposta.nao.elegivel.fornecedor.inativo");
        }

        @Test
        @DisplayName("Bloqueia a escolha de proposta vencida")
        void bloqueiaPropostaVencida() {
            Proposta vencida = adicionar(proposta(13L, delta, "700.00", 1, HOJE.minusDays(2)));

            assertThatThrownBy(() -> service.decidir(5L,
                    new DecidirCotacaoRequest(CriterioDecisao.MENOR_PRECO, vencida.getId(), "Preferência")))
                    .isInstanceOf(NegocioException.class)
                    .extracting("chave").isEqualTo("proposta.nao.elegivel.vencida");
        }

        @Test
        @DisplayName("Bloqueia a escolha de proposta de outra cotação")
        void bloqueiaPropostaDeOutraCotacao() {
            assertThatThrownBy(() -> service.decidir(5L,
                    new DecidirCotacaoRequest(CriterioDecisao.MENOR_PRECO, 777L, "Qualquer")))
                    .isInstanceOf(NegocioException.class)
                    .extracting("chave").isEqualTo("proposta.nao.pertence");
        }
    }

    @Test
    @DisplayName("Decisão automática fecha a cotação, classifica as propostas e emite o pedido de compra")
    void decisaoGeraPedidoEFechaCotacao() {
        Proposta vencedora = adicionar(proposta(10L, alfa, "927.42", 5));
        vencedora.setPrecoUnitario(new BigDecimal("24.90"));
        vencedora.setValorFrete(new BigDecimal("35.00"));
        vencedora.setValorImpostoCalculado(new BigDecimal("95.62"));
        Proposta segunda = adicionar(proposta(11L, beta, "932.24", 2));
        Proposta terceira = adicionar(proposta(12L, gama, "940.74", 10));
        Proposta vencida = adicionar(proposta(13L, delta, "800.00", 1, HOJE.minusDays(1)));

        ResultadoDecisaoResponse resultado = service.decidir(5L, automatica(CriterioDecisao.MENOR_PRECO));

        assertThat(cotacao.getStatus()).isEqualTo(StatusCotacao.FECHADA);
        assertThat(cotacao.getPropostaVencedoraId()).isEqualTo(10L);
        assertThat(vencedora.getStatus()).isEqualTo(StatusProposta.VENCEDORA);
        assertThat(vencedora.getPontuacaoCalculada()).isEqualByComparingTo("100.0000");
        assertThat(segunda.getStatus()).isEqualTo(StatusProposta.REJEITADA);
        assertThat(terceira.getStatus()).isEqualTo(StatusProposta.REJEITADA);
        assertThat(vencida.getStatus()).isEqualTo(StatusProposta.DESQUALIFICADA);
        verify(propostaRepository).saveAll(propostas);

        ArgumentCaptor<PedidoCompra> pedidoCaptor = ArgumentCaptor.forClass(PedidoCompra.class);
        verify(pedidoCompraRepository).save(pedidoCaptor.capture());
        PedidoCompra pedido = pedidoCaptor.getValue();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedidoCompra.EMITIDO);
        assertThat(pedido.getNumeroPedido()).isEqualTo("PC-2026-00001");
        assertThat(pedido.getQuantidade()).isEqualByComparingTo("32");
        assertThat(pedido.getValorUnitario()).isEqualByComparingTo("24.90");
        assertThat(pedido.getValorFrete()).isEqualByComparingTo("35.00");
        assertThat(pedido.getValorImposto()).isEqualByComparingTo("95.62");
        assertThat(pedido.getValorTotal()).isEqualByComparingTo("927.42");
        assertThat(pedido.getPrazoEntregaDias()).isEqualTo(5);
        assertThat(pedido.getFornecedor()).isSameAs(alfa);

        assertThat(resultado.statusCotacao()).isEqualTo(StatusCotacao.FECHADA);
        assertThat(resultado.ranking()).hasSize(3);
        assertThat(resultado.pedidoCompra().id()).isEqualTo(99L);
        assertThat(resultado.escolhaManual()).isFalse();
    }

    @Test
    @DisplayName("PONTUACAO_PONDERADA pode eleger vencedora diferente do MENOR_PRECO")
    void ponderadaEscolheOutraVencedora() {
        adicionar(proposta(10L, alfa, "927.42", 5));
        Proposta rapida = adicionar(proposta(11L, beta, "932.24", 2));
        adicionar(proposta(12L, gama, "940.74", 10));

        ResultadoDecisaoResponse resultado = service.decidir(5L, automatica(CriterioDecisao.PONTUACAO_PONDERADA));

        assertThat(resultado.propostaVencedora().id()).isEqualTo(rapida.getId());
        assertThat(rapida.getPontuacaoCalculada()).isEqualByComparingTo("99.6381");
    }

    @Test
    @DisplayName("Critério ausente usa MENOR_PRECO")
    void criterioPadrao() {
        adicionar(proposta(10L, alfa, "900.00", 9));
        adicionar(proposta(11L, beta, "901.00", 1));
        adicionar(proposta(12L, gama, "902.00", 1));

        ResultadoDecisaoResponse resultado = service.decidir(5L, new DecidirCotacaoRequest(null, null, null));

        assertThat(resultado.criterio()).isEqualTo(CriterioDecisao.MENOR_PRECO);
        assertThat(resultado.propostaVencedora().id()).isEqualTo(10L);
    }

    @Nested
    @DisplayName("Registro de propostas (Regra C)")
    class Registro {

        @Test
        @DisplayName("Recusa fornecedor que não foi convidado")
        void recusaNaoConvidado() {
            cotacao.getFornecedoresConvidados().add(alfa);
            when(fornecedorRepository.findById(2L)).thenReturn(Optional.of(beta));

            assertThatThrownBy(() -> service.registrar(5L, registro(2L)))
                    .isInstanceOf(NegocioException.class)
                    .extracting("chave").isEqualTo("cotacao.fornecedor.nao.convidado");
        }

        @Test
        @DisplayName("Recusa proposta depois da data limite da cotação")
        void recusaForaDoPrazo() {
            cotacao.setDataLimite(HOJE.minusDays(1));

            assertThatThrownBy(() -> service.registrar(5L, registro(1L)))
                    .isInstanceOf(NegocioException.class)
                    .extracting("chave").isEqualTo("cotacao.prazo.encerrado");
        }

        @Test
        @DisplayName("Recusa fornecedor inativo")
        void recusaFornecedorInativo() {
            when(fornecedorRepository.findById(9L)).thenReturn(Optional.of(fornecedor(9L, "Inativo", false)));

            assertThatThrownBy(() -> service.registrar(5L, registro(9L)))
                    .isInstanceOf(NegocioException.class)
                    .extracting("chave").isEqualTo("fornecedor.inativo");
        }

        @Test
        @DisplayName("Grava a proposta com o total calculado sobre a quantidade da cotação")
        void gravaComTotalCalculado() {
            when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(alfa));
            when(propostaRepository.save(any(Proposta.class))).thenAnswer(inv -> inv.getArgument(0));

            var resposta = service.registrar(5L, registro(1L));

            assertThat(resposta.valorMercadoria()).isEqualByComparingTo("796.80");
            assertThat(resposta.valorImpostoCalculado()).isEqualByComparingTo("95.62");
            assertThat(resposta.valorTotal()).isEqualByComparingTo("927.42");
            assertThat(resposta.status()).isEqualTo(StatusProposta.PENDENTE);
        }

        private RegistrarPropostaRequest registro(Long fornecedorId) {
            return new RegistrarPropostaRequest(fornecedorId, new BigDecimal("24.90"), new BigDecimal("35.00"),
                    new BigDecimal("12"), BigDecimal.ZERO, 5, HOJE.plusDays(20));
        }
    }
}
