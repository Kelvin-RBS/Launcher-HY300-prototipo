# Instala o RetroArch de 32 bits no projetor.
# 1) Baixe o RetroArch_ra32.apk da versão estável mais recente em https://buildbot.libretro.com/stable/
#    (pasta da versão > android > RetroArch_ra32.apk) ou em https://www.retroarch.com
# 2) Ajuste o caminho abaixo e rode na pasta platform-tools, com o projetor conectado.
$apk = "C:\Users\SEU_USUARIO\Downloads\RetroArch_ra32.apk"
.\adb install -r $apk
.\adb shell pm list packages | findstr -i retroarch
cmd /c ".\adb shell dumpsys package com.retroarch.ra32 > retroarch_dump.txt"
