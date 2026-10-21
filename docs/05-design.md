# Design

## Decisões
- **Tela de 1280×720**, controle por **direcional + OK** (sem toque). Foco sempre visível.
- Estrutura: título e descrição do item em foco, fileira "Favoritos" com cartões coloridos, cartão em foco ampliado com borda clara. Relógio e ícones de status no topo.
- **Plano de fundo:** paisagem de lago ao pôr do sol (imagem do autor), com degradê escuro à esquerda e embaixo para manter o texto legível.
- **Poucos elementos**, tipografia grande (Roboto) e animações curtas: o hardware é limitado.
- **Cartões:** HDMI, Projeção, Imagem, Netflix, YouTube, Espelhar, Apps, Ajustes.

## Som de navegação
Um tique curto a cada troca de foco, mais agudo para a direita e mais grave para a esquerda, um som abafado no fim da fileira e um toque mais grave ao abrir. No protótipo é sintetizado (aprox. 50 ms). No aparelho, as opções são o som de navegação do Android (`sound_effects_enabled=1` já está ativo) ou um arquivo curto próprio com `SoundPool`.

## Fora do escopo (de propósito)
Bateria, rastreamento, loja da fabricante, OTA/recovery, controle de energia.
