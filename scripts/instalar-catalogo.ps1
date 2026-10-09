. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-SemOverrideBanco
# A importação é manual: o lançador não repõe saldos nem troca os cadastros ao abrir.
& (Join-Path $PSScriptRoot 'backup.ps1') | Out-Host
Confirmar-Compilacao
$ambienteCatalogo=Get-AmbienteLocal
Invoke-JavaProjeto 'util.InstalarCatalogo' $ambienteCatalogo.Config @((Join-Path $raizProjeto 'catalogo'))
