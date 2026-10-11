param([string]$JavaHome = $env:JAVA_HOME)
. (Join-Path $PSScriptRoot 'comum.ps1')
$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $javac = Get-JavaExecutavel 'javac'
    $jar = Get-JavaExecutavel 'jar'
} else {
    $javac = Join-Path $JavaHome 'bin/javac.exe'
    $jar = Join-Path $JavaHome 'bin/jar.exe'
}
$classes = Join-Path $raizProjeto 'build/classes'
$driver = Join-Path $raizProjeto 'lib/postgresql-42.7.13.jar'
if (-not (Test-Path -LiteralPath $driver)) { throw 'Driver PostgreSQL ausente na pasta lib.' }
# Limpa somente classes geradas, após validar o alvo e recusar atalhos/junções.
$raizAbsoluta = [IO.Path]::GetFullPath($raizProjeto)
if (-not [IO.Path]::GetFullPath($classes).StartsWith($raizAbsoluta + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Diretorio de compilacao fora do projeto.' }
foreach ($pastaVerificada in @((Join-Path $raizProjeto 'build'), $classes)) {
    if ((Test-Path -LiteralPath $pastaVerificada) -and ((Get-Item -LiteralPath $pastaVerificada).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Diretorio de compilacao nao pode ser um atalho/juncao.' }
}
if (Test-Path -LiteralPath $classes) { Remove-Item -LiteralPath $classes -Recurse -Force }
New-Item -ItemType Directory -Force -Path $classes, (Join-Path $raizProjeto 'dist/lib') | Out-Null
$fontes = @(Get-ChildItem -LiteralPath (Join-Path $raizProjeto 'src') -Filter '*.java' -Recurse | ForEach-Object {
    '"' + $_.FullName.Substring($raizAbsoluta.Length).TrimStart([char[]]'\/').Replace('\','/') + '"'
})
$argumentos = Join-Path $raizProjeto 'build/fontes.txt'
[IO.File]::WriteAllLines($argumentos, $fontes, [Text.UTF8Encoding]::new($false))
Push-Location -LiteralPath $raizProjeto
try {
    & $javac --release 17 -encoding UTF-8 -cp $driver -d $classes '@build/fontes.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilacao Java.' }
} finally { Pop-Location }
$manifesto = Join-Path $raizProjeto 'build/MANIFEST.MF'
[IO.File]::WriteAllText($manifesto, "Manifest-Version: 1.0`nClass-Path: lib/postgresql-42.7.13.jar`n`n", [Text.UTF8Encoding]::new($false))
$jarGerado = Join-Path $raizProjeto 'build/OBotBarato.jar'
& $jar --create --file $jarGerado --manifest $manifesto --main-class view.TelaLoginUnificado -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar o JAR.' }
$driverDestino = Join-Path $raizProjeto 'dist/lib/postgresql-42.7.13.jar'
if (-not (Test-Path -LiteralPath $driverDestino) -or (Get-FileHash -LiteralPath $driver).Hash -ne (Get-FileHash -LiteralPath $driverDestino).Hash) {
    Copy-Item -LiteralPath $driver -Destination $driverDestino -Force
}
$pacote = Join-Path $raizProjeto 'dist/OBotBarato.jar'
try {
    if (Test-Path -LiteralPath $pacote) { [IO.File]::Replace($jarGerado, $pacote, [NullString]::Value) }
    else { [IO.File]::Move($jarGerado, $pacote) }
} catch {
    # Windows bloqueia o JAR em uso. Preserva a sessao e publica outro arquivo completo.
    $pacote = Join-Path $raizProjeto ('dist/OBotBarato-' + [Guid]::NewGuid().ToString('N') + '.jar')
    [IO.File]::Move($jarGerado, $pacote)
    Write-Host 'Aplicativo aberto: versao nova separada. O lancador escolhe o pacote mais recente ao reabrir.'
}
Write-Host "Compilado: $($fontes.Count) fontes Java. Pacote: $pacote"
