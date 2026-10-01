param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $javac = (Get-Command javac -ErrorAction Stop).Source
    $jar = Join-Path (Split-Path $javac) 'jar.exe'
} else {
    $javac = Join-Path $JavaHome 'bin/javac.exe'
    $jar = Join-Path $JavaHome 'bin/jar.exe'
}
$classes = Join-Path $raizProjeto 'build/classes'
$driver = Join-Path $raizProjeto 'lib/postgresql-42.7.13.jar'
New-Item -ItemType Directory -Force -Path $classes, (Join-Path $raizProjeto 'dist/lib') | Out-Null
$fontes = @(Get-ChildItem -LiteralPath (Join-Path $raizProjeto 'src') -Filter '*.java' -Recurse | ForEach-Object {
    '"' + [IO.Path]::GetRelativePath($raizProjeto, $_.FullName).Replace('\','/') + '"'
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
& $jar --create --file (Join-Path $raizProjeto 'dist/OBotBarato.jar') --manifest $manifesto --main-class view.TelaLoginUnificado -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar o JAR.' }
Copy-Item -LiteralPath $driver -Destination (Join-Path $raizProjeto 'dist/lib/') -Force
Write-Host "Compilado: $($fontes.Count) fontes Java. Pacote: dist/OBotBarato.jar"
