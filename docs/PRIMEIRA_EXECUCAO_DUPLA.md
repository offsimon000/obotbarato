# Primeira execução em outro computador

O banco PostgreSQL é local a cada computador. Sua dupla não usa o banco que está no computador do autor: o primeiro início cria uma base própria e carrega nela os mesmos dados fictícios de apresentação.

1. Baixe o ZIP mais recente em [Releases do OBotBarato](https://github.com/offsimon000/obotbarato/releases/latest) e extraia a pasta inteira em um local gravável, como Documentos. Não execute o projeto de dentro do ZIP.
2. Instale um JDK 17 ou posterior e PostgreSQL para Windows. Durante a instalação do PostgreSQL, inclua as ferramentas de linha de comando (`initdb`, `pg_ctl`, `psql` e `createdb`).
3. Abra o PowerShell na pasta extraída `OBotBarato` e gere o atalho:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File scripts/criar-atalho.ps1
   ```

4. Dê dois cliques em **Executar OBotBarato** na Área de Trabalho ou na pasta do projeto. Na primeira vez, aguarde a preparação do PostgreSQL, a compilação e a carga do catálogo/dados de demonstração. Nas próximas aberturas, o banco existente será reutilizado.
5. Entre com uma conta fictícia, por exemplo `admin` / `admin123`, `vendedor` / `vendedor123` ou `cliente@demo.local` / `cliente123`. As contas da simulação usam `cliente01@uso.obotbarato.test` até `cliente20@uso.obotbarato.test`, todas com senha `demo12345`.

O atalho é um arquivo `.lnk` que contém o caminho local daquela cópia, por isso cada pessoa o cria no próprio computador. Não é um `.exe` nem precisa ser enviado ao GitHub. O instalador do PostgreSQL e o JDK também são pré-requisitos e não fazem parte do ZIP. Se a abertura falhar, consulte `.local/abertura-desktop.log` na pasta do projeto.

Se a dupla quiser ver os mesmos dados atualizados ao mesmo tempo em dois computadores, seria necessário configurar um PostgreSQL compartilhado em um servidor. Para instalar e apresentar o projeto, cada computador tem sua base local independente.
