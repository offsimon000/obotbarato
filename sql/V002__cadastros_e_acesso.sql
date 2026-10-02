ALTER TABLE categorias ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE produtos ADD COLUMN imagem BYTEA;
ALTER TABLE produtos ADD CONSTRAINT tamanho_imagem CHECK (octet_length(imagem) <= 5242880);
CREATE UNIQUE INDEX clientes_email_normalizado ON clientes(lower(trim(email)));
CREATE UNIQUE INDEX clientes_cpf_normalizado ON clientes(regexp_replace(cpf, '[^0-9]', '', 'g')) WHERE cpf IS NOT NULL AND cpf <> '';
CREATE UNIQUE INDEX usuarios_login_normalizado ON usuarios(lower(trim(login)));
CREATE UNIQUE INDEX categorias_nome_normalizado ON categorias(lower(trim(nome)));
CREATE TABLE acessos_cliente (
    id_cliente INTEGER PRIMARY KEY REFERENCES clientes(id) ON DELETE CASCADE,
    codigo_hash CHAR(64) NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    tentativas INTEGER NOT NULL DEFAULT 0 CHECK(tentativas BETWEEN 0 AND 5),
    emitido_por INTEGER NOT NULL REFERENCES usuarios(id)
);
CREATE TABLE movimentos_estoque (
    id BIGSERIAL PRIMARY KEY,
    id_produto INTEGER NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
    data_movimento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo VARCHAR(20) NOT NULL CHECK(tipo IN ('inicial', 'entrada', 'saida', 'ajuste', 'venda')),
    quantidade INTEGER NOT NULL,
    saldo INTEGER NOT NULL CHECK(saldo >= 0),
    motivo VARCHAR(255) NOT NULL,
    id_usuario INTEGER REFERENCES usuarios(id),
    id_cliente INTEGER REFERENCES clientes(id)
);
CREATE INDEX movimentos_produto_data ON movimentos_estoque(id_produto, data_movimento DESC);
-- Registra abertura sem inventar um histórico anterior.
INSERT INTO movimentos_estoque(id_produto,tipo,quantidade,saldo,motivo)
SELECT id_produto,'inicial',quantidade,quantidade,'Saldo anterior à implantação do histórico' FROM estoque;
