package com.compras.cotacoes.cotacao;

import com.compras.cotacoes.cotacao.dto.AbrirCotacaoRequest;
import com.compras.cotacoes.cotacao.dto.CotacaoDetalheResponse;
import com.compras.cotacoes.cotacao.dto.CotacaoResumoResponse;
import com.compras.cotacoes.cotacao.dto.CriarCotacaoRequest;
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

@Tag(name = "Cotações", description = "Criação, abertura e consulta das cotações")
@RestController
@RequestMapping("/api/cotacoes")
@RequiredArgsConstructor
public class CotacaoController {

    private final CotacaoService cotacaoService;

    @Operation(summary = "Cria uma cotação manualmente", description = "Status inicial RASCUNHO (padrão) ou ABERTA.")
    @PostMapping
    public ResponseEntity<CotacaoDetalheResponse> criar(@Valid @RequestBody CriarCotacaoRequest request) {
        CotacaoDetalheResponse criada = cotacaoService.criar(request);
        return ResponseEntity.created(URI.create("/api/cotacoes/" + criada.id())).body(criada);
    }

    @Operation(summary = "Lista cotações")
    @GetMapping
    public List<CotacaoResumoResponse> listar(
            @Parameter(description = "RASCUNHO, ABERTA, FECHADA ou CANCELADA")
            @RequestParam(required = false) StatusCotacao status) {
        return cotacaoService.listar(status);
    }

    @Operation(summary = "Detalha a cotação",
            description = "Inclui as propostas calculadas e a prévia de recomendação pelos dois critérios.")
    @GetMapping("/{id}")
    public CotacaoDetalheResponse detalhar(@PathVariable Long id) {
        return cotacaoService.detalhar(id);
    }

    @Operation(summary = "Abre uma cotação em RASCUNHO", description = "Opcionalmente define convidados e data limite.")
    @PatchMapping("/{id}/abrir")
    public CotacaoDetalheResponse abrir(@PathVariable Long id,
                                        @Valid @RequestBody(required = false) AbrirCotacaoRequest request) {
        return cotacaoService.abrir(id, request);
    }
}
