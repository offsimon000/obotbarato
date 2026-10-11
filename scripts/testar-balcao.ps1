. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-SemOverrideBanco
Confirmar-Compilacao
$ambienteTesteBalcao=Get-AmbienteLocal
Invoke-ControlePostgres $ambienteTesteBalcao start
$pastaTesteBalcao=Join-Path $raizProjeto ('.local/teste-balcao-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $pastaTesteBalcao | Out-Null
$configTesteBalcao=Join-Path $pastaTesteBalcao 'banco.properties'
[IO.File]::WriteAllText($configTesteBalcao,"url=jdbc:postgresql://127.0.0.1:$($ambienteTesteBalcao.Porta)/obotbarato_test`nusuario=$($ambienteTesteBalcao.Usuario)`nsenha=$($ambienteTesteBalcao.Senha)`n",[Text.UTF8Encoding]::new($false))
$senhaAnteriorBalcao=$env:PGPASSWORD
$criadaBalcao=$false
Push-Location -LiteralPath $raizProjeto
try {
    $env:PGPASSWORD=$ambienteTesteBalcao.Senha
    & (Join-Path $ambienteTesteBalcao.PgBin 'createdb.exe') -w -h 127.0.0.1 -p $ambienteTesteBalcao.Porta -U $ambienteTesteBalcao.Usuario obotbarato_test
    if ($LASTEXITCODE -ne 0) { throw 'Banco obotbarato_test indisponivel ou ja existente; nenhum banco existente foi alterado.' }
    $criadaBalcao=$true
    Invoke-JavaProjeto 'util.InicializarBanco' $configTesteBalcao @('--demo') | Out-Host
    Invoke-JavaProjeto 'util.InstalarCatalogo' $configTesteBalcao @((Join-Path $raizProjeto 'catalogo')) | Out-Host
    New-Item -ItemType Directory -Force -Path 'build/test/classes' | Out-Null
    $cpTesteBalcao=(Get-PacoteProjeto)+';dist/lib/postgresql-42.7.13.jar'
    & (Get-JavaExecutavel 'javac') --release 17 -encoding UTF-8 -cp $cpTesteBalcao -d 'build/test/classes' 'test/FluxoVendaBalcao.java'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar teste de balcao.' }
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$configTesteBalcao" -cp ('build/test/classes;'+$cpTesteBalcao) FluxoVendaBalcao | Tee-Object -FilePath (Join-Path $raizProjeto 'docs/validacao-venda-balcao.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de balcao nao aprovado.' }
} finally {
    if ($criadaBalcao) {
        # Nome fixo de base criada com sucesso por este roteiro; nunca o banco principal.
        & (Join-Path $ambienteTesteBalcao.PgBin 'dropdb.exe') -w -h 127.0.0.1 -p $ambienteTesteBalcao.Porta -U $ambienteTesteBalcao.Usuario obotbarato_test
        if ($LASTEXITCODE -ne 0) { Write-Warning 'Confira limpeza da base de teste criada por este roteiro.' }
    }
    $env:PGPASSWORD=$senhaAnteriorBalcao
    Pop-Location
}
