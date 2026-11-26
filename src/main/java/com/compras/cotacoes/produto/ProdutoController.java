package com.compras.cotacoes.produto;

import com.compras.cotacoes.produto.dto.CriarProdutoRequest;
import com.compras.cotacoes.produto.dto.MovimentarEstoqueRequest;
import com.compras.cotacoes.produto.dto.ProdutoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Produtos", description = "Cadastro de produtos e movimentação de estoque")
@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @Operation(summary = "Cadastra um produto",
            description = "Se o saldo inicial já estiver no estoque mínimo ou abaixo, gera uma cotação RASCUNHO de reposição.")
    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody CriarProdutoRequest request) {
        ProdutoResponse criado = produtoService.criar(request);
        return ResponseEntity.created(URI.create("/api/produtos/" + criado.id())).body(criado);
    }

    @Operation(summary = "Lista produtos")
    @GetMapping
    public List<ProdutoResponse> listar(
            @Parameter(description = "true = apenas produtos com saldo no estoque mínimo ou abaixo")
            @RequestParam(defaultValue = "false") boolean apenasNecessidadeCompra) {
        return produtoService.listar(apenasNecessidadeCompra);
    }

    @Operation(summary = "Movimenta o estoque (ENTRADA, SAIDA ou AJUSTE)",
            description = "Impede saldo negativo. Uma SAIDA ou AJUSTE que leve o saldo ao mínimo gera cotação de reposição.")
    @PatchMapping("/{id}/estoque")
    public ProdutoResponse movimentarEstoque(@PathVariable Long id,
                                             @Valid @RequestBody MovimentarEstoqueRequest request) {
        return produtoService.movimentarEstoque(id, request);
    }
}
