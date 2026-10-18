# OBotBarato

Sistema acadêmico **desktop Java Swing com PostgreSQL** para perfumaria e cosméticos. Projeto de Sistemas de Informação da UNIPAR, Paranavaí, de Giovana Paula Felix dos Santos e Samuel da Silva Guimarães, sob orientação do Prof. Me. Jaime William Dias. Apresentação prevista para **28/10/2026**.

## Baixar o projeto

Repositório público: [offsimon000/obotbarato](https://github.com/offsimon000/obotbarato).

Para usar a entrega, abra [Releases](https://github.com/offsimon000/obotbarato/releases/latest), baixe o ZIP do OBotBarato em **Assets** e extraia a pasta inteira. O pacote inclui o JAR compilado, código, documentos, catálogo e scripts. Instale JDK 17+ e PostgreSQL; depois crie o atalho com o comando da próxima seção. Em uma cópia nova, prepare o ambiente e carregue a demonstração conforme as instruções abaixo. O banco e os dados locais desta máquina não são enviados.

Para editar o código, clone o repositório ou use **Code → Download ZIP**. O projeto pode ser aberto pelo `build.xml` no Apache NetBeans, pelo workspace do VS Code ou compilado pelos scripts. A branch `main` contém a entrega publicada; `codex/finalizacao` conserva a linha de desenvolvimento desta versão.

## Abrir sem o VS Code

Nesta máquina, dê dois cliques em **Executar OBotBarato**, na Área de Trabalho. Há também um atalho com esse nome na pasta do projeto. Ele prepara/reutiliza o banco, compila somente quando necessário e abre a janela do sistema sem carregar as extensões Java/Spring do editor. O PowerShell termina após iniciar o aplicativo; os dados existentes são conservados.

Em outra máquina ou depois de mover a pasta, crie os atalhos novamente, na raiz do projeto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/criar-atalho.ps1
```

Atalhos `.lnk` contêm caminhos locais e são ignorados pelo Git. O pacote inclui os scripts para gerá-los, sem lançadores `.cmd`. JDK e PostgreSQL continuam necessários. Problemas de abertura são mostrados em uma janela e registrados em `.local/abertura-desktop.log`.

## Executar pelo VS Code

1. Instale **JDK 17 ou posterior**, **PostgreSQL** e **VS Code** com **Extension Pack for Java**.
2. Abra **OBotBarato.code-workspace**, aguarde a importação do Java e pressione **F5** com **OBotBarato - Desktop** selecionado.
3. Entre com uma das contas fictícias abaixo. A preparação compila e inicia um banco local próprio, sem usar o serviço PostgreSQL antigo.
4. Em uma cópia nova, use **Terminal → Executar Tarefa → OBotBarato: carregar demonstração** para instalar o catálogo e o movimento fictício. Na base atual, eles já foram instalados.

| Perfil | Login | Senha fictícia |
|---|---|---|
| Administrador | admin | admin123 |
| Vendedor | vendedor | vendedor123 |
| Cliente original | cliente@demo.local | cliente123 |
| Clientes da simulação | cliente01@uso.obotbarato.test até cliente20@uso.obotbarato.test | demo12345 |

As contas são de demonstração. Reexecutar a preparação ou a carga não redefine senhas nem repõe saldos consumidos. Tutorial: [Execução no VS Code](docs/EXECUTAR_VS_CODE.md).

Alternativa pelo terminal integrado, na pasta deste README:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/executar.ps1
```

Os scripts PowerShell também servem para compilação, preparação e manutenção. O depurador do VS Code executa `view.TelaLoginUnificado`. **Ctrl+Shift+B** compila; **Shift+F5** encerra. Para carregar uma alteração, encerre todas as janelas antigas e execute novamente.

## Executar pelo Apache NetBeans

1. Instale o JDK 17 ou posterior e o Apache NetBeans com suporte a Java. Abra **File → Open Project** e selecione a pasta `OBotBarato` (a que contém `build.xml` e `nbproject/project.xml`).
2. Selecione o JDK 17 nas plataformas Java, se solicitado, e clique em **Run Project** (F6).
3. Na primeira execução, o NetBeans prepara o banco PostgreSQL local isolado, carrega os produtos e a simulação fictícia uma única vez, e abre a tela de login. A configuração está em `nbproject`, e o driver já está em `lib`.

Guia completo: [Executar no NetBeans](docs/EXECUTAR_NETBEANS.md). Uma cópia do banco completo também está disponível na Release; instruções para restaurar uma cópia em banco novo estão em [database/README.md](database/README.md).

## Funcionalidades

- Login unificado com administrador, vendedor e cliente; autocadastro, ativação e recuperação por código entregue pelo administrador.
- Manutenção de usuários, clientes, categorias, produtos com imagens e cupons, conforme as permissões.
- Estoque com saldo, mínimo, movimentações, baixa transacional e devolução única no cancelamento.
- Loja com busca sem acentos/maiúsculas, fragmentos e palavras em qualquer ordem, categoria, preço máximo, detalhes, favoritos e carrinho.
- Checkout com entrega, cupom, descontos e pagamento simulado; venda de balcão com retirada imediata e dinheiro simulado.
- Pedidos, pagamento e histórico; preparo, envio e entrega; cancelamento autorizado antes do envio.
- Perfil privado, avaliações de compras entregues/pagas e notificações internas com leitura individual.
- Dashboard, relatórios de vendas/pedidos/produtos/estoque e exportação CSV em formato brasileiro.

O catálogo tem **30 produtos fictícios** com imagens, preços e descrições. A carga de uso diário acrescenta **20 clientes, 60 pedidos, 18 avaliações e 40 favoritos**. Consulte [Simulação](docs/SIMULACAO_USO_DIARIO.md) e [Catálogo](docs/CATALOGO_APRESENTACAO.md). Alterações na demonstração mudam os indicadores normalmente.

## Documentação da entrega

- [Documentação acadêmica em Word](docs/final/Documentacao_OBotBarato.docx) e [PDF](docs/final/Documentacao_OBotBarato.pdf).
- [Matriz entre funcionalidades, código, banco e testes](docs/MATRIZ_RASTREABILIDADE.md).
- [Revisão, ensaio e envio ao GitHub](docs/REVISAO_E_APRESENTACAO.md).
- Diagramas editáveis e figuras em `docs/diagramas`; [slides editáveis](docs/final/Apresentacao_OBotBarato.pptx) e [slides em PDF](docs/final/Apresentacao_OBotBarato.pdf).
- Guias por módulo e evidências em `docs`; testes em `test`.

## Estrutura

| Pasta | Conteúdo |
|---|---|
| src | Java: modelos, DAOs, utilitários, tema e telas Swing |
| sql | Migrations V001 a V006, com checksum e aplicação transacional |
| lib | Driver JDBC PostgreSQL 42.7.13 e suas licenças dentro do JAR |
| config | Exemplo de configuração; a configuração real é ignorada |
| catalogo | Dados e imagens do catálogo fictício |
| scripts | Compilação, ambiente, testes, backup e empacotamento |
| test | Testes com PostgreSQL real e fluxos Swing |
| docs | Documentação, diagramas, guias e evidências |
| .vscode | Tarefas e configuração portátil de depuração no VS Code |
| nbproject | Configuração do projeto Apache NetBeans com Ant |
| database | Instruções e banco fictício de demonstração publicado na Release |

O build do NetBeans usa Ant e compila para `build/netbeans`; o script de distribuição usa `javac --release 17` e gera `dist/OBotBarato.jar` com driver relativo em `dist/lib`. As duas compilações não apagam os artefatos uma da outra. O inicializador lê as migrations na pasta `sql` do projeto. Se uma sessão bloquear o JAR, o script publica um JAR com identificador e usa o mais recente ao reabrir.

## Banco e configuração

O preparador detecta PostgreSQL instalado nas pastas padrão, PATH ou `OBOTBARATO_PG_BIN`. O JDK é detectado via `JAVA_HOME`, PATH e instalações padrão. Para caminhos personalizados:

```powershell
$env:JAVA_HOME = 'C:/caminho/jdk-17'
$env:OBOTBARATO_PG_BIN = 'C:/caminho/PostgreSQL/bin'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/executar.ps1
```

A base gerenciada é persistente em `.local/desenvolvimento`, tem porta própria, senha aleatória e escuta somente em `127.0.0.1`. `config/banco.properties`, `.local`, `build`, `dist` e `entrega` são ignorados pelo Git. Fechar a aplicação conserva o banco. `scripts/parar-banco.ps1` para somente esse cluster; a próxima execução o inicia novamente.

Para banco externo, crie uma base **nova UTF-8**, copie `config/banco.exemplo.properties` para `config/banco.properties`, preencha os dados e compile. Execute `util.InicializarBanco --demo` com classpath `build/classes;lib/postgresql-42.7.13.jar`, depois `scripts/executar.ps1 -BancoExterno`. Variáveis `DB_URL`, `DB_USUARIO`, `DB_SENHA` têm precedência; `-Dobotbarato.config=caminho` seleciona outro arquivo. Configuração externa sem a marca de ambiente gerenciado é preservada. Não aplique V001 sobre uma base legada: não há importador automático de dados antigos.

## Testar e fazer backup

Execute as tarefas no VS Code ou pelo terminal:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/testar-integracao.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/testar-simulacao.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verificar-base.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/backup.ps1
```

A integração cria e para um cluster exclusivo; testa módulos, migrations, permissões, rollback, concorrência e fluxos Swing. Simulação e busca usam uma base nova, recusam uma de mesmo nome existente e apagam somente a que criaram. Feche a aplicação para a verificação de persistência, pois ela reinicia o cluster local e remove suas próprias fixtures.

Backups ficam em `.local/backups`. `scripts/restaurar-backup.ps1 -Arquivo caminho.dump` restaura sempre em **banco novo**, conservando o principal. Abra a cópia com `scripts/executar.ps1 -Config caminho-da-configuração-gerada`. Guarde backups fora da máquina; não envie configurações com senha ou dados locais ao GitHub.

## Limites do projeto

Pagamento, boleto e entrega são **simulações acadêmicas**. Não há cobrança bancária, emissão fiscal, rastreamento de transportadora, e-mail/SMS, devolução após envio ou estorno financeiro real. Notificações ficam no aplicativo, com atualização manual. A loja é uma tela Swing sem site público. Uma credencial técnica conecta ao banco; as permissões pertencem à aplicação. Algumas consultas de manutenção são síncronas. Os testes demonstram os cenários documentados, sem prometer ausência absoluta de falhas em qualquer ambiente.

JDK 17+, PostgreSQL UTF-8 e Windows são pré-requisitos da entrega validada. Referências: [PostgreSQL JDBC](https://jdbc.postgresql.org/download/) e [Java no VS Code](https://code.visualstudio.com/docs/java/java-debugging).
