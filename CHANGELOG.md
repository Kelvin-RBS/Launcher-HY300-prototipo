# Changelog

## v19
- App Jogos HY300 0.3.0: suporte a **NES**, além do Super Nintendo. O app escolhe o sistema pela extensão (ou pelo conteúdo do `.zip`) e o núcleo certo (`fceumm` para NES). Primeiro jogo de NES: Super Mario Bros. 3.
- Sem o núcleo do sistema, o app avisa "Falta o núcleo de ...". O Snes9x EX+ continua servindo só ao Super Nintendo.
- O script `05-enviar-jogos.ps1` envia as pastas `snes` e `nes`.

## v18
- **README em inglês** (`README.en.md`), com opção de troca de idioma no topo dos dois README. O original continua em português.
- Launcher 0.4.2: o menu da engrenagem ficou sem os parênteses de "Projeção" e "Atualização local", sem "Fuso horário" (fica em Data e hora) e com **"Sobre o dispositivo" por último**.
- App Jogos HY300 0.2.2: o **cursor não é mais cortado** nas bordas (o botão não aumenta mais ao receber o foco) e a **lista de jogos não sobrepõe mais o título** ao rolar.
- Protótipo: nova tela de jogos e página "Aplicativos" nas Configurações.
- Verificado: NDK 27.0.12077973 e CMake 3.22.1 (as versões que o projeto usa) estão instalados e o projeto compila.

## v17
- Launcher 0.4.1: removido o atalho fixo **Gerenciar aplicativos** e o item **Instalar apps**. Os apps ficam só em **Configurações > Aplicativos** (antes "Aplicativos (todos)").
- App Jogos HY300 0.2.1: **segurar Start por 3 segundos** durante o jogo pergunta "Fechar o jogo?"; se sim, volta à lista de jogos. Select + Start continua abrindo o menu de pausa.

## v16
- App Jogos HY300 0.2.0: **motor próprio** (C++ e Kotlin) que roda o núcleo libretro do Snes9x, sem controles de toque e em português. Menu de pausa com Select + Start (continuar, reiniciar, salvar e carregar estado, ajustes, sair para a lista de jogos). Sem o núcleo, continua usando o Snes9x EX+.
- Botões e linhas com **cursor bem visível** (borda branca grossa, fundo azul e aumento) nas telas do app de jogos.
- A lista aceita jogos em `.zip` (só com ROM de Super Nintendo). Jogos novos: Donkey Kong Country 1, 2 e 3, Super Mario Kart, Goof Troop e Choplifter III.
- `docs/16-motor-proprio.md`. Os núcleos (`*_libretro_android.so`) ficam fora do Git.

## v15
- Launcher 0.4.0: novo atalho fixo **Gerenciar aplicativos** (todos os apps; OK abre; segurar OK por 2 s desinstala, com confirmação do Android; o primeiro item abre a loja para instalar). A opção **Aplicativos (todos)** também entrou no menu da engrenagem.
- App Jogos HY300 0.1.2: o nome do jogo aparece sem "PT-BR" e sem o que estiver entre parênteses ou colchetes.

## v14
- App Jogos HY300 0.1.1: lê os jogos da pasta pública `/storage/emulated/0/Jogos/` (o adb não escreve em `Android/data`). Precisa do acesso a todos os arquivos, liberado por `appops`.
- Configurações do app mostram a pasta e se o acesso está liberado.

## v13
- **Mudança de rumo:** o RetroArch foi removido do projeto (código, scripts, configuração e documentos). A base para jogos passa a ser o **Snes9x EX+**.
- Novo app **Jogos HY300** (`br.hy300.games`, módulo `games`): lista só com o nome dos jogos, engrenagem com configurações, tela que pede e aguarda um controle Bluetooth, e abertura do jogo no Snes9x EX+.
- Launcher 0.3.0: o atalho Jogos abre o app novo.
- `docs/14-app-jogos.md` e `docs/15-analise-snes9x-ex.md` (análise do APK e da ROM).

