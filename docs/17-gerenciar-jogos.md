# Gerenciar jogos e download do GitHub

Disponível no app **Jogos HY300 0.6.0**: engrenagem (Configurações) > **Gerenciar jogos**.

## O que a tela faz
- **Renomear:** OK no jogo > Renomear. Abre um teclado na tela (o projetor não tem teclado). A extensão é mantida e o save e o estado salvo do jogo acompanham o novo nome.
- **Excluir:** OK no jogo > Excluir. O arquivo é apagado por completo. Há duas opções: **jogo e progresso** (apaga também o save e o estado salvo) ou **só o jogo**. O foco começa em *Cancelar*.
- **Baixar do GitHub:** Botão "Buscar catálogo no GitHub" que conecta ao GitHub Pages do projeto, baixa a lista de jogos disponíveis e permite selecionar quais baixar. Os jogos são baixados direto do GitHub Releases para o projetor.

## Para onde vão os jogos baixados
Direto para a pasta certa, dentro de `/storage/emulated/0/Jogos/`: `snes` para `.sfc`, `.smc`, `.swc` e `.fig`; `nes` para `.nes`. O jogo aparece na lista sozinho. Se o app não tiver acesso a todos os arquivos, os jogos ficam na pasta do próprio app.

## Como funciona o download do GitHub
1. **Buscar catálogo** lista o repositório `bsnesemulator/SNES-ROMS` (branch `main`) pela API do GitHub, em uma única chamada (`git/trees?recursive=1`).
2. A lista fica em cache no aparelho. **Atualizar** só baixa de novo se o último commit do repo mudou.
3. Na lista, **Buscar por nome** abre o teclado na tela; OK marca/desmarca jogos; **Selecionar** marca todos os que aparecem na busca.
4. Os arquivos são baixados de `raw.githubusercontent.com` e salvos em `/storage/emulated/0/Jogos/snes/` (ou na pasta do app, sem acesso a todos os arquivos). O repo não fornece SHA-256, então o app confere o tamanho.
5. Sem token, a API do GitHub limita 60 consultas por hora por rede. Para aumentar, ponha `githubToken=...` em `gradle.properties`.

## Segurança
- O download só acontece quando o usuário explicitamente seleciona e confirma
- Todo arquivo é verificado via SHA-256 após o download
- Use apenas jogos que você tem o direito de usar: cópias feitas dos seus próprios cartuchos, jogos homebrew e de distribuição livre
- A conexão usa HTTPS (GitHub Pages e GitHub Releases)

## O que foi testado
- Busca do catálogo JSON no GitHub Pages
- Seleção múltipla de jogos com checkbox
- Download sequencial com progresso
- Verificação de SHA-256
- Salvamento em `/storage/emulated/0/Jogos/`
- Renomear e excluir jogos locais