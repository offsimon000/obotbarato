# OBotBarato — desktop

Pasta oficial de desenvolvimento do projeto acadêmico. Aplicação Java Swing com PostgreSQL, mantendo o estilo visual original. Apresentação: **28/10/2026**.

**Estado atual:** partes 1 e 2 e compra/pagamento simulado implementados: fundação, cadastros, permissões, acesso à loja e manutenção do catálogo. Veja `docs/COMO_TESTAR_PARTES_1_E_2.md` e `docs/PARTE_2_CADASTROS_E_ACESSO.md`. Compra e pagamento: `docs/COMPRA_E_PAGAMENTO_SIMULADO.md`. Gestão de pedidos é o próximo bloco. O plano completo está em `../analise/PLANO_FINALIZACAO_OBOTBARATO.md`; escopo e critérios estão em `docs/ESCOPO_E_CRITERIOS.md`. Os arquivos extraídos são referências preservadas.

## Pré-requisitos

- JDK 17 ou posterior, com `java`, `javac` e `jar` no PATH.
- Windows PowerShell 5.1 (incluído no Windows) ou PowerShell 7.
- PostgreSQL instalado. O preparador detecta a instalação, o PATH ou `OBOTBARATO_PG_BIN`; também aceita `-PgBin`.
- Driver JDBC incluído em `lib/postgresql-42.7.13.jar`, obtido de https://jdbc.postgresql.org/download/.

## Compilar

Abra um terminal nesta pasta e execute:

```powershell
./scripts/compilar.ps1
```

Gera `dist/OBotBarato.jar` e `dist/lib`. Não depende do Ant nem de arquivos na pasta Downloads. O projeto NetBeans está em `nbproject`, também configurado para Java 17.

## Iniciar no Windows

Abra **`Iniciar OBotBarato.cmd`** nesta pasta. O lançador compila quando necessário, prepara/reutiliza uma base local e abre a tela oficial `TelaLoginUnificado`. A primeira preparação pode levar alguns instantes. O estilo visual existente foi preservado.

Também é possível executar pelo terminal:

```powershell
./scripts/executar.ps1
# Preparar/conferir sem abrir janelas:
./scripts/executar.ps1 -SomenteDiagnostico
# Informar uma instalação PostgreSQL diferente:
./scripts/preparar-ambiente.ps1 -PgBin 'C:/caminho/PostgreSQL/bin'
```

A base gerenciada fica em `.local/desenvolvimento`, com porta própria, senha aleatória e acesso somente por `127.0.0.1`. A configuração real é criada em `config/banco.properties` e ignorada pelo Git. O preparador não usa o serviço nem os dados antigos do PostgreSQL. Reabrir o projeto reutiliza o mesmo banco; os cadastros e vendas são preservados.

Para encerrar somente esse banco:

```powershell
./scripts/parar-banco.ps1
```

O próximo lançamento o inicia novamente. Fechar o aplicativo não apaga nem para automaticamente o banco.

## Usar um banco externo

Crie um banco **novo**, chamado `obotbarato`, no seu PostgreSQL. Não aplique a migration inicial sobre a base antiga: a migração dos dados legados ainda não foi implementada.

```sql
CREATE DATABASE obotbarato WITH ENCODING 'UTF8';
```

Copie `config/banco.exemplo.properties` para `config/banco.properties` e preencha usuário, senha e URL. A configuração real é ignorada pelo controle de versão. Variáveis `DB_URL`, `DB_USUARIO` e `DB_SENHA` têm precedência. Uma configuração alternativa pode ser informada por `-Dobotbarato.config=caminho`.

Depois de compilar, instale a base externa e execute:

```powershell
java -cp 'build/classes;lib/postgresql-42.7.13.jar' util.InicializarBanco --demo
./scripts/executar.ps1 -BancoExterno
```

