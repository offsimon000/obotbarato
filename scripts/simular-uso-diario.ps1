param([string]$DataReferencia)
. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-SemOverrideBanco
& (Join-Path $PSScriptRoot 'backup.ps1') | Out-Host
Confirmar-Compilacao
$ambienteSimulacao=Get-AmbienteLocal
$argumentosSimulacao=@()
if ($DataReferencia) { $argumentosSimulacao += $DataReferencia }
Invoke-JavaProjeto 'util.SimularUsoDiario' $ambienteSimulacao.Config $argumentosSimulacao | Tee-Object -FilePath (Join-Path $raizProjeto 'docs/ultima-carga-uso-diario.txt')
