param([Parameter(Mandatory=$true)][string]$Arquivo)
. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-SemOverrideBanco
if (-not (Test-Path -LiteralPath $Arquivo -PathType Leaf)) { throw 'Arquivo de backup nao encontrado.' }
$arquivoCompleto = (Resolve-Path -LiteralPath $Arquivo).Path
$ambiente = Get-AmbienteLocal
Invoke-ControlePostgres $ambiente start
$bancoNovo = 'obotbarato_restaurado_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '_' + [Guid]::NewGuid().ToString('N').Substring(0,8)
$senhaAnterior = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $ambiente.Senha
    & (Join-Path $ambiente.PgBin 'createdb.exe') -w -h 127.0.0.1 -p $ambiente.Porta -U $ambiente.Usuario -E UTF8 $bancoNovo
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel criar o banco de restauracao.' }
    & (Join-Path $ambiente.PgBin 'pg_restore.exe') -w -h 127.0.0.1 -p $ambiente.Porta -U $ambiente.Usuario -d $bancoNovo --exit-on-error --single-transaction --no-owner --no-privileges $arquivoCompleto
    if ($LASTEXITCODE -ne 0) { throw "Restauracao falhou; banco principal preservado. Destino: $bancoNovo" }
} finally { $env:PGPASSWORD = $senhaAnterior }
$pasta = Join-Path $raizProjeto ('.local/restauracoes/' + $bancoNovo)
New-Item -ItemType Directory -Path $pasta | Out-Null
$config = Join-Path $pasta 'banco.properties'
[IO.File]::WriteAllText($config, "url=jdbc:postgresql://127.0.0.1:$($ambiente.Porta)/$bancoNovo`nusuario=$($ambiente.Usuario)`nsenha=$($ambiente.Senha)`n", [Text.UTF8Encoding]::new($false))
Confirmar-Compilacao
Invoke-JavaProjeto 'util.DiagnosticoBanco' $config | Out-Host
Write-Host "Restauracao validada em banco novo. Configuracao: $config"
return $config
