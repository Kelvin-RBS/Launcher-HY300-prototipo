# Abre telas e tira capturas. Só abre telas, não altera configurações.
$telas = [ordered]@{ projecao="ProjectionActivity"; imagem="PictureModeActivity"; sinal="SignalActivity" }
foreach ($nome in $telas.Keys) {
  .\adb shell am start -n "com.htc.htcsettings/com.htc.activity.$($telas[$nome])" | Out-Null
  Start-Sleep 3
  cmd /c ".\adb exec-out screencap -p > $nome.png"
  .\adb shell input keyevent KEYCODE_HOME
  Start-Sleep 1
}
# HDMI (volta para a tela inicial após 8 s)
.\adb shell am start -n com.softwinner.awlivetv/com.softwinner.awlivetv.MainActivity
Start-Sleep 8
.\adb shell input keyevent KEYCODE_HOME
