# Análise do Snes9x EX+ e da ROM

## Snes9x EX+ 1.5.85 (XAPK enviado)
- Pacote `com.explusalpha.Snes9xPlus`, `minSdk` 21, `targetSdk` 36, versão 16010585.
- **Quase tudo é código nativo**: o Java tem só cerca de 60 KB (`classes.dex`), e o motor está em `libmain.so` (3,7 MB). A activity principal é `com.imagine.BaseActivity` (motor "Imagine").
- **Este XAPK só traz a versão de 64 bits** (`config.arm64_v8a`). **O projetor é de 32 bits** (`zygote32`, `armeabi-v7a`, cortex-a7), então esse pacote não serve nele. O emulador instalado no aparelho é outra compilação, que ainda será examinada.
- Aceita abrir jogos por `ACTION_VIEW` com `file://` ou `content://` e as extensões `.smc`, `.sfc`, `.swc`, `.fig`, `.bs`, `.st`, `.mgd`, `.rar`, `.7z` e zip. É por isso que o app Jogos consegue abri-lo com a ROM.
- Declara suporte a gamepad e a TV (Leanback) e `requestLegacyExternalStorage`.

## Licenças (fontes públicas, a conferir antes de distribuir)
- O código do emulador está no GitHub (Rakashazi/emu-ex-plus-alpha). O motor Imagine e o EmuFramework são **GPLv3**.
- O núcleo **Snes9x tem licença não comercial**. Uso pessoal é compatível, mas redistribuição comercial não.
- Qualquer app derivado precisa respeitar a GPLv3 (oferecer o código-fonte).

## Caminhos para a fase 2 (motor próprio)
| Caminho | Prós | Contras |
|---|---|---|
| **A. Bifurcar o EmuEx (Snes9x EX+)** | Fidelidade total, já otimizado e testado | Build complexo (sistema próprio com NDK, pensado para Linux), pouco prático no Windows; interface própria é difícil de trocar |
| **B. Frontend próprio com o núcleo libretro do Snes9x** | Controle total do vídeo, áudio e entrada; sem tela de toque; compila no Android Studio com NDK e CMake | Precisa escrever o frontend (C++ e Kotlin) e usar o núcleo `snes9x_libretro_android.so` de 32 bits (licença não comercial) |
| **C. Ficar com o motor externo** | Já funciona e rodou melhor | Ajustes finos e visual do emulador ficam fora do nosso controle |

**Recomendação:** começar pela fase 1 (já pronta) e, depois dos primeiros testes, seguir pelo caminho **B**. Desenho previsto: vídeo em `SurfaceView` com buffer do tamanho do quadro e escala feita pelo sistema, áudio por `AudioTrack` em modo de baixa latência, laço de emulação sincronizado com o áudio, entrada por `InputDevice` para qualquer controle.

## A ROM (Super Mario World PT-BR)
- 1 MiB (1.048.576 bytes), **sem** cabeçalho de copiadora, mapeamento **LoROM**, checksum válido.
- Título no cabeçalho: `SNESFOREVER.COM.BR` (foi trocado pelo grupo da tradução), região 0x01 (América do Norte), 2 KiB de SRAM com bateria.
- CRC32 `7025FC7B`, SHA-1 `9873b476ca6b1bec506fc7554d09fc94bf7c9b19`.
- É uma ROM comum, sem chip especial: qualquer versão do Snes9x a executa.
