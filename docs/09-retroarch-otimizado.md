# RetroArch otimizado para o projetor

Objetivo: usar o [RetroArch](https://github.com/libretro/RetroArch) como base dos jogos de 16 bits, o mais leve possível e **sem as partes de toque**, que o aparelho não usa.

## O que está feito e o que não está
- **Não foi instalado nem compilado ainda.** O ambiente em que este projeto foi analisado não tem acesso ao projetor, à rede nem ao Android SDK. A instalação precisa ser feita no seu computador (script em `scripts/03-instalar-retroarch.ps1`).
- O que existe aqui é o **plano** e uma **configuração de exemplo** (`retroarch/retroarch-projetor.cfg`).

## Passo 1: instalar a versão certa
O aparelho é de 32 bits. A documentação do RetroArch informa que os APKs com sufixo `ra32` são os de 32 bits (`RetroArch_ra32.apk`), disponíveis no buildbot e no site oficial. O pacote dessa versão é `com.retroarch.ra32`.

## Passo 2: desativar o toque sem recompilar (recomendado primeiro)
Muitas das partes de toque se desligam por configuração, sem mexer no código:
- `input_overlay_enable = "false"`: tira os controles virtuais da tela.
- `menu_pointer_enable = "false"`: tira a navegação por ponteiro nos menus.

O arquivo `retroarch/retroarch-projetor.cfg` traz essas chaves. O launcher pode passar o arquivo ao abrir o RetroArch (a validar, ver `08-jogos-retroarch.md`).

## Passo 3: reduzir o que o RetroArch carrega
- Instalar **só os núcleos de 16 bits** que você vai usar (por exemplo, para Super Nintendo e Mega Drive), em vez do pacote completo.
- Usar o menu mais simples (`rgui`) pode economizar memória. Precisa ser testado.

## Passo 4 (só se valer a pena): compilar uma versão própria
O fluxo oficial de build para Android usa Gradle em `pkg/android/phoenix` (`./gradlew assembleDebug`). Dá para tirar código de toque e overlay, mas:
- **O ganho tende a ser pequeno.** Quase todo o consumo vem do núcleo do emulador e do vídeo, e não dos controles de toque. Meça antes de compilar.
- O `configure` do RetroArch tem opções como `--disable-overlay`, `--disable-networking` e `--disable-cdrom`, vistas na configuração de CI do projeto. Elas são de builds não Android, e os nomes das opções do build Android (Gradle/NDK) **precisam ser confirmados no código-fonte**.
- **Não use `--disable-menu`**: ele removeria o menu, e você ainda precisa dele.
- Para o aparelho de 32 bits, o build precisa gerar a variante `armeabi-v7a`.
- O RetroArch é GPLv3. Se você distribuir uma versão modificada, precisa publicar o código dela.

## Como medir
Antes e depois de qualquer mudança: tempo de abertura, uso de memória (`adb shell dumpsys meminfo com.retroarch.ra32`) e fluidez com um jogo de Super Nintendo.
