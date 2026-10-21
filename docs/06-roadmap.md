# Roadmap e pendências

## Decisões pendentes
- Ajustes do design que o autor vai enviar.
- Lista final de cartões fixos (incluir Pluto TV e Mercado Play?).
- Fundo fixo ou com opção de trocar o papel de parede.

## Próximos passos
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
- Descompilar o `htcsettings` com JADX (instruções de configuração do `REA_JADX_MCP_JAR`: JDK 17+, JAR `jadx-headless-mcp` 0.7.1 e variável de ambiente definida antes de abrir o programa que roda o rea).
