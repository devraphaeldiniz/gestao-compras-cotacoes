package com.compras.cotacoes.cotacao;

import com.compras.cotacoes.cotacao.dto.CotacaoResumoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CotacaoMapper {

    @Mapping(target = "id", source = "cotacao.id")
    @Mapping(target = "produtoId", source = "cotacao.produto.id")
    @Mapping(target = "produtoSku", source = "cotacao.produto.sku")
    @Mapping(target = "produtoNome", source = "cotacao.produto.nome")
    @Mapping(target = "unidade", source = "cotacao.produto.unidade")
    @Mapping(target = "quantidadePropostas", source = "quantidadePropostas")
    CotacaoResumoResponse paraResumo(Cotacao cotacao, long quantidadePropostas);
}
