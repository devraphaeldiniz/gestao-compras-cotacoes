package com.compras.cotacoes.pedido;

import com.compras.cotacoes.historico.dto.EventoHistoricoResponse;
import com.compras.cotacoes.pedido.dto.PedidoCompraResponse;
import com.compras.cotacoes.pedido.dto.ReceberPedidoRequest;
import com.compras.cotacoes.shared.web.CancelamentoRequest;
import com.compras.cotacoes.shared.web.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Pedidos de compra", description = "Pedidos emitidos na decisão, recebimento (total ou parcial) e cancelamento")
@RestController
@RequestMapping("/api/pedidos-compra")
@RequiredArgsConstructor
public class PedidoCompraController {

    private final PedidoCompraService pedidoCompraService;

    @Operation(summary = "Lista pedidos de compra (paginado, mais recentes primeiro)")
    @GetMapping
    public PaginaResponse<PedidoCompraResponse> listar(
            @Parameter(description = "EMITIDO, PARCIALMENTE_RECEBIDO, RECEBIDO ou CANCELADO")
            @RequestParam(required = false) StatusPedidoCompra status,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return pedidoCompraService.listar(status, pagina, tamanho);
    }

    @Operation(summary = "Consulta um pedido")
    @GetMapping("/{id}")
    public PedidoCompraResponse buscar(@PathVariable Long id) {
        return pedidoCompraService.buscar(id);
    }

    @Operation(summary = "Recebe o pedido, no todo ou em parte",
            description = "Sem corpo, recebe todo o saldo pendente. Dá entrada no estoque, registra no extrato "
                    + "e atualiza último custo e preço médio ponderado.")
    @PostMapping("/{id}/receber")
    public PedidoCompraResponse receber(@PathVariable Long id,
                                        @Valid @RequestBody(required = false) ReceberPedidoRequest request) {
        return pedidoCompraService.receber(id, request);
    }

    @Operation(summary = "Cancela o saldo não recebido do pedido", description = "Motivo obrigatório.")
    @PatchMapping("/{id}/cancelar")
    public PedidoCompraResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelamentoRequest request) {
        return pedidoCompraService.cancelar(id, request.motivo());
    }

    @Operation(summary = "Histórico do pedido: emissão, recebimentos e cancelamento")
    @GetMapping("/{id}/historico")
    public List<EventoHistoricoResponse> historico(@PathVariable Long id) {
        return pedidoCompraService.historico(id);
    }
}
