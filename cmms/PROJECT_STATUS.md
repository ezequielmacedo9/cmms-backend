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
- **FASE C (CI/CD + observabilidade + authz)** — GitHub Actions (build+test gate) nos 2 repos; `AuthorizationIntegrationTest` (matriz de roles por HTTP: VISUALIZADOR não escreve, GESTOR não deleta, anônimo barrado); Sentry no backend (`sentry-spring-boot-starter-jakarta`) e no frontend (`@sentry/browser`, lazy), ambos no-op sem DSN; corrigido o drift de patch dos pacotes `@angular/*` (animations/service-worker realinhados a 21.2.1, `npm ci` volta a funcionar)
- **FASE E (relatórios gerenciais)** — `GET /api/relatorios/gerencial` (GESTOR+) com cumprimento de preventiva, MTBF, disponibilidade, valor de estoque e top ofensores (escopado por empresa); corrigido bug do **PDF que truncava em 1 página** (agora pagina A4 com cabeçalho repetido + rodapé numerado); seção "Visão gerencial" no frontend. KPIs de custo/MTTR/disponibilidade-real dependem da FASE D

**Estado atual:**
- Backend: 100 testes, BUILD SUCCESS, zero warnings, HEAD `8564c6b` (relatórios gerenciais + PDF)
- Frontend: 45 testes, prod build 489kB, zero warnings, HEAD `487fc6f` (visão gerencial)
- Pendências resolvidas: CSS morto removido; SW não intercepta `/api/`; drift @angular/* corrigido; CI sem `--legacy-peer-deps`; PDF multipágina

---

## ⏳ Pendências

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

ESTADO: 100 testes backend (HEAD 8564c6b), 45 frontend (HEAD 487fc6f), BUILD SUCCESS, zero warnings.

FASES 11, A (multi-tenancy), B (billing sem gateway), C (CI/CD + observabilidade) e E
(relatorios gerenciais) CONCLUIDAS:
- FASE 11 (frontend): TableState, ExportService, ConfirmDialogService, tour, ShortcutsHelp.
- FASE A: SaaS multi-empresa (Empresa + empresa_id, TenantResolver, tudo escopado, IDOR
  fechado, cadastro POST /api/auth/register + /cadastro, Flyway V4, MultiTenantIsolationTest).
- FASE B (backend): Plano + Assinatura + Flyway V5; BillingController; quota de plano;
  checkout sem gateway (link vazio).
- FASE C: GitHub Actions (build+test) nos 2 repos; AuthorizationIntegrationTest (matriz de
  roles); Sentry no backend e frontend (no-op sem DSN); drift @angular/* corrigido.
- FASE E: GET /api/relatorios/gerencial (cumprimento preventiva, MTBF, disponibilidade, valor
  de estoque, top ofensores); PDF de relatorio agora pagina (nao trunca); secao "Visao
  gerencial" no frontend.

PROXIMA FASE — opcoes: FASE D (Ordem de Servico real: workflow/tecnico=usuario/anexos/
checklist/baixa de estoque/custo+tempo) que tambem destrava os KPIs de custo/MTTR/
disponibilidade; FASE B.2 (gateway Asaas/Stripe + webhook); FASE F (UX mobile: tabelas em
cards, reactive forms, abrir ativo por QR). Confirme antes de comecar.

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
