param([string]$PgBin)
. (Join-Path $PSScriptRoot 'comum.ps1')
$PgBin = Get-PostgresBin $PgBin
$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
foreach ($nome in @('DB_URL', 'DB_USUARIO', 'DB_SENHA')) {
    if ($null -ne [Environment]::GetEnvironmentVariable($nome)) {
        throw "Variavel $nome definida. Abra um terminal sem overrides para testar a base isolada."
    }
}
& (Join-Path $PSScriptRoot 'compilar.ps1')
$pastaTeste = Join-Path $raizProjeto ('.local/teste-' + [Guid]::NewGuid().ToString('N'))
$dados = Join-Path $pastaTeste 'postgres'
New-Item -ItemType Directory -Path $pastaTeste | Out-Null
$bytesSenha = New-Object byte[] 24
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytesSenha) } finally { $rng.Dispose() }
$senhaTeste = [Convert]::ToBase64String($bytesSenha)
$arquivoSenha = Join-Path $pastaTeste 'senha.txt'
[IO.File]::WriteAllText($arquivoSenha, $senhaTeste, [Text.UTF8Encoding]::new($false))
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$listener.Start()
$portaTeste = $listener.LocalEndpoint.Port
$listener.Stop()
$arquivoConfig = Join-Path $pastaTeste 'banco.properties'
[IO.File]::WriteAllText($arquivoConfig,
    "url=jdbc:postgresql://127.0.0.1:$portaTeste/obotbarato_test`nusuario=obotbarato_test`nsenha=$senhaTeste`n",
    [Text.UTF8Encoding]::new($false))
