# Configurações do launcher novo

Tela com menu à esquerda e conteúdo à direita, navegada pelo direcional. Esc volta um nível.

| Página | Conteúdo | Como funciona no aparelho |
|---|---|---|
| **Rede** | Wi-Fi, rede atual, buscar redes | Abre a tela original (`WIFIActivity`). Mostrar a rede atual é nativo |
| **Bluetooth** | Liga/desliga, pareados, buscar | Abre a tela original (`BluetoothSettingActivity`) |
| **Idioma** | Idioma do aparelho e método de entrada | Abre a tela original (`LanguageActivity`) |
| **Projeção** | Zoom da tela; método de projeção (projeção desktop, projeção traseira de mesa, elenco de içamento, projeção traseira); correção de keystone; brilho e cor (modo de imagem, brilho, contraste, tonalidade, saturação, nitidez) | Exige permissão de sistema (API oculta `PrjScreen`). Abre a `ProjectionActivity` original. Confirmado pelo REA: **Zoom, Método de projeção e Keystone não são exportadas**, então só dá abrir a Projeção. **Brilho e cor** (`PictureModeActivity`) abre direto |
| **Áudio** | Volume do projetor; **bip ao mover o cursor** | Volume via `AudioManager` (nativo). O bip pode ser uma preferência do próprio launcher ou a configuração `sound_effects_enabled` do sistema (o original usa essa; alterá-la exige a permissão "modificar configurações do sistema") |
| **Data e hora** | Atualização automática, fuso horário, formato de hora e de data | Abre `DateTimeActivity`; o fuso horário tem tela própria exportada (`TimeZoneActivity`). Alterar formatos pelo launcher pode exigir a permissão "modificar configurações do sistema" |
| **Sobre o dispositivo** | Modelo, versão do Android, número da versão, número de série, endereço MAC, armazenamento e RAM (total e livre), atualização local e online | Modelo, Android, versão, armazenamento e RAM: nativo (`Build`, `StatFs`, `ActivityManager`). **Serial e MAC: leitura por app comum **a testar** no Android 11 (pode ser bloqueada)**, então o plano é abrir `AboutDeviceActivity` |

## Pontos de atenção
- **O protótipo mostra a experiência desejada.** Parte dos ajustes só funciona de verdade abrindo a tela original, porque um app comum não tem permissão de sistema para alterá-los.
- **Atualização local e online:** abrir as telas originais `SystemUpdateActivity` (local, por pendrive) e `UpdateActivity` (online), sempre com aviso antes. Não chamar recovery nem OTA direto. Detalhes em `11-atualizacao-e-sobre.md`.
- **Dados reais do aparelho usados no protótipo:** modelo `adt3` (nome `HY300`), Android 11, versão `UI_Veng.projector.20250221.201033`. Armazenamento e RAM no protótipo são **exemplos**.
- **Rótulos dos métodos de projeção** seguem o texto da tela original; "elenco de içamento" parece tradução literal e pode ser ajustado.
