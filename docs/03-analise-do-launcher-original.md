# Análise do launcher original

**Pacote:** `com.htc.htclauncherhighenglishd08` · **Versão:** 1.4.5 · APK de 3,9 MB (2,8 MB de código).

## O que ele é
Uma grade de apps com ícones de status. Telas: `HomeActivity`, `AppsActivity`, `AppFavoritesActivity`, `GuideActivity`, `SignalActivity`, `UsbActivity`, `LowBatteryActivity`, `WeiDongActivity`. Receivers de Wi-Fi, Bluetooth, Ethernet, USB, bateria, hora e instalação de apps. Lê arquivos `hideApp.ini` para esconder apps.

## Privilégios
Roda como app de sistema (`sharedUserId=android.uid.system`, assinado com a chave da fabricante). Por isso **não pode ser substituído** por um app comum, mas um launcher novo pode ser instalado ao lado e definido como padrão.

Ele **não** tem permissão para keystone, foco ou imagem. Isso fica em outros apps (ver `04-mapa-de-funcoes.md`).

## Peso
Quase todo o código são bibliotecas (Jackson, Glide, OkHttp, Baidu), pesadas para um aparelho em modo `low_ram`.

## Rastreamento e privacidade (a partir das strings do código e do manifest)
- Analytics da Baidu (`hmma.baidu.com`, `dxp.baidu.com`), configurada para coleta de localização por GPS, Wi-Fi e rede celular.
- API de loja da fabricante (`store-api.aodintech.com`).
- Apps relacionados: `com.hotack.silentsdk` (usuário) e `com.htc.eventuploadservice` (sistema).
- ID da Baidu gravado nas configurações do sistema (`com.baidu.deviceid.v2`).

> Pelas strings não dá para afirmar **o que** é enviado, só que o código existe. O launcher novo não inclui nada disso.

## O que não foi analisado
O código do **launcher original** ainda não foi descompilado: a análise dele usou manifest, strings do `classes.dex` e dumps do `dumpsys`. O JADX do REA já está configurado e foi usado no `htcsettings` (ver `10-analise-htcsettings.md`).