## v12
- Launcher 0.2.0: **7 atalhos** de apps editáveis (antes 5), com Aptoide TV e Cast Receiver no padrão.
- Corrigida a borda branca de foco dos ícones do topo, que era cortada.
- **Tela de Jogos** (`GamesActivity`): lista as ROMs de `/Jogos` e abre no RetroArch (versão 32 bits). Ainda não testada no aparelho.
- Pasta `jogos/` (fora do Git) com um jogo de Super Nintendo, e script `scripts/05-enviar-jogos.ps1`.
- `docs/14-jogos-no-launcher.md`.

## v11
- Primeira versão do **projeto Android** (`android/`): tela inicial, até 5 atalhos de apps com o banner ou ícone do próprio app, atalhos fixos (Jogos, Arquivos, HDMI, Gerenciar atalhos), menu da engrenagem e tela Gerenciar atalhos.
- Guia de Android Studio em `android/README.md` e arquitetura em `docs/13-projeto-android.md`.
- Ainda não compilado nem testado no aparelho.

## v10
- Como criar a chave de assinatura do launcher (`docs/12`) e `.gitignore` bloqueando chaves e arquivos locais do Android.

## v9
- Plano de atualização do **launcher** pelo GitHub (Releases + `latest.json` no Pages): `docs/12`, exemplo em `docs/exemplos/` e script `scripts/04-gerar-latest-json.ps1`.
- Decisão registrada: seguir pelo launcher por enquanto; firmware próprio fica para depois.

## v8
- Protótipo: ícone de Configurações trocado por uma **engrenagem** de 8 dentes.
- Nova análise: atualização local e online, e origem dos dados de "Sobre o dispositivo" (`docs/11`).
- Achado: o `htcsettings` também tem analytics da Baidu.

## v7
- JADX configurado no REA. Primeira decompilação: `com.htc.htcsettings` (`docs/10-analise-htcsettings.md`).
- Mapeados os cartões da tela de Projeção e quais activities são exportadas.
- Correção: o cartão de keystone abre `CorrectionIndexActivity`, e não `KeystonecorrectionActivity`.
- Anotado que o original usa `sound_effects_enabled` para o bip.

## v6
- README: link do protótipo online no topo, seção "Como este projeto é feito" (IA e REA), aviso de firmware futuro, "por enquanto" no original, créditos corrigidos (fundo gerado por IA) e removida a menção ao vídeo de inspiração.
- `index.html` na raiz para publicar o protótipo no GitHub Pages.
- RetroArch: plano de instalação e otimização sem toque (`docs/09`), configuração de exemplo (`retroarch/`) e script de instalação (`scripts/03`).
- Roadmap: seção de firmware próprio.

## v5
- README reescrito com a visão de **projetor gamer** e a lista atual de recursos.

## v4
- Novo atalho fixo **Jogos** (ícone de controle estilo Super Nintendo), na frente de Arquivos. Por enquanto mostra "Em breve".
- Removidos os cadeados dos atalhos fixos.
- Plano futuro de jogos de 16 bits com RetroArch (`docs/08-jogos-retroarch.md`).

## v3
- Tela de **Configurações** completa: Rede, Bluetooth, Idioma, Projeção, Áudio, Data e hora e Sobre o dispositivo.
- Rede e Bluetooth (topo) abrem a página correspondente; a engrenagem abre a lista.
- Botão de som removido da tela inicial; o **bip do cursor** agora é ligado e desligado em Áudio.
- Atalhos editáveis voltam a ser só de apps instalados; Projeção, Imagem e Áudio saíram dos atalhos e vivem nas Configurações.
- Relógio segue fuso horário e formato de hora escolhidos.

## v2
- Fileira de atalhos: até 5 editáveis + 3 fixos (Arquivos, HDMI, Gerenciar atalhos).
- Tela "Gerenciar atalhos" para adicionar e remover atalhos.
- Engrenagem de Configurações no topo; Rede, Bluetooth e Configurações navegáveis pelo direcional.
- Plano de fundo definido (lago ao pôr do sol).
- Cartão Arquivos indica pendrive conectado (tecla P simula no protótipo).

## v1
- Protótipo inicial, documentação e scripts de extração.
