# Jogos retrô (RetroArch) — plano futuro

**Estado atual:** o atalho fixo **Jogos** já existe no protótipo, na frente de Arquivos, mas só mostra "Em breve". Não há emulador integrado.

## Ideia
Uma área fixa de jogos de 16 bits (Super Nintendo, Mega Drive etc.) usando o **RetroArch** como base.

## Como deve funcionar (a validar)
- O launcher **não embute** o emulador: ele abre o RetroArch instalado, passando o núcleo e o jogo. É o modo mais simples e evita misturar licenças (o RetroArch é GPLv3).
- O RetroArch aceita ser aberto por outro app com extras de intent (`ROM`, `LIBRETRO` para o núcleo, `CONFIGFILE`) na activity `RetroActivityFuture`. Isso é o padrão usado por outros frontends, mas o **nome exato do pacote e da activity precisa ser confirmado no aparelho**.
- A tela de Jogos do launcher listaria os jogos de uma pasta (interna ou pendrive) e abriria cada um no núcleo certo.

## Pontos de atenção
- **Arquitetura do aparelho:** 32 bits (`armeabi-v7a`), `low_ram`, Allwinner H713. É preciso usar a versão do RetroArch compatível com 32 bits. Núcleos de 16 bits são leves e devem rodar bem, mas isso precisa ser testado.
- **Controle:** o controle remoto do projetor só tem direcional e OK. Para jogar de verdade, use um controle Bluetooth ou USB (o aparelho tem uma porta USB e Bluetooth).
- **Jogos (ROMs):** o repositório não inclui nenhum. Use apenas jogos que você tem direito de usar.
- **Atraso do áudio:** áudio por Bluetooth costuma ter atraso e pode atrapalhar jogos. Teste com o alto-falante do aparelho ou fone com fio (saída P2).

## Próximos passos
1. Instalar o RetroArch (32 bits) no projetor e testar um núcleo de Super Nintendo.
2. Descobrir pacote e activity reais por `adb shell pm list packages` e `dumpsys package`.
3. Testar a abertura de um jogo por `adb shell am start` com os extras.
4. Só então desenhar a tela de Jogos no launcher.
