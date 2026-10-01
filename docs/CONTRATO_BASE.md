# Contrato da base desktop — primeiro bloco

## Entrada e execução

A entrada oficial é `view.TelaLoginUnificado`. O JAR, os scripts e os nomes antigos `view.TelaLogin` e `view.loja.TelaLojaLogin` encaminham para essa tela. Os aliases antigos só existem para compatibilidade; não mantêm outra implementação de login.

O lançador prepara ou reutiliza o cluster local gerenciado. Os dados ficam em `.local/desenvolvimento/postgres`; usuário, senha aleatória, porta e configuração ficam em arquivos locais ignorados pelo Git. O PostgreSQL escuta somente em `127.0.0.1`. O serviço e a base anteriormente instalados não são usados pelo preparador.

Configurações externas existentes são preservadas. As variáveis `DB_URL`, `DB_USUARIO` e `DB_SENHA` têm precedência sobre o arquivo informado por `-Dobotbarato.config`, ou sobre `config/banco.properties` quando não há arquivo alternativo. Os scripts de manutenção local recusam overrides `DB_*` para evitar agir em outra base.

## Esquema e regras atuais

O banco possui oito tabelas de negócio e uma de controle de migrations:

| Tabela | Chave e regras principais |
|---|---|
| `usuarios` | ID; login único; nome, senha e perfil obrigatórios; perfil `admin` ou `vendedor`; ativo. |
| `categorias` | ID; nome obrigatório e único. |
| `produtos` | ID; nome obrigatório; preço `NUMERIC(10,2)` não negativo; categoria e responsável por cadastro por FK; ativo. |
| `estoque` | Produto como PK/FK; quantidade e mínimo não negativos; padrão inicial 0 e mínimo 5. |
| `clientes` | ID; nome/e-mail obrigatórios; e-mail e CPF únicos; senha opcional para cadastro administrativo; responsável e ativo. CPF, telefone e endereço vazios são normalizados para NULL. |
| `pedidos` | ID; cliente obrigatório por FK; data padrão do servidor; status e total obrigatórios; responsável pelo registro opcional. |
| `itens_pedido` | ID; pedido/produto por FK; quantidade positiva; preço unitário decimal não negativo. |
| `favoritos` | PK composta cliente/produto, impedindo duplicação; FKs. |
| `schema_migrations` | Nome da migration como PK, SHA-256 do arquivo e data de aplicação. |

Existem índices de categoria do produto, cliente/data do pedido e pedido/produto dos itens. As restrições de FK impedem referências inexistentes; as restrições únicas e de valores também são aplicadas no banco.

Os status **atuais** são `aberto`, `finalizado` e `cancelado`; o padrão Java e SQL é `aberto`. O painel e o checkout existentes registram vendas como `finalizado`; relatórios de venda filtram esse status. O fluxo de preparo/envio/entrega e a operação de cancelamento ainda serão implementados na etapa 6, com novas migrations. A existência do valor `cancelado` na base não significa que o cancelamento já esteja disponível pela interface.

## Transações, dinheiro e erros

Cada operação independente recebe uma nova conexão. `Transacao.executar` controla commit, rollback e fechamento. `PedidoDAO` passa a mesma conexão à baixa de estoque, que não fecha a conexão recebida. O UPDATE de baixa só afeta o produto quando há quantidade suficiente, inclusive em compras concorrentes.

Pedido e produto só recebem ID depois de confirmada a transação composta. Falha ao criar estoque inicial desfaz o produto. Falha em qualquer item desfaz pedido, itens e baixas anteriores. Quantidade zero/negativa não representa uma movimentação válida. Atualização direta do saldo permite zero e rejeita negativo.

Preços, subtotais e total nos modelos/persistência usam `BigDecimal`, escala de dois centavos e arredondamento HALF_UP. A API de leitura `double` das telas antigas foi mantida por compatibilidade. As regras adicionais de desconto e pagamento pertencem à etapa 5.

As operações públicas dos DAOs não substituem falha de banco por lista vazia, senha incorreta ou resultado de gravação falso. Operações sem um registro correspondente retornam ausência e os formulários verificam o resultado. `Erros` traduz falhas para mensagens da interface, incluindo indisponibilidade, driver ausente, duplicidade e restrições; os detalhes técnicos vão para o registro. A atualização automática de hash legado pode registrar um aviso sem negar uma autenticação já verificada; a revisão de senhas completa pertence à etapa 4.

## Instalação, demonstração e restauração

`InicializarBanco` aplica migrations na ordem dos nomes, com exclusão mútua transacional e registro de checksum. Reexecutar não reaplica versões. Alterar arquivo já aplicado é rejeitado. Uma migration falha não deixa esquema parcial ou registro de versão confirmado. A base legada sem histórico de migrations permanece como referência; este instalador trabalha em base nova.

`--demo` é separado da criação do esquema. Instala três contas e cinco produtos fictícios. As senhas são geradas no formato SHA-256 com salt aceito pelo código atual. Reexecutar não redefine as contas, duplica produtos ou repõe estoque já consumido.

`backup.ps1` salva o banco local em formato custom do PostgreSQL. `restaurar-backup.ps1` sempre cria um banco novo, preservando o principal, e gera uma configuração separada. A validação confere registros, itens, total, estoque e auditoria após reinício e após restauração.
