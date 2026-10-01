param([string]$PgBin)
. (Join-Path $PSScriptRoot 'comum.ps1')
$PgBin = Get-PostgresBin $PgBin
$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
foreach ($nome in @('DB_URL', 'DB_USUARIO', 'DB_SENHA')) {
    if ($null -ne [Environment]::GetEnvironmentVariable($nome)) {
        throw "Variavel $nome definida. Abra um terminal sem overrides para testar a base isolada."
    }
}
& (Join-Path $PSScriptRoot 'compilar.ps1')
$pastaTeste = Join-Path $raizProjeto ('.local/teste-' + [Guid]::NewGuid().ToString('N'))
$dados = Join-Path $pastaTeste 'postgres'
New-Item -ItemType Directory -Path $pastaTeste | Out-Null
$bytesSenha = New-Object byte[] 24
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytesSenha) } finally { $rng.Dispose() }
$senhaTeste = [Convert]::ToBase64String($bytesSenha)
$arquivoSenha = Join-Path $pastaTeste 'senha.txt'
[IO.File]::WriteAllText($arquivoSenha, $senhaTeste, [Text.UTF8Encoding]::new($false))
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$listener.Start()
$portaTeste = $listener.LocalEndpoint.Port
$listener.Stop()
$arquivoConfig = Join-Path $pastaTeste 'banco.properties'
[IO.File]::WriteAllText($arquivoConfig,
    "url=jdbc:postgresql://127.0.0.1:$portaTeste/obotbarato_test`nusuario=obotbarato_test`nsenha=$senhaTeste`n",
    [Text.UTF8Encoding]::new($false))
$pgCtl = Join-Path $PgBin 'pg_ctl.exe'
$iniciado = $false
Push-Location -LiteralPath $raizProjeto
try {
    & (Join-Path $PgBin 'initdb.exe') -D $dados -U obotbarato_test --pwfile=$arquivoSenha -A scram-sha-256 --encoding=UTF8 --locale=C > (Join-Path $pastaTeste 'initdb.log') 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Falha no initdb. Consulte $pastaTeste/initdb.log" }
    $processo = Start-Process -FilePath $pgCtl -ArgumentList @('-D', ('"' + $dados + '"'), '-l', ('"' + (Join-Path $pastaTeste 'servidor.log') + '"'), '-o', ('"-h 127.0.0.1 -p ' + $portaTeste + '"'), '-w', 'start') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $pastaTeste 'inicio.log') -RedirectStandardError (Join-Path $pastaTeste 'inicio-erro.log')
    # Aguarda só pg_ctl. Start-Process -Wait aguarda também o servidor filho permanente.
    if (-not $processo.WaitForExit(30000)) { throw 'Tempo limite ao iniciar cluster isolado.' }
    if ($processo.ExitCode -ne 0) { throw "Falha ao iniciar PostgreSQL isolado. Consulte $pastaTeste/inicio-erro.log" }
    $iniciado = $true
    $senhaAnterior = $env:PGPASSWORD
    try {
        $env:PGPASSWORD = $senhaTeste
        & (Join-Path $PgBin 'createdb.exe') -h 127.0.0.1 -p $portaTeste -U obotbarato_test obotbarato_test
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar base de teste.' }
    } finally { $env:PGPASSWORD = $senhaAnterior }
    New-Item -ItemType Directory -Force -Path 'build/test/classes' | Out-Null
    & javac --release 17 -encoding UTF-8 -cp 'build/classes;lib/postgresql-42.7.13.jar' -d 'build/test/classes' 'test/IntegracaoBanco.java'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar testes.' }
    & java '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' IntegracaoBanco | Tee-Object -FilePath (Join-Path $pastaTeste 'resultado.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de integracao falhou.' }
    New-Item -ItemType Directory -Force -Path 'docs' | Out-Null
    Copy-Item -LiteralPath (Join-Path $pastaTeste 'resultado.txt') -Destination 'docs/ultimo-teste-integracao.txt' -Force
} finally {
    if ($iniciado -or (Test-Path -LiteralPath (Join-Path $dados 'postmaster.pid'))) {
        & $pgCtl -D $dados -m fast -w stop > (Join-Path $pastaTeste 'parada.log') 2>&1
        if ($LASTEXITCODE -ne 0) { Write-Warning "Confira parada do cluster isolado em $dados" }
    }
    Pop-Location
}
Write-Host 'Teste encerrado; cluster isolado parado. Resultados em docs/ultimo-teste-integracao.txt.'
