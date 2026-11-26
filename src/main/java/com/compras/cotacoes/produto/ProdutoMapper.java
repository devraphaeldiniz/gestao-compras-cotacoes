package com.compras.cotacoes.produto;

import com.compras.cotacoes.produto.dto.CriarProdutoRequest;
import com.compras.cotacoes.produto.dto.ProdutoResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {

    @Mapping(target = "necessitaCompra", expression = "java(produto.precisaReposicao())")
    ProdutoResponse paraResposta(Produto produto);

    List<ProdutoResponse> paraRespostas(List<Produto> produtos);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ultimoPrecoCompra", ignore = true)
    @Mapping(target = "precoMedioCompra", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    Produto paraEntidade(CriarProdutoRequest request);
}
