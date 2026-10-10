CREATE TABLE avaliacoes (
    id BIGSERIAL PRIMARY KEY,
    id_cliente INTEGER NOT NULL REFERENCES clientes(id),
    id_produto INTEGER NOT NULL REFERENCES produtos(id),
    id_pedido INTEGER NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    nota SMALLINT NOT NULL CHECK(nota BETWEEN 1 AND 5),
    comentario VARCHAR(1000),
    criada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(id_cliente,id_produto)
);
CREATE TABLE notificacoes (
    id BIGSERIAL PRIMARY KEY,
    chave VARCHAR(140) NOT NULL UNIQUE,
    destino VARCHAR(10) NOT NULL CHECK(destino IN ('cliente','equipe')),
    id_cliente INTEGER REFERENCES clientes(id) ON DELETE CASCADE,
    tipo VARCHAR(20) NOT NULL CHECK(tipo IN ('pedido','pagamento','estoque')),
    mensagem VARCHAR(600) NOT NULL,
    criada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_pedido INTEGER REFERENCES pedidos(id) ON DELETE CASCADE,
    id_produto INTEGER REFERENCES produtos(id) ON DELETE CASCADE,
    CHECK((destino='cliente' AND id_cliente IS NOT NULL) OR (destino='equipe' AND id_cliente IS NULL))
);
CREATE INDEX notificacoes_cliente_data ON notificacoes(id_cliente,id DESC);
CREATE TABLE leituras_notificacoes (
    id BIGSERIAL PRIMARY KEY,
    id_notificacao BIGINT NOT NULL REFERENCES notificacoes(id) ON DELETE CASCADE,
    id_usuario INTEGER REFERENCES usuarios(id) ON DELETE CASCADE,
    id_cliente INTEGER REFERENCES clientes(id) ON DELETE CASCADE,
    lida_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK((id_usuario IS NOT NULL AND id_cliente IS NULL) OR (id_usuario IS NULL AND id_cliente IS NOT NULL))
);
CREATE UNIQUE INDEX leitura_usuario_unica ON leituras_notificacoes(id_notificacao,id_usuario) WHERE id_usuario IS NOT NULL;
CREATE UNIQUE INDEX leitura_cliente_unica ON leituras_notificacoes(id_notificacao,id_cliente) WHERE id_cliente IS NOT NULL;
ALTER TABLE estoque ADD COLUMN alerta_ativo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE estoque ADD COLUMN episodio_alerta BIGINT NOT NULL DEFAULT 0 CHECK(episodio_alerta>=0);
-- Não se inventam avaliações, eventos de pedido ou avisos financeiros anteriores.
