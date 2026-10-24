# Changelog

## v11
- Primeira versão do **projeto Android** (`android/`): tela inicial, até 5 atalhos de apps com o banner ou ícone do próprio app, atalhos fixos (Jogos, Arquivos, HDMI, Gerenciar atalhos), menu da engrenagem e tela Gerenciar atalhos.
- Guia de Android Studio em `android/README.md` e arquitetura em `docs/13-projeto-android.md`.
- Ainda não compilado nem testado no aparelho.

## v10
- Como criar a chave de assinatura do launcher (`docs/12`) e `.gitignore` bloqueando chaves e arquivos locais do Android.

## v9
- Plano de atualização do **launcher** pelo GitHub (Releases + `latest.json` no Pages): `docs/12`, exemplo em `docs/exemplos/` e script `scripts/04-gerar-latest-json.ps1`.
- Decisão registrada: seguir pelo launcher por enquanto; firmware próprio fica para depois.

## v8
- Protótipo: ícone de Configurações trocado por uma **engrenagem** de 8 dentes.
- Nova análise: atualização local e online, e origem dos dados de "Sobre o dispositivo" (`docs/11`).
- Achado: o `htcsettings` também tem analytics da Baidu.

## v7
- JADX configurado no REA. Primeira decompilação: `com.htc.htcsettings` (`docs/10-analise-htcsettings.md`).
- Mapeados os cartões da tela de Projeção e quais activities são exportadas.
- Correção: o cartão de keystone abre `CorrectionIndexActivity`, e não `KeystonecorrectionActivity`.
- Anotado que o original usa `sound_effects_enabled` para o bip.

## v6
- README: link do protótipo online no topo, seção "Como este projeto é feito" (IA e REA), aviso de firmware futuro, "por enquanto" no original, créditos corrigidos (fundo gerado por IA) e removida a menção ao vídeo de inspiração.
- `index.html` na raiz para publicar o protótipo no GitHub Pages.
- RetroArch: plano de instalação e otimização sem toque (`docs/09`), configuração de exemplo (`retroarch/`) e script de instalação (`scripts/03`).
- Roadmap: seção de firmware próprio.

## v5
- README reescrito com a visão de **projetor gamer** e a lista atual de recursos.

## v4
- Novo atalho fixo **Jogos** (ícone de controle estilo Super Nintendo), na frente de Arquivos. Por enquanto mostra "Em breve".
- Removidos os cadeados dos atalhos fixos.
- Plano futuro de jogos de 16 bits com RetroArch (`docs/08-jogos-retroarch.md`).

## v3
- Tela de **Configurações** completa: Rede, Bluetooth, Idioma, Projeção, Áudio, Data e hora e Sobre o dispositivo.
- Rede e Bluetooth (topo) abrem a página correspondente; a engrenagem abre a lista.
- Botão de som removido da tela inicial; o **bip do cursor** agora é ligado e desligado em Áudio.
- Atalhos editáveis voltam a ser só de apps instalados; Projeção, Imagem e Áudio saíram dos atalhos e vivem nas Configurações.
- Relógio segue fuso horário e formato de hora escolhidos.

## v2
- Fileira de atalhos: até 5 editáveis + 3 fixos (Arquivos, HDMI, Gerenciar atalhos).
- Tela "Gerenciar atalhos" para adicionar e remover atalhos.
- Engrenagem de Configurações no topo; Rede, Bluetooth e Configurações navegáveis pelo direcional.
- Plano de fundo definido (lago ao pôr do sol).
- Cartão Arquivos indica pendrive conectado (tecla P simula no protótipo).

## v1
- Protótipo inicial, documentação e scripts de extração.
