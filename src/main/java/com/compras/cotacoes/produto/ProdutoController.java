package com.compras.cotacoes.produto;

import com.compras.cotacoes.produto.dto.AtualizarProdutoRequest;
import com.compras.cotacoes.produto.dto.AtualizarStatusProdutoRequest;
import com.compras.cotacoes.produto.dto.CriarProdutoRequest;
import com.compras.cotacoes.produto.dto.MovimentacaoEstoqueResponse;
import com.compras.cotacoes.produto.dto.MovimentarEstoqueRequest;
import com.compras.cotacoes.produto.dto.ProdutoResponse;
import com.compras.cotacoes.shared.web.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Produtos", description = "Cadastro de produtos, movimentação e extrato de estoque")
@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;
    private final ExtratoEstoqueService extratoEstoque;

    @Operation(summary = "Cadastra um produto",
            description = "O saldo inicial vira a primeira linha do extrato. Saldo no mínimo ou abaixo dispara a reposição.")
    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody CriarProdutoRequest request) {
        ProdutoResponse criado = produtoService.criar(request);
        return ResponseEntity.created(URI.create("/api/produtos/" + criado.id())).body(criado);
    }

    @Operation(summary = "Lista produtos (paginado)")
    @GetMapping
    public PaginaResponse<ProdutoResponse> listar(
            @Parameter(description = "true = apenas produtos com saldo no estoque mínimo ou abaixo")
            @RequestParam(defaultValue = "false") boolean apenasNecessidadeCompra,
            @Parameter(description = "Filtra ativos (true) ou inativos (false)") @RequestParam(required = false) Boolean ativo,
            @Parameter(description = "Trecho do SKU ou do nome") @RequestParam(required = false) String busca,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return produtoService.listar(apenasNecessidadeCompra, ativo, busca, pagina, tamanho);
    }

    @Operation(summary = "Consulta um produto")
    @GetMapping("/{id}")
    public ProdutoResponse buscar(@PathVariable Long id) {
        return produtoService.buscar(id);
    }

    @Operation(summary = "Atualiza nome, unidade e estoque mínimo", description = "SKU e saldo não mudam por aqui.")
    @PutMapping("/{id}")
    public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarProdutoRequest request) {
        return produtoService.atualizar(id, request);
    }

    @Operation(summary = "Ativa ou inativa um produto", description = "Produto inativo não entra em reposição nem em novas cotações.")
    @PatchMapping("/{id}/status")
    public ProdutoResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AtualizarStatusProdutoRequest request) {
        return produtoService.alterarStatus(id, request.ativo());
    }

    @Operation(summary = "Movimenta o estoque (ENTRADA, SAIDA ou AJUSTE)",
            description = "Impede saldo negativo e registra no extrato. SAIDA ou AJUSTE que leve ao mínimo dispara a reposição.")
    @PatchMapping("/{id}/estoque")
    public ProdutoResponse movimentarEstoque(@PathVariable Long id, @Valid @RequestBody MovimentarEstoqueRequest request) {
        return produtoService.movimentarEstoque(id, request);
    }

    @Operation(summary = "Extrato de estoque do produto (mais recentes primeiro)")
    @GetMapping("/{id}/movimentacoes")
    public PaginaResponse<MovimentacaoEstoqueResponse> extrato(@PathVariable Long id,
                                                               @RequestParam(required = false) Integer pagina,
                                                               @RequestParam(required = false) Integer tamanho) {
        return extratoEstoque.extrato(id, pagina, tamanho);
    }
}
