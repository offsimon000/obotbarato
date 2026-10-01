. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-SemOverrideBanco
$ambiente = Get-AmbienteLocal
Invoke-ControlePostgres $ambiente start
$pasta = Join-Path $raizProjeto '.local/backups'
New-Item -ItemType Directory -Force -Path $pasta | Out-Null
$arquivo = Join-Path $pasta ('obotbarato_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '_' + [Guid]::NewGuid().ToString('N').Substring(0,8) + '.dump')
$senhaAnterior = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $ambiente.Senha
    & (Join-Path $ambiente.PgBin 'pg_dump.exe') -w -h 127.0.0.1 -p $ambiente.Porta -U $ambiente.Usuario -d $ambiente.Banco -Fc --no-owner --no-privileges -f $arquivo
    if ($LASTEXITCODE -ne 0) { throw 'Backup nao concluido.' }
} finally { $env:PGPASSWORD = $senhaAnterior }
Write-Host "Backup concluido: $arquivo"
return $arquivo
