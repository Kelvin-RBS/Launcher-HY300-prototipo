# Atualização do sistema e "Sobre o dispositivo" (REA + JADX)

Análise estática do `com.htc.htcsettings` (v1.6.8). O app não foi executado: tudo abaixo é leitura de código e precisa de teste no aparelho antes de virar funcionalidade.

## Atualização local (pendrive)
- Tela: `SystemUpdateActivity` (exportada). O botão `udiskupgrade_btn` faz o trabalho.
- O que ele faz: envia o broadcast `android.hotack.update.service` para o componente `android.rockchip.update.service/.RKUpdateReceiver` e mostra um aviso de "verificando". **Quem procura o pacote e aplica a atualização é um serviço de sistema**, não o `htcsettings`. Esse pacote aparece na lista de apps do projetor.
- O app também referencia `RecoverySystem`, `update.zip` e `update_signed.zip`: o fluxo termina reiniciando no recovery.
- Existe um desvio para um modelo específico (`hy320_zh_yidong`), que abre `com.softwinner.update.MainActivity`. Não sabemos se é o caso do seu aparelho.
- **Recomendação:** o launcher novo deve **abrir a `SystemUpdateActivity` original** e deixar o usuário tocar no botão, com um aviso antes. Disparar o broadcast direto exigiria que o receiver aceite chamadas de fora, o que não foi verificado, e atualizar firmware tem risco.

## Atualização online
- Tela: `UpdateActivity` (exportada). Usa uma thread, diálogos e o receiver `UpdateOtaReceiver` (ação `ota_install_htc`).
- O app referencia o pacote `com.htc.htcotaupdate`, o arquivo `ota_file` e as ações `android.hotack.copy.ota` e `android.hotack.copy.close`. A consulta ao servidor **não está no `htcsettings`**: deve estar no `com.htc.htcotaupdate`, ainda não analisado.
- **Recomendação:** abrir a `UpdateActivity` original. Quando o firmware próprio chegar, avaliar se a atualização online da fabricante deve continuar existindo.

## Sobre o dispositivo: de onde vêm os dados
| Dado | Fonte no original | No launcher novo |
|---|---|---|
| RAM total e livre | `/proc/meminfo` (`MemTotal`) e `availMem` | Nativo (`ActivityManager`), sem permissão especial |
| Armazenamento total e livre | `StatFs` | Nativo |
| Número de série | propriedade `ro.serialno` | **A testar**: o Android 11 pode bloquear a leitura por app comum |
| MAC com fio | `/sys/class/net/eth0/address` | **A testar**: leitura de `/sys` costuma ser bloqueada |
| MAC Wi-Fi | `getMacAddress` | **A testar**: apps comuns costumam receber um valor fictício |

Se o teste falhar, o plano é abrir a `AboutDeviceActivity` original para essas linhas.

## Achado de privacidade
O próprio `htcsettings` contém o analytics da Baidu (`hmma.baidu.com`, `dxp.baidu.com`), como o launcher original. Abrir as telas dele não é problema para o launcher novo, mas esse rastreamento continua no sistema. Só uma troca de firmware remove.

## Próximas análises
`com.htc.htcotaupdate` (servidor e fluxo da atualização online) e `com.softwinner.awlivetv` (entrada HDMI).
