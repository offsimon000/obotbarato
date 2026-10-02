# Como testar as partes 1 e 2

Parte 1 = organização, execução, banco e integridade (etapas 1 a 3 do plano). Parte 2 = cadastros, permissões, acesso e catálogo (etapa 4). Compra completa, pagamento e gestão de pedidos pertencem aos próximos blocos.

## Abrir o aplicativo

1. Abra a pasta `OBotBarato` e dê dois cliques em **Iniciar OBotBarato.cmd**. O lançador compila se necessário, inicia o banco local e aplica as migrations pendentes.
2. Aguarde a tela de login. É necessário Java JDK 17 e PostgreSQL instalados; ambos já foram identificados nesta máquina.
3. Use as contas fictícias abaixo. Não altere suas senhas/logins durante o roteiro; crie contas adicionais para testar manutenção.

| Perfil | Login | Senha |
|---|---|---|
| Administrador | `admin` | `admin123` |
| Vendedor | `vendedor` | `vendedor123` |
| Cliente | `cliente@demo.local` | `cliente123` |

## Parte 1: execução e persistência

| Ação | Resultado esperado |
|---|---|
| Entrar com as três contas, saindo entre elas | Admin/vendedor abrem o painel; cliente abre a loja. |
| Digitar senha incorreta | Login recusado, sem entrar no sistema. |
| Como admin, criar uma categoria `Teste apresentação` e um produto de R$ 12,34 com estoque inicial 7 e mínimo 4 | Cadastro aparece após salvar; saldo 7. |
| Fechar e abrir novamente o aplicativo | Categoria, produto, preço e saldo permanecem. |
| Em Estoque, selecionar o produto e tentar uma saída de 100 | Mensagem de estoque insuficiente; saldo continua 7; nenhum movimento falso. |
| Fazer entrada de 3 e saída de 2 | Saldo final 8; histórico mostra abertura + entrada + saída. |

Para testar transação de pedido com vários itens, concorrência da última unidade e rollback sem arriscar seus dados, use o teste automático abaixo. O roteiro completo de backup/restauração também é automatizado.

## Parte 2: usuários e permissões

1. Entre como **admin**. O painel deve mostrar **Usuários** e **Categorias**.
2. Em Usuários, crie `Vendedor Teste`, login `vendedor_teste`, perfil vendedor, senha `teste12345`. Salve, edite o nome com a senha vazia e confirme que a mesma senha ainda funciona.
3. Tente repetir o login em outra conta: o sistema deve recusar. Desative a conta de teste e tente fazer login: deve recusar. Como admin, reative-a pelo formulário.
4. Se houver somente um administrador ativo, tente desativá-lo ou mudar seu perfil para vendedor. A operação deve ser recusada. Não crie um segundo admin só para desativar a conta de demonstração.
5. Entre como **vendedor**. Ele deve consultar produtos/estoque/relatórios, cadastrar ou editar clientes e registrar vendas. Não deve ter Usuários/Categorias, botões de manutenção de produtos, movimentação manual de estoque, desativação de clientes ou emissão de códigos.
6. Saia com **Sair**. As janelas da sessão são fechadas e aparece o login. Entre como cliente: ele deve ver a loja e seus pedidos, sem painel administrativo.

Os testes automáticos também tentam chamar diretamente as operações sem autorização, verificando a proteção além dos botões.

## Parte 2: clientes e acesso à loja

1. Como vendedor ou admin, cadastre `Cliente Teste`, e-mail `cliente.teste@exemplo.local`. CPF, telefone e endereço podem ficar vazios. Esse cadastro começa com **Acesso loja: Pendente**.
2. Teste e-mail inválido, CPF `111.111.111-11`, telefone incompleto e e-mail duplicado. O sistema deve recusar. CPF opcional informado precisa ter dígitos verificadores válidos; uma máscara diferente não pode duplicar o mesmo CPF.
3. Como **admin**, selecione o cliente e clique **Código de acesso**. Confirme que identificou o titular; copie o código exibido. Ele vale por 30 minutos e uma utilização. A entrega é manual, sem envio de e-mail integrado.
4. Clique Sair. No login, abra **Ativar acesso / Esqueci minha senha**. Informe e-mail, código e senha nova de 8 a 128 caracteres, confirmando-a. Salve e faça login com o cliente.
5. Saia e tente reutilizar o mesmo código: deve ser recusado. Para recuperar uma senha depois, o admin emite outro código; a senha anterior deixa de autenticar após a troca.
6. Como admin, desative o cliente de teste. Seu login deve ser recusado. Reative-o: a senha é preservada.
7. Teste o autocadastro pelo botão de criar conta, usando outro e-mail. Após cadastro válido, deve entrar na loja.
8. Nos campos de CPF e telefone, selecione e substitua números no meio, cole um valor completo e apague tudo. O texto deve ser editado na posição correta. Telefone fixo e celular têm formatos próprios.

## Parte 2: categorias, imagens e estoque

1. Como admin, edite o produto de teste e selecione uma imagem PNG/JPEG de até 5 MB e 4096 × 4096 pixels. Salve, reinicie e confira a imagem na loja com uma conta de cliente. Ela fica no banco e acompanha o backup.
2. Retorne ao admin, remova a imagem, salve e confira o placeholder. Teste preço inválido e categoria não selecionada: cadastro recusado.
3. Tente desativar a categoria com produto ativo: deve ser recusado. Desative o produto ou transfira-o para outra categoria e repita: deve funcionar.
4. Produto/categoria inativos permanecem na manutenção e saem do catálogo. Para reativar o produto, sua categoria precisa estar ativa. Não há exclusão física pelo aplicativo.
5. Em Estoque, mude o mínimo para 9 e confira o alerta do produto com saldo 8. O mínimo mostrado deve ser 9. Negativos e quantidade zero nas movimentações devem ser recusados.
6. Abra Histórico: confira data, tipo, quantidade com sinal, saldo, motivo e responsável. Uma venda também registra baixa dentro da mesma transação do pedido.

## Testes automáticos

Você também pode dar dois cliques em **Testar Partes 1 e 2.cmd** para executar os dois roteiros. Feche o aplicativo antes do roteiro que reinicia o banco. Abra PowerShell na pasta `OBotBarato` e execute:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\testar-integracao.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verificar-base.ps1
```

O primeiro comando usa cluster temporário isolado: testa atualização V1/V2, regressões da parte 1, regras da parte 2 e construção das telas. Ele não altera a base de desenvolvimento. O segundo verifica a base local, reinício, backup e restauração em banco novo; cria uma venda fictícia identificada e remove apenas seus próprios registros ao concluir. O serviço/banco PostgreSQL original é preservado.

Procure **APROVADO** e ausência de erros nos resultados. Evidências ficam em `docs/ultimo-teste-integracao.txt`, `docs/validacao-parte2.txt` e `docs/validacao-base.txt`. Esses testes verificam regras e persistência; o roteiro manual acima confere também suas interações com as telas.

## Backup manual

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\backup.ps1
```

Guarde o caminho `.dump` exibido. Para restaurar, use:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\restaurar-backup.ps1 -Arquivo "CAMINHO_DO_BACKUP.dump"
```

A restauração cria outro banco e mostra sua configuração; não substitui sua base principal. Para abrir a cópia:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\executar.ps1 -BancoExterno -Config "CAMINHO_DA_CONFIGURACAO_RESTAURADA.properties"
```

Se algo falhar, anote conta, tela, ação e mensagem. Não altere SQL ou apague `.local` para tentar corrigir: essa pasta contém seu banco e backups.
