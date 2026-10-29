# Jogos

Pasta de partida para os jogos do app **Jogos HY300**.

```
jogos/
  snes/   Super Nintendo  (.sfc, .smc, .swc, .fig)
  nes/    NES             (.nes)
```

- **Os jogos nunca vão para o GitHub:** o `.gitignore` bloqueia tudo dentro de `snes/`. Não os publique.
- Use apenas jogos que você tem direito de usar.
- Os jogos podem ser `.zip` ou `.sfc/.smc`. A lista está em `LISTA.md`.
- `nes/` guarda os jogos de NES (`.nes` ou `.zip`). Eles precisam do núcleo do NES, diferente do Super Nintendo.
- No projetor, o app lê os jogos de `/storage/emulated/0/Jogos/`.
- Para enviar: `scripts/05-enviar-jogos.ps1` (veja `docs/14-app-jogos.md`).
