# Projeto Android — Launcher HY300

**Versão 0.4.** Três apps: `app` (launcher), `games` (Jogos HY300) e `tv` (TV AO VIVO). Foi escrita sem compilador à disposição, então **pode haver erros de compilação**. Se aparecerem, copie a mensagem completa da aba *Build* do Android Studio e envie.

## O que já faz
- Tela inicial com o fundo, o topo (Rede, Bluetooth, engrenagem e hora), o título e a descrição do item em foco e a fileira de atalhos.
- **Até 7 atalhos de apps com o visual do próprio app** (o banner de TV, ou o ícone se não houver banner). Por padrão, se estiverem instalados: Netflix, YouTube, TV AO VIVO, Unit TV, Pluto TV, Mercado Play e Aptoide TV.
- **Atalhos fixos:** Jogos (ainda "em breve"), Arquivos, HDMI e Gerenciar atalhos.
- **Engrenagem:** menu que abre as telas originais do sistema.
- **Gerenciar atalhos:** lista de apps instalados; OK adiciona ou remove (limite de 7).

Ainda **não** tem: as telas próprias de Configurações do protótipo, a tela de Jogos e a atualização pelo GitHub.

## Passo a passo no Android Studio
1. **Abrir:** *File > Open* e escolha esta pasta `android` (a que tem o `settings.gradle.kts`). Se perguntar, marque *Trust Project*.
2. **Aguardar o Gradle Sync.** Avisos comuns:
   - *"Failed to find Platform SDK with path: platforms;android-34"* ou um link *Install missing SDK*: clique para instalar (ou *Tools > SDK Manager > SDK Platforms > Android 14 (API 34)*).
   - Sugestão de **atualizar o Android Gradle Plugin / Gradle** (Upgrade Assistant): pode aceitar.
   - Erro de JDK: *Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK* e escolha o JDK embutido do Android Studio (`jbr`).
3. **Conectar ao projetor:** abra a aba *Terminal* do Android Studio e rode `adb connect 192.168.1.13:5555` (use o IP atual do projetor). Ele aparece na lista de dispositivos no topo. Se o `adb` não for reconhecido, use `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb`. Se aparecer erro de versão do servidor do adb, rode `adb kill-server` e conecte de novo.
4. **Executar:** botão verde ▶ (*Run 'app'*). O Android Studio instala e abre o launcher no projetor.
5. **Tornar padrão:** aperte o botão **Home** do controle. O Android pergunta qual tela inicial usar: escolha *Launcher HY300* e *Sempre*.

## Voltar ao launcher original
O original continua instalado. Para restaurar o padrão (comando **ainda não testado**):

```
adb shell cmd package set-home-activity com.htc.htclauncherhighenglishd08/com.htc.activity.HomeActivity
```

Ou desinstale o novo: `adb uninstall br.hy300.launcher`.

## Gerar o APK
*Build > Build Bundle(s) / APK(s) > Build APK(s)*. O arquivo sai em `app/build/outputs/apk/debug/app-debug.apk`. Instale com `adb install -r app-debug.apk`. Esse APK é de **teste**. Para versões publicadas, crie a chave e o `keystore.properties` (veja `docs/12-atualizacao-do-launcher-pelo-github.md` e `keystore.properties.example`).

## O que conferir no aparelho
- Navegação com o direcional (foco visível em cada item) e o som de navegação. Em teoria o Android toca o som sozinho quando "sons do sistema" está ligado; **a confirmar**.
- Se cada app mostra o banner ou o ícone certo. Para usar sempre o ícone, troque `PREFER_BANNER` para `false` em `AppRepo.kt`.
- Se **Arquivos** abre o gerenciador de arquivos, se **HDMI** abre a entrada e se a engrenagem lista as telas.
- Se Netflix e YouTube (apps de TV) aparecem na lista de *Gerenciar atalhos*.


## App TV AO VIVO (módulo `tv`)
No topo do Android Studio, escolha **tv** ao lado do ▶ e execute. O app abre em tela cheia com a lista de canais, no mesmo visual do launcher.

**Como ele recebe os canais** (tudo pelo controle, sem teclado):
1. Desça até **⚙ Fonte**, a última linha das categorias, e aperte OK.
2. Escolha uma das três fontes:
   - **Arquivo .m3u** — a lista precisa estar em `Download/tvaovivo.m3u` do projetor. Pelo PC: `adb push lista.m3u /sdcard/Download/tvaovivo.m3u`.
   - **Endereço (URL)** — cole o link da lista (o teclado aparece na tela).
   - **Xtream Codes** — servidor, usuário e senha; a lista é montada sozinho.
3. **Testar e salvar**. A tela mostra quantos canais vieram e quantas categorias.

Também dá para mandar a lista sem digitar nada: com o projetor conectado, rode
`adb shell am start -a android.intent.action.VIEW -d "http://.../lista.m3u" -n br.hy300.tv/.TvHomeActivity`.
O app salva o link como fonte, baixa e já abre com os canais.

**Filtro 4K/8K:** ligado por padrão. O projetor entrega 720p, então SD e HD ficam e só 4K/8K (UHD, 2160p, 4320p) saem da lista. Se a lista for toda 4K, o app avisa e mostra tudo. O botão **Canais 4K/8K: Removidos/Mantidos** fica na mesma tela de fonte.

**No controle:** ▲▼ navega, OK abre o canal, ◀▶ troca de coluna, números sintonizam direto (ex.: 1 e 2 vai para o canal 12), CH+ / CH- passam canal. No player, OK abre a lista de canais; dentro dela as duas primeiras linhas são **Imagem** (Preencher/Ajustar/16:9/4:3) e **Favoritar canal**. Segurar OK na lista da tela inicial também favorita. A tecla MENU não é usada: o sistema do projetor captura essa tecla e abre as configurações.

## App Jogos HY300 (módulo `games`)
No topo do Android Studio, escolha **games** ao lado do ▶ e execute para instalá-lo. Detalhes e fluxo: `docs/14-app-jogos.md`.
Para enviar o jogo: rode `scripts/05-enviar-jogos.ps1` (envia para `/storage/emulated/0/Jogos/` e libera o acesso do app a essa pasta).
