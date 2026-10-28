# Projeto Android: arquitetura e decisões

Código em `android/` (Kotlin). Como abrir e instalar: `android/README.md`.

## Arquivos
| Arquivo | Função |
|---|---|
| `HomeActivity.kt` | Tela inicial: fundo, topo, título do item em foco e fileira de atalhos |
| `games/` (módulo) | App **Jogos HY300** (`br.hy300.games`): lista de jogos, tela de controle, configurações e entrega da ROM ao emulador. Ver `14-app-jogos.md` |
| `AppsActivity.kt` | Configurações > Aplicativos: todos os apps; abre (OK) e desinstala (segurar OK por 2 s) |
| `ManageActivity.kt` | Tela "Gerenciar atalhos" (escolha dos até 7 apps) |
| `AppRepo.kt` | Lista os apps instalados (comuns e de TV), guarda os atalhos escolhidos e define os padrões |
| `Util.kt` | Conversão de dp |
| `res/drawable/ic_*.xml` | Ícones vetoriais (Wi-Fi, Bluetooth, engrenagem, controle, pasta, HDMI, ajustes) |

## Decisões
- **Sem bibliotecas externas** (nem AndroidX): APK pequeno e rápido para um aparelho `low_ram`. A interface é montada em código.
- **`minSdk` e `targetSdk` 30** (Android 11, o do projetor). O `compileSdk` é 34 só para compilar.
- **Ícone do app:** cada atalho usa o **banner de TV** do app quando existir (formato próprio para cartões, como o do YouTube e da Netflix) e, se não houver, o **ícone**. Chave `PREFER_BANNER` em `AppRepo.kt`.
- **Apps de TV:** a busca usa as duas categorias, `LEANBACK_LAUNCHER` e `LAUNCHER`, porque muitos apps de TV só têm a primeira. A abertura prefere a intent de TV.
- **Visibilidade (Android 11):** o manifest declara `<queries>` para enxergar os outros apps e os pacotes do sistema que usamos, sem pedir a permissão ampla de listar todos os apps.
- **Foco e som:** usa o foco nativo do Android com o direcional. O som de navegação deve vir do sistema (a confirmar no aparelho).
- **Atalhos salvos** em `SharedPreferences` (lista de pacotes). Se a lista nunca foi editada, vale o padrão.

## Pacotes e telas do sistema usados
`com.htc.htcsettings` (telas de Configurações, nomes confirmados no manifest), `com.softwinner.awlivetv` (HDMI, testado por adb), `com.softwinner.TvdFileManager` (Arquivos, aberto pela intent principal do pacote; a confirmar).

## Próximos passos
1. Compilar, corrigir erros e testar no projetor.
2. Telas próprias de Configurações (como no protótipo), abrindo as originais quando o ajuste exigir permissão de sistema.
3. Testar o app Jogos (`14`) e criar o motor de emulação próprio (fase 2).
4. Atualização do launcher pelo GitHub (`12`).
