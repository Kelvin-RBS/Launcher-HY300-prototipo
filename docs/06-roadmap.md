# Roadmap e pendências

## Decisões pendentes
- Apps fora dos 5 atalhos não têm porta de entrada: decidir se haverá "Todos os apps" em algum lugar.
- Reordenar atalhos (hoje só adiciona e remove).
- Ajustes do design que o autor vai enviar.
- Lista final de cartões fixos (incluir Pluto TV e Mercado Play?).
- Fundo fixo ou com opção de trocar o papel de parede.

## Próximos passos
0. (Futuro) Área de jogos de 16 bits com RetroArch: ver `08-jogos-retroarch.md`.
1. Aplicar as alterações de design no protótipo.
2. Escrever o projeto Android (Kotlin, sem bibliotecas pesadas, categoria HOME/LEANBACK).
3. Implementar a navegação por foco e o som de troca.
4. Ligar os atalhos às telas do mapa de funções (`04-mapa-de-funcoes.md`).
5. Testar no aparelho: instalar via `adb install`, definir como padrão, validar HDMI e Projeção.
6. Plano de volta: `adb shell pm clear` do launcher novo ou desinstalar. O original nunca é removido.

## Pontos a investigar
- Se os broadcasts `intent.start.*` abrem as telas (e quais).
- Se existe parâmetro para abrir o keystone direto pela `ProjectionActivity`.
- Nomes exatos das activities de Wi-Fi, Bluetooth, som etc.
- Nome da activity do gerenciador de arquivos (`TvdFileManager`) e da tela principal do `htcsettings`.
- (Feito para o `htcsettings`) Descompilar o `htcsettings` com JADX (instruções de configuração do `REA_JADX_MCP_JAR`: JDK 17+, JAR `jadx-headless-mcp` 0.7.1 e variável de ambiente definida antes de abrir o programa que roda o rea).

## Atualização do launcher
Plano em `12-atualizacao-do-launcher-pelo-github.md`: o launcher se atualiza por Releases do GitHub, sem mexer no firmware. Decisão: **continuar pelo launcher por enquanto**, por ser mais seguro.

## Futuro: firmware próprio
A ideia de longo prazo é **trocar o firmware** do projetor. Por enquanto o launcher é instalado como app e o original continua no aparelho.
Antes de qualquer troca:
- Guardar uma **cópia do firmware original** e anotar versão e build (`UI_Veng.projector.20250221.201033`).
- Descobrir se existe **modo de recuperação** para o chip (em placas Allwinner costuma existir um modo próprio de gravação, mas isso precisa ser pesquisado para o HY300).
- Entender que trocar firmware **pode inutilizar o aparelho**. Só avançar com um plano de volta.
