$ErrorActionPreference = 'Stop'
$raizProjeto = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$OutputEncoding = [Text.UTF8Encoding]::new($false)

function Get-JavaExecutavel([string]$Nome = 'java') {
    if ($env:JAVA_HOME) {
        $arquivo = Join-Path $env:JAVA_HOME ('bin/' + $Nome + '.exe')
        if (Test-Path -LiteralPath $arquivo) { return $arquivo }
    }
    $comando = Get-Command $Nome -ErrorAction SilentlyContinue
    if (-not $comando) { throw "Java nao encontrado ($Nome). Instale o JDK 17 ou configure JAVA_HOME/PATH." }
    return $comando.Source
}

function Get-PostgresBin([string]$Preferido) {
    if ($Preferido) { $pasta = $Preferido }
    elseif ($env:OBOTBARATO_PG_BIN) { $pasta = $env:OBOTBARATO_PG_BIN }
    elseif (Get-Command pg_ctl -ErrorAction SilentlyContinue) { $pasta = Split-Path (Get-Command pg_ctl).Source }
    else {
        $instalacoes = @(Get-ChildItem -LiteralPath (Join-Path $env:ProgramFiles 'PostgreSQL') -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^\d+(\.\d+)*$' } | Sort-Object { [version]($_.Name + '.0') } -Descending)
        if ($instalacoes.Count -eq 0) { throw 'PostgreSQL nao encontrado. Informe -PgBin com a pasta bin da instalacao.' }
        $pasta = Join-Path $instalacoes[0].FullName 'bin'
    }
    foreach ($exe in @('initdb', 'pg_ctl', 'createdb', 'psql', 'pg_dump', 'pg_restore')) {
        if (-not (Test-Path -LiteralPath (Join-Path $pasta ($exe + '.exe')))) { throw "Executavel PostgreSQL ausente: $exe" }
    }
    return [IO.Path]::GetFullPath($pasta)
}

function Confirmar-SemOverrideBanco {
    foreach ($nome in @('DB_URL', 'DB_USUARIO', 'DB_SENHA')) {
        if ($null -ne [Environment]::GetEnvironmentVariable($nome)) {
            throw "Variavel $nome definida. Remova os overrides antes de usar a base local gerenciada."
        }
    }
}

function Confirmar-Compilacao {
    $jar = Join-Path $raizProjeto 'dist/OBotBarato.jar'
    $recompilar = -not (Test-Path -LiteralPath $jar)
    if (-not $recompilar) {
        $dataJar = (Get-Item -LiteralPath $jar).LastWriteTimeUtc
        $recompilar = @(Get-ChildItem -LiteralPath (Join-Path $raizProjeto 'src') -Recurse -Filter '*.java' | Where-Object { $_.LastWriteTimeUtc -gt $dataJar }).Count -gt 0
    }
    if ($recompilar) { & (Join-Path $PSScriptRoot 'compilar.ps1') }
}

function Get-AmbienteLocal {
    $pasta = Join-Path $raizProjeto '.local/desenvolvimento'
    $metadados = Join-Path $pasta 'ambiente.json'
    if (-not (Test-Path -LiteralPath $metadados)) { throw 'Ambiente local ausente. Execute scripts/preparar-ambiente.ps1.' }
    $estado = Get-Content -LiteralPath $metadados -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($estado.Projeto -ne 'OBotBarato-local-v1' -or $estado.Banco -ne 'obotbarato' -or $estado.Usuario -ne 'obotbarato_dev' -or [int]$estado.Porta -lt 1024 -or [int]$estado.Porta -gt 65535) {
        throw 'Metadados invalidos do ambiente local; nenhum servidor foi alterado.'
    }
    return [PSCustomObject]@{
        Pasta=$pasta; Dados=(Join-Path $pasta 'postgres'); Config=(Join-Path $pasta 'banco.properties');
        PgBin=(Get-PostgresBin $estado.PgBin); Porta=[int]$estado.Porta; Banco=$estado.Banco; Usuario=$estado.Usuario;
        Senha=(Get-Content -LiteralPath (Join-Path $pasta 'senha.txt') -Raw -Encoding UTF8).Trim()
    }
}

function Invoke-ControlePostgres($Ambiente, [ValidateSet('start','stop')] [string]$Acao) {
    # O alvo sempre deriva da pasta gerenciada do projeto, nunca do serviço instalado.
    $esperado = [IO.Path]::GetFullPath((Join-Path $raizProjeto '.local/desenvolvimento/postgres'))
    if ([IO.Path]::GetFullPath($Ambiente.Dados) -ne $esperado) { throw 'Diretorio do cluster fora do ambiente gerenciado.' }
    $exe = Join-Path $Ambiente.PgBin 'pg_ctl.exe'
    & $exe -D $Ambiente.Dados status *> $null
    $rodando = $LASTEXITCODE -eq 0
    if (($Acao -eq 'start' -and $rodando) -or ($Acao -eq 'stop' -and -not $rodando)) { return }
    $argsPg = @('-D', ('"' + $Ambiente.Dados + '"'), '-w', '-t', '60')
    if ($Acao -eq 'start') {
        $argsPg += @('-l', ('"' + (Join-Path $Ambiente.Pasta 'servidor.log') + '"'), '-o', ('"-h 127.0.0.1 -p ' + $Ambiente.Porta + '"'), 'start')
    } else { $argsPg += @('-m', 'fast', 'stop') }
    $nomeLog = $Acao + '-' + [Guid]::NewGuid().ToString('N')
    $processo = Start-Process -FilePath $exe -ArgumentList $argsPg -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $Ambiente.Pasta ($nomeLog + '.log')) -RedirectStandardError (Join-Path $Ambiente.Pasta ($nomeLog + '-erro.log'))
    $handleProcesso = $processo.Handle
    if (-not $processo.WaitForExit(60000)) { throw "Tempo limite no PostgreSQL. Consulte $($Ambiente.Pasta)/$nomeLog-erro.log" }
    # O estado real do cluster também evita ExitCode nulo no Windows PowerShell 5.1.
    & $exe -D $Ambiente.Dados status *> $null
    $estadoFinal = $LASTEXITCODE
    if (($Acao -eq 'start' -and $estadoFinal -ne 0) -or ($Acao -eq 'stop' -and $estadoFinal -ne 3)) {
        throw "Falha no PostgreSQL. Consulte $($Ambiente.Pasta)/$nomeLog-erro.log"
    }
}

function Invoke-JavaProjeto([string]$Classe, [string]$Config, [string[]]$Argumentos = @()) {
    Push-Location -LiteralPath $raizProjeto
    try {
        $opcoesJava = @('-Dfile.encoding=UTF-8')
        if ($Config) { $opcoesJava += "-Dobotbarato.config=$Config" }
        & (Get-JavaExecutavel) @opcoesJava -cp 'dist/OBotBarato.jar;dist/lib/postgresql-42.7.13.jar' $Classe @Argumentos
        if ($LASTEXITCODE -ne 0) { throw "Operacao Java nao concluida: $Classe" }
    } finally { Pop-Location }
}
