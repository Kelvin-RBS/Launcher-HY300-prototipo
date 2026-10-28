# Motor próprio: Snes9x HY300

O app **Jogos HY300** agora tem um emulador embutido, sem controles de toque e com tudo em português. Ele roda o **núcleo libretro do Snes9x** (fornecido por você) e é feito para este projetor.

## O que é nosso e o que não é
- **Nosso (escrito neste projeto):** o "frontend" em C++ e Kotlin: vídeo, áudio, entrada de controles, menu de pausa, estados salvos, save do jogo e os ajustes.
- **Da comunidade:** o **núcleo de emulação** (`snes9x2010`), baixado pronto do site do libretro. Não foi escrito nem compilado aqui. A licença do Snes9x é **não comercial**: uso pessoal é ok, e por isso o núcleo **nunca vai para o GitHub**.

## Como funciona
- **Vídeo:** o núcleo entrega cada quadro em RGB565; o app copia para uma `SurfaceView` com o tamanho do quadro e o **sistema faz a escala** (sem OpenGL), o que é leve para o cortex-a7.
- **Áudio:** `AudioTrack` em modo de baixa latência. A escrita bloqueia quando o buffer enche, e isso dá o ritmo (60 quadros por segundo) sem relógio extra.
- **Entrada:** qualquer gamepad (Xbox, Nintendo Switch, PlayStation, genérico), por teclas, direcional digital e analógico esquerdo. Os gatilhos L2 e R2 também valem como L e R.
- **ROMs em `.zip`:** o app abre o zip e extrai a ROM de dentro. Zips sem ROM de Super Nintendo (como o de NES) não aparecem na lista.
- **Save do jogo (SRAM)** é gravado ao pausar e ao sair, e **um estado salvo** por jogo.

## Fechar o jogo com o controle conectado
- **Segure Start por 3 segundos:** aparece a pergunta **"Fechar o jogo?"** com **"Não, continuar"** (já selecionado) e **"Sim, fechar o jogo"**. Se escolher Sim, o jogo fecha e volta à lista do app de jogos. O save do jogo é mantido.
- **Segure Select + Start por cerca de 1 segundo** (ou aperte Voltar ou o botão Guide): abre o menu completo com **Continuar · Reiniciar · Salvar estado · Carregar estado · Ajustes · Sair para a lista de jogos**.

No menu e na pergunta, o direcional navega, A confirma e B volta. Observação: enquanto você segura Start, o próprio jogo recebe o botão Start e pode pausar. Se escolher "Não, continuar", aperte Start de novo.

## Ajustes (Configurações > Ajustes do emulador)
Proporção da tela (4:3, 8:7 ou tela cheia), botões do controle (por posição, estilo Xbox, ou por rótulo, estilo Nintendo), latência do áudio e mostrar quadros por segundo. Não há opções de toque.

## Como instalar (resumo; o passo a passo completo está no chat)
1. Android Studio > *Tools > SDK Manager > SDK Tools*: instalar **NDK (Side by side)** e **CMake**.
2. Baixar o núcleo de 32 bits em `https://buildbot.libretro.com/nightly/android/latest/armeabi-v7a/` (arquivo `snes9x2010_libretro_android.so.zip`), extrair e renomear para `libsnes9x2010_libretro_android.so`.
3. Colocar em `android/games/src/main/jniLibs/armeabi-v7a/`.
4. Sincronizar e rodar o módulo **games**.

Sem o núcleo (ou se a parte nativa não compilar), o app continua usando o **Snes9x EX+** instalado, como antes. Para compilar sem a parte nativa, ponha `hy300.native=false` em `android/gradle.properties`.

## O que foi testado
- A lógica do C++ foi testada **no computador** com um núcleo libretro falso: carregar o núcleo, entrada, áudio, estado e save passaram.
- O código Kotlin **não foi compilado** e nada rodou no projetor ainda.

## A confirmar no aparelho
- Se o núcleo `snes9x2010` roda a 60 quadros por segundo no cortex-a7 (use "Mostrar quadros por segundo").
- Se o som fica estável com a latência "Média" ou se precisa de "Alta".
- Se o controle Bluetooth escolhido chega ao app com os botões certos.
- Se o botão Guide/Home chega ao app ou vai direto para a tela inicial do sistema (nesse caso, use Select + Start).
