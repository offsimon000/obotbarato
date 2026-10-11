. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-SemOverrideBanco
Confirmar-Compilacao
$ambienteTesteSim=Get-AmbienteLocal
Invoke-ControlePostgres $ambienteTesteSim start
$pastaTesteSim=Join-Path $raizProjeto ('.local/teste-simulacao-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $pastaTesteSim | Out-Null
$configTesteSim=Join-Path $pastaTesteSim 'banco.properties'
[IO.File]::WriteAllText($configTesteSim,"url=jdbc:postgresql://127.0.0.1:$($ambienteTesteSim.Porta)/obotbarato_test`nusuario=$($ambienteTesteSim.Usuario)`nsenha=$($ambienteTesteSim.Senha)`n",[Text.UTF8Encoding]::new($false))
$senhaAnteriorSim=$env:PGPASSWORD
$criadaSim=$false
Push-Location -LiteralPath $raizProjeto
try {
    $env:PGPASSWORD=$ambienteTesteSim.Senha
    & (Join-Path $ambienteTesteSim.PgBin 'createdb.exe') -w -h 127.0.0.1 -p $ambienteTesteSim.Porta -U $ambienteTesteSim.Usuario obotbarato_test
    if ($LASTEXITCODE -ne 0) { throw 'Banco obotbarato_test indisponivel ou ja existente; nenhum banco existente foi alterado.' }
    $criadaSim=$true
    Invoke-JavaProjeto 'util.InicializarBanco' $configTesteSim @('--demo') | Out-Host
    Invoke-JavaProjeto 'util.InstalarCatalogo' $configTesteSim @((Join-Path $raizProjeto 'catalogo')) | Out-Host
    New-Item -ItemType Directory -Force -Path 'build/test/classes' | Out-Null
    $cpTesteSim=(Get-PacoteProjeto)+';dist/lib/postgresql-42.7.13.jar'
    & (Get-JavaExecutavel 'javac') --release 17 -encoding UTF-8 -cp $cpTesteSim -d 'build/test/classes' 'test/SimulacaoUsoDiario.java'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar teste de simulacao.' }
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$configTesteSim" -cp ('build/test/classes;'+$cpTesteSim) SimulacaoUsoDiario | Tee-Object -FilePath (Join-Path $raizProjeto 'docs/validacao-simulacao-uso.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste da simulacao nao aprovado.' }
} finally {
    if ($criadaSim) {
        # Nome fixo de base criada com sucesso por este roteiro; nunca o banco principal.
        & (Join-Path $ambienteTesteSim.PgBin 'dropdb.exe') -w -h 127.0.0.1 -p $ambienteTesteSim.Porta -U $ambienteTesteSim.Usuario obotbarato_test
        if ($LASTEXITCODE -ne 0) { Write-Warning 'Confira limpeza da base de teste criada por este roteiro.' }
    }
    $env:PGPASSWORD=$senhaAnteriorSim
    Pop-Location
}
