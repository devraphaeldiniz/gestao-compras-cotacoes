package com.compras.cotacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.compras.cotacoes.cotacao.PrazosCotacaoService;
import com.compras.cotacoes.support.Fabrica;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** Ciclo completo via HTTP contra PostgreSQL real, com autenticação por perfil. */
class FluxoComprasIntegracaoTest extends IntegracaoBase {

    @Autowired
    private PrazosCotacaoService prazos;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Ciclo completo: reposição consolidada, 3 propostas, decisão, recebimento parcial, extrato e histórico")
    void cicloCompletoDeCompra() throws Exception {
        long alfa = criarFornecedor("Alfa Suprimentos");
        long beta = criarFornecedor("Beta Distribuidora");
        long gama = criarFornecedor("Gama Atacado");
        long omega = criarFornecedor("Omega Comercial");
        mockMvc.perform(json(patch("/api/fornecedores/{id}/status", omega), "{\"ativo\":false}").with(comprador()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));

        // 1. Duas saídas abaixo do mínimo: a reposição consolida os dois produtos num único rascunho automático
        long papel = criarProduto("FLX-PAPEL", "50", "20");
        long toner = criarProduto("FLX-TONER", "30", "10");
        saida(papel, 35); // saldo 15 -> (20 - 15) + 20 = 25
        saida(toner, 25); // saldo 5  -> (10 - 5) + 10 = 15

        String lista = mockMvc.perform(get("/api/cotacoes?produtoId={id}", papel).with(comprador()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andReturn().getResponse().getContentAsString();
        long cotacaoId = ((Number) JsonPath.read(lista, "$.conteudo[0].id")).longValue();
        mockMvc.perform(get("/api/cotacoes/{id}", cotacaoId).with(comprador()))
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.origem").value("AUTOMATICA"))
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[?(@.produtoSku == 'FLX-PAPEL')].quantidade").value(25.0))
                .andExpect(jsonPath("$.itens[?(@.produtoSku == 'FLX-TONER')].quantidade").value(15.0));

        // 2. Abre convidando 3 fornecedores ativos
        mockMvc.perform(json(patch("/api/cotacoes/{id}/abrir", cotacaoId),
                        "{\"fornecedoresIds\":[%d,%d,%d],\"dataLimite\":\"%s\"}".formatted(alfa, beta, gama, LocalDate.now().plusDays(7)))
                        .with(comprador()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ABERTA"));

        // 3. Propostas com preço por item; com 2 a decisão é bloqueada; comprador não decide (segregação de funções)
        List<Long> produtos = List.of(papel, toner);
        // Alfa: 25x20 + 15x20 = 800 + frete 30 + 12% (96) = 926,00
        mockMvc.perform(json(post("/api/cotacoes/{id}/propostas", cotacaoId), proposta(alfa, produtos, "20", 5, "30"))
                        .with(comprador()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.valorMercadoria").value(800.0))
                .andExpect(jsonPath("$.valorTotal").value(926.0));
        mockMvc.perform(json(post("/api/cotacoes/{id}/propostas", cotacaoId), proposta(beta, produtos, "19", 2, "80"))
                        .with(comprador()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.valorTotal").value(931.2));
        mockMvc.perform(json(post("/api/cotacoes/{id}/propostas", cotacaoId), proposta(omega, produtos, "10", 1, "0"))
                        .with(comprador()))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(json(post("/api/cotacoes/{id}/decidir", cotacaoId), "{}").with(aprovador()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(containsString("no mínimo 3 propostas válidas")));

        mockMvc.perform(json(post("/api/cotacoes/{id}/propostas", cotacaoId), proposta(gama, produtos, "21", 10, "0"))
                        .with(comprador()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.valorTotal").value(940.8));
        mockMvc.perform(json(post("/api/cotacoes/{id}/decidir", cotacaoId), "{}").with(comprador()))
                .andExpect(status().isForbidden());

        // 4. Decisão pelo aprovador: pedido com os 2 itens e custo efetivo rateado
        String decisao = mockMvc.perform(json(post("/api/cotacoes/{id}/decidir", cotacaoId), "{\"criterio\":\"MENOR_PRECO\"}")
                        .with(aprovador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCotacao").value("FECHADA"))
                .andExpect(jsonPath("$.pedidoCompra.status").value("EMITIDO"))
                .andExpect(jsonPath("$.pedidoCompra.valorTotal").value(926.0))
                .andExpect(jsonPath("$.pedidoCompra.emitidoPor").value("aprovador"))
                .andExpect(jsonPath("$.pedidoCompra.itens", hasSize(2)))
                // papel: (500 + 126 x 500/800) / 25 = 23,15
                .andExpect(jsonPath("$.pedidoCompra.itens[?(@.produtoSku == 'FLX-PAPEL')].custoUnitarioEfetivo").value(23.15))
                .andReturn().getResponse().getContentAsString();
        long pedidoId = ((Number) JsonPath.read(decisao, "$.pedidoCompra.id")).longValue();
        mockMvc.perform(get("/api/cotacoes/{id}", cotacaoId).with(comprador()))
                .andExpect(jsonPath("$.decididoPor").value("aprovador"))
                .andExpect(jsonPath("$.criterioDecisao").value("MENOR_PRECO"))
                .andExpect(jsonPath("$.propostas[?(@.status == 'REJEITADA')]", hasSize(2)));

        // 5. Recebimento parcial e depois o restante
        mockMvc.perform(json(post("/api/pedidos-compra/{id}/receber", pedidoId),
                        "{\"itens\":[{\"produtoId\":%d,\"quantidade\":10}]}".formatted(papel)).with(aprovador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARCIALMENTE_RECEBIDO"))
                .andExpect(jsonPath("$.itens[?(@.produtoSku == 'FLX-PAPEL')].quantidadePendente").value(15.0));
        assertThat(saldo(papel)).isEqualByComparingTo("25");
        mockMvc.perform(post("/api/pedidos-compra/{id}/receber", pedidoId).with(aprovador()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RECEBIDO"));
        assertThat(saldo(papel)).isEqualByComparingTo("40");
        assertThat(saldo(toner)).isEqualByComparingTo("20");
        mockMvc.perform(get("/api/produtos/{id}", papel).with(comprador()))
                .andExpect(jsonPath("$.precoMedioCompra").value(23.15))
                .andExpect(jsonPath("$.necessitaCompra").value(false));

        // 6. Extrato e histórico contam a história inteira
        mockMvc.perform(get("/api/produtos/{id}/movimentacoes", papel).with(comprador()))
                .andExpect(jsonPath("$.totalElementos").value(4))
                .andExpect(jsonPath("$.conteudo[*].tipo", hasItems("RECEBIMENTO", "SAIDA", "SALDO_INICIAL")))
                .andExpect(jsonPath("$.conteudo[0].tipo").value("RECEBIMENTO"))
                .andExpect(jsonPath("$.conteudo[0].usuario").value("aprovador"));
        mockMvc.perform(get("/api/cotacoes/{id}/historico", cotacaoId).with(comprador()))
                .andExpect(jsonPath("$[*].acao", hasItems("COTACAO_CRIADA", "COTACAO_ITEM_ADICIONADO", "COTACAO_ABERTA",
                        "PROPOSTA_REGISTRADA", "COTACAO_DECIDIDA")))
                .andExpect(jsonPath("$[?(@.acao == 'COTACAO_DECIDIDA')].usuario").value("aprovador"));
        mockMvc.perform(get("/api/pedidos-compra/{id}/historico", pedidoId).with(aprovador()))
                .andExpect(jsonPath("$[?(@.acao == 'PEDIDO_RECEBIMENTO')]", hasSize(2)));

        // 7. Erros localizados pelo Accept-Language
        mockMvc.perform(json(post("/api/cotacoes/{id}/decidir", cotacaoId), "{}").with(aprovador())
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(jsonPath("$.mensagem").value(containsString("is not OPEN")));
        mockMvc.perform(json(post("/api/cotacoes/{id}/decidir", cotacaoId), "{}").with(aprovador())
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR"))
                .andExpect(jsonPath("$.mensagem").value(containsString("não está ABERTA")));
    }

    @Test
    @DisplayName("Cancelamentos: cotação pelo comprador e saldo de pedido pelo aprovador, sempre com motivo")
    void cancelamentos() throws Exception {
        long produto = criarProduto("FLX-CANC", "100", "1");
        long cotacaoId = criarCotacaoAberta(produto, 10);
        mockMvc.perform(cancelamento("/api/cotacoes/{id}/cancelar", cotacaoId, "").with(comprador()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(cancelamento("/api/cotacoes/{id}/cancelar", cotacaoId, "Demanda suspensa").with(comprador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelamento").value("Demanda suspensa"))
                .andExpect(jsonPath("$.canceladoPor").value("comprador"));
        mockMvc.perform(cancelamento("/api/cotacoes/{id}/cancelar", cotacaoId, "De novo").with(comprador()))
                .andExpect(status().isUnprocessableContent());

        long pedidoId = pedidoEmitido("FLX-PED-CANC");
        mockMvc.perform(cancelamento("/api/pedidos-compra/{id}/cancelar", pedidoId, "Fornecedor sem estoque").with(comprador()))
                .andExpect(status().isForbidden());
        mockMvc.perform(cancelamento("/api/pedidos-compra/{id}/cancelar", pedidoId, "Fornecedor sem estoque").with(aprovador()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        mockMvc.perform(post("/api/pedidos-compra/{id}/receber", pedidoId).with(aprovador()))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("Produto não entra em duas cotações em andamento; o mesmo produto em duas linhas é recusado")
    void produtoEmUmaCotacaoPorVez() throws Exception {
        long produto = criarProduto("FLX-UNICO", "100", "1");
        criarCotacaoAberta(produto, 5);
        mockMvc.perform(json(post("/api/cotacoes"), "{\"itens\":[{\"produtoId\":%d,\"quantidade\":3}]}".formatted(produto))
                        .with(comprador()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value(containsString("FLX-UNICO")));
        long outro = criarProduto("FLX-DUPLO", "100", "1");
        mockMvc.perform(json(post("/api/cotacoes"),
                        "{\"itens\":[{\"produtoId\":%d,\"quantidade\":1},{\"produtoId\":%d,\"quantidade\":2}]}".formatted(outro, outro))
                        .with(comprador()))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("Prazos: cotação vencida sem 3 propostas é cancelada; com 3 fica pronta para decisão")
    void prazosAutomaticos() throws Exception {
        long semPropostas = criarCotacaoAberta(criarProduto("FLX-PRAZO-1", "100", "1"), 5);
        long produto = criarProduto("FLX-PRAZO-2", "100", "1");
        long completa = criarCotacaoAberta(produto, 5);
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(json(post("/api/cotacoes/{id}/propostas", completa),
                            proposta(criarFornecedor("Prazo " + i), List.of(produto), "10", 3, "0")).with(comprador()))
                    .andExpect(status().isCreated());
        }
        jdbc.update("update cotacoes set data_limite = ? where id in (?, ?)", LocalDate.now().minusDays(1), semPropostas, completa);

        PrazosCotacaoService.Resultado resultado = prazos.processar();

        assertThat(resultado.canceladas()).isGreaterThanOrEqualTo(1);
        assertThat(resultado.prontasParaDecisao()).isGreaterThanOrEqualTo(1);
        mockMvc.perform(get("/api/cotacoes/{id}", semPropostas).with(comprador()))
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.canceladoPor").value("sistema"));
        mockMvc.perform(get("/api/cotacoes/{id}", completa).with(comprador()))
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.prazoEncerradoEm").isNotEmpty())
                .andExpect(jsonPath("$.podeDecidir").value(true));
    }

    @Test
    @DisplayName("Validação e 404 no idioma pedido (header ou ?lang=), CPF/CNPJ com dígito verificador e 409 em duplicidade")
    void mensagensEValidacoes() throws Exception {
        String invalido = "{\"sku\":\"\",\"nome\":\"x\",\"unidade\":\"UN\",\"saldoAtual\":-1,\"estoqueMinimo\":1}";
        mockMvc.perform(json(post("/api/produtos"), invalido).with(comprador()).header(HttpHeaders.ACCEPT_LANGUAGE, "en-US"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[?(@.campo == 'sku')].mensagem").value("is required"));
        mockMvc.perform(json(post("/api/produtos"), invalido).with(comprador()).header(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR"))
                .andExpect(jsonPath("$.campos[?(@.campo == 'sku')].mensagem").value("é obrigatório"));
        mockMvc.perform(get("/api/cotacoes/{id}?lang=en", 987654).with(comprador()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.mensagem").value("Quotation 987654 not found"));
        mockMvc.perform(get("/api/cotacoes/{id}", 987654).with(comprador()))
                .andExpect(jsonPath("$.mensagem").value("Cotação 987654 não encontrada"));

        String documento = Fabrica.cnpj(proximo());
        String fornecedor = "{\"razaoSocial\":\"Dup\",\"documento\":\"%s\",\"email\":\"d@t.co\",\"prazoMedioDias\":1}";
        mockMvc.perform(json(post("/api/fornecedores"), fornecedor.formatted(documento)).with(comprador()))
                .andExpect(status().isCreated());
        mockMvc.perform(json(post("/api/fornecedores"), fornecedor.formatted(documento)).with(comprador()))
                .andExpect(status().isConflict());
        mockMvc.perform(json(post("/api/fornecedores"), fornecedor.formatted("11222333000182")).with(comprador()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("documento"));

        mockMvc.perform(json(post("/api/produtos"), "{\"sku\":\"FLX-DUP\",\"nome\":\"x\",\"unidade\":\"UN\",\"saldoAtual\":5,\"estoqueMinimo\":1}")
                        .with(comprador())).andExpect(status().isCreated());
        mockMvc.perform(json(post("/api/produtos"), "{\"sku\":\"flx-dup\",\"nome\":\"x\",\"unidade\":\"UN\",\"saldoAtual\":5,\"estoqueMinimo\":1}")
                        .with(comprador())).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Listagens paginadas com tamanho limitado a 100 e busca por texto")
    void paginacao() throws Exception {
        for (int i = 0; i < 3; i++) {
            criarProduto("FLX-PAG-" + i, "10", "1");
        }
        mockMvc.perform(get("/api/produtos?busca=flx-pag&tamanho=2").with(comprador()))
                .andExpect(jsonPath("$.conteudo", hasSize(2)))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        mockMvc.perform(get("/api/produtos?busca=flx-pag&tamanho=2&pagina=1").with(comprador()))
                .andExpect(jsonPath("$.conteudo", hasSize(1)));
        mockMvc.perform(get("/api/fornecedores?tamanho=5000").with(comprador()))
                .andExpect(jsonPath("$.tamanho").value(100));
    }

    private void saida(long produtoId, int quantidade) throws Exception {
        mockMvc.perform(json(patch("/api/produtos/{id}/estoque", produtoId),
                        "{\"tipo\":\"SAIDA\",\"quantidade\":%d,\"observacao\":\"Requisição interna\"}".formatted(quantidade))
                        .with(comprador()))
                .andExpect(status().isOk());
    }

    private long pedidoEmitido(String sku) throws Exception {
        long produto = criarProduto(sku, "100", "1");
        long cotacao = criarCotacaoAberta(produto, 10);
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(json(post("/api/cotacoes/{id}/propostas", cotacao),
                    proposta(criarFornecedor(sku + " F" + i), List.of(produto), String.valueOf(10 + i), 3, "0")).with(comprador()));
        }
        String decisao = mockMvc.perform(json(post("/api/cotacoes/{id}/decidir", cotacao), "{}")
                        .contentType(MediaType.APPLICATION_JSON).with(aprovador()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(decisao, "$.pedidoCompra.id")).longValue();
    }
}
