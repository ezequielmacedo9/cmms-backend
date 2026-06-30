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
- **FASE E (relatórios gerenciais)** — `GET /api/relatorios/gerencial` (GESTOR+) com cumprimento de preventiva, MTBF, disponibilidade, valor de estoque e top ofensores (escopado por empresa); corrigido bug do **PDF que truncava em 1 página** (agora pagina A4 com cabeçalho repetido + rodapé numerado); seção "Visão gerencial" no frontend
- **FASE D (ordem de serviço)** — `Manutencao` enriquecida (tecnicoId, tempoExecucaoMinutos, custoMaoObra, dataAbertura/Conclusao) + entidade `ManutencaoPeca` (peça consumida com qtd + snapshot de custo) que **dá baixa no estoque**; workflow de status `PUT /api/manutencoes/{id}/status` (carimba conclusão); Flyway V6; relatórios passam a ter **custo de manutenção** e **MTTR**; frontend: form de OS (tempo/custo/peças) + ação "Concluir"
- **FASE B.2 (gateway de pagamento)** — `PagamentoService` integra Asaas (link recorrente) quando `ASAAS_API_KEY` está setada; sem chave é no-op (ativação manual). `POST /api/billing/webhook` (público, token) confirma o pagamento e ativa a assinatura
- **FASE F (UX mobile)** — tabelas viram cards no mobile (≤700px) via CSS global, sem scroll horizontal nas 4 listas
- **Hardening** — upgrade jjwt 0.11.5 → 0.12.6 (API atual, sem deprecadas)

**Estado atual:**
- Backend: 105 testes, BUILD SUCCESS, zero warnings, HEAD `e9f5c06`
- Frontend: 45 testes, prod build 490kB, zero warnings, HEAD `7cbc1a0`
- Resolvidas: CSS morto; SW /api; drift @angular/*; CI sem `--legacy-peer-deps`; PDF multipágina; custo/MTTR; jjwt

---

## ⏳ Backlog (precisa de sistemas/credenciais externas ou é polish grande)

### Precisa de recurso externo (não dá pra finalizar autonomamente)
- **Pagamento real**: precisa de conta + `ASAAS_API_KEY`/webhook token (integração já escrita, no-op sem chave)
- **Sentry ativo**: precisa de DSN (`SENTRY_DSN`) — SDK já plugado nos 2 repos
- **IoT/sensores, ERP (SAP/TOTVS), app mobile nativo**: sistemas/hardware/codebase externos
- **SLA 99,9% + backup automatizado**: decisões de infra + tiers pagos (Render/Postgres)

### Polish grande (vertical/refactor dedicado)
- **FASE D.2**: checklist + anexos da OS (anexos = upload base64/blob + UI)
- **2FA obrigatório no login**: flag `seguranca.2fa.obrigatorio` ainda não força o fluxo (mudança de contrato front+back)
- **ConfiguracaoSistema por-empresa**: hoje global (bleed cosmético) — refactor de PK + escopo
- **i18n** (pt/en/es); **reactive forms** + validação inline; **ler QR para abrir ativo** (lib de câmera)

### FASE 10.3 — Cobertura 80%
- Subiu bastante com os testes de integração (multi-tenant, authz, billing, relatórios) e unitários novos; medir JaCoCo atualizado

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

ESTADO: 105 testes backend (HEAD e9f5c06), 45 frontend (HEAD 7cbc1a0), BUILD SUCCESS, zero warnings.

ROADMAP IMPLEMENTAVEL CONCLUIDO. Fases entregues (todas com build verde + push):
- FASE 11: TableState/ExportService/ConfirmDialog/tour/ShortcutsHelp.
- FASE A: multi-tenancy (Empresa + empresa_id, TenantResolver, tudo escopado, IDOR fechado,
  cadastro self-service, Flyway V4, teste de isolamento).
- FASE B + B.2: billing (Plano/Assinatura, BillingController, quota) + gateway Asaas + webhook.
- FASE C: CI/CD (GitHub Actions) nos 2 repos, Sentry back+front, testes authz/IDOR, fix drift Angular.
- FASE D: ordem de servico (status, tecnico=usuario, tempo/custo, baixa de estoque via
  ManutencaoPeca, Flyway V6) -> destravou custo/MTTR nos relatorios.
- FASE E: relatorios gerenciais + PDF multipagina.
- FASE F: tabelas responsivas (cards no mobile).
- Hardening: jjwt 0.12.6.

BACKLOG (precisa de credencial/sistema externo OU e polish grande): pagamento real
(ASAAS_API_KEY), Sentry ativo (DSN), IoT/ERP/app nativo, SLA/backup (infra paga); D.2
(checklist+anexos), 2FA obrigatorio no login, ConfiguracaoSistema por-empresa, i18n,
reactive forms, QR-scan. Detalhes na secao Backlog acima.

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
