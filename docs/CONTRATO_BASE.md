# Contrato da base desktop — fundação, cadastros e compra

## Entrada e execução

A entrada oficial é `view.TelaLoginUnificado`. O JAR, os scripts e os nomes antigos `view.TelaLogin` e `view.loja.TelaLojaLogin` encaminham para essa tela. Os aliases antigos só existem para compatibilidade; não mantêm outra implementação de login.

O lançador prepara ou reutiliza o cluster local gerenciado. Os dados ficam em `.local/desenvolvimento/postgres`; usuário, senha aleatória, porta e configuração ficam em arquivos locais ignorados pelo Git. O PostgreSQL escuta somente em `127.0.0.1`. O serviço e a base anteriormente instalados não são usados pelo preparador.

Configurações externas existentes são preservadas. As variáveis `DB_URL`, `DB_USUARIO` e `DB_SENHA` têm precedência sobre o arquivo informado por `-Dobotbarato.config`, ou sobre `config/banco.properties` quando não há arquivo alternativo. Os scripts de manutenção local recusam overrides `DB_*` para evitar agir em outra base.

## Esquema e regras atuais

O banco possui treze tabelas de negócio e uma de controle de migrations:

| Tabela | Chave e regras principais |
|---|---|
| `usuarios` | ID; login único; nome, senha e perfil obrigatórios; perfil `admin` ou `vendedor`; ativo. |
| `categorias` | ID; nome obrigatório e único; ativo. |
| `produtos` | ID; nome obrigatório; preço `NUMERIC(10,2)` não negativo; categoria e responsável por cadastro por FK; ativo. |
| `estoque` | Produto como PK/FK; quantidade e mínimo não negativos; padrão inicial 0 e mínimo 5. |
| `clientes` | ID; nome/e-mail obrigatórios; e-mail e CPF únicos; senha opcional para cadastro administrativo; responsável e ativo. CPF, telefone e endereço vazios são normalizados para NULL. |
| `pedidos` | ID; cliente obrigatório por FK; data padrão do servidor; status e total obrigatórios; responsável pelo registro opcional. |
| `itens_pedido` | ID; pedido/produto por FK; quantidade positiva; preço unitário decimal não negativo. |
| `favoritos` | PK composta cliente/produto, impedindo duplicação; FKs. |
| `acessos_cliente` | Cliente PK/FK; hash do código, validade, tentativas e emissor. |
| `movimentos_estoque` | ID; produto/data/tipo/delta/saldo/motivo e responsável. |
| `pagamentos` | Pedido PK/FK; método e status de simulação, datas e confirmação administrativa. |
| `historico_pedidos` | Pedido por FK; estado anterior/novo, data, motivo, IDs e nome do responsável no momento da operação. |
| `cupons` | Código único normalizado, percentual, validade inclusiva, ativação e responsáveis. |
| `schema_migrations` | Nome da migration como PK, SHA-256 do arquivo e data de aplicação. |

Existem índices de categoria do produto, cliente/data do pedido e pedido/produto dos itens. As restrições de FK impedem referências inexistentes; as restrições únicas e de valores também são aplicadas no banco.

Os status atuais são `aberto`, `em_preparo`, `enviado`, `entregue` e `cancelado`. Toda compra nova da loja começa aberta; pagamento confirmado é independente da entrega. Venda no balcão registra retirada entregue e pagamento simulado em dinheiro. Equipe avança uma etapa por vez, com pagamento confirmado; cliente cancela próprio aberto e equipe cancela antes do envio. Faturamento seleciona pagamentos confirmados e pedidos não cancelados. Regras completas: GESTAO_E_CANCELAMENTO_PEDIDOS.md.

## Transações, dinheiro e erros

Cada operação independente recebe uma nova conexão. `Transacao.executar` controla commit, rollback e fechamento. `PedidoDAO` passa a mesma conexão à baixa de estoque, que não fecha a conexão recebida. O UPDATE de baixa só afeta o produto quando há quantidade suficiente, inclusive em compras concorrentes.

Pedido e produto só recebem ID depois de confirmada a transação composta. Falha ao criar estoque inicial desfaz o produto. Falha em qualquer item desfaz pedido, itens e baixas anteriores. Quantidade zero/negativa não representa uma movimentação válida. Atualização direta do saldo permite zero e rejeita negativo.

