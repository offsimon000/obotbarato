# OBotBarato — desktop

Pasta oficial de desenvolvimento do projeto acadêmico. Aplicação Java Swing com PostgreSQL, mantendo o estilo visual original. Apresentação: **28/10/2026**.

**Estado atual:** primeira base corrigida; ainda não é a entrega final. O plano completo está em `../analise/PLANO_FINALIZACAO_OBOTBARATO.md`. Os arquivos extraídos são referências preservadas.

## Pré-requisitos

- JDK 17 ou posterior, com `java`, `javac` e `jar` no PATH.
- PowerShell 7 para os scripts.
- PostgreSQL. Os testes desta máquina usam a instalação 18 em `C:/Program Files/PostgreSQL/18/bin`.
- Driver JDBC incluído em `lib/postgresql-42.7.13.jar`, obtido de https://jdbc.postgresql.org/download/.

## Compilar

Abra um terminal nesta pasta e execute:

```powershell
./scripts/compilar.ps1
```

Gera `dist/OBotBarato.jar` e `dist/lib`. Não depende do Ant nem de arquivos na pasta Downloads. O projeto NetBeans está em `nbproject`, também configurado para Java 17.

## Banco de desenvolvimento ou demonstração

Crie um banco **novo**, chamado `obotbarato`, no seu PostgreSQL. Não aplique a migration inicial sobre a base antiga: a migração dos dados legados ainda não foi implementada.

```sql
CREATE DATABASE obotbarato WITH ENCODING 'UTF8';
```

Copie `config/banco.exemplo.properties` para `config/banco.properties` e preencha usuário, senha e URL. A configuração real é ignorada pelo controle de versão. Variáveis `DB_URL`, `DB_USUARIO` e `DB_SENHA` têm precedência. Uma configuração alternativa pode ser informada por `-Dobotbarato.config=caminho`.

Depois de compilar:

```powershell
java -cp 'build/classes;lib/postgresql-42.7.13.jar' util.InicializarBanco --demo
./scripts/executar.ps1
```

Sem `--demo`, o inicializador só aplica o esquema. As migrations são transacionais e registradas com checksum em `schema_migrations`. Arquivos já aplicados não devem ser modificados; evoluções devem criar uma nova migration.

Contas fictícias instaladas por `--demo`:

| Ambiente | Login | Senha |
|---|---|---|
| Painel | admin | admin123 |
| Painel | vendedor | vendedor123 |
| Loja | cliente@demo.local | cliente123 |

O seed usa o formato de senha aceito pelo código existente, SHA-256 com salt aleatório. Reexecutá-lo não redefine senhas, duplica produtos ou repõe estoque consumido. As credenciais acima são exclusivamente de demonstração.

## Testes em banco separado

```powershell
./scripts/testar-integracao.ps1
# Para outra instalação PostgreSQL:
./scripts/testar-integracao.ps1 -PgBin 'C:/caminho/PostgreSQL/bin'
```

O script cria um cluster exclusivo em `.local/teste-<identificador>`, escolhe uma porta livre, usa uma senha aleatória e escuta somente em `127.0.0.1`. Não inicia o serviço nem altera a base PostgreSQL antiga. Ao terminar, para o cluster criado, preservando seus logs. As variáveis `DB_*` precisam estar ausentes para evitar redirecionar os testes.

As regressões verificam instalação repetida, autenticação, conexões independentes, valores decimais, compra com um/vários itens, rollback por estoque insuficiente, quantidades inválidas, falha no estoque inicial do produto, disputa pela última unidade, relatório por período e filtro de histórico do cliente. A evidência da última execução aprovada fica em `docs/ultimo-teste-integracao.txt`.

## Limites desta versão de desenvolvimento

As correções iniciais tratam a integridade da persistência. Checkout completo, preço revalidado, permissões, gestão de categorias/usuários, pagamento simulado persistido, fluxo de entrega/cancelamento, cupons, avaliações, notificações, dashboard e documentação acadêmica final ainda dependem das próximas etapas. O JAR gerado serve para desenvolvimento; a instalação completa e o pacote da apresentação ainda serão validados.
