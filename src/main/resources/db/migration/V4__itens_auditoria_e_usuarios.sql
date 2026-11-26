-- =====================================================================
-- V4: cotação com vários itens, recebimento parcial, cancelamentos,
--     extrato de estoque, histórico de eventos e usuários.
--
-- Os dados existentes (um produto por cotação/pedido) são convertidos
-- para o modelo de itens sem perda de valores.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuários (autenticação e auditoria)
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id             BIGSERIAL    PRIMARY KEY,
    username       VARCHAR(60)  NOT NULL,
    nome           VARCHAR(120) NOT NULL,
    email          VARCHAR(150),
    senha_hash     VARCHAR(100) NOT NULL,
    perfil         VARCHAR(20)  NOT NULL,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMP    NOT NULL,
    atualizado_em  TIMESTAMP    NOT NULL,
    CONSTRAINT uk_usuarios_username UNIQUE (username),
    CONSTRAINT ck_usuarios_perfil CHECK (perfil IN ('ADMIN', 'COMPRADOR', 'APROVADOR'))
);

-- ---------------------------------------------------------------------
-- Cotação: itens, origem, decisão e cancelamento
-- ---------------------------------------------------------------------
CREATE TABLE cotacao_itens (
    id          BIGSERIAL      PRIMARY KEY,
    cotacao_id  BIGINT         NOT NULL,
    produto_id  BIGINT         NOT NULL,
    quantidade  NUMERIC(19, 4) NOT NULL,
    CONSTRAINT fk_cotacao_itens_cotacao FOREIGN KEY (cotacao_id) REFERENCES cotacoes (id) ON DELETE CASCADE,
    CONSTRAINT fk_cotacao_itens_produto FOREIGN KEY (produto_id) REFERENCES produtos (id),
    CONSTRAINT uk_cotacao_itens_produto UNIQUE (cotacao_id, produto_id),
    CONSTRAINT ck_cotacao_itens_quantidade CHECK (quantidade > 0)
);
CREATE INDEX idx_cotacao_itens_produto ON cotacao_itens (produto_id);

INSERT INTO cotacao_itens (cotacao_id, produto_id, quantidade)
SELECT id, produto_id, quantidade FROM cotacoes;

ALTER TABLE cotacoes
    ADD COLUMN origem              VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN criado_por          VARCHAR(60)  NOT NULL DEFAULT 'sistema',
    ADD COLUMN criterio_decisao    VARCHAR(30),
    ADD COLUMN decidido_por        VARCHAR(60),
    ADD COLUMN decidido_em         TIMESTAMP,
    ADD COLUMN prazo_encerrado_em  TIMESTAMP,
    ADD COLUMN motivo_cancelamento VARCHAR(500),
    ADD COLUMN cancelado_por       VARCHAR(60),
    ADD COLUMN cancelado_em        TIMESTAMP,
    ADD CONSTRAINT ck_cotacoes_origem CHECK (origem IN ('AUTOMATICA', 'MANUAL')),
    ADD CONSTRAINT ck_cotacoes_criterio CHECK (criterio_decisao IS NULL OR criterio_decisao IN ('MENOR_PRECO', 'PONTUACAO_PONDERADA'));

-- Cotações de reposição criadas pela Regra A passam a ser identificadas pela origem
UPDATE cotacoes SET origem = 'AUTOMATICA' WHERE observacao LIKE 'Reposição automática%' OR observacao LIKE 'Automatic replenishment%';

-- produto/quantidade agora vivem nos itens (remove também os índices que dependiam deles)
ALTER TABLE cotacoes DROP COLUMN produto_id, DROP COLUMN quantidade;

-- ---------------------------------------------------------------------
-- Proposta: preço por item
-- ---------------------------------------------------------------------
CREATE TABLE proposta_itens (
    id               BIGSERIAL      PRIMARY KEY,
    proposta_id      BIGINT         NOT NULL,
    cotacao_item_id  BIGINT         NOT NULL,
    preco_unitario   NUMERIC(19, 4) NOT NULL,
    valor_mercadoria NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_proposta_itens_proposta FOREIGN KEY (proposta_id) REFERENCES propostas (id) ON DELETE CASCADE,
    CONSTRAINT fk_proposta_itens_cotacao_item FOREIGN KEY (cotacao_item_id) REFERENCES cotacao_itens (id),
    CONSTRAINT uk_proposta_itens UNIQUE (proposta_id, cotacao_item_id),
    CONSTRAINT ck_proposta_itens_preco CHECK (preco_unitario > 0)
);

INSERT INTO proposta_itens (proposta_id, cotacao_item_id, preco_unitario, valor_mercadoria)
SELECT p.id, ci.id, p.preco_unitario, ROUND(ci.quantidade * p.preco_unitario, 2)
FROM propostas p
JOIN cotacao_itens ci ON ci.cotacao_id = p.cotacao_id;

ALTER TABLE propostas ADD COLUMN valor_mercadoria NUMERIC(19, 2);
UPDATE propostas SET valor_mercadoria = valor_total - valor_frete - valor_imposto_calculado;
ALTER TABLE propostas
    ALTER COLUMN valor_mercadoria SET NOT NULL,
    DROP COLUMN preco_unitario;

