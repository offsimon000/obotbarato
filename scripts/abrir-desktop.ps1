param([switch]$SomenteDiagnostico)
$ErrorActionPreference='Stop'
$raizAberturaDesktop=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$pastaAberturaDesktop=Join-Path $raizAberturaDesktop '.local'
New-Item -ItemType Directory -Force -Path $pastaAberturaDesktop | Out-Null
$logAberturaDesktop=Join-Path $pastaAberturaDesktop 'abertura-desktop.log'
try {
    # Prepara o banco local e instala a demonstracao completa apenas na primeira abertura.
    # Assim o atalho funciona tambem em uma copia nova baixada por outra pessoa.
    & (Join-Path $PSScriptRoot 'preparar-netbeans.ps1') *>> $logAberturaDesktop
    if ($SomenteDiagnostico) { Write-Output 'OK: ambiente pronto para abrir pelo atalho.'; return }
    . (Join-Path $PSScriptRoot 'comum.ps1')
    $javaDesktop=Get-JavaExecutavel 'javaw'
    $jarDesktop=Get-PacoteProjeto
    # javaw abre somente a interface Swing. O PowerShell termina apos iniciar.
    $processoDesktop=Start-Process -FilePath $javaDesktop -ArgumentList @('-Dfile.encoding=UTF-8','-jar',('"'+$jarDesktop+'"')) -WorkingDirectory $raizAberturaDesktop -PassThru -RedirectStandardOutput (Join-Path $pastaAberturaDesktop 'desktop-saida.log') -RedirectStandardError (Join-Path $pastaAberturaDesktop 'desktop-erros.log')
    [IO.File]::WriteAllText((Join-Path $pastaAberturaDesktop 'desktop-processo.json'),(@{Id=$processoDesktop.Id;IniciadoEm=$processoDesktop.StartTime.ToUniversalTime().ToString('o');Pacote=$jarDesktop} | ConvertTo-Json),[Text.UTF8Encoding]::new($false))
    Write-Output ('OBotBarato iniciado. Processo: '+$processoDesktop.Id)
} catch {
    $mensagemAberturaDesktop=$_.Exception.Message
    Add-Content -LiteralPath $logAberturaDesktop -Value $mensagemAberturaDesktop -Encoding UTF8
    if ($SomenteDiagnostico) {throw}
    Add-Type -AssemblyName System.Windows.Forms
    [Windows.Forms.MessageBox]::Show("Nao foi possivel abrir o OBotBarato.`n`n$mensagemAberturaDesktop`n`nDetalhes em .local/abertura-desktop.log.",'OBotBarato',[Windows.Forms.MessageBoxButtons]::OK,[Windows.Forms.MessageBoxIcon]::Error) | Out-Null
    exit 1
}
