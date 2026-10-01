param([string]$FixtureExistente)
. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-SemOverrideBanco
$log = Join-Path $raizProjeto 'docs/validacao-base.txt'
New-Item -ItemType Directory -Force -Path (Join-Path $raizProjeto 'docs') | Out-Null
$servicosAntes = @(Get-Service '*postgres*' | Select-Object Name,Status)
& (Join-Path $PSScriptRoot 'preparar-ambiente.ps1') | Tee-Object -FilePath $log
$ambiente = Get-AmbienteLocal
Invoke-JavaProjeto 'util.DiagnosticoBanco' $ambiente.Config @('--demo') | Tee-Object -FilePath $log -Append
if ($FixtureExistente) { $FixtureExistente = (Resolve-Path -LiteralPath $FixtureExistente).Path }
Push-Location -LiteralPath $raizProjeto
try {
    New-Item -ItemType Directory -Force -Path 'build/test/classes' | Out-Null
    & (Get-JavaExecutavel 'javac') --release 17 -encoding UTF-8 -cp 'dist/OBotBarato.jar;dist/lib/postgresql-42.7.13.jar' -d 'build/test/classes' 'test/CicloPersistencia.java' 'test/InicializacaoTelas.java'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar verificacao da base.' }
    $fixture = if ($FixtureExistente) { $FixtureExistente } else { Join-Path $ambiente.Pasta ('fixture-' + [Guid]::NewGuid().ToString('N') + '.properties') }
    $cp = 'build/test/classes;dist/OBotBarato.jar;dist/lib/postgresql-42.7.13.jar'
    function Invoke-Ciclo([string]$Acao, [string]$Config) {
        & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$Config" -cp $cp CicloPersistencia $Acao $fixture | Tee-Object -FilePath $log -Append
        if ($LASTEXITCODE -ne 0) { throw "Falha na verificacao de persistencia: $Acao" }
    }
    if ($FixtureExistente) { Invoke-Ciclo 'verificar' $ambiente.Config }
    else { Invoke-Ciclo 'criar' $ambiente.Config }
    Invoke-ControlePostgres $ambiente stop
    & (Join-Path $PSScriptRoot 'preparar-ambiente.ps1') | Tee-Object -FilePath $log -Append
    Invoke-Ciclo 'verificar' $ambiente.Config
    $backup = & (Join-Path $PSScriptRoot 'backup.ps1')
    $restaurada = & (Join-Path $PSScriptRoot 'restaurar-backup.ps1') -Arquivo $backup
    Invoke-Ciclo 'verificar' $restaurada
    Invoke-Ciclo 'limpar' $restaurada
    Invoke-Ciclo 'limpar' $ambiente.Config
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$($ambiente.Config)" -cp $cp InicializacaoTelas | Tee-Object -FilePath $log -Append
    if ($LASTEXITCODE -ne 0) { throw 'Falha na inicializacao das telas.' }
    # Sem o JAR do driver no classpath, o diagnóstico deve falhar claramente.
    $preferenciaAnterior = $ErrorActionPreference
    try {
        # stderr é esperado neste cenário; o PowerShell 5.1 não deve interromper o teste.
        $ErrorActionPreference = 'Continue'
        & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$($ambiente.Config)" -cp 'build/classes' util.DiagnosticoBanco *> (Join-Path $ambiente.Pasta 'driver-ausente.log')
        $codigoFalhaEsperada = $LASTEXITCODE
    } finally { $ErrorActionPreference = $preferenciaAnterior }
    if ($codigoFalhaEsperada -eq 0) { throw 'Diagnostico aceitou driver ausente.' }
    if (-not (Select-String -LiteralPath (Join-Path $ambiente.Pasta 'driver-ausente.log') -Pattern 'Driver do banco ausente' -Quiet)) { throw 'Mensagem de driver ausente nao corresponde ao erro.' }
    'OK: driver ausente identificado sem abrir o aplicativo.' | Tee-Object -FilePath $log -Append
    foreach ($antes in $servicosAntes) {
        if ((Get-Service -Name $antes.Name).Status -ne $antes.Status) { throw 'Estado do servico PostgreSQL original mudou.' }
    }
    'OK: estado dos servicos PostgreSQL originais preservado.' | Tee-Object -FilePath $log -Append
    'APROVADO: instalacao, autenticacao, reinicio, backup/restauracao e inicializacao das telas.' | Tee-Object -FilePath $log -Append
    [IO.File]::WriteAllText($log, [IO.File]::ReadAllText($log), [Text.UTF8Encoding]::new($false))
} finally { Pop-Location }
