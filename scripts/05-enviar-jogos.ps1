# Envia a pasta "jogos" para o projetor e libera o acesso a arquivos do launcher.
# Rode na pasta platform-tools, com o projetor conectado (.\adb connect IP:5555).
# Exemplo: .\05-enviar-jogos.ps1 -Pasta "C:\caminho\Launcher-HY300-prototipo\jogos"
param([string]$Pasta = ".\jogos")

foreach ($sistema in "snes", "megadrive") {
  .\adb shell mkdir -p "/storage/emulated/0/Jogos/$sistema"
  $dir = Join-Path $Pasta $sistema
  if (Test-Path $dir) {
    Get-ChildItem $dir -File | Where-Object { $_.Name -ne ".gitkeep" } | ForEach-Object {
      Write-Host "Enviando $($_.Name) ..."
      .\adb push $_.FullName "/storage/emulated/0/Jogos/$sistema/"
    }
  }
}
# Permite que o launcher liste a pasta /Jogos
.\adb shell appops set br.hy300.launcher MANAGE_EXTERNAL_STORAGE allow
Write-Host "Pronto."
