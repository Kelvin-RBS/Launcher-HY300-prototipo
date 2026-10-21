# Execute na pasta platform-tools, com o projetor conectado (.\adb connect IP:5555)
$pkgs = "com.htc.htclauncherhighenglishd08","com.htc.htcsettings","com.softwinner.awlivetv","com.softwinner.vis","com.softwinner.awsource","com.softwinner.awmanager","com.softwinner.settingsassist"
foreach ($p in $pkgs) {
  $path = ((.\adb shell pm path $p) -replace "package:","").Trim()
  .\adb pull $path "$p.apk"
  cmd /c ".\adb shell dumpsys package $p > $($p)_dump.txt"
}
cmd /c ".\adb shell getprop > props.txt"
cmd /c ".\adb shell pm list packages -s > apps_sistema.txt"
cmd /c ".\adb shell pm list packages -3 > apps_usuario.txt"
cmd /c ".\adb shell settings list global > settings_global.txt"
cmd /c ".\adb shell settings list system > settings_system.txt"
cmd /c ".\adb shell dumpsys tv_input > tv_input.txt"
