# Dashboard, relatórios e navegação — 09/10/2026

Bloco de apresentação implementado em desktop Swing, com o tema branco/azul existente. Não altera esquema, migrations, contas nem dados do catálogo.

## Navegação

`Tema.botaoVoltar` e `Navegacao` definem a mesma aparência e o mesmo retorno para as telas secundárias. Notificações, Meu perfil, Avaliação e Formulário de produto receberam o botão; favoritos, carrinho e demais módulos usam a mesma base. As páginas de cadastro/recuperação do login também usam esse estilo.

Voltar e fechar pelo X encerram a janela secundária e trazem sua origem para a frente. Não reabrem uma origem quando a identidade da sessão mudou. Login, painel principal e loja são entradas principais; usam sair/fechar. Diálogos de confirmação mantêm suas ações de confirmar/cancelar.

Durante gravações de perfil, avaliação, leitura, produto, venda, gestão de pedido e confirmação de pagamento, a saída aguarda a conclusão. Checkout também protege a confirmação e a consulta do cupom. O botão Resumo retorna à etapa anterior do checkout com a mesma aparência de navegação.

## Regras dos indicadores e relatórios

Admin e vendedor possuem Dashboard e Relatórios no painel. Cliente não pode consultar esses dados. Cada consulta revalida a conta ativa no banco.

As duas entradas usam `TelaRelatorios`, `RelatorioDAO` e as mesmas abas. Dashboard inicia na Visão geral; Relatórios inicia em Vendas por dia. O filtro padrão é o primeiro dia do mês até hoje, com datas `dd/mm/aaaa`, barras automáticas, dias inicial e final incluídos. O período é definido pela **data de criação do pedido**, inclusive se seu boleto for confirmado posteriormente.

| Informação | Regra |
|---|---|
| Faturamento líquido | Soma de `pedidos.total` dos pagamentos confirmados, excluindo pedidos cancelados, no período. Inclui loja e balcão, independentemente da etapa de entrega. |
| Vendas confirmadas | Quantidade de pedidos que entram no faturamento. |
| Ticket médio | Faturamento / vendas confirmadas, arredondado a centavos com HALF_UP; zero quando não há vendas. |
| Pagamento pendente | Pedidos não cancelados com pagamento pendente no período. |
| Pedidos cancelados | Quantidade de cancelamentos entre pedidos criados no período; não representa estorno real. |
| Estoque crítico | Produtos ativos cujo saldo atual é menor ou igual ao mínimo; não depende das datas. |
| Vendas por dia | Quantidade, subtotal bruto, desconto do cupom, desconto do pagamento e total líquido dos pedidos válidos. |
| Mais vendidos | Top 10 no mesmo período/seleção financeira, por unidades, depois valor bruto, nome e ID. Valor bruto = quantidade × preço do item gravado; não distribui descontos do pedido entre produtos. |
| Pedidos | Todos os pedidos criados no período, incluindo pendentes/cancelados, com origem, estados e parcelas históricas. A soma desta aba não é o faturamento. |

Produtos desativados continuam aparecendo em vendas históricas. O preço atual do catálogo não recalcula itens antigos. Descontos vêm das parcelas persistidas do pedido. Os gráficos mostram faturamento nos últimos sete dias do intervalo (dias sem venda com zero) e pedidos nos cinco estados de entrega.

Uma transação de leitura com isolamento REPEATABLE READ mantém indicadores, tabelas e estoque no mesmo snapshot. Dinheiro permanece `BigDecimal`; apenas a geometria dos gráficos usa `double`. A consulta executa em `SwingWorker`, sem bloquear a interface. Atualizar consulta novamente; não há atualização automática/push. Erro de consulta limpa os dados financeiros da tela e mostra a falha, evitando apresentar dados antigos como atuais.

## CSV

Selecione uma das quatro abas de tabela e clique em Exportar CSV. Escolha onde salvar; substituição de arquivo existente exige confirmação. Exportação usa exatamente as colunas e linhas carregadas na tabela; não consulta novamente o banco para trocar silenciosamente os números.

