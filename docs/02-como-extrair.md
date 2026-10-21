# Como extrair os dados do aparelho (ADB)

1. No projetor, ative as opções de desenvolvedor (toque 7 vezes em *Número da versão*) e a **Depuração USB**.
2. Descubra o IP em *Configurações > Wi-Fi*.
3. Baixe o *SDK Platform-Tools* do Google e abra o PowerShell **dentro da pasta `platform-tools`** (o ZIP cria uma subpasta).
4. Conecte: `.\adb connect IP:5555` e aceite o aviso na tela do projetor.
5. Rode os scripts de `scripts/`.

## Armadilhas encontradas
- No PowerShell é preciso `.\adb`, e não `adb`.
- `comando > arquivo.txt` no PowerShell gera **UTF-16**. Use `cmd /c ".\adb ... > arquivo.txt"` para sair em UTF-8.
- Imagens (`screencap`) também precisam de `cmd /c`, senão o PowerShell corrompe o PNG.
- O caminho do APK tem `~~` e `==`: use aspas.
