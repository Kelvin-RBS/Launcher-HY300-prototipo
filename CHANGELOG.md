# Changelog

## v26
- App Jogos (correção): **busca de catálogo no GitHub** (bsnesemulator/SNES-ROMS) voltava vazia. O filtro comparava o sistema com extensões de arquivo e descartava todos os jogos; a listagem usava a API "contents" (limite de 1000 itens, sem paginação); a checagem de atualização comparava ETag com SHA. Agora lista o repo inteiro via `git/trees?recursive=1`, usa cache (e cai no cache se a API limitar), monta a URL de download com codificação correta e a tela usa lista leve com busca pelo teclado na tela.
- App Jogos 0.6.0: **Download de jogos do GitHub** substituindo o envio pela rede.
  - A tela "Gerenciar jogos" agora tem o botão "Buscar catálogo no GitHub" que baixa a lista de jogos disponíveis do GitHub Pages (`games.json`).
  - Seleção múltipla com checkboxes (inclui "Selecionar todos") e download sequencial com progresso.
  - Jogos são baixados do GitHub Releases (`https://github.com/Kelvin-RBS/Launcher-HY300-prototipo/releases/download/...`) direto para `/storage/emulated/0/Jogos/{sistema}/`.
  - Verificação de integridade via SHA-256 após cada download.
  - Script `scripts/07-gerar-games-json.ps1` para gerar o catálogo automaticamente.
  - Exemplo de catálogo em `docs/exemplos/games.example.json`.
  - **Removido:** servidor HTTP local (GameServer), página web de envio (`gerenciador.html`), PIN de acesso, upload pela rede.
  - Nova dependência: OkHttp 4.12.0.
  - Mantidos: renomear e excluir jogos locais.

## v25
- App Jogos 0.5.0: **Gerenciar jogos** (engrenagem > Gerenciar jogos), a quarta opção do menu de Configurações.
  - **Renomear** com teclado na tela; a extensão é mantida e o save e o estado salvo acompanham o nome.
  - **Excluir** apaga o arquivo do jogo por completo, com confirmação (o foco começa em Cancelar) e a escolha de apagar também o progresso.
  - **Envio pela rede:** a tela abre um servidor web (`http://IP:8080` com PIN de 4 dígitos) enquanto estiver aberta. No celular ou PC, na mesma rede, dá para enviar, renomear e apagar jogos; o que for enviado vai direto para a pasta certa (`snes` ou `nes`). Bloqueio por 30 s depois de 5 PINs errados, limite de 64 MB, só `.sfc .smc .swc .fig .nes .zip`, nomes limpos (nada grava fora da pasta). Nova permissão: `INTERNET` (necessária para abrir o servidor).
- **Sem download de sites de ROMs.** O pedido de baixar jogos de bsnes.org e do GitHub bsnesemulator **não foi atendido**: são coleções de jogos comerciais sem licença. O projeto não se liga a nenhuma fonte de ROMs; o envio pela rede serve para colocar os arquivos que você já tem.
- README: créditos corrigidos (Snes9x 2010, FCEUmm e libretro; o FCEUmm estava faltando).
- Testado no computador com requisições reais (PIN, bloqueio, envios, recusas, renomear, apagar, acentos) e o Kotlin compila. **Não testado:** no projetor nem num navegador de celular. Detalhes em `docs/17-gerenciar-jogos.md`.

## v24
- **Primeiro relatório real de atraso** (Super Mario Bros. 3, NES, 104 s de jogo, controle e caixa de som ambos por Bluetooth): o NES da v23 **abre e roda**. O relatório mostrou que o som saía por uma caixa Bluetooth (A2DP) com **274 ms de atraso real** (máx 370), 95 falhas de áudio, e que o controle enviava um relatório a cada **58 ms** (o normal é ~8 ms). Hipótese: o som e o controle dividem o mesmo rádio Bluetooth; a confirmar medindo com o som fora do Bluetooth.
- Diagnóstico corrigido (Jogos 0.4.2), dois erros meus que distorciam as conclusões:
  - O "tempo por quadro" incluía a espera por buffer de vídeo (bloqueio, não processamento) e acusava "processador no limite" sem estar. Agora mede só o processamento do núcleo (o motor informa a espera de vídeo de cada quadro).
  - O tempo de entrada incluía a espera normal pelo próximo quadro (~8 ms em média) e era sempre marcado como alto. Agora separa "chegada ao app" de "espera pelo quadro".
- Novas conclusões: som e controle por Bluetooth ao mesmo tempo; esperas longas de vídeo (picos do sistema de tela); a mensagem de caminho rápido de áudio não aparece mais quando a saída é Bluetooth (a causa é o Bluetooth).
- Novo botão **"Zerar medições"** na janela do diagnóstico, para comparar antes e depois de mudar algo sem reabrir o jogo. A saída de áudio é relida a cada segundo.
- Testado: simulação do caso real (as conclusões saem como esperado) e o Kotlin compila. **Não testado:** no projetor.