-- ---------------------------------------------------------------------
-- Pedido: itens, recebimento parcial e cancelamento
-- ---------------------------------------------------------------------
CREATE TABLE pedido_itens (
    id                     BIGSERIAL      PRIMARY KEY,
    pedido_id              BIGINT         NOT NULL,
    produto_id             BIGINT         NOT NULL,
    quantidade             NUMERIC(19, 4) NOT NULL,
    quantidade_recebida    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    valor_unitario         NUMERIC(19, 4) NOT NULL,
    custo_unitario_efetivo NUMERIC(19, 4) NOT NULL,
    CONSTRAINT fk_pedido_itens_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos_compra (id) ON DELETE CASCADE,
    CONSTRAINT fk_pedido_itens_produto FOREIGN KEY (produto_id) REFERENCES produtos (id),
    CONSTRAINT uk_pedido_itens_produto UNIQUE (pedido_id, produto_id),
    CONSTRAINT ck_pedido_itens_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_pedido_itens_recebida CHECK (quantidade_recebida >= 0 AND quantidade_recebida <= quantidade)
);
CREATE INDEX idx_pedido_itens_produto ON pedido_itens (produto_id);

INSERT INTO pedido_itens (pedido_id, produto_id, quantidade, quantidade_recebida, valor_unitario, custo_unitario_efetivo)
SELECT id, produto_id, quantidade,
       CASE WHEN status = 'RECEBIDO' THEN quantidade ELSE 0 END,
       valor_unitario, ROUND(valor_total / quantidade, 4)
FROM pedidos_compra;

ALTER TABLE pedidos_compra ADD COLUMN valor_mercadoria NUMERIC(19, 2);
UPDATE pedidos_compra SET valor_mercadoria = valor_total - valor_frete - valor_imposto;
ALTER TABLE pedidos_compra
    ALTER COLUMN valor_mercadoria SET NOT NULL,
    DROP COLUMN produto_id,
    DROP COLUMN quantidade,
    DROP COLUMN valor_unitario,
    ADD COLUMN emitido_por         VARCHAR(60) NOT NULL DEFAULT 'sistema',
    ADD COLUMN motivo_cancelamento VARCHAR(500),
    ADD COLUMN cancelado_por       VARCHAR(60),
    ADD COLUMN cancelado_em        TIMESTAMP,
    ALTER COLUMN status TYPE VARCHAR(30),
    DROP CONSTRAINT ck_pedidos_status,
    ADD CONSTRAINT ck_pedidos_status CHECK (status IN ('EMITIDO', 'PARCIALMENTE_RECEBIDO', 'RECEBIDO', 'CANCELADO'));

-- ---------------------------------------------------------------------
-- Extrato de estoque
-- ---------------------------------------------------------------------
CREATE TABLE movimentacoes_estoque (
    id               BIGSERIAL      PRIMARY KEY,
    produto_id       BIGINT         NOT NULL,
    tipo             VARCHAR(20)    NOT NULL,
    quantidade       NUMERIC(19, 4) NOT NULL,
    saldo_anterior   NUMERIC(19, 4) NOT NULL,
    saldo_posterior  NUMERIC(19, 4) NOT NULL,
    custo_unitario   NUMERIC(19, 4),
    referencia       VARCHAR(60),
    observacao       VARCHAR(500),
    usuario          VARCHAR(60)    NOT NULL,
    criado_em        TIMESTAMP      NOT NULL,
    CONSTRAINT fk_movimentacoes_produto FOREIGN KEY (produto_id) REFERENCES produtos (id),
    CONSTRAINT ck_movimentacoes_tipo CHECK (tipo IN ('SALDO_INICIAL', 'ENTRADA', 'SAIDA', 'AJUSTE', 'RECEBIMENTO')),
    CONSTRAINT ck_movimentacoes_saldo CHECK (saldo_posterior >= 0)
);
CREATE INDEX idx_movimentacoes_produto_data ON movimentacoes_estoque (produto_id, criado_em DESC, id DESC);

-- Ponto de partida do extrato para os produtos que já existiam
INSERT INTO movimentacoes_estoque (produto_id, tipo, quantidade, saldo_anterior, saldo_posterior, observacao, usuario, criado_em)
SELECT id, 'SALDO_INICIAL', saldo_atual, 0, saldo_atual, 'Saldo existente na migração V4', 'sistema', now()
FROM produtos;

-- ---------------------------------------------------------------------
-- Histórico de eventos (quem fez o quê e quando)
-- ---------------------------------------------------------------------
CREATE TABLE historico_eventos (
    id          BIGSERIAL     PRIMARY KEY,
    entidade    VARCHAR(20)   NOT NULL,
    entidade_id BIGINT        NOT NULL,
    acao        VARCHAR(40)   NOT NULL,
    detalhe     VARCHAR(1000),
    usuario     VARCHAR(60)   NOT NULL,
    criado_em   TIMESTAMP     NOT NULL,
    CONSTRAINT ck_historico_entidade CHECK (entidade IN ('PRODUTO', 'FORNECEDOR', 'COTACAO', 'PEDIDO'))
);
CREATE INDEX idx_historico_entidade ON historico_eventos (entidade, entidade_id, criado_em, id);

-- ---------------------------------------------------------------------
-- Notificações enviadas a fornecedores (evita convite ou lembrete duplicado)
-- ---------------------------------------------------------------------
CREATE TABLE notificacoes_fornecedor (
    id            BIGSERIAL   PRIMARY KEY,
    cotacao_id    BIGINT      NOT NULL,
    fornecedor_id BIGINT      NOT NULL,
    tipo          VARCHAR(20) NOT NULL,
    enviado_em    TIMESTAMP   NOT NULL,
    CONSTRAINT fk_notificacoes_cotacao FOREIGN KEY (cotacao_id) REFERENCES cotacoes (id) ON DELETE CASCADE,
    CONSTRAINT fk_notificacoes_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedores (id),
    CONSTRAINT uk_notificacoes UNIQUE (cotacao_id, fornecedor_id, tipo),
    CONSTRAINT ck_notificacoes_tipo CHECK (tipo IN ('CONVITE', 'LEMBRETE'))
);
