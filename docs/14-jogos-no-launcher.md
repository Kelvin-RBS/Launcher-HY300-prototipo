# Jogos no launcher (RetroArch)

O atalho fixo **Jogos** abre a tela `GamesActivity`, que lista os jogos da pasta do projetor e abre o escolhido no RetroArch.

## Como funciona
1. O launcher procura os jogos em `/storage/emulated/0/Jogos/snes/` e `/storage/emulated/0/Jogos/megadrive/`.
2. Ao escolher um jogo, ele abre o RetroArch (`com.retroarch.ra32`, a versão de 32 bits; também tenta `com.retroarch` e `com.retroarch.aarch64`) na activity `com.retroarch.browser.retroactivity.RetroActivityFuture`, enviando o caminho do jogo (`ROM`) e o do núcleo (`LIBRETRO`).
3. Núcleos usados: `snes9x` (Super Nintendo) e `genesis_plus_gx` (Mega Drive).

## O que precisa existir no aparelho
- **RetroArch de 32 bits** instalado (`RetroArch_ra32.apk`).
- **Os núcleos baixados** dentro do RetroArch: *Online Updater > Core Downloader*. Sem isso o RetroArch não abre o jogo.
- **Os jogos** na pasta `/storage/emulated/0/Jogos/`.
- **Acesso a arquivos liberado** para o launcher (ele precisa listar a pasta). Como o Android 11 exige uma permissão especial, o jeito prático é pelo computador: `adb shell appops set br.hy300.launcher MANAGE_EXTERNAL_STORAGE allow`.

## O que ainda não foi verificado
Nada disso rodou no aparelho ainda. Pontos a confirmar no primeiro teste:
- Se o nome da activity e os dois extras (`ROM` e `LIBRETRO`) bastam nessa versão do RetroArch.
- Se o RetroArch consegue ler a pasta `/Jogos` (ele precisa da própria permissão de arquivos).
- Se o caminho do núcleo (`/data/data/<pacote>/cores/`) é o correto na versão instalada.
- Se o controle remoto serve só para o menu: **para jogar, use um controle Bluetooth ou USB.**

## Otimização (sem toque)
Veja `09-retroarch-otimizado.md`. O mais simples primeiro: desligar o overlay de toque dentro do RetroArch (*Configurações > Exibição na tela > Sobreposição na tela*; o nome pode variar conforme a versão).

## Jogos (ROMs) e Git
A pasta `jogos/` do repositório é só o ponto de partida. O `.gitignore` impede que qualquer jogo seja enviado ao GitHub.
