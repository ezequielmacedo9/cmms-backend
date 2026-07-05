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
- **FASE D.2 (checklist + anexos da OS)** — entidades `ManutencaoChecklistItem` e `ManutencaoAnexo` (arquivo base64 no banco, ~3MB, escopo por empresa) + Flyway V7; endpoints: `GET /api/manutencoes/{id}` (detalhe completo), add/toggle/remove checklist, add/download/remove anexo (listagem usa projeção só de metadados — o blob só sai no download); frontend: modal "Detalhes" na lista de OS com checklist interativo e upload/download de anexos via FileReader/data-URI
- **ConfiguracaoSistema por-empresa** — PK surrogate + `empresa_id` com unique (empresa_id, chave), catálogo default semeado lazy por empresa; `get/getInt/getBool` recebem empresaId explícito (lockout roda antes da autenticação); Flyway V8 replica as linhas globais para cada empresa; fix no frontend: settings pede `unpaged=true`
- **2FA obrigatório no login** — `LoginRequestDTO.totpCode` opcional; usuário com TOTP ativo: sem código → 401 `TOTP_REQUIRED`, código errado → 401 `TOTP_INVALID` (auditado); empresa com `seguranca.2fa.obrigatorio` e usuário sem adesão → `twoFactorSetupRequired=true` e o frontend redireciona a `/perfil?setup2fa=1`; login page revela campo do código quando o backend pede; `TotpServiceTest` novo (cross-check RFC 6238)
- **QR code de máquina (deep-link)** — fechado IDOR no `QrCodeController` (agora `findByIdAndEmpresaId`); botão "QR code" na lista de máquinas baixa o PNG autenticado; o `?id=` do QR agora é consumido: abre a página com a máquina focada + histórico aberto
- **FASE 10.3 (cobertura 80%) CONCLUÍDA** — JaCoCo 42.5% → **80.7%** de instruções, 148 testes. Novos: `OperationalFlowIntegrationTest` (perfil, 2FA E2E com código RFC 6238 real, checklist/anexos via HTTP, relatórios PDF/XLSX, QR), smoke dos geradores de relatório, `PasswordResetServiceTest`, `RefreshTokenServiceTest`, `EmailServiceTest`, `RateLimitFilterTest`, `NotificationServiceTest`. Bônus: o teste E2E pegou bug no frontend (envelope usa `code`, não `errorCode`) — corrigido
- **Scanner de QR in-app** — `QrScannerComponent` (getUserMedia câmera traseira + decode com **jsQR 1.4.0 importado dinamicamente**, chunk lazy fora do bundle inicial); botão "Escanear" em máquinas aceita URL de deep-link ou id numérico, foca a máquina e abre o histórico; trata permissão negada e encerra a MediaStream
- **Reactive forms + validação inline** — os 4 formulários de entidade (máquinas, OS, estoque, convite de usuário) migrados de ngModel para `FormGroup` tipado (`NonNullableFormBuilder`) com `mat-error`/`.field-error` por campo, `markAllAsTouched` no submit inválido e merge com o registro original ao editar; senha do convite alinhada à política do backend (mín. 8)
- **i18n (pt/en/es) CONCLUÍDO** — i18n em runtime: chave = texto pt-BR (fallback natural), dicionários en/es (**~500 chaves**) carregados por dynamic import no `APP_INITIALIZER` (fora do bundle em pt); troca de idioma persiste em localStorage + reload (pipe `t` puro, zero custo de CD); seletores de idioma na sidebar e no login; **100% dos templates** convertidos (shell, dashboard, máquinas, OS com checklist/anexos, estoque, usuários, settings, relatórios, billing, perfil com fluxo 2FA, auditoria, login/cadastro/senha, 404/403, scanner QR); **mensagens de runtime também traduzidas** — `ToastService.show`/`NotificationService.push` traduzem no funil único (todos os ~70 toasts + painel de notificações), diálogos de confirmação, tour de onboarding e painel de atalhos

**Estado atual:**
- Backend: 148 testes, cobertura 80.7%, BUILD SUCCESS, zero warnings, HEAD `f6c5ef4`
- Frontend: 11 suites verdes, prod build OK, zero warnings, HEAD `6ed7e7a`
- **ROADMAP DE CÓDIGO 100% CONCLUÍDO** — resta apenas o que depende de credenciais/infra externa
- Resolvidas: CSS morto; SW /api; drift @angular/*; CI sem `--legacy-peer-deps`; PDF multipágina; custo/MTTR; jjwt

---

## ⏳ Backlog (precisa de sistemas/credenciais externas ou é polish grande)

### Precisa de recurso externo (não dá pra finalizar autonomamente)
- **Pagamento real**: precisa de conta + `ASAAS_API_KEY`/webhook token (integração já escrita, no-op sem chave)
- **Sentry ativo**: precisa de DSN (`SENTRY_DSN`) — SDK já plugado nos 2 repos
- **IoT/sensores, ERP (SAP/TOTVS), app mobile nativo**: sistemas/hardware/codebase externos
- **SLA 99,9% + backup automatizado**: decisões de infra + tiers pagos (Render/Postgres)

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

ESTADO: 148 testes backend / cobertura 80.7% (HEAD f6c5ef4), 11 suites frontend (HEAD 6ed7e7a), BUILD SUCCESS, zero warnings. ROADMAP DE CODIGO 100% CONCLUIDO (incl. i18n de runtime: toasts, dialogos, tour, atalhos).

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
- FASE D.2: checklist + anexos (base64) da OS, Flyway V7, modal de detalhe no frontend.
- ConfiguracaoSistema por-empresa (Flyway V8) + fix settings unpaged.
- 2FA no login (TOTP_REQUIRED/TOTP_INVALID + twoFactorSetupRequired) front+back.
- QR de maquina: IDOR fechado, botao de download, deep-link ?id= consumido.
- FASE 10.3: cobertura JaCoCo 80.7% (era 42.5%), 148 testes, incl. integracao
  E2E de perfil/2FA/OS/relatorios/QR.
- Scanner QR in-app (jsQR lazy) + reactive forms com validacao inline nos
  4 formularios de entidade.
- i18n runtime pt/en/es: 100% dos templates, dicionarios lazy, seletor de
  idioma na sidebar e no login.

BACKLOG (somente itens que dependem de credencial/sistema/infra externa):
pagamento real (ASAAS_API_KEY), Sentry ativo (DSN), IoT/ERP/app nativo,
SLA/backup (infra paga), Lighthouse medido em prod, rotacao do JWT secret
antigo no historico git. Nao ha mais vertical de codigo pendente.

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
