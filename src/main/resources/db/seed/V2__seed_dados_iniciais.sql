-- =====================================================================
-- V2: dados de demonstração (fica em db/seed; os testes não carregam)
--
-- Cenário: o papel A4 está abaixo do estoque mínimo e já tem uma cotação
-- ABERTA com 3 propostas, pronta para decisão. Os dois critérios apontam
-- vencedores diferentes, o que mostra bem a diferença entre eles:
--   MENOR_PRECO         -> Papelaria Central (R$ 927,42)
--   PONTUACAO_PONDERADA -> Distribuidora Norte (R$ 932,24, entrega em 2 dias)
-- =====================================================================

INSERT INTO produtos (sku, nome, unidade, saldo_atual, estoque_minimo, ultimo_preco_compra, preco_medio_compra, ativo, criado_em, atualizado_em)
VALUES
    ('PAP-A4-500', 'Papel sulfite A4 75g - caixa com 10 resmas', 'CX', 8, 20, 26.5000, 25.8000, TRUE, now(), now()),
    ('TON-HP-85A', 'Toner compatível HP 85A', 'UN', 15, 5, 89.9000, 92.3000, TRUE, now(), now()),
    ('LUV-NIT-M', 'Luva nitrílica tamanho M - caixa com 100', 'CX', 40, 30, 32.0000, 31.4000, TRUE, now(), now());

INSERT INTO fornecedores (razao_social, documento, email, prazo_medio_dias, ativo, criado_em, atualizado_em)
VALUES
    ('Papelaria Central Ltda', '11222333000181', 'vendas@papelariacentral.exemplo', 5, TRUE, now(), now()),
    ('Distribuidora Norte S.A.', '22333444000172', 'comercial@distnorte.exemplo', 2, TRUE, now(), now()),
    ('Atacado Sul Suprimentos ME', '33444555000163', 'cotacoes@atacadosul.exemplo', 10, TRUE, now(), now()),
    ('Comercial Antiga Eireli', '44555666000154', 'contato@comercialantiga.exemplo', 15, FALSE, now(), now());

-- Cotação de reposição do papel A4: (20 - 8) + 20 = 32 caixas
INSERT INTO cotacoes (codigo, produto_id, quantidade, status, data_limite, observacao, criado_em, atualizado_em)
SELECT 'COT-' || to_char(now(), 'YYYY') || '-' || lpad(nextval('seq_codigo_cotacao')::text, 5, '0'),
       p.id, 32, 'ABERTA', current_date + 15,
       'Reposição automática: saldo 8 atingiu o estoque mínimo 20 do produto PAP-A4-500',
       now(), now()
FROM produtos p
WHERE p.sku = 'PAP-A4-500';

INSERT INTO cotacao_fornecedores (cotacao_id, fornecedor_id)
SELECT c.id, f.id
FROM cotacoes c
JOIN produtos p ON p.id = c.produto_id AND p.sku = 'PAP-A4-500'
JOIN fornecedores f ON f.documento IN ('11222333000181', '22333444000172', '33444555000163');

-- Valores já calculados com a mesma fórmula da CalculadoraPropostaService:
--   mercadoria = 32 x preço; imposto = mercadoria x 12% + fixo; total = mercadoria + frete + imposto
INSERT INTO propostas (cotacao_id, fornecedor_id, preco_unitario, valor_frete, imposto_percentual, valor_imposto_fixo,
                       valor_imposto_calculado, valor_total, prazo_entrega_dias, data_validade, status, criado_em, atualizado_em)
SELECT c.id, f.id, v.preco, v.frete, 12, v.fixo, v.imposto, v.total, v.prazo, current_date + 30, 'PENDENTE', now(), now()
FROM cotacoes c
JOIN produtos p ON p.id = c.produto_id AND p.sku = 'PAP-A4-500'
JOIN (VALUES
        ('11222333000181', 24.9000, 35.00,  0.00, 95.62,  927.42, 5),
        ('22333444000172', 23.5000, 80.00, 10.00, 100.24, 932.24, 2),
        ('33444555000163', 22.9000, 120.00, 0.00, 87.94,  940.74, 10)
     ) AS v (documento, preco, frete, fixo, imposto, total, prazo) ON TRUE
JOIN fornecedores f ON f.documento = v.documento;
