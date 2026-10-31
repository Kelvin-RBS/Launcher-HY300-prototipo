# Projeto Android — Launcher HY300

**Launcher 0.4.4 e Jogos 0.4.0.** Dois apps: `app` (launcher) e `games` (Jogos HY300). Foi escrita sem compilador à disposição, então **pode haver erros de compilação**. Se aparecerem, copie a mensagem completa da aba *Build* do Android Studio e envie.

## O que já faz
- Tela inicial com o fundo, o topo (Rede, Bluetooth, engrenagem e hora), o título e a descrição do item em foco e a fileira de atalhos.
- **Até 7 atalhos de apps com o visual do próprio app** (o banner de TV, ou o ícone se não houver banner). Por padrão, se estiverem instalados: Netflix, YouTube, Unit TV, Pluto TV, Mercado Play e Aptoide TV.
- **Atalhos fixos:** Jogos (abre o app Jogos HY300), Arquivos, HDMI e Gerenciar atalhos.
- **Engrenagem:** menu que abre as telas originais do sistema.
- **Gerenciar atalhos:** lista de apps instalados; OK adiciona ou remove (limite de 7).

Ainda **não** tem: as telas próprias de Configurações do protótipo e a atualização pelo GitHub.

## Passo a passo no Android Studio
1. **Abrir:** *File > Open* e escolha esta pasta `android` (a que tem o `settings.gradle.kts`). Se perguntar, marque *Trust Project*.
2. **Aguardar o Gradle Sync.** Avisos comuns:
   - *"Failed to find Platform SDK with path: platforms;android-34"* ou um link *Install missing SDK*: clique para instalar (ou *Tools > SDK Manager > SDK Platforms > Android 14 (API 34)*).
   - Sugestão de **atualizar o Android Gradle Plugin / Gradle** (Upgrade Assistant): pode aceitar.
   - Erro de JDK: *Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK* e escolha o JDK embutido do Android Studio (`jbr`).
3. **Conectar ao projetor:** abra a aba *Terminal* do Android Studio e rode `adb connect 192.168.1.13:5555` (use o IP atual do projetor). Ele aparece na lista de dispositivos no topo. Se o `adb` não for reconhecido, use `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb`. Se aparecer erro de versão do servidor do adb, rode `adb kill-server` e conecte de novo.
4. **Executar:** botão verde ▶ (*Run 'app'*). O Android Studio instala e abre o launcher no projetor.
5. **Tornar padrão:** aperte o botão **Home** do controle. O Android pergunta qual tela inicial usar: escolha *Launcher HY300* e *Sempre*.

## Mudar a pasta do projeto de lugar
O projeto não guarda nenhum caminho fixo, então dá para mover a pasta inteira (inclusive a `.git`, que é oculta).
1. Feche o Android Studio e mova a pasta. Fora do Git ficam coisas que **precisam ir junto**: `jogos/` (ROMs), os núcleos `*_libretro_android.so` em `games/src/main/jniLibs/armeabi-v7a/` e, se existir, o `keystore.properties`.
2. Abra de novo: *File > Open* e escolha a pasta `android` no novo local. O `local.properties` (caminho do SDK) é recriado sozinho; o SDK, o NDK e o CMake continuam onde estavam.
3. Se o Gradle reclamar de cache, apague as pastas `.gradle` e `build` dentro de `android` e faça *File > Sync Project with Gradle Files*.
4. Em outro disco ou pendrive, o Git pode recusar com *dubious ownership*: rode `git config --global --add safe.directory "CAMINHO/DA/PASTA"` (com barras normais).

## Voltar ao launcher original
O original continua instalado. Para restaurar o padrão (comando **ainda não testado**):

```
adb shell cmd package set-home-activity com.htc.htclauncherhighenglishd08/com.htc.activity.HomeActivity
```

Ou desinstale o novo: `adb uninstall br.hy300.launcher`.

## Gerar o APK
*Build > Build Bundle(s) / APK(s) > Build APK(s)*. O arquivo sai em `app/build/outputs/apk/debug/app-debug.apk`. Instale com `adb install -r app-debug.apk`. Esse APK é de **teste**. Para versões publicadas, crie a chave e o `keystore.properties` (veja `docs/12-atualizacao-do-launcher-pelo-github.md` e `keystore.properties.example`).

## O que conferir no aparelho
- Navegação com o direcional (foco visível em cada item) e o tique ao mover o cursor (som do próprio app desde a v20; **a confirmar** no aparelho).
- Se cada app mostra o banner ou o ícone certo. Para usar sempre o ícone, troque `PREFER_BANNER` para `false` em `AppRepo.kt`.
- Se **Arquivos** abre o gerenciador de arquivos, se **HDMI** abre a entrada e se a engrenagem lista as telas.
- Se Netflix e YouTube (apps de TV) aparecem na lista de *Gerenciar atalhos*.


## App Jogos HY300 (módulo `games`)
No topo do Android Studio, escolha **games** ao lado do ▶ e execute para instalá-lo. Detalhes e fluxo: `docs/14-app-jogos.md`.
Para enviar o jogo: rode `scripts/05-enviar-jogos.ps1` (envia para `/storage/emulated/0/Jogos/` e libera o acesso do app a essa pasta).
