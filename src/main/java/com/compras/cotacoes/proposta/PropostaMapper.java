package com.compras.cotacoes.proposta;

import com.compras.cotacoes.proposta.dto.ItemRankingResponse;
import com.compras.cotacoes.proposta.dto.PropostaResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PropostaMapper {

    @Mapping(target = "cotacaoId", source = "cotacao.id")
    @Mapping(target = "fornecedorId", source = "fornecedor.id")
    @Mapping(target = "fornecedorRazaoSocial", source = "fornecedor.razaoSocial")
    @Mapping(target = "fornecedorAtivo", source = "fornecedor.ativo")
    @Mapping(target = "valorMercadoria", expression = "java(proposta.calcularValorMercadoria())")
    PropostaResponse paraResposta(Proposta proposta);

    List<PropostaResponse> paraRespostas(List<Proposta> propostas);

    default ItemRankingResponse paraItemRanking(AvaliadorPropostasService.PropostaAvaliada avaliada) {
        Proposta proposta = avaliada.proposta();
        return new ItemRankingResponse(
                avaliada.posicao(),
                proposta.getId(),
                proposta.getFornecedor().getId(),
                proposta.getFornecedor().getRazaoSocial(),
                proposta.getValorTotal(),
                proposta.getPrazoEntregaDias(),
                avaliada.pontuacao());
    }
}