Preços, subtotais e total nos modelos/persistência usam `BigDecimal`, escala de dois centavos e arredondamento HALF_UP. A API de leitura `double` das telas antigas foi mantida por compatibilidade. PIX aplica 10% e débito 5%; subtotal/desconto/total e pagamento são persistidos na etapa 5.

As operações públicas dos DAOs não substituem falha de banco por lista vazia, senha incorreta ou resultado de gravação falso. Operações sem um registro correspondente retornam ausência e os formulários verificam o resultado. `Erros` traduz falhas para mensagens da interface, incluindo indisponibilidade, driver ausente, duplicidade e restrições; os detalhes técnicos vão para o registro. Falhas de migração de hash são propagadas; MD5 e SHA256+salt antigos são migrados para PBKDF2 no login. As permissões são revalidadas por `Acesso` no banco.

## Instalação, demonstração e restauração

`InicializarBanco` aplica migrations na ordem dos nomes, com exclusão mútua transacional e registro de checksum. Reexecutar não reaplica versões. Alterar arquivo já aplicado é rejeitado. Uma migration falha não deixa esquema parcial ou registro de versão confirmado. A base legada sem histórico de migrations permanece como referência; este instalador trabalha em base nova.

`--demo` é separado da criação do esquema. Instala três contas, cinco produtos fictícios e APRESENTA10 (10%, 30 dias). As senhas são geradas com PBKDF2-HMAC-SHA256 e salt aleatório. Reexecutar não redefine as contas, duplica produtos ou repõe estoque já consumido.

`backup.ps1` salva o banco local em formato custom do PostgreSQL. `restaurar-backup.ps1` sempre cria um banco novo, preservando o principal, e gera uma configuração separada. A validação confere registros, itens, total, estoque e auditoria após reinício e após restauração.

## Evolução da parte 2

A V2 preserva a V1 e adiciona manutenção, índices normalizados, imagens, recuperação por código e histórico de estoque. Produto, saldo inicial e movimento são atômicos; vendas também registram movimentos dentro da mesma transação. O alerta lê o mínimo salvo. Regras completas, matriz e formatos: PARTE_2_CADASTROS_E_ACESSO.md. Testes: COMO_TESTAR_PARTES_1_E_2.md.

## Compra e pagamento

A V3 inclui subtotal/desconto, origem, UUID idempotente, dados de entrega e pagamentos. O total deve ser subtotal menos desconto. Crédito/débito/PIX são confirmados como simulação; boleto exige admin. Pedidos anteriores preservam valores e recebem pagamento em dinheiro correspondente ao status anterior. Regras e testes: COMPRA_E_PAGAMENTO_SIMULADO.md.

## Gestão e cancelamento

A V4 separa o fluxo logístico do pagamento: antigo finalizado de balcão vira entregue; de loja vira aberto com pagamento preservado. Registra o estado da implantação, sem inventar história anterior nem devolver saldo de cancelamentos legados. Histórico de status, vínculo dos novos movimentos ao pedido, tipo devolucao, índice único de devolução por pedido/produto e data de cancelamento do pagamento são acrescentados sem alterar as migrations anteriores. Cancelamento exige motivo e confirma devolução, movimento, pedido, pagamento e histórico numa única transação; repetição autorizada não duplica efeitos. Consultas pessoais sempre restringem o titular no banco.

## Datas na interface

Desde 09/10, filtros e relatórios apresentam `dd/mm/aaaa` com barras automáticas e validação estrita em `Datas`. Detalhes/históricos apresentam `dd/mm/aaaa HH:mm`. A conversão para LocalDate/ISO ocorre ao chamar os DAOs; armazenamento TIMESTAMP/DATE e parâmetros SQL continuam tipados. A máscara e o formato visual não alteram dados anteriores nem exigem migration. Guia: AJUSTES_INTERFACE_E_DATAS.md.

## Perfil e cupons

V5 acrescenta cupons e snapshots de código/percentual/desconto do cupom/desconto de pagamento no pedido. Cupom reduz o subtotal primeiro; PIX/débito incidem no restante. Desconto total deve ser a soma das parcelas. Descontos anteriores são preservados como desconto de pagamento. Perfil é privado e exige senha atual; atualização dos dados/senha e revogação de códigos pendentes são atômicas. Entregas já gravadas não mudam quando o perfil é editado. Guia: PERFIL_E_CUPONS.md.
