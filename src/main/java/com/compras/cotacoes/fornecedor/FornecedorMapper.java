package com.compras.cotacoes.fornecedor;

import com.compras.cotacoes.fornecedor.dto.CriarFornecedorRequest;
import com.compras.cotacoes.fornecedor.dto.FornecedorResponse;
import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FornecedorMapper {

    FornecedorResponse paraResposta(Fornecedor fornecedor);

    List<FornecedorResponse> paraRespostas(Collection<Fornecedor> fornecedores);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    Fornecedor paraEntidade(CriarFornecedorRequest request);
}
