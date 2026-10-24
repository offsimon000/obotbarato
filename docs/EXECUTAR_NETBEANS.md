# Abrir e executar no Apache NetBeans

O projeto usa Java 17, Apache Ant e PostgreSQL. O driver JDBC já está em `lib`, então não é necessário baixar dependências pelo NetBeans. O nível e o bytecode do projeto ficam em Java 17; o JDK 18 do computador da faculdade pode compilar e executar esse bytecode.

## Preparar o computador

1. Tenha um JDK, o PostgreSQL para Windows e o Apache NetBeans com suporte a Java. A instalação do PostgreSQL precisa incluir as ferramentas de linha de comando (`initdb`, `pg_ctl`, `psql` e `createdb`), usadas para criar o banco local isolado.
2. Baixe ou clone a pasta completa do repositório. Não abra apenas `src`.
3. No NetBeans, escolha **File → Open Project** e selecione a pasta `OBotBarato`, que contém `build.xml` e `nbproject/project.xml`.
4. Clique em **Run Project** (F6). O projeto já define a classe principal, Java 17 como nível de compilação e o driver JDBC local; não é preciso escolher uma classe `main` manualmente.

O JDK 18 da faculdade serve como plataforma do projeto, mantendo a compilação no nível Java 17. Há uma ressalva específica do IDE: o [Apache NetBeans 13 declara suporte de execução no JDK 11 e no JDK 17](https://netbeans.apache.org/front/main/download/nb13/nb13/), não no JDK 18. Se o NetBeans 13 apresentar erro por causa do próprio JDK com que o IDE iniciou, configure o runtime do NetBeans para JDK 17; isso é separado do JDK usado pelo projeto.

## Executar

Na primeira execução, o alvo Ant prepara um PostgreSQL local próprio do OBotBarato em `.local/desenvolvimento`, aplica as migrations, carrega os 30 produtos e cria os registros fictícios do roteiro. Depois o NetBeans abre a tela de login. A carga de demonstração roda uma vez; apertar Run novamente não repõe estoque nem duplica vendas.

O banco local usa uma porta livre e escuta apenas em `127.0.0.1`. Ele fica separado do serviço PostgreSQL instalado e conserva os dados entre execuções. O projeto precisa das ferramentas do PostgreSQL instaladas, mas não é necessário criar manualmente usuário ou banco. Também existe um backup PostgreSQL para download na Release; veja as instruções em `database/README.md`.

## Contas de demonstração

| Perfil | Login | Senha |
|---|---|---|
| Administrador | `admin` | `admin123` |
| Vendedor | `vendedor` | `vendedor123` |
| Cliente | `cliente@demo.local` | `cliente123` |

As senhas são públicas e servem apenas para apresentação acadêmica.

## Compilar sem iniciar

Com Ant instalado (o NetBeans já inclui o necessário), no terminal integrado:

```powershell
ant compile
```

O NetBeans grava classes em `build/netbeans/classes`; os scripts e o atalho desktop continuam usando as próprias pastas `build/classes` e `dist`.

Os cenários automatizados ficam em `test` e são compilados/executados pelos scripts PowerShell documentados no README. Eles não são registrados como uma suíte de testes do NetBeans, evitando o aviso de caminho de testes modulares sem `module-info.java` no NetBeans 13.
