package com.compras.cotacoes.proposta;

import static com.compras.cotacoes.support.Fabrica.HOJE;
import static com.compras.cotacoes.support.Fabrica.fornecedor;
import static com.compras.cotacoes.support.Fabrica.proposta;
import static org.assertj.core.api.Assertions.assertThat;

import com.compras.cotacoes.fornecedor.Fornecedor;
import com.compras.cotacoes.proposta.AvaliadorPropostasService.PropostaAvaliada;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AvaliadorPropostasServiceTest {

    private final AvaliadorPropostasService avaliador = new AvaliadorPropostasService();

    private final Fornecedor alfa = fornecedor(1L, "Alfa", true);
    private final Fornecedor beta = fornecedor(2L, "Beta", true);
    private final Fornecedor gama = fornecedor(3L, "Gama", true);

    @Test
    @DisplayName("MENOR_PRECO: menor valor total vence; empate no total é decidido pelo menor prazo")
    void menorPrecoDesempataPorPrazo() {
        Proposta a = proposta(1L, alfa, "1000.00", 10);
        Proposta b = proposta(2L, beta, "1000.00", 5);
        Proposta c = proposta(3L, gama, "1100.00", 1);

        List<PropostaAvaliada> ranking = avaliador.classificar(List.of(a, b, c), CriterioDecisao.MENOR_PRECO);

        assertThat(ranking).extracting(PropostaAvaliada::proposta).containsExactly(b, a, c);
        assertThat(ranking).extracting(PropostaAvaliada::posicao).containsExactly(1, 2, 3);
        assertThat(ranking.getFirst().pontuacao()).isEqualByComparingTo("100.0000");
        assertThat(ranking.get(2).pontuacao()).isEqualByComparingTo("90.9091");
    }

    @Test
    @DisplayName("MENOR_PRECO: com total e prazo empatados, vence a proposta de maior validade")
    void menorPrecoDesempataPorValidade() {
        Proposta curta = proposta(1L, alfa, "500.00", 3, HOJE.plusDays(5));
        Proposta longa = proposta(2L, beta, "500.00", 3, HOJE.plusDays(60));

        List<PropostaAvaliada> ranking = avaliador.classificar(List.of(curta, longa), CriterioDecisao.MENOR_PRECO);

        assertThat(ranking.getFirst().proposta()).isSameAs(longa);
    }

    @Test
    @DisplayName("PONTUACAO_PONDERADA: 70% preço + 30% prazo pode eleger proposta que não é a mais barata")
    void pontuacaoPonderadaSetentaTrinta() {
        Proposta barataLenta = proposta(1L, alfa, "1000.00", 10);   // 70 + 30*(2/10) = 76
        Proposta rapida = proposta(2L, beta, "1100.00", 2);          // 63.6364 + 30 = 93.6364
        Proposta cara = proposta(3L, gama, "1200.00", 5);            // 58.3333 + 12 = 70.3333

        List<PropostaAvaliada> ranking = avaliador.classificar(
                List.of(barataLenta, rapida, cara), CriterioDecisao.PONTUACAO_PONDERADA);

        assertThat(ranking).extracting(PropostaAvaliada::proposta).containsExactly(rapida, barataLenta, cara);
        assertThat(ranking).extracting(PropostaAvaliada::pontuacao)
                .usingElementComparator(java.math.BigDecimal::compareTo)
                .containsExactly(new java.math.BigDecimal("93.6364"),
                        new java.math.BigDecimal("76.0000"),
                        new java.math.BigDecimal("70.3333"));
    }

    @Test
    @DisplayName("PONTUACAO_PONDERADA: prazo 0 vale os 30 pontos e não causa divisão por zero")
    void pontuacaoPonderadaComPrazoZero() {
        Proposta imediata = proposta(1L, alfa, "1000.00", 0);  // 63 + 30 = 93
        Proposta barata = proposta(2L, beta, "900.00", 5);     // 70 + (0/5)*30 = 70

        List<PropostaAvaliada> ranking = avaliador.classificar(
                List.of(barata, imediata), CriterioDecisao.PONTUACAO_PONDERADA);

        assertThat(ranking.getFirst().proposta()).isSameAs(imediata);
        assertThat(ranking.getFirst().pontuacao()).isEqualByComparingTo("93.0000");
        assertThat(ranking.get(1).pontuacao()).isEqualByComparingTo("70.0000");
    }

    @Test
    @DisplayName("PONTUACAO_PONDERADA: todas com prazo 0 empatam no prazo e o preço decide")
    void todasComPrazoZero() {
        Proposta a = proposta(1L, alfa, "800.00", 0);
        Proposta b = proposta(2L, beta, "1000.00", 0);

        List<PropostaAvaliada> ranking = avaliador.classificar(List.of(b, a), CriterioDecisao.PONTUACAO_PONDERADA);

        assertThat(ranking.getFirst().proposta()).isSameAs(a);
        assertThat(ranking.getFirst().pontuacao()).isEqualByComparingTo("100.0000");
        assertThat(ranking.get(1).pontuacao()).isEqualByComparingTo("86.0000");
    }

    @Test
    @DisplayName("PONTUACAO_PONDERADA: empate de pontuação é decidido pelo menor valor total")
    void pontuacaoEmpatadaDesempataPorTotal() {
        Proposta a = proposta(1L, alfa, "1000.00", 4);
        Proposta b = proposta(2L, beta, "1000.00", 4);

        List<PropostaAvaliada> ranking = avaliador.classificar(List.of(b, a), CriterioDecisao.PONTUACAO_PONDERADA);

        assertThat(ranking.getFirst().pontuacao()).isEqualByComparingTo(ranking.get(1).pontuacao());
        assertThat(ranking.getFirst().proposta()).isSameAs(a); // mesmo total e prazo: menor id
    }

    @Test
    @DisplayName("Elegibilidade: exclui fornecedor inativo, proposta vencida e proposta já decidida")
    void filtraInelegiveis() {
        Proposta valida = proposta(1L, alfa, "100.00", 1);
        Proposta fornecedorInativo = proposta(2L, fornecedor(9L, "Inativo", false), "90.00", 1);
        Proposta vencida = proposta(3L, beta, "80.00", 1, HOJE.minusDays(1));
        Proposta venceHoje = proposta(4L, gama, "95.00", 1, HOJE);
        Proposta rejeitada = proposta(5L, gama, "70.00", 1);
        rejeitada.setStatus(StatusProposta.REJEITADA);

        List<Proposta> elegiveis = avaliador.filtrarElegiveis(
                List.of(valida, fornecedorInativo, vencida, venceHoje, rejeitada), HOJE);

        assertThat(elegiveis).containsExactly(valida, venceHoje);
        assertThat(avaliador.motivoInelegibilidade(vencida, HOJE)).get()
                .extracting("chave").isEqualTo("proposta.nao.elegivel.vencida");
        assertThat(avaliador.motivoInelegibilidade(fornecedorInativo, HOJE)).get()
                .extracting("chave").isEqualTo("proposta.nao.elegivel.fornecedor.inativo");
    }

    @Test
    @DisplayName("Lista vazia gera ranking vazio")
    void semPropostas() {
        assertThat(avaliador.classificar(List.of(), CriterioDecisao.MENOR_PRECO)).isEmpty();
    }
}
