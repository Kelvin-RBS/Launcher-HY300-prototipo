# App Jogos HY300

App separado (`br.hy300.games`, módulo `android/games`) com o visual do launcher, feito só para jogos. O atalho fixo **Jogos** do launcher abre este app.

## Fluxo
1. **Lista:** mostra só o nome de cada jogo (Super Mario World é o primeiro). A engrenagem no topo abre as configurações.
2. **Ao escolher um jogo**, o app confere se há um **controle conectado** (Bluetooth ou USB: Xbox, Nintendo Switch, PlayStation e genéricos).
3. **Sem controle:** aparece a tela "Conecte um controle". Ela **aguarda a conexão** e abre o jogo sozinha quando um controle conectar. **OK** volta à lista. Há também o botão **Parear controle**, que abre o Bluetooth do sistema.
4. **Com controle:** o jogo abre.

## Fases
**Fase 1 (esta versão): motor externo.** Quem emula é o **Snes9x EX+** já instalado no projetor, o emulador que você testou e que rodou melhor. O app entrega a ROM por um `ContentProvider` próprio (acesso temporário ao emulador) e abre o emulador com a intent `VIEW`. Os ajustes de vídeo, áudio e botões ficam no emulador.

**Fase 2: motor próprio e nativo.** Um emulador embutido no app, sem tela de toque, com ajustes pensados para este hardware (cortex-a7, 32 bits, `low_ram`, OpenGL ES 3.2). Caminhos possíveis, a decidir: ver `15-analise-snes9x-ex.md`.

## Onde ficam os jogos
Na pasta pública **`/storage/emulated/0/Jogos/`** (ou em `Jogos/snes/`). O adb não consegue escrever dentro de `Android/data` no Android 11 (testado: `Permission denied`), por isso a pasta é pública. Para o app **ler** essa pasta, ele precisa do acesso a todos os arquivos, liberado uma vez por adb:

```
adb shell appops set br.hy300.games MANAGE_EXTERNAL_STORAGE allow
```

Para enviar jogos: `scripts/05-enviar-jogos.ps1` ou `adb push arquivo.sfc /storage/emulated/0/Jogos/`. O app também lê a pasta do próprio app, mas ela só aceita arquivos colocados pelo próprio app.

## O que ainda não foi verificado
- Se o Snes9x EX+ instalado aceita a ROM entregue por `content://` e abre o jogo direto.
- Se o controle é reconhecido pelo Android como gamepad (o app só procura dispositivos do tipo gamepad ou joystick).
- Nada disso foi compilado ou testado no aparelho ainda.
