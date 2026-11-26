package com.compras.cotacoes.proposta;

import com.compras.cotacoes.proposta.dto.DecidirCotacaoRequest;
import com.compras.cotacoes.proposta.dto.PropostaResponse;
import com.compras.cotacoes.proposta.dto.RegistrarPropostaRequest;
import com.compras.cotacoes.proposta.dto.ResultadoDecisaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Propostas e decisão", description = "Registro de propostas e escolha da vencedora")
@RestController
@RequestMapping("/api/cotacoes/{cotacaoId}")
@RequiredArgsConstructor
public class PropostaController {

    private final PropostaService propostaService;

    @Operation(summary = "Registra a proposta de um fornecedor",
            description = "Exige cotação ABERTA, dentro do prazo, fornecedor ativo e convidado (se houver lista).")
    @PostMapping("/propostas")
    public ResponseEntity<PropostaResponse> registrar(@PathVariable Long cotacaoId,
                                                      @Valid @RequestBody RegistrarPropostaRequest request) {
        PropostaResponse criada = propostaService.registrar(cotacaoId, request);
        return ResponseEntity.created(URI.create("/api/cotacoes/" + cotacaoId + "/propostas/" + criada.id()))
                .body(criada);
    }

    @Operation(summary = "Decide a cotação e emite o pedido de compra",
            description = "Exige no mínimo 3 propostas elegíveis (422 caso contrário). Escolha manual diferente "
                    + "da recomendada exige justificativa.")
    @PostMapping("/decidir")
    public ResultadoDecisaoResponse decidir(@PathVariable Long cotacaoId,
                                            @Valid @RequestBody(required = false) DecidirCotacaoRequest request) {
        return propostaService.decidir(cotacaoId, request == null ? new DecidirCotacaoRequest(null, null, null) : request);
    }
}
