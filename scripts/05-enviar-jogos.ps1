# Envia os jogos da pasta "jogos\snes" para a pasta pública /Jogos do projetor.
# Projetor conectado (adb connect IP:5555). No Terminal do Android Studio, na pasta do projeto:
#      .\scripts\05-enviar-jogos.ps1
param([string]$Pasta = ".\jogos\snes")

adb shell mkdir -p /storage/emulated/0/Jogos
Get-ChildItem $Pasta -File | Where-Object { $_.Extension -in ".sfc", ".smc", ".swc", ".fig", ".zip" } | ForEach-Object {
  Write-Host "Enviando $($_.Name) ..."
  adb push $_.FullName /storage/emulated/0/Jogos/
}
# Libera o acesso do app Jogos HY300 a essa pasta (só precisa uma vez)
adb shell appops set br.hy300.games MANAGE_EXTERNAL_STORAGE allow
Write-Host "Pronto."
