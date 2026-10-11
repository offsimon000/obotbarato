param([string]$PgBin)
. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-SemOverrideBanco
Confirmar-Compilacao
$pastaLocal = Join-Path $raizProjeto '.local/desenvolvimento'
$metadados = Join-Path $pastaLocal 'ambiente.json'
$configOficial = Join-Path $raizProjeto 'config/banco.properties'
$marcaConfig = '# OBotBarato: ambiente local gerenciado'
if ((Test-Path -LiteralPath $configOficial) -and -not ((Get-Content -LiteralPath $configOficial -Raw -Encoding UTF8).StartsWith($marcaConfig))) {
    throw 'Configuracao de banco ja existente foi preservada. Use executar.ps1 -BancoExterno, ou guarde essa configuracao antes de preparar o ambiente local.'
}
if (-not (Test-Path -LiteralPath $metadados)) {
    $bin = Get-PostgresBin $PgBin
    if (Test-Path -LiteralPath (Join-Path $pastaLocal 'postgres/PG_VERSION')) { throw 'Cluster sem metadados encontrado. Dados preservados; verifique a pasta .local/desenvolvimento.' }
    New-Item -ItemType Directory -Force -Path $pastaLocal | Out-Null
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $listener.Start(); $porta = $listener.LocalEndpoint.Port; $listener.Stop()
    $bytes = New-Object byte[] 24
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $senha = [Convert]::ToBase64String($bytes)
    [IO.File]::WriteAllText((Join-Path $pastaLocal 'senha.txt'), $senha, [Text.UTF8Encoding]::new($false))
    $estado = [PSCustomObject]@{ Projeto='OBotBarato-local-v1'; PgBin=$bin; Porta=$porta; Banco='obotbarato'; Usuario='obotbarato_dev' }
    [IO.File]::WriteAllText($metadados, ($estado | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
    $config = "$marcaConfig`nurl=jdbc:postgresql://127.0.0.1:$porta/obotbarato`nusuario=obotbarato_dev`nsenha=$senha`n"
    [IO.File]::WriteAllText((Join-Path $pastaLocal 'banco.properties'), $config, [Text.UTF8Encoding]::new($false))
}
$ambiente = Get-AmbienteLocal
if (-not (Test-Path -LiteralPath (Join-Path $ambiente.Dados 'PG_VERSION'))) {
    Write-Host 'Preparando a base local pela primeira vez. Aguarde alguns instantes...'
    $arquivoSenha = Join-Path $ambiente.Pasta 'senha.txt'
    & (Join-Path $ambiente.PgBin 'initdb.exe') -D $ambiente.Dados -U $ambiente.Usuario --pwfile=$arquivoSenha -A scram-sha-256 --encoding=UTF8 --locale=C *> (Join-Path $ambiente.Pasta 'initdb.log')
    if ($LASTEXITCODE -ne 0) { throw "Falha ao inicializar a base local. Consulte $($ambiente.Pasta)/initdb.log" }
}
Write-Host 'Conferindo o banco local do OBotBarato...'
Invoke-ControlePostgres $ambiente start
$senhaAnterior = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $ambiente.Senha
    $existe = & (Join-Path $ambiente.PgBin 'psql.exe') -X -w -h 127.0.0.1 -p $ambiente.Porta -U $ambiente.Usuario -d postgres -tA -c "SELECT 1 FROM pg_database WHERE datname='obotbarato'"
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel verificar a base local.' }
    if ($existe -ne '1') {
        & (Join-Path $ambiente.PgBin 'createdb.exe') -w -h 127.0.0.1 -p $ambiente.Porta -U $ambiente.Usuario -E UTF8 $ambiente.Banco
        if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel criar o banco local.' }
    }
} finally { $env:PGPASSWORD = $senhaAnterior }
Invoke-JavaProjeto 'util.InicializarBanco' $ambiente.Config @('--demo')
Copy-Item -LiteralPath $ambiente.Config -Destination $configOficial -Force
Invoke-JavaProjeto 'util.DiagnosticoBanco' $ambiente.Config
Write-Host 'Ambiente local pronto. Use F5 no VS Code ou scripts/executar.ps1.'
