param([switch]$BancoExterno, [switch]$SomenteDiagnostico, [string]$Config)
. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-Compilacao
$configOficial = Join-Path $raizProjeto 'config/banco.properties'
$configExterna = (Test-Path -LiteralPath $configOficial) -and -not ((Get-Content -LiteralPath $configOficial -Raw -Encoding UTF8).StartsWith('# OBotBarato: ambiente local gerenciado'))
$temOverride = $null -ne $env:DB_URL -or $null -ne $env:DB_USUARIO -or $null -ne $env:DB_SENHA
if (-not $BancoExterno -and -not $Config -and -not $configExterna -and -not $temOverride) {
    & (Join-Path $PSScriptRoot 'preparar-ambiente.ps1')
    $Config = (Get-AmbienteLocal).Config
} elseif (-not $Config -and (Test-Path -LiteralPath $configOficial)) { $Config = $configOficial }
if ($Config) { $Config = (Resolve-Path -LiteralPath $Config).Path }
Invoke-JavaProjeto 'util.DiagnosticoBanco' $Config
if ($SomenteDiagnostico) { return }
Push-Location -LiteralPath $raizProjeto
try {
    $argsJava = @('-Dfile.encoding=UTF-8')
    if ($Config) { $argsJava += "-Dobotbarato.config=$Config" }
    & (Get-JavaExecutavel) @argsJava -jar (Get-PacoteProjeto)
    if ($LASTEXITCODE -ne 0) { throw 'Aplicativo terminou com erro.' }
} finally { Pop-Location }
