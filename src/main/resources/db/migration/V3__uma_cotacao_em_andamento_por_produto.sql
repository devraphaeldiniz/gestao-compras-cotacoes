-- =====================================================================
-- V3: no máximo uma cotação em andamento (RASCUNHO ou ABERTA) por produto.
--
-- A regra já é checada no serviço e as movimentações de estoque são
-- serializadas por bloqueio de linha; o índice parcial é a última barreira
-- contra duplicidade em condições de corrida (ex.: criação manual ao mesmo
-- tempo que a reposição automática).
-- (V2 fica em db/seed: só existe quando os dados de demonstração são carregados.)
-- =====================================================================

CREATE UNIQUE INDEX uk_cotacoes_produto_em_andamento
    ON cotacoes (produto_id)
    WHERE status IN ('RASCUNHO', 'ABERTA');
