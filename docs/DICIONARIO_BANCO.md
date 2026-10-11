# Dicionário do banco

Esquema atual consultado no PostgreSQL após V001 a V006. São 16 tabelas de negócio e a tabela técnica schema_migrations. Tipos e nulabilidade refletem o banco; migrations em sql registram também índices, CHECKs e valores padrão. DER estrutural em diagramas.

## acessos_cliente

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id_cliente | int4 | Não | PK, FK → clientes.id |
| codigo_hash | bpchar | Não |  |
| expira_em | timestamp | Não |  |
| tentativas | int4 | Não |  |
| emitido_por | int4 | Não | FK → usuarios.id |

## avaliacoes

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int8 | Não | PK |
| id_cliente | int4 | Não | FK → clientes.id |
| id_produto | int4 | Não | FK → produtos.id |
| id_pedido | int4 | Não | FK → pedidos.id |
| nota | int2 | Não |  |
| comentario | varchar | Sim |  |
| criada_em | timestamp | Não |  |
| atualizada_em | timestamp | Não |  |

## categorias

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int4 | Não | PK |
| nome | varchar | Não |  |
| ativo | bool | Não |  |

## clientes

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int4 | Não | PK |
| nome | varchar | Não |  |
| cpf | varchar | Sim |  |
| email | varchar | Não |  |
| telefone | varchar | Sim |  |
| endereco | varchar | Sim |  |
| senha | varchar | Sim |  |
| ativo | bool | Não |  |
| criado_por | int4 | Sim | FK → usuarios.id |

## cupons

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int4 | Não | PK |
| codigo | varchar | Não |  |
| percentual | numeric | Não |  |
| inicio | date | Não |  |
| fim | date | Não |  |
| ativo | bool | Não |  |
| criado_por | int4 | Não | FK → usuarios.id |
| atualizado_por | int4 | Não | FK → usuarios.id |
| atualizado_em | timestamp | Não |  |

## estoque

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id_produto | int4 | Não | PK, FK → produtos.id |
| quantidade | int4 | Não |  |
| estoque_minimo | int4 | Não |  |
| alerta_ativo | bool | Não |  |
| episodio_alerta | int8 | Não |  |

## favoritos

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id_cliente | int4 | Não | PK, PK, FK → clientes.id |
| id_produto | int4 | Não | PK, PK, FK → produtos.id |

## historico_pedidos

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int8 | Não | PK |
| id_pedido | int4 | Não | FK → pedidos.id |
| data_evento | timestamp | Não |  |
| status_anterior | varchar | Sim |  |
| status_novo | varchar | Não |  |
| motivo | varchar | Não |  |
| responsavel | varchar | Não |  |
| id_usuario | int4 | Sim | FK → usuarios.id |
| id_cliente | int4 | Sim | FK → clientes.id |

## itens_pedido

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int4 | Não | PK |
| id_pedido | int4 | Não | FK → pedidos.id |
| id_produto | int4 | Não | FK → produtos.id |
| quantidade | int4 | Não |  |
| preco_unitario | numeric | Não |  |

## leituras_notificacoes

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int8 | Não | PK |
| id_notificacao | int8 | Não | FK → notificacoes.id |
| id_usuario | int4 | Sim | FK → usuarios.id |
| id_cliente | int4 | Sim | FK → clientes.id |
| lida_em | timestamp | Não |  |

## movimentos_estoque

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int8 | Não | PK |
| id_produto | int4 | Não | FK → produtos.id |
| data_movimento | timestamp | Não |  |
| tipo | varchar | Não |  |
| quantidade | int4 | Não |  |
| saldo | int4 | Não |  |
| motivo | varchar | Não |  |
| id_usuario | int4 | Sim | FK → usuarios.id |
| id_cliente | int4 | Sim | FK → clientes.id |
| id_pedido | int4 | Sim | FK → pedidos.id |

## notificacoes

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int8 | Não | PK |
| chave | varchar | Não |  |
| destino | varchar | Não |  |
| id_cliente | int4 | Sim | FK → clientes.id |
| tipo | varchar | Não |  |
| mensagem | varchar | Não |  |
| criada_em | timestamp | Não |  |
| id_pedido | int4 | Sim | FK → pedidos.id |
| id_produto | int4 | Sim | FK → produtos.id |

## pagamentos

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id_pedido | int4 | Não | PK, FK → pedidos.id |
| metodo | varchar | Não |  |
| status | varchar | Não |  |
| simulado | bool | Não |  |
| criado_em | timestamp | Não |  |
| confirmado_em | timestamp | Sim |  |
| confirmado_por | int4 | Sim | FK → usuarios.id |
| cancelado_em | timestamp | Sim |  |

## pedidos

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int4 | Não | PK |
| id_cliente | int4 | Não | FK → clientes.id |
| data_pedido | timestamp | Não |  |
| status | varchar | Não |  |
| total | numeric | Não |  |
| registrado_por | int4 | Sim | FK → usuarios.id |
| subtotal | numeric | Não |  |
| desconto | numeric | Não |  |
| origem | varchar | Não |  |
| chave_compra | uuid | Sim |  |
| destinatario | varchar | Sim |  |
| telefone_entrega | varchar | Sim |  |
| endereco_entrega | varchar | Sim |  |
| id_cupom | int4 | Sim | FK → cupons.id |
| codigo_cupom | varchar | Sim |  |
| percentual_cupom | numeric | Não |  |
| desconto_cupom | numeric | Não |  |
| desconto_pagamento | numeric | Não |  |

## produtos

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int4 | Não | PK |
| nome | varchar | Não |  |
| descricao | text | Sim |  |
| preco | numeric | Não |  |
| id_categoria | int4 | Sim | FK → categorias.id |
| ativo | bool | Não |  |
| criado_por | int4 | Sim | FK → usuarios.id |
| imagem | bytea | Sim |  |

## schema_migrations

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| versao | varchar | Não | PK |
| checksum | varchar | Não |  |
| aplicada_em | timestamp | Não |  |

## usuarios

| Campo | Tipo | Aceita NULL | Chaves |
|---|---|---|---|
| id | int4 | Não | PK |
| nome | varchar | Não |  |
| login | varchar | Não |  |
| senha | varchar | Não |  |
| perfil | varchar | Não |  |
| ativo | bool | Não |  |
