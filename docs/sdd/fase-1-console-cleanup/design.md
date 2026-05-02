# Design: Fase 1 — Diagnóstico e Limpeza de Console

## Arquitetura

Headers de segurança são aplicados em duas camadas:

```
[Vercel Edge] → vercel.json headers → todos os responses
[Spring Boot] → SecurityConfig headers → /api/** responses (já configurado)
```

O Sentry SDK intercepta erros no browser antes de chegarem ao console:

```
[Erro JS] → Sentry.init filter → descarta ruído → reporta erro genuíno → Sentry dashboard
```

## Decisões e Trade-offs (ADR)

### ADR-002: CSP com `unsafe-inline`
**Decisão:** Permitir `'unsafe-inline'` em `script-src` e `style-src` por ora.  
**Motivo:** Angular (sem SSR) injeta scripts inline; Material injeta estilos inline. Sem isso, a app quebra.  
**Trade-off:** Reduz eficácia do CSP contra XSS. Mitigar com SSR + nonces na Fase 3.  
**Revisão:** Remover `unsafe-inline` quando SSR estiver ativo (Fase 3).

### ADR-003: Sentry free tier
**Decisão:** Usar Sentry free (5k erros/mês) com `sampleRate: 0.1` em produção.  
**Motivo:** Suficiente para o volume atual; custo zero.  
**Trade-off:** Perda de amostra em picos. Aceito até Fase 6 (observabilidade completa).

## Headers Configurados (vercel.json)

```
Content-Security-Policy:
  default-src 'self'
  script-src 'self' 'unsafe-inline' https://accounts.google.com
  style-src 'self' 'unsafe-inline' https://fonts.googleapis.com
  font-src 'self' https://fonts.gstatic.com
  img-src 'self' data: https:
  connect-src 'self' https://*.onrender.com https://www.asaas.com https://sandbox.asaas.com https://sentry.io https://*.ingest.sentry.io
  frame-src https://accounts.google.com
  object-src 'none'
  base-uri 'self'
  form-action 'self'

Strict-Transport-Security: max-age=63072000; includeSubDomains; preload
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: browsing-topics=(), interest-cohort=(), camera=(), microphone=(), geolocation=()
```

## Filtros Sentry

Ignorar eventos que correspondam a:
- `runtime.lastError`
- `message port closed`
- `ResizeObserver loop limit exceeded`
- `ResizeObserver loop completed with undelivered notifications`
- Origem de extensão Chrome (`chrome-extension://`)

## Plano de Rollback
- `vercel.json` é versionado — reverter commit restaura headers anteriores.
- Sentry pode ser desabilitado setando `SENTRY_DSN=` vazio no Vercel.