Formato: UTF-8 com BOM, separador `;`, cabeçalho, aspas escapadas, datas brasileiras e vírgula decimal sem `R$` nas células monetárias (unidade indicada no cabeçalho). Textos que poderiam virar fórmulas de planilha recebem apóstrofo. Escrita em arquivo temporário na pasta de destino, seguida de publicação do arquivo completo. Arquivo aberto/bloqueado ou pasta sem acesso gera aviso de falha.

Visão geral e tabelas vazias não exportam. Alterar datas sem Atualizar também bloqueia exportação; o rodapé informa o período realmente exibido. Estoque é identificado como atual no cabeçalho/rodapé e no nome do arquivo. A aba Pedidos contém dados de clientes e é destinada à equipe autorizada.

## Como conferir na máquina

1. Feche a versão antiga e abra **F5 no VS Code**. Entre no painel com `admin` / `admin123`.
2. Abra Notificações: confira Voltar à esquerda do título. Teste Voltar e depois abra novamente e feche pelo X. Confira também Produtos, Clientes, Estoque, Pedidos, Pagamentos, Cupons, Usuários e Categorias. Na loja, confira Perfil, Favoritos, Carrinho, Meus pedidos e Avaliação de um pedido elegível.
3. Abra Dashboard. Confira os seis indicadores e os dois gráficos. Digite datas só com números e clique em Atualizar. Período sem pedidos deve mostrar zero, sem erro. Data impossível/invertida deve ser recusada.
4. Abra Vendas por dia: a soma do líquido deve bater com o faturamento. Em Mais vendidos, confira unidades e valor **bruto**. Em Pedidos, confira o cancelado/pendente sem somá-lo ao faturamento. Estoque baixo mostra saldos atuais.
5. Clique Exportar CSV em uma tabela preenchida, salve e abra/importe no Excel ou LibreOffice com separador ponto e vírgula. Confira acentos, datas, colunas e valores. Altere o filtro: exportação fica bloqueada até Atualizar.
6. Para gerar movimento atual, compre na loja com PIX/cupom e confira o novo total no painel após Atualizar. Um boleto deve entrar só depois da confirmação pelo admin. Cancelar antes do envio retira a venda do faturamento, conserva histórico e devolve estoque uma vez.

Se o aplicativo estiver aberto ao compilar, o Windows pode bloquear seu JAR. O compilador publica uma nova cópia com identificador e o lançador/scripts escolhem o JAR completo mais recente. A sessão aberta continua usando a versão anterior até fechar/reabrir. Não é necessário apagar dados ou recompilar à mão. Scripts detectam o JDK 17+ instalado nas pastas padrão quando JAVA_HOME/PATH ainda não foram configurados.

## Evidências

`RelatoriosFinanceiros`: nove cenários com PostgreSQL real, acesso, intervalos, descontos, cancelamento, estoque atual, histórico de inativos e CSV. Exemplo exclusivo da base de teste: R$ 200 bruto, R$ 20 cupom, R$ 18 PIX = R$ 162; outra venda de R$ 100 resulta em faturamento R$ 262 e ticket R$ 131. Boleto de R$ 100 confirmado leva a R$ 362 e ticket R$ 120,67. Cancelado e pedido no dia seguinte permanecem excluídos.

`InicializacaoTelas` exige Voltar em todas as telas secundárias entre as 23 instâncias construídas. `FluxoRelatoriosNavegacao` confere filtros, abas, valores do dashboard/tabela/CSV, estados vazios, retorno por botão/X e proteção durante operação e após logout. Evidência: `validacao-relatorios-navegacao.txt`; capturas em `validacao-ui/dashboard.png` e `relatorios-*.png`.

Os módulos funcionais estão implementados. A documentação consolidada está em final, com diagramas editáveis em diagramas, matriz de rastreabilidade e roteiro de revisão/apresentação. Este guia detalha as regras dos relatórios e complementa o documento acadêmico.

Conferência adicional em 10/10/2026: nova compilação com Windows PowerShell 5.1 e detecção do JDK sem JAVA_HOME/PATH Java. Dashboard e menu conferidos na base local após retomada do servidor: 30 produtos ativos, 855 unidades, 1 pedido e seis migrations preservados; faturamento conferido por consulta independente. Evidências: `validacao-dashboard-base-local.txt`, `validacao-lancador-atualizado.txt` e captura `validacao-ui/dashboard-base-local.png`.
