package com.compras.cotacoes.proposta;

import com.compras.cotacoes.shared.exception.NegocioException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Regra D: elegibilidade e ranking das propostas.
 *
 * <p>Pontuação (0 a 100, 4 casas):
 * <ul>
 *   <li>MENOR_PRECO: (menor total / total da proposta) x 100. É informativa; a ordem vem do valor total.</li>
 *   <li>PONTUACAO_PONDERADA: (menor total / total) x 70 + componente de prazo x 30, em que o prazo vale
 *       30 se a entrega for imediata (0 dias) e, senão, (menor prazo / prazo) x 30. Como o divisor é
 *       sempre o prazo da própria proposta (maior que zero nesse ramo), não há divisão por zero mesmo
 *       quando o menor prazo é 0.</li>
 * </ul>
 */
@Service
public class AvaliadorPropostasService {

    private static final BigDecimal PESO_PRECO = new BigDecimal("70");
    private static final BigDecimal PESO_PRAZO = new BigDecimal("30");
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final int ESCALA_PONTUACAO = 4;
    private static final MathContext PRECISAO = MathContext.DECIMAL64;

    private static final Comparator<Long> ID_NULO_POR_ULTIMO = Comparator.nullsLast(Comparator.naturalOrder());

    public record PropostaAvaliada(Proposta proposta, BigDecimal pontuacao, int posicao) {
    }

    /** Vazio se a proposta pode vencer; senão, o motivo (já como exceção de negócio localizável). */
    public Optional<NegocioException> motivoInelegibilidade(Proposta proposta, LocalDate hoje) {
        if (proposta.getStatus() != StatusProposta.PENDENTE) {
            return Optional.of(new NegocioException("proposta.nao.elegivel.status", proposta.getId(), proposta.getStatus()));
        }
        if (!proposta.getFornecedor().isAtivo()) {
            return Optional.of(new NegocioException("proposta.nao.elegivel.fornecedor.inativo",
                    proposta.getId(), proposta.getFornecedor().getRazaoSocial()));
        }
        if (proposta.getDataValidade().isBefore(hoje)) {
            return Optional.of(new NegocioException("proposta.nao.elegivel.vencida",
                    proposta.getId(), proposta.getDataValidade()));
        }
        return Optional.empty();
    }

    public boolean elegivel(Proposta proposta, LocalDate hoje) {
        return motivoInelegibilidade(proposta, hoje).isEmpty();
    }

    public List<Proposta> filtrarElegiveis(Collection<Proposta> propostas, LocalDate hoje) {
        return propostas.stream().filter(p -> elegivel(p, hoje)).toList();
    }

    /** Ordena as propostas (já filtradas como elegíveis) da melhor para a pior segundo o critério. */
    public List<PropostaAvaliada> classificar(List<Proposta> elegiveis, CriterioDecisao criterio) {
        if (elegiveis.isEmpty()) {
            return List.of();
        }
        BigDecimal menorTotal = elegiveis.stream().map(Proposta::getValorTotal).min(Comparator.naturalOrder()).orElseThrow();
        int menorPrazo = elegiveis.stream().mapToInt(Proposta::getPrazoEntregaDias).min().orElseThrow();

        record Pontuada(Proposta proposta, BigDecimal pontuacao) {
        }

        List<Pontuada> pontuadas = new ArrayList<>(elegiveis.stream()
                .map(p -> new Pontuada(p, pontuar(p, criterio, menorTotal, menorPrazo)))
                .toList());

        Comparator<Pontuada> porTotal = Comparator.comparing(x -> x.proposta().getValorTotal());
        Comparator<Pontuada> porPrazo = Comparator.comparing(x -> x.proposta().getPrazoEntregaDias());
        Comparator<Pontuada> porId = Comparator.comparing(x -> x.proposta().getId(), ID_NULO_POR_ULTIMO);

        Comparator<Pontuada> ordem = switch (criterio) {
            case MENOR_PRECO -> porTotal
                    .thenComparing(porPrazo)
                    .thenComparing(Comparator.comparing((Pontuada x) -> x.proposta().getDataValidade()).reversed())
                    .thenComparing(porId);
            case PONTUACAO_PONDERADA -> Comparator.comparing(Pontuada::pontuacao).reversed()
                    .thenComparing(porTotal)
                    .thenComparing(porPrazo)
                    .thenComparing(porId);
        };
        pontuadas.sort(ordem);

        List<PropostaAvaliada> ranking = new ArrayList<>(pontuadas.size());
        for (int i = 0; i < pontuadas.size(); i++) {
            ranking.add(new PropostaAvaliada(pontuadas.get(i).proposta(), pontuadas.get(i).pontuacao(), i + 1));
        }
        return List.copyOf(ranking);
    }

    private BigDecimal pontuar(Proposta proposta, CriterioDecisao criterio, BigDecimal menorTotal, int menorPrazo) {
        BigDecimal pontuacao = switch (criterio) {
            case MENOR_PRECO -> componentePreco(menorTotal, proposta.getValorTotal(), CEM);
            case PONTUACAO_PONDERADA -> componentePreco(menorTotal, proposta.getValorTotal(), PESO_PRECO)
                    .add(componentePrazo(menorPrazo, proposta.getPrazoEntregaDias()));
        };
        return pontuacao.setScale(ESCALA_PONTUACAO, RoundingMode.HALF_UP);
    }

    private static BigDecimal componentePreco(BigDecimal menorTotal, BigDecimal total, BigDecimal peso) {
        return menorTotal.divide(total, PRECISAO).multiply(peso);
    }

    private static BigDecimal componentePrazo(int menorPrazo, int prazo) {
        if (prazo == 0) {
            return PESO_PRAZO;
        }
        return BigDecimal.valueOf(menorPrazo).divide(BigDecimal.valueOf(prazo), PRECISAO).multiply(PESO_PRAZO);
    }
}
