# Seleção de produtos e sobreposição no balcão — 11/10/2026

A tabela de itens ficava no SOUTH do painel central, reservando sua altura preferida. Ao abrir/redimensionar, ela tomava o espaço da seleção de produto e podia sobrepor o cliente. O catálogo não estava sendo removido.

Cliente e seleção de produto/quantidade/Adicionar agora formam um bloco no topo; a tabela ocupa o espaço restante no centro, com rolagem. A janela tem mínimo de 780 × 520 para preservar os comandos. O título recebe espaço flexível no cabeçalho. O tema existente permanece.

CPF ausente não aparece como `null`. Produto, preço unitário, subtotal, total e confirmação usam moeda brasileira a partir de BigDecimal. Quantidade digitada é confirmada antes de adicionar; valor inválido é recusado. Adicionar depende da seleção de produto; Concluir retirada depende de cliente e itens; Remover depende da linha selecionada. Durante gravação, esses comandos ficam bloqueados. A finalização mantém a regra existente: balcão, entregue, dinheiro simulado confirmado e baixa transacional de estoque.

## Conferir

1. Feche **todas as janelas** da versão antiga e abra `Iniciar OBotBarato.cmd`. Sair para o login ou fechar somente Nova venda não reinicia o processo Java e pode manter a tela antiga.
2. Painel → Pedidos → Nova venda no balcão. Escolha cliente e produto, digite quantidade e clique + Adicionar.
3. Confira que os campos ficam acima do cabeçalho da tabela, sem sobreposição, inclusive com a janela menor. Cliente sem CPF mostra o nome sem `null`; valores usam vírgula decimal.
4. Para registrar uma venda de teste, confira o total e clique Concluir retirada. Isso cria uma venda simulada e reduz o estoque. Para apenas conferir aparência, volte sem concluir.

## Evidências

`scripts/testar-balcao.ps1` cria um banco separado e executa `FluxoVendaBalcao`. O mesmo fluxo integra `scripts/testar-integracao.ps1`. Confere visibilidade em 780 × 520, 780 × 600 e 960 × 700; separação da tabela; cliente sem CPF; quantidade digitada; adicionar/remover; bloqueios durante gravação; falha proposital de pagamento com rollback; nova tentativa; confirmação única e saldo. Exemplo: três unidades de R$ 10,25 resultam em R$ 30,75 e saldo de oito para cinco. O erro proposital é parte do teste, não uma falha da base de uso.

Modo `--somente-layout` foi executado na base local com o catálogo/clientes reais da demonstração: inclui um item somente na tela e fecha sem vender. Quantidade de pedidos e saldo total antes/depois foram preservados. Nenhuma nova venda foi registrada na base local por essa verificação.

Resultados: `validacao-venda-balcao.txt`, `validacao-balcao-local.txt`. Capturas: `validacao-ui/venda-balcao.png` e `venda-balcao-base-local.png`. Compilação atual: 70 fontes Java. Sem alteração de esquema, catálogo ou dados de demonstração.
