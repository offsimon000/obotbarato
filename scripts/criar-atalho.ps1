param([switch]$SomentePastaProjeto)
. (Join-Path $PSScriptRoot 'comum.ps1')
$scriptAtalhoDesktop=Join-Path $raizProjeto 'scripts/abrir-desktop.ps1'
$powershellAtalhoDesktop=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
$iconeAtalhoDesktop=Get-JavaExecutavel 'javaw'
$descricaoAtalhoDesktop='Abrir OBotBarato sem VS Code; usa o banco e os dados do projeto.'
$pastasAtalhoDesktop=@($raizProjeto)
if (-not $SomentePastaProjeto) {
    $areaTrabalhoAtalhoDesktop=[Environment]::GetFolderPath('Desktop')
    if ($areaTrabalhoAtalhoDesktop) { $pastasAtalhoDesktop+=$areaTrabalhoAtalhoDesktop }
}
$shellAtalhoDesktop=New-Object -ComObject WScript.Shell
try {
    foreach ($pastaAtalhoDesktop in $pastasAtalhoDesktop) {
        $arquivoAtalhoDesktop=Join-Path $pastaAtalhoDesktop 'Executar OBotBarato.lnk'
        if (Test-Path -LiteralPath $arquivoAtalhoDesktop) {
            $anteriorAtalhoDesktop=$shellAtalhoDesktop.CreateShortcut($arquivoAtalhoDesktop)
            # Reconhece somente atalhos deste gerador, inclusive apos mover a pasta.
            if ($anteriorAtalhoDesktop.Description -ne $descricaoAtalhoDesktop -or $anteriorAtalhoDesktop.TargetPath -ne $powershellAtalhoDesktop -or $anteriorAtalhoDesktop.Arguments -notmatch 'scripts[\\/]abrir-desktop\.ps1"$') {throw ('Ja existe outro atalho com esse nome: '+$arquivoAtalhoDesktop)}
        }
        $atalhoDesktop=$shellAtalhoDesktop.CreateShortcut($arquivoAtalhoDesktop)
        $atalhoDesktop.TargetPath=$powershellAtalhoDesktop
        $atalhoDesktop.Arguments='-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "'+$scriptAtalhoDesktop+'"'
        $atalhoDesktop.WorkingDirectory=$raizProjeto
        $atalhoDesktop.IconLocation=$iconeAtalhoDesktop+',0'
        $atalhoDesktop.Description=$descricaoAtalhoDesktop
        $atalhoDesktop.Save()
        Write-Output ('Atalho criado: '+$arquivoAtalhoDesktop)
    }
} finally {[Runtime.InteropServices.Marshal]::FinalReleaseComObject($shellAtalhoDesktop) | Out-Null}
