# Spec: Fase 1 — Diagnóstico e Limpeza de Console

## Objetivo
Eliminar todos os warnings/errors de console em produção que sejam de responsabilidade do produto. Separar ruído de extensão de erros reais. Configurar Sentry para capturar apenas erros genuínos.

## User Stories
- **US-01** Como desenvolvedor, quero abrir o console do navegador em produção e ver zero erros vermelhos que sejam do nosso código.
- **US-02** Como devops, quero receber alerta no Sentry quando um usuário encontrar um erro 500 real, sem poluição de extensões Chrome.
- **US-03** Como time de segurança, quero que o frontend tenha score A+ em securityheaders.com.
- **US-04** Como usuário, quero que o browser não exiba warnings sobre APIs removidas causados por headers mal configurados.

## Erros Atuais e Classificação

| Mensagem | Origem | Ação |
|---|---|---|
| `runtime.lastError: The message port closed` | Extensão Chrome (não é nosso código) | Filtrar no Sentry + documentar em KNOWN_NON_ISSUES.md |
| `Browsing Topics API removed from Permissions-Policy` | Header Vercel padrão | Adicionar header explícito `browsing-topics=()` no vercel.json |
| `login:1 Unchecked runtime.lastError` | Idem extensão | Filtrar Sentry |

## Critérios de Aceitação (mensuráveis)
- Score securityheaders.com ≥ A (meta: A+).
- Mozilla Observatory ≥ A.
- Zero erros vermelhos no console em aba anônima (sem extensões).
- Sentry configurado com filtro de ruído de extensão.
- `Permissions-Policy` sem warnings no Chrome DevTools.
- Todos os headers de segurança presentes: CSP, HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy.

## Fora de Escopo
- Erros de extensões instaladas no navegador do usuário (documentados como não-bugs).
- Sentry performance tracing (Fase 6).

## Riscos
| Risco | Mitigação |
|---|---|
| CSP muito restritiva quebra Google OAuth | Incluir `accounts.google.com` na CSP; testar login após deploy |
| `unsafe-inline` no script-src | Necessário para Angular; mitigar com nonce em versão futura (pós-SSR) |
