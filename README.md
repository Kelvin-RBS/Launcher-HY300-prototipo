# Launcher HY300 — protótipo

Um launcher novo, leve e com visual tecnológico para o projetor **HY300** (Android 11, Allwinner H713), mantendo todas as funções do original.

> **Status:** protótipo de design navegável + análise técnica do launcher original. O projeto Android (Kotlin) ainda não foi escrito.

## Por que este projeto existe
O launcher que vem no HY300 é pesado para o hardware (modo `low_ram`), envia dados para servidores de rastreamento (Baidu) e para uma loja da fabricante, e tem telas que o aparelho nem usa (bateria). A ideia é trocá-lo por um launcher próprio, **sem mexer no firmware**: ele é instalado como app comum e definido como padrão. O original continua no aparelho, então é reversível.

## O que tem aqui
| Pasta | Conteúdo |
|---|---|
| `prototype/` | Protótipo navegável (1280×720): setas, Enter, som de foco. Abra `index.html` no navegador. |
| `docs/` | Aparelho, como extrair os dados, análise do launcher original, mapa de funções, decisões de design e roadmap. |
| `scripts/` | Scripts PowerShell (ADB) usados para extrair dados e testar telas. |
| `android/` | Reservado para o projeto Kotlin. |

## Como ver o protótipo
Abra `prototype/index.html`. Use **← →** para navegar (com som), **Enter** para abrir e **M** para silenciar. No celular, toque nos cartões.

## Resumo das descobertas
- O launcher original (`com.htc.htclauncherhighenglishd08`, v1.4.5) é só uma grade de apps com ícones de status. Roda como app de sistema (uid 1000) e **não** controla keystone, foco ou imagem.
- Essas funções ficam no app `com.htc.htcsettings`. Os atalhos do launcher novo abrem as telas originais por intent.
- **HDMI** abre com `com.softwinner.awlivetv/.MainActivity` (testado).
- **Keystone** não abre direto (`Permission Denial`, activity não exportada). O caminho é **Projeção → Correção de Keystone**.
- O aparelho tem **uma** porta HDMI física, embora o sistema declare três HDMI e um AV. O launcher mostra só "HDMI".
- Sem bateria: telas de bateria baixa não entram.
- Rastreamento no original: analytics da Baidu e API de loja da fabricante. O launcher novo não inclui nada disso.

Detalhes em [`docs/`](docs/).

## Próximos passos
Ver [`docs/06-roadmap.md`](docs/06-roadmap.md).

## Avisos
- Este repositório **não** inclui APKs nem dumps do aparelho (contêm software proprietário e identificadores). Veja `.gitignore`.
- O launcher novo não altera firmware, não precisa de root e não substitui o original.
- Projeto não afiliado ao fabricante do HY300 nem à Anthropic.

## Créditos
- Estrutura visual inspirada em um launcher mostrado em um vídeo público do Instagram (@feliopz). O código, as cores e os textos aqui são próprios.
- Plano de fundo fornecido pelo autor do projeto.
- Análise e protótipo feitos com ajuda do Claude (Anthropic).

## Licença
O código e a documentação deste repositório estão sob a licença MIT (`LICENSE`), exceto o plano de fundo, cuja origem é do autor.
