# Tasks: Fase 1 — Diagnóstico e Limpeza de Console

- [x] T1 Criar `frontend-cmms/vercel.json` com headers de segurança completos (CSP, HSTS, X-Frame-Options, Referrer-Policy, Permissions-Policy)
- [x] T2 Adicionar filtro `runtime.lastError` no `main.ts` para suprimir ruído de extensão
- [ ] T3 `npm i @sentry/angular` + configurar `app.config.ts` com DSN via `VITE_SENTRY_DSN` / `NG_APP_SENTRY_DSN`
- [ ] T4 Filtro Sentry: ignorar `runtime.lastError`, `ResizeObserver loop`, extensões Chrome
- [x] T5 Criar `KNOWN_NON_ISSUES.md` documentando erros de extensão
- [x] T6 Commitar `vercel.json` + `main.ts` + docs, push, aguardar deploy Vercel
- [ ] T7 Validar score em https://securityheaders.com (meta ≥ A)
- [ ] T8 Validar score em https://observatory.mozilla.org (meta ≥ A)
- [ ] T9 Testar em aba anônima: zero erros vermelhos no console

## Dependências
- T3, T4 dependem de conta Sentry criada (DSN gerado externamente)
- T7, T8, T9 dependem de T6 (deploy Vercel)

## Critérios de Pronto (DoD)
- [ ] `vercel.json` commitado e em produção
- [ ] Console aba anônima: zero erros vermelhos de código próprio
- [ ] securityheaders.com ≥ A
- [ ] Mozilla Observatory ≥ A
- [ ] `Permissions-Policy` sem warnings no Chrome DevTools