## v23
- App Jogos 0.4.1: **NES (Super Mario Bros. 3) corrigido, a confirmar no aparelho.** O motivo apareceu na janela da v21: o fceumm respondeu `Error opening "Super Mario Bros. 3.nes"`. A ROM estava correta (cabeçalho iNES, mapper 4, 393232 bytes). O fceumm abre o jogo **pelo caminho do arquivo**, e o app só passava o nome. Agora o motor pergunta ao núcleo se ele precisa do caminho (`need_fullpath`); se precisar, o app grava a ROM em `cache/rom/` (serve também para ROM dentro de .zip) e passa o caminho completo. O SNES não muda.
- Launcher 0.4.5 e Jogos 0.4.1: **o mesmo tique em todas as telas** (Home, Gerenciar atalhos, Aplicativos, lista de Jogos, Ajustes, menu de pausa do jogo), pela nova base `NavActivity`.
- **Tique duplo corrigido de outro jeito.** O conserto da v21 (desligar o som de efeitos da janela) não funcionou: ao mover o foco com o direcional, o Android toca o som de navegação por conta própria, sem consultar a tela. Agora o app atende as teclas direcionais que nenhum componente tratou (as linhas de ajuste continuam usando esquerda/direita), então o Android não faz a navegação dele nem toca o som dele; só toca o tique do app. Nas bordas da lista não toca nada. O foco colocado pelo app (abrir a tela, atualizar a lista, abrir o menu) não toca.
- Testado: o Kotlin dos dois módulos compila sem erros (kotlinc contra o android.jar da API 30) e o motor detecta o `need_fullpath` com um núcleo falso. **Não testado:** nada rodou no projetor.

## v22
- App Jogos 0.4.0: **Diagnóstico de atraso.** No menu do jogo (Select+Start) há o novo item "Diagnóstico de atraso". Ele mostra um relatório com conclusões e o salva em `Jogos/diagnostico-atraso.txt` (para ler no PC: `scripts/06-ler-diagnostico.ps1`). Mede:
  - **controle até o jogo:** quanto cada aperto espera para ser lido, separando botões (teclas) e direcional/analógico (eventos de movimento, que o Android agrupa por quadro de tela), e o intervalo real entre os envios do controle;
  - **emulação:** tempo por quadro, ritmo dos quadros e quadros atrasados;
  - **vídeo:** quanto o app espera por um buffer de vídeo livre (espera alta = fila cheia = 1 ou 2 quadros de atraso na imagem) e a taxa da tela;
  - **áudio:** saída usada (HDMI, alto-falante, Bluetooth...), se o Android liberou o caminho rápido, a rajada do sistema e o **atraso real do som** (pela posição de reprodução do sistema).
- Novo ajuste **"Fila de vídeo curta (experimental)"**: quando a fila de vídeo enche, o app descarta um quadro (no máximo 1 a cada 30) para a imagem não ficar atrasada. Vem desligada; compare com o diagnóstico antes e depois.
- Com "Mostrar fps e diagnóstico" ligado, a tela mostra também uma linha com o atraso de entrada e a espera de vídeo.
- Não mede (e o relatório avisa): o rádio Bluetooth do controle e o processamento de imagem do projetor.
- Testado no computador: o relatório, com cenários simulados (bom, poucas medições, áudio Bluetooth, processador no limite, direcional agrupado, fila de vídeo cheia, tela em 50 Hz); a medição de vídeo e o descarte de quadros no motor C++ (janela falsa lenta). **O Kotlin dos dois módulos agora foi compilado** (kotlinc 1.9.24 contra o android.jar da API 30, sem erros), o que achou e corrigiu um erro de aspas. **Não testado:** nada rodou no projetor.

## v21
- Launcher 0.4.4: o tique dos botões do topo (Rede, Bluetooth, engrenagem) tocava duas vezes. A causa provável é o som de navegação do próprio Android somado ao tique do launcher; ele foi desligado nesta tela, então fica só o tique do launcher. Além disso, o tique nunca toca duas vezes em menos de 60 ms. **A confirmar** no aparelho.
- App Jogos 0.3.2: quando o núcleo recusa um jogo, o app agora mostra uma janela com o motivo: o que o núcleo disse (o motor passou a receber o registro do núcleo) e dados do arquivo (tamanho e, no NES, cabeçalho iNES e mapper). Motivo: o NES (Super Mario Bros. 3) foi recusado pelo fceumm sem explicação.
- **Removido o app TV AO VIVO** (módulo `tv`, `br.hy300.tv`) e o atalho padrão dele no launcher. O código continua no histórico do Git. Para tirá-lo do projetor: `adb uninstall br.hy300.tv`.
- Testado no computador: o registro do núcleo (mensagem de erro capturada e limpa a cada jogo) com um núcleo falso. **Não testado:** o Kotlin não foi compilado e nada rodou no projetor.

## v20
- Launcher 0.4.3: **tique ao mover o cursor** na tela inicial (atalhos e ícones do topo). O som é do próprio app (`SoundPool`), então não depende do ajuste de efeitos sonoros do sistema, e segue o volume principal do projetor. O foco que o app coloca sozinho ao abrir a tela não toca o tique.
- App Jogos HY300 0.3.1: **jogos novos entram na lista sozinhos.** Com a lista aberta, o app confere as pastas `Jogos`, `Jogos/snes` e `Jogos/nes` a cada 2 s; quando a pasta fica igual em duas conferências seguidas (o arquivo terminou de copiar), a lista é atualizada sem tirar o cursor do lugar e aparece o aviso "Novo jogo: ...".
- Menos atraso no som: o motor agora **converte o áudio para a taxa nativa do aparelho** (normalmente 48000 Hz; o SNES usa ~32040 Hz) com interpolação cúbica. Com outra taxa o Android perde o caminho de baixa latência. O buffer de áudio começa pequeno (latência "Baixa" passou a ser o padrão) e **aumenta sozinho se o som falhar**. A thread do emulador ganhou prioridade de áudio. Compilação nativa com `-O2`.
- "Mostrar fps" virou **"Mostrar fps e diagnóstico"**: fps, atraso do áudio (ms), falhas de áudio e o quadro de emulação mais lento do último segundo. Serve para separar o que é do app do que é do controle Bluetooth ou do projetor.
- Testado no computador: o conversor de áudio (razão de taxa exata e erro de 0,02% contra uma senoide) e o motor C++ rodando 600 quadros com um núcleo falso. **Não testado:** o Kotlin não foi compilado e nada disso rodou no projetor.

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
