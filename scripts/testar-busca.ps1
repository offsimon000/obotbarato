. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-SemOverrideBanco
Confirmar-Compilacao
$ambienteTesteBusca=Get-AmbienteLocal
Invoke-ControlePostgres $ambienteTesteBusca start
$pastaTesteBusca=Join-Path $raizProjeto ('.local/teste-busca-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $pastaTesteBusca | Out-Null
$configTesteBusca=Join-Path $pastaTesteBusca 'banco.properties'
[IO.File]::WriteAllText($configTesteBusca,"url=jdbc:postgresql://127.0.0.1:$($ambienteTesteBusca.Porta)/obotbarato_test`nusuario=$($ambienteTesteBusca.Usuario)`nsenha=$($ambienteTesteBusca.Senha)`n",[Text.UTF8Encoding]::new($false))
$senhaAnteriorBusca=$env:PGPASSWORD
$criadaBusca=$false
Push-Location -LiteralPath $raizProjeto
try {
    $env:PGPASSWORD=$ambienteTesteBusca.Senha
    & (Join-Path $ambienteTesteBusca.PgBin 'createdb.exe') -w -h 127.0.0.1 -p $ambienteTesteBusca.Porta -U $ambienteTesteBusca.Usuario obotbarato_test
    if ($LASTEXITCODE -ne 0) { throw 'Banco obotbarato_test indisponivel ou ja existente; nenhum banco existente foi alterado.' }
    $criadaBusca=$true
    Invoke-JavaProjeto 'util.InicializarBanco' $configTesteBusca @('--demo') | Out-Host
    Invoke-JavaProjeto 'util.InstalarCatalogo' $configTesteBusca @((Join-Path $raizProjeto 'catalogo')) | Out-Host
    New-Item -ItemType Directory -Force -Path 'build/test/classes' | Out-Null
    $cpTesteBusca=(Get-PacoteProjeto)+';dist/lib/postgresql-42.7.13.jar'
    & (Get-JavaExecutavel 'javac') --release 17 -encoding UTF-8 -cp $cpTesteBusca -d 'build/test/classes' 'test/BuscaProdutos.java'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar teste de busca.' }
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$configTesteBusca" -cp ('build/test/classes;'+$cpTesteBusca) BuscaProdutos | Tee-Object -FilePath (Join-Path $raizProjeto 'docs/validacao-busca.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de busca nao aprovado.' }
} finally {
    if ($criadaBusca) {
        # Nome fixo de base criada com sucesso por este roteiro; nunca o banco principal.
        & (Join-Path $ambienteTesteBusca.PgBin 'dropdb.exe') -w -h 127.0.0.1 -p $ambienteTesteBusca.Porta -U $ambienteTesteBusca.Usuario obotbarato_test
        if ($LASTEXITCODE -ne 0) { Write-Warning 'Confira limpeza da base de teste criada por este roteiro.' }
    }
    $env:PGPASSWORD=$senhaAnteriorBusca
    Pop-Location
}
