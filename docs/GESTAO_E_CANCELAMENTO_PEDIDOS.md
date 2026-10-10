# Gestão, acompanhamento e cancelamento — etapa 6

Desktop Java Swing/PostgreSQL, mantendo o tema branco e azul. Entrega e pagamento são simulações acadêmicas. Esta etapa separa venda no balcão, gestão de pedidos e acompanhamento pessoal.

## Fluxo e permissões

Pedidos novos da loja começam **abertos**, inclusive quando PIX, débito ou crédito já estão confirmados. O pagamento é independente da entrega: `pendente`, `confirmado` ou `cancelado`.

| Ação | Admin / vendedor | Cliente |
|---|---|---|
| Consultar / filtrar pedidos | Todos, inclusive clientes desativados | Somente os próprios, filtrados no banco |
| Consultar itens, valores, entrega e histórico | Todos | Somente os próprios |
| Confirmar boleto simulado | Somente admin, em Pagamentos | Não |
| Aberto → Em preparo | Sim, com pagamento confirmado | Não |
| Em preparo → Enviado → Entregue | Sim, uma etapa por operação | Não |
| Cancelar | Aberto ou em preparo | Somente próprio e aberto |
| Cancelar enviado / entregue | Não | Não |

Não há salto de etapas, retorno a estado anterior ou reabertura de cancelamento. A interface envia o estado que estava sendo exibido; se outra janela já mudou o pedido, a operação exige atualizar a lista. Isso impede que dois cliques avancem duas etapas. A autorização e as regras são conferidas novamente no DAO, independentemente dos botões.

**Venda no balcão** registra retirada imediata como `entregue`, pagamento simulado em dinheiro confirmado e desconto zero. O usuário confirma esse significado antes de gravar. Esse fluxo não representa uma encomenda com entrega posterior; para acompanhamento de entrega, use a compra pela loja. Devolução após envio/retirada e estorno financeiro real estão fora desse módulo.

## Cancelamento e auditoria

- Motivo obrigatório de 3 a 255 caracteres.
- Pedido e pagamento são bloqueados na mesma transação. Cada item devolve a quantidade efetivamente comprada, inclusive se o produto foi desativado depois.
- Itens do mesmo produto são agrupados. Produtos/estoques são bloqueados por ID, como nas compras; o saldo não pode exceder o limite inteiro do banco.
- Devolução, movimentos, cancelamento de pedido/pagamento e histórico confirmam juntos. Falha em qualquer parte desfaz tudo, inclusive devoluções anteriores.
- Cancelamento repetido pelo titular ou pela equipe autorizada retorna sem novos efeitos. Bloqueio do pedido e índice único de devolução por pedido/produto protegem a concorrência.
- O pagamento fictício passa a cancelado, com data. Datas anteriores de confirmação são preservadas. Nenhuma transferência ou estorno real ocorre.
- `historico_pedidos` registra estado anterior/novo, data, motivo, ID e nome do responsável naquele momento. Confirmar boleto também registra evento, mantendo o pedido aberto.
- Movimentos novos de venda/devolução são vinculados ao pedido. Os movimentos históricos anteriores à V4 permanecem preservados sem vínculos inventados.

## Atualização da base e relatórios

A migration `V004__gestao_e_cancelamento_pedidos.sql` adiciona histórico, vínculo de estoque, tipo `devolucao` e data do cancelamento simulado. V1/V2/V3 permanecem intactas.

O status anterior `finalizado` tinha dois significados: balcão passa a `entregue`; loja passa a `aberto`, preservando pagamento confirmado, valores e saldo. Não se presume entrega da loja sem histórico. Pedidos antigos abertos/cancelados preservam o estado e recebem somente uma anotação de implantação. A migration não devolve estoque de cancelamentos legados, pois não existe prova de como foram tratados antes. Para um registro legado inconsistente, a equipe deve conferir o saldo por manutenção; não há correção automática.

Faturamento por período usa **total final de pedidos não cancelados com pagamento confirmado**, independentemente da etapa de entrega. Pendentes/cancelados não contam. Ranking segue a mesma seleção, mas sua receita é o **valor bruto dos itens**, identificado na tela; revisão de atribuição de descontos, indicadores e exportação pertence à etapa 8.

## Como testar pela interface

1. Abra `Iniciar OBotBarato.cmd`. Entre como cliente (`cliente@demo.local` / `cliente123`) e compre um produto por PIX. Anote número e estoque anterior.
2. Em **Meus pedidos**, confira pedido **Aberto**, pagamento **confirmado**, valores e endereço em **Detalhes e histórico**. A criação deve aparecer no acompanhamento.
3. Saia e entre como vendedor (`vendedor` / `vendedor123`) → **Pedidos**. Filtre por nome/e-mail, data `dd/mm/aaaa` (barras automáticas), estado e origem Loja. Filtros vazios mostram todas as opções autorizadas.
4. Selecione o pedido e clique **Avançar entrega**. Confirme: fica **Em preparo**. Confira data e vendedor no histórico. Repita para **Enviado** e **Entregue**, uma etapa por vez. Cliente consulta o mesmo acompanhamento ao atualizar sua lista.
5. Em outra compra por boleto, tente iniciar preparo: deve recusar. Admin (`admin` / `admin123`) confirma em **Pagamentos**, depois a equipe pode iniciar preparo. Confirmação não baixa estoque novamente nem marca entrega.
6. Faça uma nova compra e, ainda aberta, cancele pelo cliente informando motivo. Pedido/pagamento ficam cancelados; saldo volta à quantidade anterior. Atualizar ou repetir a solicitação não devolve saldo novamente.
7. Faça outra compra, avance até preparo e entre como cliente: não pode cancelar. A equipe pode cancelar em preparo. Depois de enviado/entregue, ambas as contas devem receber recusa.
8. Confira motivo, responsável e datas em **Detalhes e histórico**; confira movimento `devolucao` em **Estoque → Histórico**. Valores do pedido cancelado permanecem registrados.
9. No painel **Pedidos → Nova venda no balcão**, crie uma venda. Ao confirmar retirada, ela fica **Entregue**, origem Balcão e pagamento simulado em dinheiro. Não pode ser cancelada por este fluxo.
10. Confira relatórios: pagamento pendente e cancelamentos ficam fora do faturamento. Compra confirmada ainda em preparo entra com o total depois do desconto.

## Verificação automática

`Testar Partes 1 e 2.cmd` também inclui esta etapa. Feche o aplicativo antes de executar, pois a segunda parte reinicia o banco local.

`scripts/testar-integracao.ps1` executa 20 regressões iniciais, 15 cenários de cadastros/acesso, 14 de compra/pagamento e **17 de gestão/cancelamento**, em cluster isolado. Inclui evolução V1/V3 → V4, disputa entre cancelamentos/avanços/envio/pagamento, falha após duas devoluções, privacidade, filtros e faturamento. Constrói 17 telas e aciona botões/Workers reais de checkout e gestão; o teste da gestão fornece as respostas de confirmação/motivo sem exibir diálogos. Evidência: `validacao-pedidos.txt`; capturas: `validacao-ui/gestao-pedidos.png` e `meus-pedidos.png`.

`scripts/verificar-base.ps1` verifica reinício e restauração, incluindo histórico da retirada, pedido cancelado e devolução única. Usa registros próprios identificados e remove somente essas fixtures. O serviço PostgreSQL original continua preservado.

Perfil, cupons, avaliações e notificações estão concluídos na etapa 7. Dashboard, revisão completa dos relatórios, documentação acadêmica e pacote final continuam pendentes.
