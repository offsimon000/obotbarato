$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $raizProjeto
try {
    if (-not (Test-Path -LiteralPath 'dist/OBotBarato.jar')) { & './scripts/compilar.ps1' }
    & java -jar 'dist/OBotBarato.jar'
    if ($LASTEXITCODE -ne 0) { throw 'Aplicativo terminou com erro.' }
} finally { Pop-Location }
