# 📋 CMMS Industrial Suite — Resumo de Transição

## Stack e Repositórios

**Backend** — Spring Boot 3.5.9 + Java 17 + PostgreSQL (prod) / H2 (dev)
- Repo: `https://github.com/ezequielmacedo9/cmms-backend`
- Path local: `C:\Users\USER\cmms-backend\cmms`
- Deploy: Render.com

**Frontend** — Angular 21 + standalone + signals + Vitest + ApexCharts
- Repo: `https://github.com/ezequielmacedo9/cmms-frontend`
- Path local: `C:\Users\USER\cmms-frontend`
- Deploy: Vercel

---

## ✅ Fases Concluídas (1 a 10 parcial)

- **FASE 1** — Diagnóstico completo
- **FASE 2-CRÍTICA** — JWT env var, Flyway, DataInitializer dev-only, JwtAuthFilter, specs frontend
- **FASE 2B** — Hierarquia de exceções, ApiError envelope, AuthService/GoogleAuth/PasswordReset extraídos, DTOs, constructor injection
- **FASE 2C** — Pageable em todos, @EntityGraph N+1, DashboardService queries agregadas, índices V2, HikariCP
- **FASE 2D** — OpenAPI 12 tags, TraceIdFilter, logback-spring.xml dev/prod
- **FASE 3B** — jwt.util.ts, authGuard com exp, CSP vercel.json, SRI
- **FASE 3C** — Design system completo, sidebar premium, Dashboard ApexCharts, Login 3D, EmptyStateComponent
- **FASE 4** — 52 testes backend + 44 frontend
- **FASE 5** — Revisão final, zero warnings, zero console.log
- **FASE 6** — JWT 15min + refresh rotation, rate limit 6 endpoints, logout server-side, auditoria 17 tipos
- **FASE 7** — /api/v1 bridge, CORS env var, GZIP, health check custom, soft delete, validações regex
- **FASE 8** — 404/403 custom, route animations, breadcrumb, ConfirmDialog, atalhos teclado, ExportService, TableState, toast SVG animado, print stylesheet
- **FASE 9** — Notification polling, PWA corrigido, tour onboarding CDK, SEO+JSON-LD, boot loader, skip-link
- **FASE 10-1** — 89 testes backend (+34), JaCoCo 42.5%
- **FASE 11** — Integração dos componentes nas páginas: `TableState` (busca viva + sort por coluna + paginação por signals) em maquinas/manutencoes/estoque/usuarios; `ExportService` substitui o CSV duplicado; `ConfirmDialogService` substitui os modais de delete inline; tour de onboarding ativado no Dashboard; `ShortcutsHelpComponent` (tecla `?`)
- **FASE A (multi-tenancy)** — SaaS multi-empresa row-based: entidade `Empresa` + `empresa_id` em maquinas/manutencoes/pecas/ferramentas/usuario; `TenantResolver`; todo repo/service/dashboard/relatório/auditoria escopado por empresa (IDOR fechado no get-by-id); cadastro self-service `POST /api/auth/register` (empresa + admin) + página `/cadastro`; login Google provisiona empresa própria; Flyway V4 com backfill; teste de integração de isolamento
- **FASE B (billing, sem gateway)** — Enum `Plano` (STARTER/PRO/BUSINESS/ENTERPRISE), entidade `Assinatura` (1 por empresa) + Flyway V5 (backfill TRIAL); `BillingController` casando com o contrato que o frontend já consome (`/planos`, `/minha`, `/checkout`, `/upgrade`, `/cancelar`); enforcement de quota de plano em máquinas/usuários; checkout ativa o plano sem gateway (linkPagamento vazio). Frontend já estava pronto

**Estado atual:**
- Backend: 96 testes, BUILD SUCCESS, zero warnings, HEAD `d2f1cd3` (billing)
- Frontend: 45 testes, prod build 489kB, zero warnings, HEAD `a7d59c6` (cadastro de empresa)
- Pendências resolvidas: CSS morto dos modais removido; service worker não intercepta mais `/api/` (sem erros `safeFetch/handleFetch`)

---

