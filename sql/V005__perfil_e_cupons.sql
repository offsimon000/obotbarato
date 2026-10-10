CREATE TABLE cupons (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(32) NOT NULL UNIQUE CHECK(codigo ~ '^[A-Z0-9_-]{3,32}$'),
    percentual NUMERIC(5,2) NOT NULL CHECK(percentual > 0 AND percentual <= 100),
    inicio DATE NOT NULL,
    fim DATE NOT NULL CHECK(fim >= inicio),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por INTEGER NOT NULL REFERENCES usuarios(id),
    atualizado_por INTEGER NOT NULL REFERENCES usuarios(id),
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE pedidos ADD COLUMN id_cupom INTEGER REFERENCES cupons(id);
ALTER TABLE pedidos ADD COLUMN codigo_cupom VARCHAR(32);
ALTER TABLE pedidos ADD COLUMN percentual_cupom NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK(percentual_cupom BETWEEN 0 AND 100);
ALTER TABLE pedidos ADD COLUMN desconto_cupom NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK(desconto_cupom >= 0);
ALTER TABLE pedidos ADD COLUMN desconto_pagamento NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK(desconto_pagamento >= 0);
-- Descontos anteriores vieram somente do método de pagamento.
UPDATE pedidos SET desconto_pagamento=desconto;
ALTER TABLE pedidos ADD CONSTRAINT descontos_separados CHECK(desconto=desconto_cupom+desconto_pagamento AND desconto_cupom <= subtotal AND desconto_pagamento <= subtotal-desconto_cupom);
ALTER TABLE pedidos ADD CONSTRAINT snapshot_cupom CHECK(
    (id_cupom IS NULL AND codigo_cupom IS NULL AND percentual_cupom=0 AND desconto_cupom=0)
    OR (id_cupom IS NOT NULL AND codigo_cupom IS NOT NULL AND percentual_cupom>0)
);
