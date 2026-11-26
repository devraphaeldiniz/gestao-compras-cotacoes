package com.compras.cotacoes.pedido;

import com.compras.cotacoes.pedido.dto.PedidoCompraResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Pedidos de compra", description = "Pedidos emitidos na decisão e seu recebimento")
@RestController
@RequestMapping("/api/pedidos-compra")
@RequiredArgsConstructor
public class PedidoCompraController {

    private final PedidoCompraService pedidoCompraService;

    @Operation(summary = "Lista pedidos de compra")
    @GetMapping
    public List<PedidoCompraResponse> listar(
            @Parameter(description = "EMITIDO, RECEBIDO ou CANCELADO")
            @RequestParam(required = false) StatusPedidoCompra status) {
        return pedidoCompraService.listar(status);
    }

    @Operation(summary = "Recebe o pedido",
            description = "Dá entrada no estoque e atualiza último preço e preço médio ponderado do produto.")
    @PostMapping("/{id}/receber")
    public PedidoCompraResponse receber(@PathVariable Long id) {
        return pedidoCompraService.receber(id);
    }
}
