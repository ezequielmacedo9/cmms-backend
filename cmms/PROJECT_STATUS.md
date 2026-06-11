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

**Estado atual:**
- Backend: 90 testes, BUILD SUCCESS, zero warnings, HEAD `4156d2b` (multi-tenant)
- Frontend: 45 testes, prod build 489kB, zero warnings, HEAD `a7d59c6` (cadastro de empresa)
- Pendências resolvidas: CSS morto dos modais removido; service worker não intercepta mais `/api/` (sem erros `safeFetch/handleFetch`)

---

## ⏳ Pendências

### FASE B — Billing/assinatura real (PRIORIDADE ALTA)
- Frontend chama `/api/billing/*` mas o backend NÃO tem `BillingController` → página de assinatura quebra em prod
- Criar entidades Plano/Assinatura, `BillingController` (planos/minha/checkout/upgrade/cancelar), enforcement de plano/quota por empresa
- Integração de pagamento (Asaas/Stripe) + webhook; config de chave por env
- Tornar `ConfiguracaoSistema` por-empresa (hoje global — bleed cosmético de nome/timezone entre tenants)

### FASE C — CI/CD + Sentry + testes de autorização/IDOR
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

ESTADO: 90 testes backend (HEAD 4156d2b), 45 frontend (HEAD a7d59c6), BUILD SUCCESS, zero warnings.

FASE 11 + FASE A (multi-tenancy) CONCLUIDAS:
- FASE 11 (frontend): TableState (sort+filter), ExportService, ConfirmDialogService,
  tour no Dashboard, ShortcutsHelpComponent. Pendencias (CSS morto, SW /api) resolvidas.
- FASE A (backend+frontend): SaaS multi-empresa. Entidade Empresa + empresa_id em todos os
  agregados; TenantResolver; repos/services/dashboard/relatorio/auditoria escopados (IDOR
  fechado); cadastro self-service POST /api/auth/register + pagina /cadastro; Google cria
  empresa propria; Flyway V4 com backfill; MultiTenantIsolationTest.

PROXIMA FASE (B) — Billing/assinatura real (backend nao tem BillingController; frontend ja
chama /api/billing/*): entidades Plano/Assinatura + BillingController + enforcement de plano
+ checkout/webhook (Asaas/Stripe). Tornar ConfiguracaoSistema por-empresa.
Depois: FASE C (CI/CD + Sentry + testes authz/IDOR).

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
