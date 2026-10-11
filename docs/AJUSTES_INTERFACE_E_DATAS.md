# Ajustes de interface e datas — 09/10/2026

Correções solicitadas após teste manual, mantendo o desktop e o tema branco/azul.

- Formulário de clientes distribui a largura entre os campos, com rótulos e campos opcionais identificados. Um erro de validação preserva o preenchimento para corrigir e tentar novamente.
- Botões secundários e contornados têm espaço interno para não colar o texto nas bordas. Ranking mantém explicação e botão sem disputar espaço na mesma linha.
- Datas de filtros/relatórios usam **dd/mm/aaaa**. Digite `09102026`: aparece `09/10/2026`. É possível colar, substituir dia/mês/ano, selecionar e apagar tudo. Validação recusa data incompleta, dia inexistente e ano bissexto inválido. Datas opcionais dos pedidos podem ficar vazias.
- Datas apresentadas em relatório, detalhes, histórico de pedidos e movimentações de estoque seguem o padrão brasileiro. Banco e APIs internas continuam usando datas tipadas/ISO; nenhum registro de data é alterado por essa mudança visual.
- Marca OBotBarato aparece sem asterisco no login, painel e loja.
- Loja usa cartões com altura consistente e colunas ajustadas ao espaço, busca por Enter, indicação da quantidade de produtos e mensagens de inclusão no carrinho sem interromper com uma janela. Catálogo/saldos são lidos numa consulta em segundo plano e atualizados após compra e ao retornar à loja.
- Carrinho, favoritos e pedidos reutilizam suas janelas na mesma sessão. Carrinho é um botão acessível por teclado. Carrinho vazio não permite finalizar/limpar; carrinho reservado não permite outro checkout ou edição. Fechar antes de comprar libera a edição e preserva os itens.
- Carrinho separa resumo, orientação e ações para evitar sobreposição. Quantidade/remover exigem item selecionado.
- Pedidos mostram orientação para a seleção e desabilitam cancelamento/avanço quando o estado ou pagamento não permite. Alterações ficam bloqueadas durante consulta/gravação; filtros podem ser atualizados durante leitura e só a resposta mais recente é aplicada. Estado esperado, autorização, bloqueios, transação e devolução única continuam validados no banco.

## Conferência manual

1. Feche a versão que estava aberta e execute **Iniciar OBotBarato.cmd** para carregar a versão atual.
2. Admin → Clientes → Novo Cliente: confira campos largos; tente e-mail inválido e corrija preservando o texto.
3. Relatórios → Vendas por dia: selecione tudo no início, digite `01102026`; no fim, `09102026`. Confira barras automáticas e datas válidas. Tente `31022026`: geração deve recusar.
4. Em Pedidos, digite as datas sem barras e filtre. Detalhes/histórico devem mostrar dia/mês/ano.
5. Cliente → loja: confira marca sem asterisco, cartões e botões legíveis. Adicione um produto; aviso e contador do carrinho devem mudar.
6. Clique repetidamente em Carrinho: deve existir somente uma janela. Edite quantidade e confira o subtotal. Com carrinho vazio, finalizar fica desabilitado.
7. Abra a revisão/pagamento: edição do carrinho e segundo checkout ficam bloqueados. Feche antes de confirmar: itens preservados e edição liberada.
8. Confirme uma compra simulada: estoque/carrinho atualizados; em Meus Pedidos, selecione um pedido aberto para habilitar cancelamento. Cancelados/enviados/entregues não oferecem cancelamento pelo cliente. Consulte o histórico para as regras após o preparo.

## Evidências

`scripts/testar-integracao.ps1` mantém os 66 cenários existentes, construção das 17 telas e fluxos de checkout/gestão, acrescentando `InterfaceBrasileira`: datas estritas/bissextas, digitação/colagem/edição/limpeza, largura real do formulário, filtros, marca, catálogo, janelas únicas e estados de carrinho.

Resultado em `validacao-interface.txt`. Capturas atuais em `validacao-ui/formulario-cliente.png`, `relatorios.png`, `loja.png`, `carrinho.png` e `meus-pedidos.png`.

Sem alteração de migrations, esquema ou dados pessoais. Os testes usam base isolada e janelas não visíveis; não reiniciam o banco que estiver sendo usado pelo aplicativo do usuário. O roteiro completo de backup/reinício continua disponível em `Testar Partes 1 e 2.cmd`, que deve ser executado com o aplicativo fechado.

Atualização em 11/10/2026 — Criar conta: nome, e-mail, senha e confirmação identificados como obrigatórios; CPF, telefone e endereço identificados como opcionais no cadastro. Senha informa 8 a 128 caracteres. Observação esclarece que telefone/endereço são necessários para entrega. Regras de validação e banco preservadas; 70 fontes compiladas e formulário completo renderizado/inspecionado em validacao-ui/cadastro-campos.png, sem criar contas.
