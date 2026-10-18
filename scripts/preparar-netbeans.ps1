. (Join-Path $PSScriptRoot 'comum.ps1')
Confirmar-SemOverrideBanco
& (Join-Path $PSScriptRoot 'preparar-ambiente.ps1')

$ambiente = Get-AmbienteLocal
$marcador = Join-Path $ambiente.Pasta 'demonstracao-carregada-v1.txt'
if (-not (Test-Path -LiteralPath $marcador)) {
    $senhaAnterior = $env:PGPASSWORD
    try {
        $env:PGPASSWORD = $ambiente.Senha
        $verificacao = & (Join-Path $ambiente.PgBin 'psql.exe') -X -w -h 127.0.0.1 -p $ambiente.Porta -U $ambiente.Usuario -d $ambiente.Banco -tA -c "SELECT CASE WHEN (SELECT COUNT(*) FROM produtos WHERE ativo)>=30 AND (SELECT COUNT(*) FROM clientes WHERE email LIKE 'cliente%@uso.obotbarato.test')>=20 AND (SELECT COUNT(*) FROM pedidos p JOIN clientes c ON c.id=p.id_cliente WHERE c.email LIKE 'cliente%@uso.obotbarato.test')>=50 THEN 'completa' ELSE 'incompleta' END"
        if ($LASTEXITCODE -ne 0) { throw 'Não foi possível conferir a demonstração no banco local.' }
    } finally { $env:PGPASSWORD = $senhaAnterior }
    if ($verificacao -eq 'completa') {
        Write-Host 'A demonstração já existe; preservando produtos, vendas, senhas e estoques.'
    } else {
        Write-Host 'Carregando o catálogo e os dados fictícios de apresentação (somente na primeira execução)...'
        Invoke-JavaProjeto 'util.InstalarCatalogo' $ambiente.Config @((Join-Path $raizProjeto 'catalogo'))
        Invoke-JavaProjeto 'util.SimularUsoDiario' $ambiente.Config @()
    }
    [IO.File]::WriteAllText($marcador, 'Catalogo30;clientes20;pedidos60', [Text.UTF8Encoding]::new($false))
}
Write-Host 'Banco de demonstração do NetBeans pronto.'
