-- Pagamento confirmado não implica entrega. Preserva valores e saldos existentes.
ALTER TABLE pedidos DROP CONSTRAINT pedidos_status_check;
UPDATE pedidos SET status=CASE WHEN origem='balcao' THEN 'entregue' ELSE 'aberto' END
WHERE status='finalizado';
ALTER TABLE pedidos ADD CONSTRAINT pedidos_status_check
CHECK(status IN ('aberto','em_preparo','enviado','entregue','cancelado'));
CREATE TABLE historico_pedidos (
    id BIGSERIAL PRIMARY KEY,
    id_pedido INTEGER NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    data_evento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_anterior VARCHAR(30),
    status_novo VARCHAR(30) NOT NULL CHECK(status_novo IN ('aberto','em_preparo','enviado','entregue','cancelado')),
    motivo VARCHAR(255) NOT NULL,
    responsavel VARCHAR(120) NOT NULL,
    id_usuario INTEGER REFERENCES usuarios(id),
    id_cliente INTEGER REFERENCES clientes(id)
);
CREATE INDEX historico_pedido_data ON historico_pedidos(id_pedido,id);
-- Marca a implantação; não inventa datas ou transições anteriores.
INSERT INTO historico_pedidos(id_pedido,status_novo,motivo,responsavel)
SELECT id,status,'Estado na implantação da gestão de pedidos; histórico anterior indisponível','Migração V4'
FROM pedidos;
ALTER TABLE movimentos_estoque ADD COLUMN id_pedido INTEGER REFERENCES pedidos(id) ON DELETE CASCADE;
ALTER TABLE movimentos_estoque DROP CONSTRAINT movimentos_estoque_tipo_check;
ALTER TABLE movimentos_estoque ADD CONSTRAINT movimentos_estoque_tipo_check
CHECK(tipo IN ('inicial','entrada','saida','ajuste','venda','devolucao'));
CREATE UNIQUE INDEX devolucao_unica_por_pedido_produto ON movimentos_estoque(id_pedido,id_produto)
WHERE tipo='devolucao';
ALTER TABLE pagamentos ADD COLUMN cancelado_em TIMESTAMP;
CREATE INDEX pedidos_status_origem_data ON pedidos(status,origem,data_pedido);
