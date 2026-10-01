. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-SemOverrideBanco
$ambiente = Get-AmbienteLocal
Invoke-ControlePostgres $ambiente stop
Write-Host 'Banco local do OBotBarato parado. Dados preservados.'
