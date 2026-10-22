# Análise do `com.htc.htcsettings` (REA + JADX)

APK v1.6.8 (`versionCode 25011314`), 554 classes. Analisado com o módulo Android do REA, com o JADX configurado. A análise é **estática**: o app não foi executado, e os nomes das classes são ofuscados em parte.

## Como a tela de Projeção funciona
A `ProjectionActivity` monta uma lista de cartões. Cada cartão tem uma chave de texto, e um listener interno decide o que abrir. **A activity não lê extras de intent para abrir um cartão direto.**

| Chave | O que faz | Exportada? |
|---|---|---|
| `screenZoom` | Abre `ScreenZoomActivity` (zoom da tela) | **Não** |
| `projectMode` | Abre `ProjectionModeActivity` (método de projeção) | **Não** |
| `trapezoid` | Abre `CorrectionIndexActivity` (keystone) | **Não** |
| `brightAndColor` | Abre `PictureModeActivity` (brilho e cor) | **Sim** |
| `colorTemp` | Abre `ColorTemperatureActivity` | Não |
| `brightness` | Abre `BrightnessActivity` | Sim |
| `screenSaver` | Abre `ScreenSaverActivity` | Sim |
| `pointSpeed` | Abre `PointerSpeedActivity` (velocidade do ponteiro) | Não |
| `correctionSettings` | Abre `CorrectActivity` (tela interativa de correção; grava arquivo) | Sim |
| `fanLevel` | Diálogo que chama `PrjScreen.set_fan_level` (nível da ventoinha) | n/a |
| `fanBrightness` | Diálogo (não analisado) | n/a |
| `soundEffect` | Alterna a configuração do sistema `sound_effects_enabled` (1 ou 0) | n/a |

Quais cartões aparecem depende da variante da tela (`ProjectionActivity`, `ProjectionActivity3` e `ProjectionActivity4`).

## O que isso muda para o launcher novo
1. **Abre direto:** Projeção (tela completa), **Brilho e cor** (`PictureModeActivity`), brilho e protetor de tela.
2. **Não abre direto (não exportadas):** Zoom da tela, Método de projeção e **Keystone**. O caminho é abrir a Projeção e o usuário escolher o cartão com o direcional. Alternativas: um serviço de acessibilidade que clica sozinho (o usuário precisa ativá-lo; frágil) ou, no futuro, um firmware próprio.
3. **Correção:** o teste antigo de `KeystonecorrectionActivity` usou uma classe que **não é** a do cartão de keystone. O cartão chama `CorrectionIndexActivity`. As duas não são exportadas.
4. **Bip do cursor:** o original liga e desliga a configuração `sound_effects_enabled` do sistema. O launcher novo pode manter um bip próprio (sem permissão) ou alterar a do sistema, o que exige a permissão especial "modificar configurações do sistema", concedida pelo usuário. Hoje o valor é 1 (ligado).
5. **Ventoinha:** existe um ajuste de nível da ventoinha (`PrjScreen.set_fan_level`). Não está no protótipo.
6. **`PrjScreen`** é uma classe do sistema. Não foi verificado se um app comum conseguiria chamá-la.

## Outras páginas de Configurações (manifest)
Exportadas, abrem direto: `WIFIActivity`, `BluetoothSettingActivity`, `LanguageActivity`, `LanguageListActivity`, `DateTimeActivity`, **`TimeZoneActivity`** (fuso horário), `AboutDeviceActivity`, `SoundChannelActivity`, `HotspotActivity`, `OtherSettingsActivity`, `UpdateActivity` e `SystemUpdateActivity` (o que cada uma faz ainda não foi lido).

Não exportadas: `VolumeActivity` e `AudioActivity`. O volume pelo launcher usará `AudioManager`.

## Cuidado
`CorrectActivity` é exportada, mas abre uma tela interativa de correção que grava arquivo. **Ainda não foi aberta no aparelho.** Teste só quando você quiser, sabendo que pode alterar a calibração.

## Análises seguintes
Atualização e "Sobre": ver `11-atualizacao-e-sobre.md`. Pendente: `com.softwinner.awlivetv` (entrada HDMI) e `com.htc.htcotaupdate`.
