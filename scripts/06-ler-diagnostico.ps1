# Copia o relatório de atraso do projetor para o PC e mostra na tela.
# No jogo: menu (Select+Start) > Diagnóstico de atraso. O app salva o relatório em /Jogos.
# Projetor conectado (adb connect IP:5555). Na pasta do projeto:
#      .\scripts\06-ler-diagnostico.ps1
param([string]$Destino = ".\diagnostico-atraso.txt")

adb pull /storage/emulated/0/Jogos/diagnostico-atraso.txt $Destino
if (Test-Path $Destino) {
  Get-Content $Destino -Encoding UTF8
} else {
  Write-Host "Relatório não encontrado. Abra o jogo, jogue 30 segundos e abra Diagnóstico de atraso no menu."
}
