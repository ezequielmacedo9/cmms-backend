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
- **FASE C (CI/CD + observabilidade + authz)** — GitHub Actions (build+test gate) nos 2 repos; `AuthorizationIntegrationTest` (matriz de roles por HTTP: VISUALIZADOR não escreve, GESTOR não deleta, anônimo barrado); Sentry no backend (`sentry-spring-boot-starter-jakarta`, no-op sem DSN). **Sentry frontend adiado** (ver pendência do drift Angular)

**Estado atual:**
- Backend: 97 testes, BUILD SUCCESS, zero warnings, HEAD `536174f` (CI + authz + Sentry)
- Frontend: 45 testes, prod build 489kB, zero warnings, HEAD `6d3cedc` (CI)
- Pendências resolvidas: CSS morto dos modais removido; service worker não intercepta mais `/api/` (sem erros `safeFetch/handleFetch`)

---

## ⏳ Pendências

### Drift de versões @angular/* (PRIORIDADE MÉDIA, descoberto na FASE C)
- O lockfile do frontend tem patches desalinhados: `@angular/animations` 21.2.5 e `@angular/service-worker` 21.2.10 enquanto `core`/`common` estão 21.2.1
- Isso faz `npm install`/`npm ci` falhar com ERESOLVE; o CI usa `--legacy-peer-deps` como paliativo e novas deps (ex.: `@sentry/browser`) ficam bloqueadas
- Corrigir alinhando tudo com `ng update` / reinstalando os `@angular/*` na mesma versão; depois remover o `--legacy-peer-deps` do CI e plugar o Sentry no frontend

### FASE B.2 — Gateway de pagamento (Asaas/Stripe)
- `checkout` hoje ativa o plano manualmente (linkPagamento vazio). Plugar gateway: gerar link real + webhook que confirma pagamento (ATIVA só após webhook); config de chave por env
- Tornar `ConfiguracaoSistema` por-empresa (hoje global — bleed cosmético de nome/timezone entre tenants)
- (Opcional) gate de acesso por trial expirado/inadimplente nas escritas


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

ESTADO: 97 testes backend (HEAD 536174f), 45 frontend (HEAD 6d3cedc), BUILD SUCCESS, zero warnings.

FASES 11, A (multi-tenancy), B (billing sem gateway) e C (CI/CD + observabilidade) CONCLUIDAS:
- FASE 11 (frontend): TableState, ExportService, ConfirmDialogService, tour, ShortcutsHelp.
- FASE A: SaaS multi-empresa (Empresa + empresa_id, TenantResolver, tudo escopado, IDOR
  fechado, cadastro POST /api/auth/register + /cadastro, Flyway V4, MultiTenantIsolationTest).
- FASE B (backend): Plano + Assinatura + Flyway V5; BillingController; quota de plano;
  checkout sem gateway (link vazio).
- FASE C: GitHub Actions (build+test) nos 2 repos; AuthorizationIntegrationTest (matriz de
  roles); Sentry no backend (no-op sem DSN). Sentry frontend ADIADO pelo drift Angular.

ATENCAO: drift de patch nos pacotes @angular/* (animations 21.2.5 / service-worker 21.2.10 vs
core 21.2.1) quebra npm ci; CI usa --legacy-peer-deps. Alinhar com ng update antes de novas
deps no frontend.

PROXIMA FASE — opcoes: FASE B.2 (gateway de pagamento Asaas/Stripe + webhook), FASE D (Ordem
de Servico real), ou alinhar @angular/* + Sentry frontend. Confirme antes de comecar.

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
