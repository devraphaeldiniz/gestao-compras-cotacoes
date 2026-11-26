package com.compras.cotacoes.pedido;

import com.compras.cotacoes.pedido.dto.PedidoCompraResponse;
import com.compras.cotacoes.pedido.dto.PedidoItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PedidoCompraMapper {

    @Mapping(target = "cotacaoId", source = "cotacao.id")
    @Mapping(target = "cotacaoCodigo", source = "cotacao.codigo")
    @Mapping(target = "propostaId", source = "proposta.id")
    @Mapping(target = "fornecedorId", source = "fornecedor.id")
    @Mapping(target = "fornecedorRazaoSocial", source = "fornecedor.razaoSocial")
    PedidoCompraResponse paraResposta(PedidoCompra pedido);

    @Mapping(target = "produtoId", source = "produto.id")
    @Mapping(target = "produtoSku", source = "produto.sku")
    @Mapping(target = "produtoNome", source = "produto.nome")
    @Mapping(target = "unidade", source = "produto.unidade")
    @Mapping(target = "quantidadePendente", expression = "java(item.quantidadePendente())")
    PedidoItemResponse paraItem(PedidoItem item);
}
