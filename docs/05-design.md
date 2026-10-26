# Design

## Decisões
- **Tela de 1280×720**, controle por **direcional + OK** (sem toque). Foco sempre visível.
- Estrutura: título e descrição do item em foco, fileira "Favoritos" com cartões coloridos, cartão em foco ampliado com borda clara. Relógio e ícones de status no topo.
- **Plano de fundo:** paisagem de lago ao pôr do sol (imagem gerada por IA), com degradê escuro à esquerda e embaixo para manter o texto legível.
- **Poucos elementos**, tipografia grande (Roboto) e animações curtas: o hardware é limitado.
- **Fileira de atalhos:** até **7 atalhos editáveis** à esquerda, **somente de apps instalados**, seguidos de **4 atalhos fixos**, que não podem ser removidos nem movidos: **Jogos** (ícone de controle estilo Super Nintendo), **Arquivos**, **HDMI**, **Gerenciar aplicativos** e **Gerenciar atalhos**. Os fixos não têm cadeado nem marca especial.
- **Topo:** **Rede**, **Bluetooth** e **Configurações** (engrenagem), todos focáveis. Rede e Bluetooth abrem a página correspondente das Configurações; a engrenagem abre a lista completa. O relógio segue o fuso e o formato de hora escolhidos.
- **Arquivos:** abre o gerenciador de arquivos do sistema (`com.softwinner.TvdFileManager`, activity a confirmar), com armazenamento interno e pendrive quando conectado (tecla **P** simula no protótipo).
- **Gerenciar atalhos:** tela com os 5 espaços e os apps disponíveis. Enter adiciona ou remove; o limite de 5 gera aviso.
- **Configurações:** ver `07-configuracoes.md`.

## Som de navegação
Um tique curto (desligável em Configurações > Áudio, não mais na tela inicial) a cada troca de foco, mais agudo para a direita e mais grave para a esquerda, um som abafado no fim da fileira e um toque mais grave ao abrir. No protótipo é sintetizado (aprox. 50 ms). No aparelho, as opções são o som de navegação do Android (`sound_effects_enabled=1` já está ativo) ou um arquivo curto próprio com `SoundPool`.

## Fora do escopo (de propósito)
Bateria, rastreamento, loja da fabricante, OTA/recovery, controle de energia.
