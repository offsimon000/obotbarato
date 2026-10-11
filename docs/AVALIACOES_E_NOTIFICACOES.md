# Avaliações e notificações — etapa 7

Bloco voltado à apresentação: avaliações de compras verificadas e caixa de avisos dentro do desktop, com o tema existente. Dashboard e revisão final dos relatórios ainda são o próximo bloco.

## Avaliações

Cliente → **Meus pedidos** → selecione um pedido entregue com pagamento confirmado → **Avaliar produtos**. A tela mostra somente os produtos daquele pedido e permite nota inteira de 1 a 5 e comentário opcional de até 1000 caracteres. O comentário fica visível na loja.

O DAO verifica titular, produto adquirido, entrega e pagamento no banco. Não se avalia produto não comprado, pedido de outra pessoa, aberto/em preparo/enviado ou cancelado. Admin/vendedor não publicam avaliações pela sessão de equipe.

Existe uma avaliação por cliente/produto. Salvar novamente atualiza nota/comentário, preservando data de criação e o vínculo original de compra. Gravações simultâneas não criam duas avaliações. O cliente pode avaliar um produto adquirido que foi desativado depois; a avaliação fica preservada e o item continua fora do catálogo enquanto inativo.

A loja mostra média com uma casa decimal e quantidade de avaliações nos cartões, na mesma consulta de catálogo/saldo. **Ver detalhes** mostra os 50 comentários mais recentes, nota e data; não divulga nome, CPF ou e-mail dos compradores. A média considera todos os registros, uma vez por comprador. Não há moderação, resposta do vendedor ou remoção pela interface neste bloco.

## Notificações internas

**Notificações** está na loja e no painel da equipe. A caixa oferece atualização manual, filtro de não lidas, texto completo do aviso selecionado e **Marcar como lida**. Exibe os 500 registros mais recentes do filtro.

| Evento | Destinatário | Regra |
|---|---|---|
| Criação do pedido | Titular e equipe | Estado e pagamento simulado informados no aviso de criação. |
| Preparo, envio, entrega e cancelamento | Titular e equipe | Aviso acompanha cada novo evento do histórico. |
| Confirmação de boleto | Titular e equipe | Evento de pagamento separado; repetir confirmação não duplica. |
| Estoque no mínimo ou abaixo | Equipe | Um aviso por episódio crítico. |

Cliente consulta/marca somente seus avisos. Admin/vendedor consultam avisos da equipe, sem acessar a caixa pessoal dos clientes. Cada usuário interno tem sua leitura própria: vendedor marcar não altera a leitura do administrador. Marcar um aviso duas vezes não duplica leitura.

Pedidos/notificações são gravados na mesma transação. Se a gravação do aviso falhar, a operação é desfeita; não há sucesso falso. Chave única por evento/destinatário protege duplicação. Repetição de compra, confirmação ou cancelamento já concluídos não cria outro evento/aviso.

Estoque considera produto ativo e saldo menor ou igual ao mínimo. Cadastro inicial, movimentação e alteração do mínimo verificam a condição. Enquanto permanece crítico, novas movimentações não repetem o alerta. Reposição acima do mínimo encerra o episódio; uma nova queda gera outro aviso. Alterar o mínimo também pode encerrar/iniciar um episódio. Saldo, movimento e estado do alerta são atômicos. Operações usam bloqueios de produto antes do estoque, inclusive venda/reposição concorrentes.

Os avisos são locais e persistidos no banco. Não há e-mail, SMS, serviço externo, pop-up automático, processamento em segundo plano sem o aplicativo ou atualização em tempo real da caixa. Use Atualizar para ver eventos novos.

## Banco e dados anteriores

`V006__avaliacoes_e_notificacoes.sql` cria avaliações, notificações e leituras individuais, e acrescenta estado/episódio do alerta em estoque. Restrições de nota, unicidade, destinatário e leitura acompanham as regras dos DAOs.

V1 a V5 permanecem intactas. Pedidos, pagamentos, imagens, descontos e saldos anteriores são preservados. Não se fabricam avaliações ou mensagens de eventos antigos. Estoque já crítico antes da instalação será verificado no próximo cadastro/movimento/alteração de mínimo; não há reconstrução de alertas históricos.

## Roteiro para testar/apresentar

1. Feche/reabra `Iniciar OBotBarato.cmd`. Cliente: `cliente@demo.local` / `cliente123`; admin: `admin` / `admin123`; vendedor: `vendedor` / `vendedor123`.
2. Compre um produto ativo pela loja. Abra Notificações: deve aparecer a criação e seu pagamento simulado. Marque o aviso como lido; filtre todas para reencontrá-lo.
3. Como equipe, abra Pedidos e avance até entregue, uma etapa por vez. Cliente atualiza Notificações para acompanhar os eventos.
4. Cliente → Meus pedidos → pedido entregue → Avaliar produtos. Dê nota 4 e comentário curto. Salve, volte à loja e clique Filtrar para atualizar: o cartão deve mostrar nota 4,0/5. Ver detalhes exibe o comentário.
5. Reabra a avaliação, mude a nota para 5 e salve: continua uma avaliação, com média atualizada. Pedido ainda aberto ou cancelado não habilita Avaliar produtos.
6. Como admin, crie um produto de teste com saldo 6 e mínimo 5. Em Estoque, ajuste saldo para 5: aparece alerta nas Notificações da equipe. Reduza para 4: não repete o mesmo episódio. Reponha acima de 5 e reduza de novo: deve gerar novo alerta.
7. Como vendedor, marque um aviso da equipe como lido; admin ainda deve vê-lo como não lido na sua conta.
8. Faça compra por boleto, confirme como admin e consulte o aviso de pagamento. Repetir a confirmação não gera outra mensagem.

## Validação

`scripts/testar-integracao.ps1` mantém 80 cenários anteriores e acrescenta 13 de avaliações/avisos (93 naquele bloco; **102 atuais** com relatórios/CSV). Confere privacidade, compra verificada, edição/concorrência, média, deduplicação de mensagens, leitura individual, episódios de estoque e rollback em falha de aviso. Verifica evolução até V6 e construção de 23 telas/instâncias; fluxo Swing aciona salvar avaliação e marcar leitura.

Evidência: `validacao-avaliacoes-avisos.txt`. Capturas: `validacao-ui/avaliacao-cliente.png`, `notificacoes-cliente.png` e `notificacoes-equipe.png`. O roteiro de persistência também confere avaliação, avisos e leitura de registros próprios após reinício/restauração, removendo suas fixtures ao concluir.
