package com.compras.cotacoes.cotacao;

import com.compras.cotacoes.cotacao.dto.CotacaoItemResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CotacaoMapper {

    @Mapping(target = "produtoId", source = "produto.id")
    @Mapping(target = "produtoSku", source = "produto.sku")
    @Mapping(target = "produtoNome", source = "produto.nome")
    @Mapping(target = "unidade", source = "produto.unidade")
    @Mapping(target = "saldoAtual", source = "produto.saldoAtual")
    @Mapping(target = "estoqueMinimo", source = "produto.estoqueMinimo")
    CotacaoItemResponse paraItem(CotacaoItem item);

    List<CotacaoItemResponse> paraItens(List<CotacaoItem> itens);
}
