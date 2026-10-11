# OBotBarato — desktop

Pasta oficial de desenvolvimento do projeto acadêmico. Aplicação Java Swing com PostgreSQL, mantendo o estilo visual original. Apresentação: **28/10/2026**.

**Estado atual:** etapas 1 a 6 implementadas: fundação, cadastros, permissões, catálogo, compra/pagamento simulado e gestão/cancelamento de pedidos. Guias: `docs/COMO_TESTAR_PARTES_1_E_2.md`, `docs/PARTE_2_CADASTROS_E_ACESSO.md`, `docs/COMPRA_E_PAGAMENTO_SIMULADO.md` e `docs/GESTAO_E_CANCELAMENTO_PEDIDOS.md`. Os módulos da etapa 7 também estão implementados: perfil, cupons, avaliações, notificações e dashboard. Próximo bloco: revisão final integrada, documentação acadêmica e pacote. O plano completo está em `../analise/PLANO_FINALIZACAO_OBOTBARATO.md`; escopo e critérios estão em `docs/ESCOPO_E_CRITERIOS.md`. Os arquivos extraídos são referências preservadas.

Ajustes de interface de 09/10: datas brasileiras com barras automáticas, campos/botões corrigidos e navegação do cliente revisada. Como conferir: `docs/AJUSTES_INTERFACE_E_DATAS.md`; testes em `docs/validacao-interface.txt`.

Catálogo de apresentação: 30 cosméticos fictícios com imagens próprias, descrições, preços e estoque. Cadastro/prompts/imagens estão em catalogo; regras, lista e importação manual em docs/CATALOGO_APRESENTACAO.md. A instalação local já recebeu esse catálogo; a demonstração básica de uma base nova continua separada. A loja permite abrir Ver detalhes para consultar descrição e imagem maior.

Perfil e cupons também concluídos: Meu perfil na loja; Cupons no painel admin; APRESENTA10 para demonstração. Desconto do cupom antes do pagamento, com parcelas salvas no pedido. Regras/testes: `docs/PERFIL_E_CUPONS.md` e `docs/validacao-perfil-cupons.txt`. Avaliações e notificações também concluídas; dashboard e relatórios com exportação CSV também estão implementados. Guia: `docs/DASHBOARD_RELATORIOS_E_NAVEGACAO.md`.

Avaliações e avisos internos: `docs/AVALIACOES_E_NOTIFICACOES.md` e `docs/validacao-avaliacoes-avisos.txt`. Cliente avalia produtos dos seus pedidos entregues; Notificações na loja e no painel, com leitura individual.

Simulação de uso diário instalada: 20 clientes fictícios, 60 pedidos de loja/balcão em vários estados, cupons, avaliações, favoritos e avisos. Guia/contas: `docs/SIMULACAO_USO_DIARIO.md`; validação: `docs/validacao-simulacao-uso.txt` e `docs/validacao-uso-diario-local.txt`. A carga é manual e não se repete ao abrir o sistema.

Correção de 11/10 no balcão: seleção de produto e tabela sem sobreposição, CPF ausente sem null, moeda brasileira e quantidade digitada aplicada. Guia: `docs/CORRECAO_TELA_BALCAO.md`; teste dedicado: `scripts/testar-balcao.ps1`.

## Pré-requisitos

- JDK 17 ou posterior. Scripts usam JAVA_HOME/PATH ou detectam JDK instalado nas pastas padrão de Eclipse Adoptium, Java e Microsoft. Em instalação personalizada, configure JAVA_HOME.
- Windows PowerShell 5.1 (incluído no Windows) ou PowerShell 7.
- PostgreSQL instalado. O preparador detecta a instalação, o PATH ou `OBOTBARATO_PG_BIN`; também aceita `-PgBin`.
- Driver JDBC incluído em `lib/postgresql-42.7.13.jar`, obtido de https://jdbc.postgresql.org/download/.

## Compilar

Abra um terminal nesta pasta e execute:

```powershell
./scripts/compilar.ps1
```

Gera `dist/OBotBarato.jar` e `dist/lib`. Se o aplicativo bloquear o JAR, publica `dist/OBotBarato-<identificador>.jar`; lançador e scripts escolhem o pacote mais recente ao reabrir, preservando a sessão existente. Não depende do Ant nem de arquivos na pasta Downloads. O projeto NetBeans está em `nbproject`, também configurado para Java 17.

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

O script verifica 102 cenários: 20 regressões iniciais, 15 de cadastros/acesso, 14 de compra/pagamento, 17 de gestão/cancelamento, 14 de perfil/cupons, 13 de avaliações/avisos e 9 de relatórios/CSV. Inclui evolução V1/V3 → V4/V5/V6, rollback e concorrência, permissões, filtros e faturamento. Constrói 23 telas, exige Voltar nas secundárias e aciona fluxos Swing de checkout, gestão, perfil/cupons, avaliações/avisos e dashboard/relatórios/navegação. Evidências: `docs/ultimo-teste-integracao.txt`, `docs/validacao-parte2.txt`, `docs/validacao-compra.txt` e `docs/validacao-pedidos.txt`. O teste de gestão fornece respostas aos diálogos de confirmação/motivo sem exibir janelas.

Para verificar também o ambiente persistente, reinício, restauração e construção das 23 telas atuais:

```powershell
./scripts/verificar-base.ps1
```

Esse roteiro cria registros fictícios próprios de retirada e cancelamento, confere valores, saldo, pagamento, imagem, devolução e histórico antes/depois do reinício e da restauração, e remove somente suas fixtures. As telas são construídas no EDT sem exibir janelas. Isso verifica a inicialização; não substitui os testes de interação de todos os módulos. Evidência: `docs/validacao-base.txt`; renderizações: `docs/validacao-ui`.

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

As etapas 1 a 6 tratam persistência, cadastros, compra, pagamento e entrega simulados, acompanhamento e cancelamento antes do envio. Perfil, cupons, avaliações, notificações, dashboard e relatórios/CSV estão implementados. Revisão final integrada, documentação acadêmica/diagramas e pacote de apresentação ainda dependem das próximas etapas. Não há devolução pós-envio ou estorno financeiro real. O JAR gerado serve para desenvolvimento; a instalação completa e o pacote da apresentação ainda serão validados.
