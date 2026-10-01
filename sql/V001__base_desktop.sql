-- Base da versão desktop. Dados de demonstração são instalados separadamente.
CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    login VARCHAR(60) NOT NULL UNIQUE,
    senha VARCHAR(256) NOT NULL,
    perfil VARCHAR(20) NOT NULL DEFAULT 'vendedor' CHECK (perfil IN ('admin','vendedor')),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE categorias (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(80) NOT NULL UNIQUE
);
CREATE TABLE produtos (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    descricao TEXT,
    preco NUMERIC(10,2) NOT NULL CHECK (preco >= 0),
    id_categoria INT REFERENCES categorias(id),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por INT REFERENCES usuarios(id)
);
CREATE TABLE estoque (
    id_produto INT PRIMARY KEY REFERENCES produtos(id) ON DELETE CASCADE,
    quantidade INT NOT NULL DEFAULT 0 CHECK (quantidade >= 0),
    estoque_minimo INT NOT NULL DEFAULT 5 CHECK (estoque_minimo >= 0)
);
CREATE TABLE clientes (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    cpf VARCHAR(14) UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    telefone VARCHAR(20),
    endereco VARCHAR(255),
    senha VARCHAR(256),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por INT REFERENCES usuarios(id)
);
CREATE TABLE pedidos (
    id SERIAL PRIMARY KEY,
    id_cliente INT NOT NULL REFERENCES clientes(id),
    data_pedido TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(30) NOT NULL DEFAULT 'aberto'
        CHECK (status IN ('aberto','finalizado','cancelado')),
    total NUMERIC(10,2) NOT NULL CHECK (total >= 0),
    registrado_por INT REFERENCES usuarios(id)
);
CREATE TABLE itens_pedido (
    id SERIAL PRIMARY KEY,
    id_pedido INT NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    id_produto INT NOT NULL REFERENCES produtos(id),
    quantidade INT NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(10,2) NOT NULL CHECK (preco_unitario >= 0)
);
CREATE TABLE favoritos (
    id_cliente INT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    id_produto INT NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
    PRIMARY KEY (id_cliente, id_produto)
);
CREATE INDEX idx_produtos_categoria ON produtos(id_categoria);
CREATE INDEX idx_pedidos_cliente ON pedidos(id_cliente);
CREATE INDEX idx_pedidos_data ON pedidos(data_pedido);
CREATE INDEX idx_itens_pedido_pedido ON itens_pedido(id_pedido);
CREATE INDEX idx_itens_pedido_produto ON itens_pedido(id_produto);