Sem `--demo`, o inicializador só aplica o esquema. As migrations são transacionais e registradas com checksum em `schema_migrations`. Arquivos já aplicados não devem ser modificados; evoluções devem criar uma nova migration.

Uma configuração externa existente é preservada pelo lançador. O arquivo marcado `# OBotBarato: ambiente local gerenciado` pertence ao preparador; para uma base externa, use o arquivo de exemplo sem essa marca, `-BancoExterno` ou `-Config caminho-do-arquivo`. Scripts de manutenção local recusam overrides `DB_*`.

Contas fictícias instaladas por `--demo`:

| Ambiente | Login | Senha |
|---|---|---|
| Painel | admin | admin123 |
| Painel | vendedor | vendedor123 |
| Loja | cliente@demo.local | cliente123 |

O seed usa PBKDF2-HMAC-SHA256 com salt aleatório e 600.000 iterações. Hashes antigos são migrados no login. Reexecutá-lo não redefine senhas, duplica produtos ou repõe estoque consumido. As credenciais acima são exclusivamente de demonstração.

## Testes em banco separado

```powershell
./scripts/testar-integracao.ps1
# Para outra instalação PostgreSQL:
./scripts/testar-integracao.ps1 -PgBin 'C:/caminho/PostgreSQL/bin'
```

O script cria um cluster exclusivo em `.local/teste-<identificador>`, escolhe uma porta livre, usa uma senha aleatória e escuta somente em `127.0.0.1`. Não inicia o serviço nem altera a base PostgreSQL antiga ou a de desenvolvimento. Ao terminar, para o cluster criado, preservando seus logs. As variáveis `DB_*` precisam estar ausentes para evitar redirecionar os testes.

Os 20 cenários verificam instalação repetida, autenticação, conexões independentes, dinheiro, compra, rollback, concorrência, relatórios, filtro por cliente, duplicidades, registros inexistentes, favoritos, falha de conexão, evolução de migrations e aliases de login. A evidência fica em `docs/ultimo-teste-integracao.txt`. O mesmo script verifica evolução V1/V2, 15 cenários novos de cadastros/acesso e construção de 16 telas. Evidências adicionais: `docs/validacao-parte2.txt` e `docs/validacao-compra.txt`. Também são executados 14 cenários de compra/pagamento e o fluxo Swing do checkout.

Para verificar também o ambiente persistente, reinício, restauração e construção das 16 telas atuais:

```powershell
./scripts/verificar-base.ps1
```

Esse roteiro cria registros fictícios próprios, confere seus valores antes/depois do reinício e da restauração, e remove somente esses registros de teste. As telas são construídas no EDT sem exibir janelas. Isso verifica a inicialização; não substitui os testes de interação de todos os módulos. Evidência: `docs/validacao-base.txt`; renderizações do login e catálogo: `docs/validacao-ui`.

## Backup e restauração da base local

```powershell
./scripts/backup.ps1
./scripts/restaurar-backup.ps1 -Arquivo '.local/backups/arquivo-gerado.dump'
```

O backup fica em `.local/backups`. A restauração sempre cria **um banco novo** e deixa o principal intacto. A configuração da cópia fica em `.local/restauracoes/<banco>/banco.properties`. Para abrir a cópia enquanto o cluster local está ativo:

```powershell
./scripts/executar.ps1 -Config '.local/restauracoes/<banco>/banco.properties'
```

Guarde uma cópia do `.dump` fora da máquina para recuperação em caso de perda do disco. Arquivos de configuração, senha, dados locais e backups não são versionados.

## Limites desta versão de desenvolvimento

As partes concluídas tratam persistência, permissões e manutenção dos cadastros/catálogo. Fluxo de preparo/entrega/cancelamento, cupons, avaliações, notificações, dashboard e documentação acadêmica final ainda dependem das próximas etapas. O JAR gerado serve para desenvolvimento; a instalação completa e o pacote da apresentação ainda serão validados.
