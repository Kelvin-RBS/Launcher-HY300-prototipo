**Português (BR)** · [English](README.en.md)

# Launcher HY300 — protótipo

▶ **Veja como o projeto está ficando:** [protótipo online](https://kelvin-rbs.github.io/Launcher-HY300-prototipo/)

Um launcher novo, leve e com visual tecnológico para o projetor **HY300** (Android 11, Allwinner H713), mantendo as funções do original e com o objetivo de transformá-lo em um **projetor gamer**.

> **Status:** protótipo de design navegável, análise técnica do launcher original, **launcher 0.4** e **app Jogos HY300 0.2** (com motor próprio de emulação). Os dois **compilam e rodam no projetor**. O desempenho dos jogos ainda **não foi medido**.

## Como este projeto é feito
A ideia é usar **IA** e **REA** do começo ao fim:
- **IA (Claude, da Anthropic):** análise dos dados do aparelho, documentação, protótipo e, mais adiante, o código do launcher.
- **REA (Reverse Engineer Anything):** ferramenta de engenharia reversa usada para analisar os apps do aparelho. O módulo Android do REA (com JADX) já está configurado e foi usado para decompilar o `com.htc.htcsettings`: a tela de Projeção já foi mapeada ([`docs/10-analise-htcsettings.md`](docs/10-analise-htcsettings.md)).

O autor define o rumo, testa no aparelho e decide o que entra.

## A ideia
O HY300 já projeta uma imagem grande em qualquer parede. Falta um sistema à altura: rápido, bonito, fácil de usar só com o controle remoto e pensado para jogar. A visão do projeto:

- **Tela inicial limpa**, com atalhos de apps, relógio e acesso rápido a rede, Bluetooth e configurações.
- **Jogos retrô de 16 bits** (começando pelo Super Nintendo) em um **app próprio de jogos**, com o visual do launcher e um **motor próprio** (núcleo Snes9x), sem controle de toque e compatível com controles Bluetooth em geral. Durante o jogo, **segurar Start por 3 segundos** pergunta se quer fechar o jogo e voltar à lista.
- **Consoles e PC pelo HDMI**, com a entrada a um clique.
- **Controle Bluetooth ou USB** para jogar (o controle remoto do projetor só tem direcional e OK).
- **Reversível por enquanto:** o launcher é instalado como app comum. A ideia futura é **trocar o firmware**.

> Metas, não promessas: a latência da entrada HDMI e o desempenho dos jogos neste hardware (32 bits, `low_ram`) **ainda não foram medidos**.

## Por que trocar o launcher original
Ele é pesado para o hardware, envia dados para servidores de rastreamento (Baidu) e para uma loja da fabricante, e tem telas que o aparelho não usa (bateria). O original continua instalado **por enquanto**, então dá para voltar quando quiser (**por enquanto**), pois a ideia futura é trocar o firmware.

## O que o protótipo já tem
| Parte | Descrição |
|---|---|
| **Atalhos editáveis** | Até 7 apps instalados, à esquerda da fileira |
| **Atalhos fixos** | **Jogos** (ícone de controle estilo Super Nintendo; abre o app Jogos HY300), **Arquivos** (interno e pendrive), **HDMI** e **Gerenciar atalhos** |
| **Topo** | Rede, Bluetooth e Configurações (engrenagem), todos navegáveis, e o relógio |
| **Configurações** | Rede, Bluetooth, Idioma, **Aplicativos** (todos os apps; OK abre, segurar OK por 2 s desinstala), Projeção (zoom, método, keystone, brilho e cor), Áudio (volume e bip do cursor), Data e hora e, por último, Sobre o dispositivo |
| **Jogos** | App separado com lista só com o nome dos jogos, pedido de controle Bluetooth e menu de pausa em português |
| **Som** | Tique ao mover o cursor (no launcher Android desde a v20, a confirmar no aparelho; no protótipo é desligável em Áudio) |
| **Fundo** | Lago ao pôr do sol (gerado por IA) |

Parte das Configurações, como Projeção, só funciona no aparelho abrindo a tela original do sistema, porque um app comum não tem permissão para alterar esses ajustes. Detalhes em [`docs/07-configuracoes.md`](docs/07-configuracoes.md).

## Como ver o protótipo
Abra o [protótipo online](https://kelvin-rbs.github.io/Launcher-HY300-prototipo/) ou, no seu computador, o arquivo `prototype/index.html`.
- **Setas:** navegar (com som) · **Enter:** abrir · **Esc:** voltar · **P:** simular pendrive.
- **Seta para cima** leva aos ícones do topo (Rede, Bluetooth, Configurações).
- **Gerenciar atalhos** edita os 7 atalhos da esquerda.
- No celular, toque nos cartões.

## Estrutura do repositório
| Pasta | Conteúdo |
|---|---|
| `prototype/` | Protótipo navegável (1280×720) |
| `docs/` | Aparelho, extração, análise do original, mapa de funções, design, roadmap, configurações, app de jogos e análise do Snes9x EX+ |
| `scripts/` | Scripts PowerShell (ADB) para extrair dados, testar telas, enviar jogos e gerar o `latest.json` |
| `jogos/` | Pasta de partida para os jogos (conteúdo fora do Git) |
| `android/` | Projeto Android (Kotlin): módulo `app` (launcher) e módulo `games` (app Jogos HY300). Guia em `android/README.md` |

Documentos principais: [`docs/05-design.md`](docs/05-design.md) · [`docs/06-roadmap.md`](docs/06-roadmap.md) · [`docs/14-app-jogos.md`](docs/14-app-jogos.md) · [`docs/15-analise-snes9x-ex.md`](docs/15-analise-snes9x-ex.md) · [`docs/16-motor-proprio.md`](docs/16-motor-proprio.md) · [`docs/10-analise-htcsettings.md`](docs/10-analise-htcsettings.md) · [`docs/11-atualizacao-e-sobre.md`](docs/11-atualizacao-e-sobre.md) · [`docs/12-atualizacao-do-launcher-pelo-github.md`](docs/12-atualizacao-do-launcher-pelo-github.md) · [`docs/13-projeto-android.md`](docs/13-projeto-android.md) · [`CHANGELOG.md`](CHANGELOG.md)

## Resumo das descobertas
- O launcher original (`com.htc.htclauncherhighenglishd08`, v1.4.5) é só uma grade de apps. Roda como app de sistema e **não** controla keystone, foco ou imagem.
- Essas funções ficam no `com.htc.htcsettings`. O launcher novo abre as telas originais por intent.
- **HDMI** abre com `com.softwinner.awlivetv/.MainActivity` (testado).
- **Zoom, método de projeção e keystone** não abrem direto (activities não exportadas). O caminho é abrir a Projeção e escolher o cartão. **Brilho e cor** abre direto.
- O aparelho tem **uma** porta HDMI física, mas o sistema declara três HDMI e um AV. O launcher mostra só "HDMI".
- **Atualização local** (pendrive) dispara um serviço de sistema (`android.rockchip.update.service`); a **online** é delegada ao `com.htc.htcotaupdate`. O launcher novo abre as telas originais ([`docs/11-atualizacao-e-sobre.md`](docs/11-atualizacao-e-sobre.md)).
- O próprio app de configurações (`htcsettings`) também tem analytics da Baidu; só um firmware próprio remove.
- Um app pré-instalado (Office Suite) rodava em segundo plano com cerca de 143 MB de memória e nenhum app do sistema analisado depende dele. Apps assim podem ser removidos para o usuário (de forma reversível).
- Sem bateria: as telas de bateria baixa não entram.
- Rastreamento no original: analytics da Baidu e API de loja da fabricante. O launcher novo não inclui nada disso.

## Próximos passos
1. **Testar no projetor a v22:** jogue uns 30 segundos mexendo nos botões e no direcional, abra **Diagnóstico de atraso** no menu do jogo (Select+Start) e leia as conclusões; teste a "fila de vídeo curta" e meça de novo.
2. Medir o desempenho e o atraso dos jogos e afinar o som ([`docs/16-motor-proprio.md`](docs/16-motor-proprio.md)).
3. Testar o NES (precisa do núcleo `fceumm`) e acrescentar outros sistemas.
4. **Controle remoto pelo celular:** o launcher abre um servidor na rede Wi-Fi local e o celular controla o projetor (direcional, OK, Voltar, Home) por uma página web, com PIN mostrado na tela. Começar pelo launcher e pelos jogos; fora deles depende de um serviço de acessibilidade.
5. Telas próprias de Configurações no launcher, abrindo as originais quando o ajuste exigir permissão de sistema ([`docs/07-configuracoes.md`](docs/07-configuracoes.md)).
6. Atualização do launcher pelo GitHub ([`docs/12`](docs/12-atualizacao-do-launcher-pelo-github.md)).
7. Futuro: firmware próprio ([`docs/06-roadmap.md`](docs/06-roadmap.md)).

## Avisos
- Este repositório **não** inclui APKs, dumps do aparelho, jogos (ROMs) nem o núcleo do emulador (`.so`). Veja `.gitignore`.
- Por enquanto, o launcher novo não altera o firmware nem precisa de root. **Trocar o firmware é um objetivo futuro e pode inutilizar o aparelho:** guarde uma cópia do original e tenha um plano de recuperação antes.
- Use apenas jogos que você tem direito de usar.
- Projeto não afiliado ao fabricante do HY300, ao Snes9x/EmuEx, à Nintendo nem à Anthropic.

## Créditos
- Plano de fundo gerado por IA.
- Núcleo de emulação: **Snes9x** (pela interface libretro), baixado do site do libretro e **não incluído** aqui. A licença do Snes9x é não comercial.
- Análise e protótipo feitos com IA (Claude, da Anthropic) e REA.

## Licença
O código e a documentação estão sob a licença MIT (`LICENSE`). O plano de fundo foi gerado por IA e não está coberto por essa licença.
