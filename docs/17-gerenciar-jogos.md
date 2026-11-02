# Gerenciar jogos e envio pela rede

Disponível no app **Jogos HY300 0.5.0**: engrenagem (Configurações) > **Gerenciar jogos**.

## O que a tela faz
- **Renomear:** OK no jogo > Renomear. Abre um teclado na tela (o projetor não tem teclado). A extensão é mantida e o save e o estado salvo do jogo acompanham o novo nome.
- **Excluir:** OK no jogo > Excluir. O arquivo é apagado por completo. Há duas opções: **jogo e progresso** (apaga também o save e o estado salvo) ou **só o jogo**. O foco começa em *Cancelar*.
- **Enviar pelo celular ou computador:** a tela mostra um endereço (`http://IP:8080`) e um **PIN**. No navegador do celular ou do PC, na mesma rede, abra o endereço, digite o PIN e envie os arquivos. Pela mesma página também dá para renomear e apagar.

## Para onde vão os jogos enviados
Direto para a pasta certa, dentro de `/storage/emulated/0/Jogos/`: `snes` para `.sfc`, `.smc`, `.swc` e `.fig`; `nes` para `.nes`. Um `.zip` vai para a pasta do sistema da ROM que ele contém; um `.zip` sem ROM de SNES ou NES é recusado. O jogo aparece na lista sozinho. Se o app não tiver acesso a todos os arquivos, os jogos ficam na pasta do próprio app.

## Segurança
- O servidor só fica ligado **enquanto a tela Gerenciar jogos está aberta**.
- Toda ação exige o PIN (4 dígitos, sorteado a cada abertura da tela). Depois de **5 erros seguidos** o servidor recusa tudo por 30 segundos.
- Só aceita `.sfc .smc .swc .fig .nes .zip`, no máximo **64 MB** por arquivo. Nomes são limpos (sem `/`, `..` etc.): não dá para gravar fora da pasta de jogos.
- É HTTP simples, **sem criptografia**: use só numa rede em que você confia.
- Outros sites abertos no navegador não conseguem usar o servidor (todo pedido exige um cabeçalho que o navegador bloqueia entre sites).

## De onde vêm os jogos
Este projeto **não baixa jogos da internet** e não é ligado a nenhum site de ROMs. Use jogos que você tem o direito de usar: cópias feitas dos seus próprios cartuchos, jogos homebrew e de distribuição livre. O envio pela rede serve para colocar no projetor os arquivos que você já tem.

## O que foi testado
- No computador, com requisições reais: PIN certo e errado, bloqueio após 5 erros, envio de `.sfc`, `.nes` e `.zip`, recusa de `.exe` e de `.zip` sem ROM, nome repetido, nome com `../`, limite de tamanho, renomear (com e sem save), apagar com e sem save, nomes com acento.
- O Kotlin dos dois módulos compila (kotlinc contra o android.jar da API 30).
- **Não testado:** no projetor (servidor no Android, rede, teclado na tela, janelas de Renomear e Excluir) nem a página em um navegador de celular (o JavaScript só teve a sintaxe verificada e o fluxo simulado com `fetch`).