$pgCtl = Join-Path $PgBin 'pg_ctl.exe'
$iniciado = $false
Push-Location -LiteralPath $raizProjeto
try {
    & (Join-Path $PgBin 'initdb.exe') -D $dados -U obotbarato_test --pwfile=$arquivoSenha -A scram-sha-256 --encoding=UTF8 --locale=C > (Join-Path $pastaTeste 'initdb.log') 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Falha no initdb. Consulte $pastaTeste/initdb.log" }
    $processo = Start-Process -FilePath $pgCtl -ArgumentList @('-D', ('"' + $dados + '"'), '-l', ('"' + (Join-Path $pastaTeste 'servidor.log') + '"'), '-o', ('"-h 127.0.0.1 -p ' + $portaTeste + '"'), '-w', 'start') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $pastaTeste 'inicio.log') -RedirectStandardError (Join-Path $pastaTeste 'inicio-erro.log')
    # Aguarda só pg_ctl. Start-Process -Wait aguarda também o servidor filho permanente.
    $handleProcesso=$processo.Handle
    if (-not $processo.WaitForExit(60000)) { throw 'Tempo limite ao iniciar cluster isolado.' }
    & $pgCtl -D $dados status > (Join-Path $pastaTeste 'status.log') 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Falha ao iniciar PostgreSQL isolado. Consulte $pastaTeste/inicio-erro.log" }
    $iniciado = $true
    $senhaAnterior = $env:PGPASSWORD
    try {
        $env:PGPASSWORD = $senhaTeste
        & (Join-Path $PgBin 'createdb.exe') -h 127.0.0.1 -p $portaTeste -U obotbarato_test obotbarato_test
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar base de teste.' }
    } finally { $env:PGPASSWORD = $senhaAnterior }
    New-Item -ItemType Directory -Force -Path 'build/test/classes' | Out-Null
    & (Get-JavaExecutavel 'javac') --release 17 -encoding UTF-8 -cp 'build/classes;lib/postgresql-42.7.13.jar' -d 'build/test/classes' 'test/IntegracaoBanco.java' 'test/CadastrosEAcesso.java' 'test/EvolucaoDaBase.java' 'test/InicializacaoTelas.java' 'test/CompraEPagamento.java' 'test/FluxoCheckout.java' 'test/GestaoECancelamento.java' 'test/FluxoGestaoPedidos.java' 'test/InterfaceBrasileira.java' 'test/CatalogoApresentacao.java' 'test/PerfilECupons.java' 'test/FluxoPerfilCupons.java' 'test/AvaliacoesENotificacoes.java' 'test/FluxoAvaliacoesAvisos.java' 'test/RelatoriosFinanceiros.java' 'test/FluxoRelatoriosNavegacao.java' 'test/FluxoVendaBalcao.java' 'test/BuscaProdutos.java'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar testes.' }
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' EvolucaoDaBase | Tee-Object -FilePath (Join-Path $pastaTeste 'evolucao.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de evolucao V1/V2 falhou.' }
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' IntegracaoBanco | Tee-Object -FilePath (Join-Path $pastaTeste 'resultado.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de integracao falhou.' }
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' CadastrosEAcesso | Tee-Object -FilePath (Join-Path $pastaTeste 'parte2.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de cadastros e acesso falhou.' }
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' CompraEPagamento | Tee-Object -FilePath (Join-Path $pastaTeste 'compra.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de compra e pagamento falhou.' }
    [IO.File]::WriteAllText((Join-Path $raizProjeto 'docs/validacao-compra.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'compra.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' GestaoECancelamento | Tee-Object -FilePath (Join-Path $pastaTeste 'gestao.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de gestao e cancelamento falhou.' }
    [IO.File]::WriteAllText((Join-Path $raizProjeto 'docs/validacao-pedidos.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'evolucao.txt'))+[IO.File]::ReadAllText((Join-Path $pastaTeste 'gestao.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' PerfilECupons | Tee-Object -FilePath (Join-Path $pastaTeste 'perfil-cupons.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de perfil e cupons falhou.' }
    [IO.File]::WriteAllText((Join-Path $raizProjeto 'docs/validacao-perfil-cupons.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'perfil-cupons.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' AvaliacoesENotificacoes | Tee-Object -FilePath (Join-Path $pastaTeste 'avaliacoes-avisos.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de avaliacoes e notificacoes falhou.' }
    [IO.File]::WriteAllText((Join-Path $raizProjeto 'docs/validacao-avaliacoes-avisos.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'avaliacoes-avisos.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' RelatoriosFinanceiros | Tee-Object -FilePath (Join-Path $pastaTeste 'relatorios.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de relatorios e CSV falhou.' }
    [IO.File]::WriteAllText((Join-Path $raizProjeto 'docs/validacao-relatorios-navegacao.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'relatorios.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' InicializacaoTelas | Tee-Object -FilePath (Join-Path $pastaTeste 'telas.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de inicializacao das telas falhou.' }
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' FluxoCheckout | Tee-Object -FilePath (Join-Path $pastaTeste 'fluxo-checkout.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Fluxo da interface de checkout falhou.' }
    [IO.File]::AppendAllText((Join-Path $raizProjeto 'docs/validacao-compra.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'fluxo-checkout.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' FluxoGestaoPedidos | Tee-Object -FilePath (Join-Path $pastaTeste 'fluxo-gestao.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Fluxo da interface de gestao falhou.' }
    [IO.File]::AppendAllText((Join-Path $raizProjeto 'docs/validacao-pedidos.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'fluxo-gestao.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' InterfaceBrasileira | Tee-Object -FilePath (Join-Path $pastaTeste 'interface.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste de datas e interface falhou.' }
    [IO.File]::WriteAllText((Join-Path $raizProjeto 'docs/validacao-interface.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'interface.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' FluxoPerfilCupons | Tee-Object -FilePath (Join-Path $pastaTeste 'fluxo-perfil-cupons.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Fluxo de perfil e cupons falhou.' }
    [IO.File]::AppendAllText((Join-Path $raizProjeto 'docs/validacao-perfil-cupons.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'fluxo-perfil-cupons.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' FluxoAvaliacoesAvisos | Tee-Object -FilePath (Join-Path $pastaTeste 'fluxo-avaliacoes-avisos.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Fluxo de avaliacoes e avisos falhou.' }
    [IO.File]::AppendAllText((Join-Path $raizProjeto 'docs/validacao-avaliacoes-avisos.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'fluxo-avaliacoes-avisos.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' FluxoRelatoriosNavegacao | Tee-Object -FilePath (Join-Path $pastaTeste 'fluxo-relatorios.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Fluxo de relatorios e navegacao falhou.' }
    [IO.File]::AppendAllText((Join-Path $raizProjeto 'docs/validacao-relatorios-navegacao.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'fluxo-relatorios.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' FluxoVendaBalcao | Tee-Object -FilePath (Join-Path $pastaTeste 'balcao.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Fluxo de venda no balcao falhou.' }
    [IO.File]::WriteAllText((Join-Path $raizProjeto 'docs/validacao-venda-balcao.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'balcao.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' BuscaProdutos | Tee-Object -FilePath (Join-Path $pastaTeste 'busca.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste da busca falhou.' }
    [IO.File]::WriteAllText((Join-Path $raizProjeto 'docs/validacao-busca.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'busca.txt')),[Text.UTF8Encoding]::new($false))
    & (Get-JavaExecutavel) '-Dfile.encoding=UTF-8' "-Dobotbarato.config=$arquivoConfig" -cp 'build/test/classes;build/classes;lib/postgresql-42.7.13.jar' CatalogoApresentacao | Tee-Object -FilePath (Join-Path $pastaTeste 'catalogo.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Teste do catalogo de apresentacao falhou.' }
    [IO.File]::WriteAllText((Join-Path $raizProjeto 'docs/validacao-catalogo.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'catalogo.txt')),[Text.UTF8Encoding]::new($false))
    $evidencia=[IO.File]::ReadAllText((Join-Path $pastaTeste 'evolucao.txt'))+[IO.File]::ReadAllText((Join-Path $pastaTeste 'parte2.txt'))+[IO.File]::ReadAllText((Join-Path $pastaTeste 'telas.txt'))
    [IO.File]::WriteAllText((Join-Path $raizProjeto 'docs/validacao-parte2.txt'),$evidencia,[Text.UTF8Encoding]::new($false))
    New-Item -ItemType Directory -Force -Path 'docs' | Out-Null
    [IO.File]::WriteAllText((Join-Path $raizProjeto 'docs/ultimo-teste-integracao.txt'),[IO.File]::ReadAllText((Join-Path $pastaTeste 'resultado.txt')),[Text.UTF8Encoding]::new($false))
} finally {
    if ($iniciado -or (Test-Path -LiteralPath (Join-Path $dados 'postmaster.pid'))) {
        & $pgCtl -D $dados -m fast -t 60 -w stop > (Join-Path $pastaTeste 'parada.log') 2>&1
        if ($LASTEXITCODE -ne 0) { Write-Warning "Confira parada do cluster isolado em $dados" }
    }
    Pop-Location
}
Write-Host 'Teste encerrado; cluster isolado parado. Resultados em docs/ultimo-teste-integracao.txt.'
