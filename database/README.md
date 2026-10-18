# Banco PostgreSQL de demonstração

O GitHub Release contém `obotbarato-demo.dump`, um backup PostgreSQL fictício criado para a apresentação. Ele inclui o esquema, os 30 produtos com imagens e os registros sintéticos do roteiro acadêmico. Não contém o cluster PostgreSQL desta máquina nem a senha local de conexão. Esse arquivo é um ativo da Release, fora do histórico Git, para manter leve o clone do código.

## Restaurar no Windows

Instale a mesma versão principal ou uma versão mais nova do PostgreSQL. Abra PowerShell dentro da pasta do projeto e ajuste o caminho de `pg_restore.exe` se necessário:

```powershell
$pgBin = 'C:\Program Files\PostgreSQL\18\bin'
$db = 'obotbarato_demo'
& "$pgBin\createdb.exe" -U postgres -h localhost -p 5432 -E UTF8 $db
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar o banco. Escolha outro nome ou porta; não remova bancos existentes.' }
& "$pgBin\pg_restore.exe" -U postgres -h localhost -p 5432 --no-owner --no-privileges -d $db 'database\obotbarato-demo.dump'
if ($LASTEXITCODE -ne 0) { throw 'A restauração não terminou. Remova somente o banco novo vazio que você acabou de criar e tente novamente.' }
```

O banco importado fica separado do PostgreSQL local gerenciado pelo botão Run. Para uso diário no NetBeans, abra o projeto e clique em **Run Project**: o projeto cria automaticamente uma cópia completa da demonstração no cluster local próprio. A restauração do dump serve para uma cópia portátil dos dados e para inspeção pelo pgAdmin.

O arquivo de backup será publicado como ativo de uma Release do repositório. O banco é fictício e suas contas padrão são públicas. Não use essas senhas em outro sistema.
