# Launcher HY300 — protótipo

Um launcher novo, leve e com visual tecnológico para o projetor **HY300** (Android 11, Allwinner H713), mantendo as funções do original e com o objetivo de transformá-lo em um **projetor gamer**.

> **Status:** protótipo de design navegável + análise técnica do launcher original. O projeto Android (Kotlin) e a integração com jogos ainda **não** foram escritos.

## A ideia
O HY300 já projeta uma imagem grande em qualquer parede. Falta um sistema à altura: rápido, bonito, fácil de usar só com o controle remoto e pensado para jogar. A visão do projeto:

- **Tela inicial limpa**, com atalhos de apps, relógio e acesso rápido a rede, Bluetooth e configurações.
- **Jogos retrô de 16 bits** (Super Nintendo, Mega Drive etc.) com o **RetroArch** como base, em uma área fixa de Jogos.
- **Consoles e PC pelo HDMI**, com a entrada a um clique.
- **Controle Bluetooth ou USB** para jogar (o controle remoto do projetor só tem direcional e OK).
- **Tudo reversível:** instalado como app comum, sem mexer no firmware.

> Metas, não promessas: a latência da entrada HDMI e o desempenho do RetroArch neste hardware (32 bits, `low_ram`) **ainda não foram medidos**.

## Por que trocar o launcher original
Ele é pesado para o hardware, envia dados para servidores de rastreamento (Baidu) e para uma loja da fabricante, e tem telas que o aparelho não usa (bateria). O original continua instalado, então dá para voltar quando quiser.

## O que o protótipo já tem
| Parte | Descrição |
|---|---|
| **Atalhos editáveis** | Até 5 apps instalados, à esquerda da fileira |
| **Atalhos fixos** | **Jogos** (ícone de controle estilo Super Nintendo, "em breve"), **Arquivos** (interno e pendrive), **HDMI** e **Gerenciar atalhos** |
| **Topo** | Rede, Bluetooth e Configurações (engrenagem), todos navegáveis, e o relógio |
| **Configurações** | Rede, Bluetooth, Idioma, Projeção (zoom, método, keystone, brilho e cor), Áudio (volume e bip do cursor), Data e hora, Sobre o dispositivo |
| **Som** | Tique ao mover o cursor, desligável em Áudio |
| **Fundo** | Lago ao pôr do sol |

Parte das Configurações, como Projeção, só funciona no aparelho abrindo a tela original do sistema, porque um app comum não tem permissão para alterar esses ajustes. Detalhes em [`docs/07-configuracoes.md`](docs/07-configuracoes.md).

## Como ver o protótipo
Abra `prototype/index.html` no navegador.
- **Setas:** navegar (com som) · **Enter:** abrir · **Esc:** voltar · **P:** simular pendrive.
- **Seta para cima** leva aos ícones do topo (Rede, Bluetooth, Configurações).
- **Gerenciar atalhos** edita os 5 atalhos da esquerda.
- No celular, toque nos cartões.

## Estrutura do repositório
| Pasta | Conteúdo |
|---|---|
| `prototype/` | Protótipo navegável (1280×720) |
| `docs/` | Aparelho, extração, análise do original, mapa de funções, design, roadmap, configurações e plano de jogos |
| `scripts/` | Scripts PowerShell (ADB) para extrair dados e testar telas |
| `android/` | Reservado para o projeto Kotlin |

Documentos principais: [`docs/05-design.md`](docs/05-design.md) · [`docs/06-roadmap.md`](docs/06-roadmap.md) · [`docs/08-jogos-retroarch.md`](docs/08-jogos-retroarch.md) · [`CHANGELOG.md`](CHANGELOG.md)

## Resumo das descobertas
- O launcher original (`com.htc.htclauncherhighenglishd08`, v1.4.5) é só uma grade de apps. Roda como app de sistema e **não** controla keystone, foco ou imagem.
- Essas funções ficam no `com.htc.htcsettings`. O launcher novo abre as telas originais por intent.
- **HDMI** abre com `com.softwinner.awlivetv/.MainActivity` (testado).
- **Keystone** não abre direto (`Permission Denial`). O caminho é Projeção → Correção de Keystone.
- O aparelho tem **uma** porta HDMI física, mas o sistema declara três HDMI e um AV. O launcher mostra só "HDMI".
- Sem bateria: as telas de bateria baixa não entram.
- Rastreamento no original: analytics da Baidu e API de loja da fabricante. O launcher novo não inclui nada disso.

## Próximos passos
1. Aplicar as próximas alterações de design.
2. Testar o RetroArch (32 bits) no projetor e descobrir como abri-lo por intent.
3. Escrever o projeto Android (Kotlin, sem bibliotecas pesadas).
4. Instalar no aparelho, definir como padrão e validar HDMI e Projeção.

Lista completa e pendências em [`docs/06-roadmap.md`](docs/06-roadmap.md).

## Avisos
- Este repositório **não** inclui APKs, dumps do aparelho nem jogos (ROMs). Veja `.gitignore`.
- O launcher novo não altera firmware, não precisa de root e não substitui o original.
- Use apenas jogos que você tem direito de usar.
- Projeto não afiliado ao fabricante do HY300, ao RetroArch, à Nintendo nem à Anthropic.

## Créditos
- Estrutura visual inspirada em um launcher mostrado em um vídeo público do Instagram (@feliopz). O código, as cores e os textos aqui são próprios.
- Plano de fundo fornecido pelo autor do projeto.
- Análise e protótipo feitos com ajuda do Claude (Anthropic).

## Licença
O código e a documentação estão sob a licença MIT (`LICENSE`), exceto o plano de fundo, cuja origem é do autor.
