-- =====================================================================
-- V1: estrutura do Sistema de Gestão de Compras e Cotações
-- =====================================================================

-- Sequências usadas para os códigos legíveis (COT-2026-00001, PC-2026-00001)
CREATE SEQUENCE seq_codigo_cotacao START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_numero_pedido START WITH 1 INCREMENT BY 1;

-- ---------------------------------------------------------------------
-- Produtos
-- ---------------------------------------------------------------------
CREATE TABLE produtos (
    id                   BIGSERIAL      PRIMARY KEY,
    sku                  VARCHAR(50)    NOT NULL,
    nome                 VARCHAR(150)   NOT NULL,
    unidade              VARCHAR(10)    NOT NULL,
    saldo_atual          NUMERIC(19, 4) NOT NULL,
    estoque_minimo       NUMERIC(19, 4) NOT NULL,
    ultimo_preco_compra  NUMERIC(19, 4),
    preco_medio_compra   NUMERIC(19, 4),
    ativo                BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em            TIMESTAMP      NOT NULL,
    atualizado_em        TIMESTAMP      NOT NULL,
    CONSTRAINT uk_produtos_sku UNIQUE (sku),
    CONSTRAINT ck_produtos_unidade CHECK (unidade IN ('UN', 'CX', 'KG', 'PC', 'LT', 'MT')),
    CONSTRAINT ck_produtos_saldo CHECK (saldo_atual >= 0),
    CONSTRAINT ck_produtos_estoque_minimo CHECK (estoque_minimo >= 0)
);

-- ---------------------------------------------------------------------
-- Fornecedores
-- ---------------------------------------------------------------------
CREATE TABLE fornecedores (
    id                BIGSERIAL     PRIMARY KEY,
    razao_social      VARCHAR(180)  NOT NULL,
    documento         VARCHAR(20)   NOT NULL,
    email             VARCHAR(150)  NOT NULL,
    prazo_medio_dias  INTEGER       NOT NULL,
    ativo             BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em         TIMESTAMP     NOT NULL,
    atualizado_em     TIMESTAMP     NOT NULL,
    CONSTRAINT uk_fornecedores_documento UNIQUE (documento),
    CONSTRAINT ck_fornecedores_prazo CHECK (prazo_medio_dias >= 0)
);

CREATE INDEX idx_fornecedores_ativo ON fornecedores (ativo);

-- ---------------------------------------------------------------------
-- Cotações
-- ---------------------------------------------------------------------
CREATE TABLE cotacoes (
    id                     BIGSERIAL      PRIMARY KEY,
    codigo                 VARCHAR(40)    NOT NULL,
    produto_id             BIGINT         NOT NULL,
    quantidade             NUMERIC(19, 4) NOT NULL,
    status                 VARCHAR(20)    NOT NULL,
    data_limite            DATE,
    observacao             VARCHAR(500),
    proposta_vencedora_id  BIGINT,
    criado_em              TIMESTAMP      NOT NULL,
    atualizado_em          TIMESTAMP      NOT NULL,
    CONSTRAINT uk_cotacoes_codigo UNIQUE (codigo),
    CONSTRAINT fk_cotacoes_produto FOREIGN KEY (produto_id) REFERENCES produtos (id),
    CONSTRAINT ck_cotacoes_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_cotacoes_status CHECK (status IN ('RASCUNHO', 'ABERTA', 'FECHADA', 'CANCELADA'))
);

CREATE INDEX idx_cotacoes_produto_status ON cotacoes (produto_id, status);
CREATE INDEX idx_cotacoes_status ON cotacoes (status);

-- Fornecedores convidados para a cotação (lista vazia = cotação aberta ao mercado)
CREATE TABLE cotacao_fornecedores (
    cotacao_id     BIGINT NOT NULL,
    fornecedor_id  BIGINT NOT NULL,
    CONSTRAINT pk_cotacao_fornecedores PRIMARY KEY (cotacao_id, fornecedor_id),
    CONSTRAINT fk_cotacao_fornecedores_cotacao FOREIGN KEY (cotacao_id) REFERENCES cotacoes (id) ON DELETE CASCADE,
    CONSTRAINT fk_cotacao_fornecedores_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedores (id)
);

CREATE INDEX idx_cotacao_fornecedores_fornecedor ON cotacao_fornecedores (fornecedor_id);

