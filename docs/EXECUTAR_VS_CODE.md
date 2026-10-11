# Executar no VS Code

1. Abra no VS Code o arquivo **OBotBarato.code-workspace** desta pasta. Abra a pasta OBotBarato, que contém `src`, `sql` e `.vscode`, se preferir.
2. Instale **Extension Pack for Java**, da Microsoft, quando o editor oferecer as extensões recomendadas. Nesta máquina ele já está instalado.
3. Tenha JDK 17 ou posterior e PostgreSQL instalados. A compilação usa `--release 17`; os scripts procuram instalações padrão ou `JAVA_HOME` e `OBOTBARATO_PG_BIN`.
4. Aguarde a importação do Java. Em **Executar e Depurar**, selecione **OBotBarato - Desktop** e pressione **F5**. A tarefa anterior compila os fontes e prepara o banco; depois o depurador abre `view.TelaLoginUnificado`.
5. Encerre com **Shift+F5**. Feche todas as janelas de uma versão anterior antes de conferir mudanças; sair para o login mantém o mesmo processo Java.

Na primeira execução de uma cópia nova, há uma demonstração básica. Para reproduzir os 30 produtos e 60 pedidos fictícios, use **Terminal → Executar Tarefa → OBotBarato: carregar demonstração**. A carga é manual, faz backup e preserva os dados que já existirem. Repeti-la não repõe saldos nem duplica os pedidos. Na instalação atual, a carga já foi feita.

## Tarefas disponíveis

Use **Terminal → Executar Tarefa** para executar, compilar, carregar demonstração, testar integração, testar busca, verificar persistência e fazer backup. **Ctrl+Shift+B** compila. Os scripts PowerShell são ferramentas do projeto e podem ser usados no terminal integrado:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/executar.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/testar-integracao.ps1
```

O banco próprio fica em `.local/desenvolvimento`. Configuração e senha reais são locais e ignoradas pelo Git. O PostgreSQL antigo do computador é separado. Para parar somente o banco do projeto, execute `scripts/parar-banco.ps1`; a próxima preparação o inicia novamente. Dependências compiladas ficam em `build` e `dist`, também ignoradas. O diretório `build/vscode-classes` do editor é separado do `build/classes` produzido pelos scripts.

## Problemas comuns

- **JDK não encontrado:** instale JDK 17+, configure `JAVA_HOME` para sua pasta e reabra o editor. Em **Java: Configure Java Runtime**, escolha esse JDK para projetos sem ferramenta de build.
- **PostgreSQL não encontrado:** instale PostgreSQL ou informe `OBOTBARATO_PG_BIN` com a pasta `bin`. O projeto foi validado com PostgreSQL 18 e codificação UTF-8.
- **PowerShell bloqueado:** as tarefas usam `-ExecutionPolicy Bypass` somente no processo chamado; não alteram a política global do Windows.
- **Variáveis `DB_*` configuradas:** elas têm precedência. Remova-as para usar o banco local gerenciado ou siga o procedimento de banco externo no README.
- **Editor aberto na pasta Extenção:** abra a pasta OBotBarato ou o workspace fornecido para que o F5 use o diretório correto.

Não há lançadores `.cmd` nem configuração NetBeans/Ant na entrega. O Java Swing continua desktop; o VS Code é o ambiente de edição e depuração.
