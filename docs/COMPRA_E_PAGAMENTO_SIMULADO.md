# Compra e pagamento simulado — etapa 5

Desktop Swing, mantendo o tema existente. A etapa implementa compra, descontos, entrega informada e pagamento fictício. Preparo, envio, entrega e cancelamento também estão implementados na etapa 6; veja GESTAO_E_CANCELAMENTO_PEDIDOS.md.

## Regras implementadas

- Carrinho permite adicionar, remover, limpar e alterar quantidade. Adição pela galeria e pelos favoritos usa a mesma consulta de disponibilidade. Alterar quantidade também atualiza o preço do produto e permite revisar o total.
- `Carrinho` entrega cópias dos itens e bloqueia alterações enquanto existe checkout reservado. Outra janela não consegue iniciar uma segunda confirmação. Fechar sem comprar libera o bloqueio e preserva os itens.
- Checkout mostra subtotal e exige destinatário, telefone e endereço. Os dados ficam como snapshot do pedido, sem alterar automaticamente o perfil cadastral do cliente.
- PIX: 10% de desconto; débito: 5%; crédito e boleto: sem desconto. `PagamentoSimulado` calcula os valores em BigDecimal, com arredondamento para centavos.
- Crédito/débito/PIX são aprovados automaticamente como **simulação acadêmica**. Boleto permanece pendente; somente admin confirma sua simulação no painel Pagamentos. Nenhuma integração financeira, cobrança, dados de cartão, QR Code bancário ou boleto real é utilizada.
- `CompraDAO` revalida sessão, cliente ativo, produtos/categorias ativos, preço e estoque antes de gravar. Preço diferente exige atualização do carrinho e nova revisão; não aplica aumento silenciosamente.
- Produtos/estoques são bloqueados em ordem de ID dentro da transação. Pedido, itens, baixa, movimentos e pagamento confirmam juntos; falha desfaz tudo.
- Cada tentativa recebe UUID persistido em `pedidos.chave_compra`. A chave é serializada com advisory lock; confirmação repetida retorna o pedido original e não baixa estoque novamente. Fechar/reabrir sem alterar o carrinho preserva a chave para uma eventual repetição após falha de resposta.
- O carrinho é limpo assim que a gravação é confirmada, antes de escolher Continuar comprando, Ver meus pedidos ou fechar a janela. O número do pedido e os valores efetivamente gravados aparecem na confirmação.
- Histórico pessoal exibe método/status do pagamento. Detalhes consultam itens, valores e entrega somente para a equipe ou o dono do pedido.

## Estrutura

`V003__compra_e_pagamento_simulado.sql` adiciona subtotal, desconto, origem, chave UUID e dados de entrega em pedidos; cria pagamentos com método, status, indicador obrigatório de simulação, datas e responsável pela confirmação administrativa. Restrição: total = subtotal − desconto, com valores não negativos e desconto limitado ao subtotal.

Pedidos anteriores recebem subtotal igual ao total, desconto zero, origem balcão e pagamento em dinheiro correspondente ao status anterior. As vendas do painel continuam com desconto zero/pagamento fictício em dinheiro; sua gravação agora revalida preço e ativação do produto e é exclusiva da equipe.

Com a V4, toda compra nova da loja começa `aberto`, com pagamento próprio pendente/confirmado. A confirmação de boleto muda somente o pagamento e registra evento no histórico. Preparo/envio/entrega são operações separadas da equipe. Faturamento soma total final de pedidos não cancelados com pagamento confirmado; ranking e exportação usam as regras de DASHBOARD_RELATORIOS_E_NAVEGACAO.md.

## Como testar manualmente

1. Abra **F5 no VS Code**; entre como cliente de demonstração (`cliente@demo.local` / `cliente123`).
2. Adicione um produto pela loja e outro pelos favoritos. No carrinho, altere a quantidade, remova um item e confira o total. Quantidade zero ou maior que o estoque deve ser recusada.
3. Finalize a compra. Revise destinatário, telefone e endereço; avance ao pagamento.
4. Selecione PIX e confira 10% de desconto, ou débito e 5%. Para um subtotal de R$ 99,99, PIX desconta R$ 10,00 e totaliza R$ 89,99; débito desconta R$ 5,00 e totaliza R$ 94,99.
5. Com checkout aberto, tente alterar o carrinho por outra janela ou iniciar outro checkout: deve recusar. Fechar antes da confirmação preserva os itens e libera o carrinho.
6. Confirme a compra. Deve aparecer número do pedido, pagamento **SIMULADO** e total final. Carrinho vazio e estoque menor. Feche a confirmação ou clique Ver meus pedidos: a compra não pode reaparecer no carrinho.
7. Abra Meus pedidos → selecione o pedido → Detalhes e histórico. Confira itens, desconto, total e endereço informado.
8. Faça outra compra por boleto: pagamento pendente. Saia, entre como admin (`admin` / `admin123`) → Pagamentos → selecione o pedido → Confirmar simulação. O pagamento passa a confirmado, sem nova baixa de estoque.
9. Entre como vendedor e confira que ele consulta pagamentos/detalhes, mas não tem o botão para confirmar. Retorne ao cliente e atualize o histórico: pagamento confirmado.
10. Para verificar preço/estoque alterado durante a compra, concorrência ou falha após a baixa sem arriscar dados da demonstração, use os testes automáticos.

## Verificação automática

`scripts/testar-integracao.ps1` testa a evolução de uma base V1, as 20 regressões iniciais, os 15 cenários de acesso/cadastros, 14 cenários de compra/pagamento e o fluxo Swing do checkout. O fluxo aciona os próprios componentes Java no EDT, sem automação nativa, e testa resumo → PIX → confirmação, duplo clique e fechamento/continuação. Evidência: `docs/validacao-compra.txt`; capturas em `docs/validacao-ui/checkout-*.png`.

`scripts/verificar-base.ps1` verifica também subtotal/desconto, pagamento e imagem de uma venda fictícia antes/depois do reinício e da restauração, removendo seus próprios registros ao terminar. A documentação acadêmica final será atualizada com a estrutura completa após concluir os módulos restantes.

## Cupons na etapa 7

Cupom opcional é aplicado primeiro, e PIX/débito incidem no restante. V5 grava código/percentual e as duas parcelas de desconto, preservando valores de compras antigas. Interface e confirmação usam DescontosCompra; cupom válido/percentual são revalidados antes de gravar. Veja PERFIL_E_CUPONS.md para administração, regras, exemplos e testes.