## ⏳ Pendências

### FASE B.2 — Gateway de pagamento (Asaas/Stripe)
- `checkout` hoje ativa o plano manualmente (linkPagamento vazio). Plugar gateway: gerar link real + webhook que confirma pagamento (ATIVA só após webhook); config de chave por env
- Tornar `ConfiguracaoSistema` por-empresa (hoje global — bleed cosmético de nome/timezone entre tenants)
- (Opcional) gate de acesso por trial expirado/inadimplente nas escritas

### FASE C — CI/CD + Sentry + testes de autorização/IDOR (PRIORIDADE ALTA)
- GitHub Actions (build+test gate) nos 2 repos; Sentry back+front; testes de controller por role

### FASE 10.2 — Integration tests endpoints (PRIORIDADE ALTA)
- `MaquinaControllerIntegrationTest`, `ManutencaoControllerIntegrationTest`, `PecaControllerIntegrationTest`
- `@SpringBootTest + MockMvc + token JWT real`

### FASE 10.3 — Cobertura 80%
- Atual: 42.5%
- Gaps: RelatorioService (1%), TotpService (1%), GoogleAuthService (13%), EmailService (8%), controllers HTTP (10-40%)

### Lighthouse 90+
- Otimizações aplicadas mas score não medido em produção

### Pendências de infra (não-código)
- JWT secret antigo no histórico git → `git filter-repo` + rotacionar em prod
- `APP_CORS_ALLOWED_ORIGINS` configurar no Render
- Backend free tier cold start 60s — gargalo arquitetural

---

## 🚀 Prompt para próxima sessão

```
Continuando o CMMS Industrial Suite. Fases 1 a 10 concluídas com push.
Backend: https://github.com/ezequielmacedo9/cmms-backend (Spring Boot 3.5.9 + Java 17)
Frontend: https://github.com/ezequielmacedo9/cmms-frontend (Angular 21 + standalone + signals)
Paths: C:\Users\USER\cmms-backend e C:\Users\USER\cmms-frontend

ESTADO: 96 testes backend (HEAD d2f1cd3), 45 frontend (HEAD a7d59c6), BUILD SUCCESS, zero warnings.

FASES 11, A (multi-tenancy) e B (billing sem gateway) CONCLUIDAS:
- FASE 11 (frontend): TableState (sort+filter), ExportService, ConfirmDialogService,
  tour no Dashboard, ShortcutsHelpComponent. Pendencias (CSS morto, SW /api) resolvidas.
- FASE A (backend+frontend): SaaS multi-empresa. Entidade Empresa + empresa_id em todos os
  agregados; TenantResolver; repos/services/dashboard/relatorio/auditoria escopados (IDOR
  fechado); cadastro self-service POST /api/auth/register + pagina /cadastro; Google cria
  empresa propria; Flyway V4 com backfill; MultiTenantIsolationTest.
- FASE B (backend): Plano enum + Assinatura (1 por empresa) + Flyway V5; BillingController
  (/planos /minha /checkout /upgrade /cancelar) casando com o frontend existente; enforcement
  de quota de plano em maquinas/usuarios; checkout ativa o plano sem gateway (link vazio).

PROXIMA FASE (C) — CI/CD + Sentry + testes authz/IDOR: GitHub Actions (build+test gate) nos 2
repos, Sentry back+front, testes de controller por role. Depois: FASE B.2 (gateway de
pagamento Asaas/Stripe + webhook) e ConfiguracaoSistema por-empresa.

Leia AGENTS.md. Build verde obrigatório antes de push. Commits granulares em ASCII
via git commit -F C:\WINDOWS\TEMP\opencode\commit-msg.txt. Push ao final de cada fase.
Confirme o plano antes de começar.
```

---

## 🔑 Notas técnicas

- **Commits**: usar `-F arquivo` para evitar quebra de `ç/ã` no PowerShell
- **Backend testes**: `$env:JWT_SECRET = "..."; .\mvnw.cmd test`
- **Frontend testes**: `npx --no-install ng test --watch=false`
- **JaCoCo**: `mvn test` gera em `target/site/jacoco/index.html`
