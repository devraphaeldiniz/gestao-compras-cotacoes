-- =====================================================================
-- V5 (demonstração): um usuário por perfil para explorar a aplicação.
-- SOMENTE PARA AMBIENTE LOCAL/DEMO. Em produção, não carregue db/seed:
-- o ADMIN é criado na primeira subida a partir de APP_ADMIN_SENHA.
--
--   admin      / admin123456   (ADMIN: tudo + usuários)
--   comprador  / comprador123  (COMPRADOR: cadastros, estoque, cotações, propostas)
--   aprovador  / aprovador123  (APROVADOR: decisão, recebimento, cancelamento de pedido)
-- =====================================================================

INSERT INTO usuarios (username, nome, email, senha_hash, perfil, ativo, criado_em, atualizado_em)
VALUES
    ('admin', 'Administrador', 'admin@empresa.exemplo',
     '$2a$12$L1hnPqcpFTRJvbZzjq0TUuQUnpWqdHlaWlFcOsX4kQ1/jUt8/M3Na', 'ADMIN', TRUE, now(), now()),
    ('comprador', 'Carla Compradora', 'compras@empresa.exemplo',
     '$2a$12$2LXDk1s8uH8l1ufRlIcluucw6XP9z/sFtvgDYCBF8lIvd1XLwjDDO', 'COMPRADOR', TRUE, now(), now()),
    ('aprovador', 'Antônio Aprovador', 'aprovacoes@empresa.exemplo',
     '$2a$12$mC1AyXfVmjYl7zowQDOu9OfTqyEAr/9JA2tq6wcf4HzvzCCh5Hreu', 'APROVADOR', TRUE, now(), now());
