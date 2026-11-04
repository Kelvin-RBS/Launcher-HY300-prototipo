<#
.SYNOPSIS
    Gera o arquivo games.json para o catálogo de jogos do GitHub.

.DESCRIPTION
    Este script percorre as pastas jogos/snes e jogos/nes, calcula o SHA-256 de cada arquivo
    e gera o games.json que será publicado no GitHub Pages.

    Uso:
        .\scripts\07-gerar-games-json.ps1
        .\scripts\07-gerar-games-json.ps1 -Versao "v1.0" -PastaJogos ".\jogos"
#>

param(
    [string]$Versao = "v1.0",
    [string]$PastaJogos = ".\jogos",
    [string]$Saida = ".\docs\games.json",
    [string]$GitHubPagesBase = "https://kelvin-rbs.github.io/Launcher-HY300-prototipo",
    [string]$DownloadBase = "https://github.com/Kelvin-RBS/Launcher-HY300-prototipo/releases/download"
)

$extensoesPermitidas = @(".sfc", ".smc", ".swc", ".fig", ".nes", ".zip")

function CalcularSha256($arquivo) {
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    $stream = [System.IO.File]::OpenRead($arquivo)
    $hash = $sha256.ComputeHash($stream)
    $stream.Close()
    return ($hash | ForEach-Object { "{0:x2}" -f $_ }) -join ""
}

function ObterSistema($extensao) {
    switch ($extensao.ToLower()) {
        ".sfc" { return "snes" }
        ".smc" { return "snes" }
        ".swc" { return "snes" }
        ".fig" { return "snes" }
        ".nes" { return "nes" }
        ".zip" { return "zip" } # Será determinado pelo conteúdo se necessário
        default { return "desconhecido" }
    }
}

function LimparTitulo($nome) {
    # Remove extensão
    $base = [System.IO.Path]::GetFileNameWithoutExtension($nome)
    # Substitui _ por espaço
    $base = $base -replace '_', ' '
    # Remove tags entre parênteses e colchetes
    $base = $base -replace '\([^)]*\)', ''
    $base = $base -replace '\[[^\]]*\]', ''
    # Remove tags de idioma
    $base = $base -replace '(?i)\bPT-?BR\b', ''
    # Limpa espaços extras
    $base = $base -replace '\s+', ' '
    return $base.Trim()
}

Write-Host "Gerando games.json..."
Write-Host "Versão: $Versao"
Write-Host "Pasta de jogos: $PastaJogos"

$jogos = @()
$idCounter = 0

foreach ($sistema in "snes", "nes") {
    $dir = Join-Path $PastaJogos $sistema
    if (-not (Test-Path $dir)) {
        Write-Host "Pasta não encontrada: $dir"
        continue
    }

    $arquivos = Get-ChildItem $dir -File | Where-Object { $extensoesPermitidas -contains $_.Extension.ToLower() }

    foreach ($arquivo in $arquivos) {
        $idCounter++
        $id = "$sistema-g$idCounter"
        $titulo = LimparTitulo $arquivo.Name
        $sha256 = CalcularSha256 $arquivo.FullName
        $tamanho = $arquivo.Length

        $jogo = @{
            id = $id
            title = $titulo
            system = $sistema
            fileName = $arquivo.Name
            size = $tamanho
            sha256 = $sha256
            version = $Versao
            description = ""
        }

        $jogos += $jogo
        Write-Host "  $titulo ($([math]::Round($tamanho/1MB, 1)) MB) - SHA256: $sha256"
    }
}

$catalog = @{
    version = 1
    updatedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
    games = $jogos
}

$json = $catalog | ConvertTo-Json -Depth 5 -Compress
$json | Out-File -FilePath $Saida -Encoding utf8

Write-Host ""
Write-Host "games.json gerado em: $Saida"
Write-Host "Total de jogos: $($jogos.Count)"
Write-Host ""
Write-Host "Próximos passos:"
Write-Host "  1. Crie uma Release no GitHub com a tag $Versao e anexe os arquivos de jogo"
Write-Host "  2. Faça commit e push do games.json"
Write-Host "  3. O GitHub Pages publicará em: $GitHubPagesBase/games.json"