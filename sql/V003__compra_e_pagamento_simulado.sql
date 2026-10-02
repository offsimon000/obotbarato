ALTER TABLE pedidos ADD COLUMN subtotal NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK(subtotal >= 0);
ALTER TABLE pedidos ADD COLUMN desconto NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK(desconto >= 0);
ALTER TABLE pedidos ADD COLUMN origem VARCHAR(10) NOT NULL DEFAULT 'balcao' CHECK(origem IN ('balcao','loja'));
ALTER TABLE pedidos ADD COLUMN chave_compra UUID UNIQUE;
ALTER TABLE pedidos ADD COLUMN destinatario VARCHAR(120);
ALTER TABLE pedidos ADD COLUMN telefone_entrega VARCHAR(20);
ALTER TABLE pedidos ADD COLUMN endereco_entrega VARCHAR(255);
UPDATE pedidos SET subtotal=total;
ALTER TABLE pedidos ADD CONSTRAINT valores_pedido CHECK(total = subtotal - desconto AND desconto <= subtotal);
CREATE TABLE pagamentos (
    id_pedido INTEGER PRIMARY KEY REFERENCES pedidos(id) ON DELETE CASCADE,
    metodo VARCHAR(15) NOT NULL CHECK(metodo IN ('credito','debito','pix','boleto','dinheiro')),
    status VARCHAR(15) NOT NULL CHECK(status IN ('pendente','confirmado','cancelado')),
    simulado BOOLEAN NOT NULL DEFAULT TRUE CHECK(simulado),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmado_em TIMESTAMP,
    confirmado_por INTEGER REFERENCES usuarios(id)
);
INSERT INTO pagamentos(id_pedido,metodo,status,confirmado_em)
SELECT id,'dinheiro',CASE WHEN status='finalizado' THEN 'confirmado' WHEN status='cancelado' THEN 'cancelado' ELSE 'pendente' END,
       CASE WHEN status='finalizado' THEN data_pedido ELSE NULL END FROM pedidos;
