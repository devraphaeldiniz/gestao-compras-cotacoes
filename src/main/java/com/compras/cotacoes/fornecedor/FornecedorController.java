package com.compras.cotacoes.fornecedor;

import com.compras.cotacoes.fornecedor.dto.AtualizarFornecedorRequest;
import com.compras.cotacoes.fornecedor.dto.AtualizarStatusFornecedorRequest;
import com.compras.cotacoes.fornecedor.dto.CriarFornecedorRequest;
import com.compras.cotacoes.fornecedor.dto.FornecedorResponse;
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

@Tag(name = "Fornecedores", description = "Cadastro e status dos fornecedores")
@RestController
@RequestMapping("/api/fornecedores")
@RequiredArgsConstructor
public class FornecedorController {

    private final FornecedorService fornecedorService;

    @Operation(summary = "Cadastra um fornecedor", description = "CPF/CNPJ com dígito verificador válido; duplicado retorna 409.")
    @PostMapping
    public ResponseEntity<FornecedorResponse> criar(@Valid @RequestBody CriarFornecedorRequest request) {
        FornecedorResponse criado = fornecedorService.criar(request);
        return ResponseEntity.created(URI.create("/api/fornecedores/" + criado.id())).body(criado);
    }

    @Operation(summary = "Lista fornecedores (paginado)")
    @GetMapping
    public PaginaResponse<FornecedorResponse> listar(
            @Parameter(description = "Filtra ativos (true) ou inativos (false)") @RequestParam(required = false) Boolean ativo,
            @Parameter(description = "Trecho da razão social ou do documento") @RequestParam(required = false) String busca,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return fornecedorService.listar(ativo, busca, pagina, tamanho);
    }

    @Operation(summary = "Consulta um fornecedor")
    @GetMapping("/{id}")
    public FornecedorResponse buscar(@PathVariable Long id) {
        return fornecedorService.buscar(id);
    }

    @Operation(summary = "Atualiza razão social, e-mail e prazo médio", description = "O documento não muda.")
    @PutMapping("/{id}")
    public FornecedorResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarFornecedorRequest request) {
        return fornecedorService.atualizar(id, request);
    }

    @Operation(summary = "Ativa ou inativa um fornecedor",
            description = "Propostas de fornecedores inativos deixam de ser elegíveis na decisão.")
    @PatchMapping("/{id}/status")
    public FornecedorResponse atualizarStatus(@PathVariable Long id,
                                              @Valid @RequestBody AtualizarStatusFornecedorRequest request) {
        return fornecedorService.atualizarStatus(id, request.ativo());
    }
}
