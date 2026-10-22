# Gera o latest.json a partir do APK assinado (não testado neste ambiente: confira o resultado).
# Exemplo: .\scripts\04-gerar-latest-json.ps1 -Apk .\launcher.apk -VersionCode 2 -VersionName "0.2.0" -Notas "O que mudou"
param(
  [Parameter(Mandatory=$true)][string]$Apk,
  [Parameter(Mandatory=$true)][int]$VersionCode,
  [Parameter(Mandatory=$true)][string]$VersionName,
  [string]$Notas = "",
  [string]$Repo = "Kelvin-RBS/Launcher-HY300-prototipo"
)
$hash = (Get-FileHash -Algorithm SHA256 -Path $Apk).Hash.ToLower()
$size = (Get-Item $Apk).Length
$obj = [ordered]@{
  versionCode = $VersionCode
  versionName = $VersionName
  apkUrl      = "https://github.com/$Repo/releases/download/v$VersionName/launcher.apk"
  sha256      = $hash
  sizeBytes   = $size
  minAndroid  = 30
  publishedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  notes       = $Notas
}
$json = $obj | ConvertTo-Json
# UTF-8 sem BOM, para qualquer leitor de JSON abrir sem erro
[System.IO.File]::WriteAllText("$PWD\latest.json", $json, (New-Object System.Text.UTF8Encoding $false))
Write-Host "latest.json gerado. Anexe o APK à Release v$VersionName com o nome launcher.apk."
