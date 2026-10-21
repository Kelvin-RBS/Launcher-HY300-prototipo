# Mapa de funções

Estado: **Verificado** = testado no aparelho via ADB · **Listado** = visto no manifest/dump, não testado · **Bloqueado** = testado e recusado.

| Função | Como acessar | Estado |
|---|---|---|
| HDMI | `com.softwinner.awlivetv/.MainActivity` | Verificado (abriu a tela de HDMI, "sem sinal") |
| Projeção (zoom, método de projeção, keystone, brilho e cor) | `com.htc.htcsettings/com.htc.activity.ProjectionActivity` | Verificado |
| Modo de imagem (brilho, contraste, tonalidade, saturação, nitidez) | `com.htc.activity.PictureModeActivity` | Verificado |
| Keystone direto | `com.htc.activity.KeystonecorrectionActivity` | **Bloqueado** (`Permission Denial`, não exportada). Caminho: Projeção → Correção de Keystone |
| Entrada de sinal (htcsettings) | `com.htc.activity.SignalActivity` | Abre painel só com "Fechar". Sem utilidade por ora |
| Broadcasts `intent.start.projectton` e `intent.start.signal` | `am broadcast -a ...` | Entregues (`result=0`). Efeito **não confirmado** |
| Wi-Fi, Bluetooth, som, idioma, data, brilho, hotspot, protetor de tela, outros ajustes, sobre | `WIFIActivity`, `BluetoothSettingActivity`, `SoundChannelActivity`, `LanguageActivity`, `DateTimeActivity`, `ScreenBrightnessActivity`, `HotspotActivity`, `ScreenSaverActivity`, `OtherSettingsActivity`, `AboutDeviceActivity` | Listado (exportadas). Nomes exatos a confirmar |
| Espelhamento | `com.softwinner.miracastReceiver` | Listado |

## Não usar (podem alterar calibração ou apagar dados)
`FactoryResetActivity`, `CorrectionDefaultActivity` (restaura correção de fábrica), `CorrectActivity`, `CorrectionActivity`, e `com.softwinner.settingsassist` (OTA e recovery).

## Apps citados
- `com.softwinner.vis`: serviços de entrada de TV (HDMI/CVBS) do Android TV Input Framework.
- `com.hysd.vafocus`: foco automático (referenciado no `htcsettings`).
- Como o keystone funciona por dentro: classe oculta `android.os.PrjScreen`, arquivos do sistema (`manual_Keystone_mode`, `motor_ctrl`) e o broadcast `android.intent.hotack_keystone`. Um app comum **não consegue** mexer nisso, por isso o launcher abre as telas originais.
