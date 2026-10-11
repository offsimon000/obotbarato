param([string]$Nome='OBotBarato-apresentacao')
. (Join-Path $PSScriptRoot 'comum.ps1')
if ($Nome -notmatch '^[A-Za-z0-9_-]+$') { throw 'Nome do pacote invalido.' }
Confirmar-Compilacao
foreach ($arquivoEntrega in @('docs/final/Documentacao_OBotBarato.docx','docs/final/Documentacao_OBotBarato.pdf','docs/final/Apresentacao_OBotBarato.pptx')) {
 if (-not (Test-Path -LiteralPath (Join-Path $raizProjeto $arquivoEntrega))) {throw ('Documento da entrega ausente: '+$arquivoEntrega)}
}
$saidaEntrega=Join-Path $raizProjeto 'entrega'
New-Item -ItemType Directory -Force -Path $saidaEntrega | Out-Null
$zipEntrega=Join-Path $saidaEntrega ($Nome+'-'+(Get-Date -Format 'yyyyMMdd_HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,8)+'.zip')
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$manifestoEntrega=New-Object System.Collections.Generic.List[string]
$fluxoEntrega=[IO.File]::Open($zipEntrega,[IO.FileMode]::CreateNew)
$arquivoZipEntrega=New-Object IO.Compression.ZipArchive($fluxoEntrega,[IO.Compression.ZipArchiveMode]::Create,$false)
function Add-ArquivoEntrega([string]$Origem,[string]$Destino) {
 $origemAbsolutaEntrega=[IO.Path]::GetFullPath($Origem)
 if (-not $origemAbsolutaEntrega.StartsWith($raizProjeto+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {throw 'Origem fora do projeto.'}
 if ((Get-Item -LiteralPath $Origem).Attributes -band [IO.FileAttributes]::ReparsePoint) {throw 'Atalho nao aceito no pacote.'}
 if ($Destino -match '(^|/)(\.local|\.git|nbproject|node_modules|build)(/|$)' -or $Destino -match '\.cmd$|(^|/)banco\.properties$|~\$') {throw ('Arquivo local nao permitido: '+$Destino)}
 $hashArquivoEntrega=(Get-FileHash -LiteralPath $Origem -Algorithm SHA256).Hash.ToLowerInvariant()
 $manifestoEntrega.Add($hashArquivoEntrega+'  '+$Destino)
 [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($arquivoZipEntrega,$Origem,('OBotBarato/'+$Destino),[IO.Compression.CompressionLevel]::Optimal) | Out-Null
}
try {
 foreach ($diretorioEntrega in @('src','sql','lib','catalogo','scripts','test','docs','.vscode')) {
  $pastaEntrega=Join-Path $raizProjeto $diretorioEntrega
  foreach ($arquivoEntrega in Get-ChildItem -LiteralPath $pastaEntrega -Recurse -File) {
   if ($arquivoEntrega.Name -like '~$*' -or $arquivoEntrega.Extension -eq '.pyc' -or $arquivoEntrega.FullName -match '[\\/]__pycache__[\\/]') {continue}
   Add-ArquivoEntrega $arquivoEntrega.FullName $arquivoEntrega.FullName.Substring($raizProjeto.Length+1).Replace('\','/')
  }
 }
 foreach ($arquivoEntrega in @('README.md','.gitignore','.gitattributes','OBotBarato.code-workspace','config/banco.exemplo.properties')) {Add-ArquivoEntrega (Join-Path $raizProjeto $arquivoEntrega) $arquivoEntrega}
 Add-ArquivoEntrega (Get-PacoteProjeto) 'dist/OBotBarato.jar'
 Add-ArquivoEntrega (Join-Path $raizProjeto 'lib/postgresql-42.7.13.jar') 'dist/lib/postgresql-42.7.13.jar'
 $entradaManifestoEntrega=$arquivoZipEntrega.CreateEntry('OBotBarato/SHA256SUMS.txt')
 $gravadorManifestoEntrega=New-Object IO.StreamWriter($entradaManifestoEntrega.Open(),[Text.UTF8Encoding]::new($false))
 try {$gravadorManifestoEntrega.Write([string]::Join("`n",$manifestoEntrega)+"`n")} finally {$gravadorManifestoEntrega.Dispose()}
} finally {$arquivoZipEntrega.Dispose();$fluxoEntrega.Dispose()}
$hashZipEntrega=(Get-FileHash -LiteralPath $zipEntrega -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText(($zipEntrega+'.sha256'),($hashZipEntrega+'  '+[IO.Path]::GetFileName($zipEntrega)+"`n"),[Text.UTF8Encoding]::new($false))
Write-Host ('Pacote gerado: '+$zipEntrega)
Write-Host ('Arquivos incluidos: '+$manifestoEntrega.Count+'. Sem configuracao real, dados locais ou lancadores CMD.')
