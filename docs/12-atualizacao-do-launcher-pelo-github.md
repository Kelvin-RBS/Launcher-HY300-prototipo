# Atualização do launcher pelo GitHub

Plano para o **launcher** (um app comum) se atualizar sozinho a partir do GitHub, **sem mexer no firmware**. Ainda não está implementado: o projeto Android não foi escrito.

## Visão geral
1. Você publica uma **Release** no GitHub com o APK anexado.
2. Você atualiza um arquivo pequeno, o `latest.json`, publicado no GitHub Pages.
3. O launcher consulta o `latest.json` (ao abrir e no máximo uma vez por dia). Se a versão for maior que a instalada, mostra um aviso: um ponto na engrenagem e uma linha em Configurações > Sobre o dispositivo.
4. O usuário confirma. O launcher baixa o APK, confere o SHA-256 e abre o instalador do Android, que pede a confirmação final.

Não precisa de servidor. O GitHub **não** avisa o aparelho: é o aparelho que consulta.

## O `latest.json`
Endereço (depois de ativar o Pages): `https://kelvin-rbs.github.io/Launcher-HY300-prototipo/latest.json`

Exemplo em [`exemplos/latest.example.json`](exemplos/latest.example.json). Campos:

| Campo | Uso |
|---|---|
| `versionCode` | Número inteiro que **sempre aumenta**. É o que o launcher compara |
| `versionName` | Texto da versão, como `0.2.0` |
| `apkUrl` | Endereço do APK na Release |
| `sha256` | Soma de verificação do APK |
| `sizeBytes` | Tamanho, para mostrar ao usuário |
| `minAndroid` | API mínima (30 = Android 11) |
| `publishedAt` | Data da publicação |
| `notes` | O que mudou, em português |

Use o Pages (arquivo estático) e não a API do GitHub para a consulta, porque a API sem login tem limite de 60 consultas por hora. Os arquivos de uma Release precisam ter menos de 2 GiB, folga enorme para um launcher.

## Como publicar uma versão
1. Aumente `versionCode` e `versionName` no projeto e gere o APK **assinado** com a sua chave.
2. Crie uma Release com a tag `v0.2.0` e anexe o APK com o nome `launcher.apk`.
3. Na pasta do repositório, rode `scripts/04-gerar-latest-json.ps1` (ele calcula o SHA-256 e escreve o `latest.json`).
4. Faça `git add`, `commit` e `push`. O Pages publica o arquivo em instantes.

## Criar a chave de assinatura (uma vez só)
A chave **ainda não existe**: você a cria. O JDK já traz a ferramenta `keytool`. No PowerShell, numa pasta **fora do repositório**:

```
keytool -genkeypair -v -keystore launcher-hy300.jks -alias launcher -keyalg RSA -keysize 4096 -validity 10000
```

Ele pede uma senha para o arquivo, outra para a chave e alguns dados do certificado (nome etc., pode ser básico). O resultado é o arquivo `launcher-hy300.jks`. Se o `keytool` não for reconhecido, use o caminho completo da pasta `bin` do JDK (em `Program Files\Eclipse Adoptium\...`). No Android Studio, o equivalente é *Build > Generate Signed App Bundle / APK > Create new*.

- **Onde guardar:** numa pasta sua fora do projeto, por exemplo `C:\Chaves-HY300\`, com **pelo menos duas cópias** (por exemplo, um pendrive e um cofre de senhas).
- **Anote** o alias (`launcher`) e as duas senhas em um gerenciador de senhas. Sem elas o arquivo não serve.
- **Não use a chave de debug** do Android Studio (`.android\debug.keystore`) em versões publicadas: se você trocar de chave depois, os aparelhos não conseguem atualizar.
- O projeto vai ler o caminho e as senhas de um `keystore.properties` local. O `.gitignore` já bloqueia `*.jks`, `*.keystore` e esse arquivo.

## Segurança
- **O Android só aceita atualizar um app com um APK assinado pela mesma chave.** Por isso a chave (keystore) é a peça mais importante: **guarde cópias fora do GitHub e nunca a publique.** Se ela for perdida, o launcher instalado não pode mais ser atualizado, só desinstalado e reinstalado.
- O SHA-256 protege contra download corrompido. A proteção contra um APK malicioso é a assinatura.
- Ative a **verificação em duas etapas** na sua conta do GitHub.
- A consulta revela o endereço IP do aparelho ao GitHub e nada além disso. Deixe um **interruptor** para desligar a verificação automática.

## Como o launcher instala (a validar na implementação)
- Baixar para a pasta do próprio app, conferir o SHA-256 e entregar ao instalador do Android (`PackageInstaller` ou `ACTION_VIEW` com `FileProvider`).
- Precisa da permissão `REQUEST_INSTALL_PACKAGES`, e o usuário precisa liberar "instalar apps de fontes desconhecidas" para o launcher, uma vez.
- A instalação **sempre pede confirmação**: um app comum não instala sozinho, e isso é o desejado por segurança.

## Depois, com firmware próprio
A mesma estrutura pode atualizar o sistema com um atualizador privilegiado. Esse é outro projeto, com riscos maiores (assinatura do pacote, recuperação, remoção do atualizador da fabricante). Ver `06-roadmap.md`.
