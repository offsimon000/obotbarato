# Simulação de uso diário — 10/10/2026

Carga fictícia manual instalada na base local, preservando produtos, imagens, preços, contas e registros anteriores. Datas simuladas: **13/09/2026 a 10/10/2026**. Não são vendas reais; pagamento e entrega continuam simulações acadêmicas.

## Dados criados

| Informação | Quantidade |
|---|---:|
| Clientes novos, identificados com `(Demo)` | 20 |
| Pedidos novos | 60 |
| Vendas no balcão, dinheiro e retirada concluída | 12 |
| Pedidos pela loja | 48 |
| Entregues, contando loja e balcão | 30 |
| Enviados | 6 |
| Em preparo | 6 |
| Abertos com pagamento confirmado | 6 |
| Abertos com boleto pendente | 6 |
| Cancelados antes do envio | 6 |
| Avaliações de compras entregues/pagas | 18 |
| Favoritos dos clientes novos | 40 |
| Produtos no mínimo de estoque | 3 |

Há avisos de pedidos, pagamentos e estoque. Parte dos avisos antigos da carga foi marcada como lida para simular uso; cada cliente mantém suas próprias leituras. Algumas compras usam o cupom **DEMO_DIA10**, de 10%, combinado com o desconto PIX/débito pelas mesmas regras do checkout. A validade deste cupom vai até 07/11/2026; APRESENTA10 e demais cupons anteriores permanecem intactos.

Faturamento líquido exclusivo da simulação: **R$ 14.231,08**, considerando 48 pedidos válidos confirmados. Boletos pendentes e cancelados não entram nesse valor. Em outubro, o dashboard, incluindo a venda anterior, mostrou **R$ 4.677,71**, 27 vendas confirmadas, seis pendentes e quatro cancelados. Os outros dois cancelamentos pertencem a setembro. Esses valores são uma referência logo após a carga; compras, confirmações e cancelamentos posteriores os atualizam.

## Contas para testar

Conta habitual preservada: `cliente@demo.local` / `cliente123`. Ela recebeu oito pedidos novos com exemplos de entrega, envio, preparo, aberto, boleto e cancelamento, sem alteração dos dados ou senha.

Os novos clientes usam `cliente01@uso.obotbarato.test` até `cliente20@uso.obotbarato.test`, todos com senha de demonstração **demo12345**. Exemplo: cliente01 é Ana Martins (Demo). Nomes, endereços e telefones são fictícios; nenhum CPF foi preenchido. Senhas são armazenadas com PBKDF2 e salt individual. Não há envio de e-mail/SMS.

## Como conferir

1. Reabra `Iniciar OBotBarato.cmd`, entre com `admin` / `admin123` e abra Dashboard. Confira movimento em outubro, gráficos, boletos pendentes e estoque crítico.
2. Para ver toda a simulação, digite **13/09/2026** até **10/10/2026** e clique Atualizar. As vendas anteriores que estiverem nesse período também entram no relatório.
3. Em Pedidos, filtre os cinco estados e as duas origens. Consulte detalhes para ver itens, descontos, histórico e responsáveis. Em Pagamentos, consulte boleto pendente; se confirmar algum, o faturamento muda.
4. Em Estoque ou Relatórios → Estoque baixo, confira os três perfumes populares no mínimo. As baixas vêm das vendas; cancelamentos tiveram devolução única. Repor esses itens muda o indicador e o estado do alerta.
5. Entre na loja com cliente01 e confira favoritos, pedidos e avaliações. A conta habitual também permite testar pedidos em vários estados. Se cancelar um aberto ou fizer uma compra, os números deixarão de ser os da referência inicial.

## Reexecutar com segurança

Não é necessário executar novamente para usar os dados: a carga já foi instalada. Para uma base local preparada sem esses dados:

```powershell
./scripts/simular-uso-diario.ps1
# Referência explícita, sem datas futuras:
./scripts/simular-uso-diario.ps1 -DataReferencia 2026-10-10
```

O roteiro faz backup antes de carregar. A carga inteira ocorre em uma transação, com UUIDs fixos e bloqueio contra duas execuções simultâneas. Se já existem todos os pedidos, não recria clientes, não restaura senhas, não repõe saldo nem redefine estados/datas. Conflito de identificadores ou conjunto incompleto bloqueia a execução e preserva os dados. A carga exige administrador ativo e catálogo com saldo suficiente. O lançador comum nunca executa a simulação automaticamente.

`SimulacaoUsoDAO` é um gerador de fixtures: usa os cálculos de `DescontosCompra`, movimentos/devoluções de `EstoqueDAO` e a auditoria/notificação de `GestaoPedidoDAO`. Os estados e eventos fictícios são gerados conforme as transições válidas; os registros novos recebem datas retroativas coerentes. O saldo parte da quantidade disponível no momento da carga. Não é uma reconstrução de inventário histórico anterior à instalação do catálogo, nem modifica datas de registros anteriores.

Nenhuma tabela/coluna/migration nova foi necessária. O catálogo continua com 30 produtos; o saldo foi reduzido pelas vendas efetivas da simulação. Não há importação automática ao abrir o aplicativo.

## Evidências e backups

- `validacao-simulacao-uso.txt`: nove cenários em banco separado, incluindo permissões, sessão recusada, rollback em falha de pagamento, UUID parcial, conservação de dados/saldo, faturamento, avaliações, datas e reexecução.
- `validacao-uso-diario-local.txt`: fingerprints dos registros anteriores preservados e estoque conciliado por produto na base local, além de renderização das telas.
- `ultima-carga-uso-diario.txt`: resultado da carga aplicada.
- Capturas: `validacao-ui/dashboard-uso-diario.png` e `pedidos-uso-diario.png`.
- Backup imediatamente anterior: `.local/backups/obotbarato_20261010_235448_611496dd.dump`.
- Backup com os dados preenchidos: `.local/backups/obotbarato_20261010_235714_a2335a1f.dump`.

O teste dedicado é `scripts/testar-simulacao.ps1`. Ele cria uma base nova `obotbarato_test` no cluster gerenciado, aplica esquema/catálogo e remove somente a base que criou após testar. Se já existe um banco com esse nome, aborta sem alterá-lo. A base principal e o serviço PostgreSQL antigo são preservados. Os 102 cenários da regressão anterior permanecem registrados; esses nove cenários específicos são adicionais.
