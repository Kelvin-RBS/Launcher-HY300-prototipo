# Launcher HY300 — protótipo

▶ **Veja como o projeto está ficando:** [protótipo online](https://kelvin-rbs.github.io/Launcher-HY300-prototipo/)

Um launcher novo, leve e com visual tecnológico para o projetor **HY300** (Android 11, Allwinner H713), mantendo as funções do original e com o objetivo de transformá-lo em um **projetor gamer**.

> **Status:** protótipo de design navegável + análise técnica do launcher original. O projeto Android (Kotlin) e a integração com jogos ainda **não** foram escritos.

## Como este projeto é feito
A ideia é usar **IA** e **REA** do começo ao fim:
- **IA (Claude, da Anthropic):** análise dos dados do aparelho, documentação, protótipo e, mais adiante, o código do launcher.
- **REA (Reverse Engineer Anything):** ferramenta de engenharia reversa usada para analisar os apps do aparelho. O módulo Android do REA (com JADX) já está configurado e foi usado para decompilar o `com.htc.htcsettings`: a tela de Projeção já foi mapeada ([`docs/10-analise-htcsettings.md`](docs/10-analise-htcsettings.md)).

O autor define o rumo, testa no aparelho e decide o que entra.

## A ideia
O HY300 já projeta uma imagem grande em qualquer parede. Falta um sistema à altura: rápido, bonito, fácil de usar só com o controle remoto e pensado para jogar. A visão do projeto:

- **Tela inicial limpa**, com atalhos de apps, relógio e acesso rápido a rede, Bluetooth e configurações.
- **Jogos retrô de 16 bits** (Super Nintendo, Mega Drive etc.) com o **RetroArch** como base, em uma área fixa de Jogos.
- **Consoles e PC pelo HDMI**, com a entrada a um clique.
- **Controle Bluetooth ou USB** para jogar (o controle remoto do projetor só tem direcional e OK).
- **Reversível por enquanto:** o launcher é instalado como app comum. A ideia futura é **trocar o firmware**.

> Metas, não promessas: a latência da entrada HDMI e o desempenho do RetroArch neste hardware (32 bits, `low_ram`) **ainda não foram medidos**.

## Por que trocar o launcher original
Ele é pesado para o hardware, envia dados para servidores de rastreamento (Baidu) e para uma loja da fabricante, e tem telas que o aparelho não usa (bateria). O original continua instalado **por enquanto**, então dá para voltar quando quiser (**por enquanto**), pois a ideia futura é trocar o firmware.

## O que o protótipo já tem
| Parte | Descrição |
|---|---|
| **Atalhos editáveis** | Até 5 apps instalados, à esquerda da fileira |
| **Atalhos fixos** | **Jogos** (ícone de controle estilo Super Nintendo, "em breve"), **Arquivos** (interno e pendrive), **HDMI** e **Gerenciar atalhos** |
| **Topo** | Rede, Bluetooth e Configurações (engrenagem), todos navegáveis, e o relógio |
| **Configurações** | Rede, Bluetooth, Idioma, Projeção (zoom, método, keystone, brilho e cor), Áudio (volume e bip do cursor), Data e hora, Sobre o dispositivo |
| **Som** | Tique ao mover o cursor, desligável em Áudio |
| **Fundo** | Lago ao pôr do sol (gerado por IA) |

Parte das Configurações, como Projeção, só funciona no aparelho abrindo a tela original do sistema, porque um app comum não tem permissão para alterar esses ajustes. Detalhes em [`docs/07-configuracoes.md`](docs/07-configuracoes.md).

## Como ver o protótipo
Abra o [protótipo online](https://kelvin-rbs.github.io/Launcher-HY300-prototipo/) ou, no seu computador, o arquivo `prototype/index.html`.
- **Setas:** navegar (com som) · **Enter:** abrir · **Esc:** voltar · **P:** simular pendrive.
- **Seta para cima** leva aos ícones do topo (Rede, Bluetooth, Configurações).
- **Gerenciar atalhos** edita os 5 atalhos da esquerda.
- No celular, toque nos cartões.

## Estrutura do repositório
| Pasta | Conteúdo |
|---|---|
| `prototype/` | Protótipo navegável (1280×720) |
| `docs/` | Aparelho, extração, análise do original, mapa de funções, design, roadmap, configurações, jogos e RetroArch |
| `scripts/` | Scripts PowerShell (ADB) para extrair dados, testar telas, instalar o RetroArch e gerar o `latest.json` |
| `retroarch/` | Configuração de exemplo do RetroArch para o projetor |
| `android/` | Reservado para o projeto Kotlin |

Documentos principais: [`docs/05-design.md`](docs/05-design.md) · [`docs/06-roadmap.md`](docs/06-roadmap.md) · [`docs/08-jogos-retroarch.md`](docs/08-jogos-retroarch.md) · [`docs/09-retroarch-otimizado.md`](docs/09-retroarch-otimizado.md) · [`docs/10-analise-htcsettings.md`](docs/10-analise-htcsettings.md) · [`docs/11-atualizacao-e-sobre.md`](docs/11-atualizacao-e-sobre.md) · [`docs/12-atualizacao-do-launcher-pelo-github.md`](docs/12-atualizacao-do-launcher-pelo-github.md) · [`CHANGELOG.md`](CHANGELOG.md)

## Resumo das descobertas
- O launcher original (`com.htc.htclauncherhighenglishd08`, v1.4.5) é só uma grade de apps. Roda como app de sistema e **não** controla keystone, foco ou imagem.
- Essas funções ficam no `com.htc.htcsettings`. O launcher novo abre as telas originais por intent.
- **HDMI** abre com `com.softwinner.awlivetv/.MainActivity` (testado).
- **Zoom, método de projeção e keystone** não abrem direto (activities não exportadas). O caminho é abrir a Projeção e escolher o cartão. **Brilho e cor** abre direto.
- O aparelho tem **uma** porta HDMI física, mas o sistema declara três HDMI e um AV. O launcher mostra só "HDMI".
- **Atualização local** (pendrive) dispara um serviço de sistema (`android.rockchip.update.service`); a **online** é delegada ao `com.htc.htcotaupdate`. O launcher novo abre as telas originais ([`docs/11-atualizacao-e-sobre.md`](docs/11-atualizacao-e-sobre.md)).
- O próprio app de configurações (`htcsettings`) também tem analytics da Baidu; só um firmware próprio remove.
- Sem bateria: as telas de bateria baixa não entram.
- Rastreamento no original: analytics da Baidu e API de loja da fabricante. O launcher novo não inclui nada disso.

## Próximos passos
1. Instalar o RetroArch de 32 bits no projetor e testar um jogo de Super Nintendo ([`docs/09-retroarch-otimizado.md`](docs/09-retroarch-otimizado.md)).
2. Descobrir como abrir o RetroArch por intent e aplicar a configuração sem toque.
3. Escrever o projeto Android (Kotlin, sem bibliotecas pesadas), com atualização do launcher pelo GitHub ([`docs/12`](docs/12-atualizacao-do-launcher-pelo-github.md)).
4. Instalar no aparelho, definir como padrão e validar HDMI e Projeção.
5. Futuro: firmware próprio ([`docs/06-roadmap.md`](docs/06-roadmap.md)).

## Avisos
- Este repositório **não** inclui APKs, dumps do aparelho nem jogos (ROMs). Veja `.gitignore`.
- Por enquanto, o launcher novo não altera o firmware nem precisa de root. **Trocar o firmware é um objetivo futuro e pode inutilizar o aparelho:** guarde uma cópia do original e tenha um plano de recuperação antes.
- Use apenas jogos que você tem direito de usar.
- Projeto não afiliado ao fabricante do HY300, ao RetroArch, à Nintendo nem à Anthropic.

## Créditos
- Plano de fundo gerado por IA.
- Análise e protótipo feitos com IA (Claude, da Anthropic) e REA.

## Licença
O código e a documentação estão sob a licença MIT (`LICENSE`). O plano de fundo foi gerado por IA e não está coberto por essa licença.