-- ---------------------------------------------------------------------
-- Propostas
-- ---------------------------------------------------------------------
CREATE TABLE propostas (
    id                       BIGSERIAL      PRIMARY KEY,
    cotacao_id               BIGINT         NOT NULL,
    fornecedor_id            BIGINT         NOT NULL,
    preco_unitario           NUMERIC(19, 4) NOT NULL,
    valor_frete              NUMERIC(19, 2) NOT NULL DEFAULT 0,
    imposto_percentual       NUMERIC(7, 4)  NOT NULL DEFAULT 0,
    valor_imposto_fixo       NUMERIC(19, 2) NOT NULL DEFAULT 0,
    valor_imposto_calculado  NUMERIC(19, 2) NOT NULL DEFAULT 0,
    valor_total              NUMERIC(19, 2) NOT NULL,
    prazo_entrega_dias       INTEGER        NOT NULL,
    data_validade            DATE           NOT NULL,
    status                   VARCHAR(20)    NOT NULL,
    pontuacao_calculada      NUMERIC(10, 4),
    justificativa_escolha    VARCHAR(500),
    criado_em                TIMESTAMP      NOT NULL,
    atualizado_em            TIMESTAMP      NOT NULL,
    CONSTRAINT uk_propostas_cotacao_fornecedor UNIQUE (cotacao_id, fornecedor_id),
    CONSTRAINT fk_propostas_cotacao FOREIGN KEY (cotacao_id) REFERENCES cotacoes (id),
    CONSTRAINT fk_propostas_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedores (id),
    CONSTRAINT ck_propostas_preco CHECK (preco_unitario > 0),
    CONSTRAINT ck_propostas_frete CHECK (valor_frete >= 0),
    CONSTRAINT ck_propostas_imposto_percentual CHECK (imposto_percentual >= 0),
    CONSTRAINT ck_propostas_imposto_fixo CHECK (valor_imposto_fixo >= 0),
    CONSTRAINT ck_propostas_total CHECK (valor_total > 0),
    CONSTRAINT ck_propostas_prazo CHECK (prazo_entrega_dias >= 0),
    CONSTRAINT ck_propostas_status CHECK (status IN ('PENDENTE', 'VENCEDORA', 'REJEITADA', 'DESQUALIFICADA'))
);

CREATE INDEX idx_propostas_fornecedor ON propostas (fornecedor_id);

-- A vencedora só pode ser uma proposta existente
ALTER TABLE cotacoes
    ADD CONSTRAINT fk_cotacoes_proposta_vencedora
    FOREIGN KEY (proposta_vencedora_id) REFERENCES propostas (id);

-- ---------------------------------------------------------------------
-- Pedidos de compra
-- ---------------------------------------------------------------------
CREATE TABLE pedidos_compra (
    id                  BIGSERIAL      PRIMARY KEY,
    numero_pedido       VARCHAR(40)    NOT NULL,
    cotacao_id          BIGINT         NOT NULL,
    proposta_id         BIGINT         NOT NULL,
    fornecedor_id       BIGINT         NOT NULL,
    produto_id          BIGINT         NOT NULL,
    quantidade          NUMERIC(19, 4) NOT NULL,
    valor_unitario      NUMERIC(19, 4) NOT NULL,
    valor_frete         NUMERIC(19, 2) NOT NULL,
    valor_imposto       NUMERIC(19, 2) NOT NULL,
    valor_total         NUMERIC(19, 2) NOT NULL,
    prazo_entrega_dias  INTEGER        NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    data_recebimento    TIMESTAMP,
    criado_em           TIMESTAMP      NOT NULL,
    atualizado_em       TIMESTAMP      NOT NULL,
    CONSTRAINT uk_pedidos_numero UNIQUE (numero_pedido),
    CONSTRAINT uk_pedidos_cotacao UNIQUE (cotacao_id),
    CONSTRAINT uk_pedidos_proposta UNIQUE (proposta_id),
    CONSTRAINT fk_pedidos_cotacao FOREIGN KEY (cotacao_id) REFERENCES cotacoes (id),
    CONSTRAINT fk_pedidos_proposta FOREIGN KEY (proposta_id) REFERENCES propostas (id),
    CONSTRAINT fk_pedidos_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedores (id),
    CONSTRAINT fk_pedidos_produto FOREIGN KEY (produto_id) REFERENCES produtos (id),
    CONSTRAINT ck_pedidos_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_pedidos_status CHECK (status IN ('EMITIDO', 'RECEBIDO', 'CANCELADO'))
);

CREATE INDEX idx_pedidos_status ON pedidos_compra (status);
CREATE INDEX idx_pedidos_fornecedor ON pedidos_compra (fornecedor_id);
CREATE INDEX idx_pedidos_produto ON pedidos_compra (produto_id);
