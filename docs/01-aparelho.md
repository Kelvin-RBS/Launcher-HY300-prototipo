# O aparelho

| Item | Valor |
|---|---|
| Modelo | HY300 (`device_name=HY300`; o sistema se declara "ADT-3 / askey") |
| SoC | Allwinner H713 (`sun50iw12p1`) |
| Android | 11, 32 bits (armeabi-v7a), modo `low_ram` |
| Tela | 1280×720, 240 dpi (cerca de 853×480 dp) |
| Portas físicas | 1 HDMI, 1 USB, 1 saída de fone P2 (3,5 mm), botão liga/desliga, botão reset, entrada de energia AC |
| Bateria | Não tem (só tomada) |
| Controle remoto | Power, fonte, espelhamento, mudo, modo mouse, direcional + OK, voltar, menu, home, volume +/- |

## Pontos de atenção
- O botão **fonte** do controle abre apenas o menu de desligar/reiniciar do sistema.
- Com o aparelho ligado na tomada e "desligado", um sensor de infravermelho continua ativo e liga o projetor pelo controle. É o modo de espera do sistema: **o launcher não deve mexer em energia nem em teclas de energia**.
- O sistema declara HDMI1, HDMI2, HDMI3 e CVBS, mas só há uma porta HDMI física.
