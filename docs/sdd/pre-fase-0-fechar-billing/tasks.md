# Tasks: Pré-Fase 0 — Fechar Billing e Publicar em Produção

- [x] T1 Adicionar validação do `asaas-access-token` no `BillingController.webhook()`
- [x] T2 Criar `render.yaml` declarando o serviço backend com variáveis de ambiente
- [x] T3 `git push origin main` backend (4 commits)
- [x] T4 `git push origin main` frontend (2 commits)
- [ ] T5 No dashboard Render: configurar `ASAAS_API_KEY`, `ASAAS_WEBHOOK_TOKEN` (manual — ver VALIDATION.md)
- [ ] T6 Aguardar redeploy automático do Render e confirmar healthcheck
- [ ] T7 Smoke test: `curl` criando empresa, obtendo token, chamando `/api/billing/minha`
- [ ] T8 Smoke test webhook: `curl` simulando `PAYMENT_CONFIRMED` e verificando mudança de status
- [x] T9 `git tag v0.3.0-billing && git push --tags`

## Dependências
- T5 depende de T3 (Render precisa do código com a validação do token)
- T7, T8 dependem de T5 e T6

## Critérios de Pronto (DoD)
- [ ] Webhook responde 200 em < 500ms (medir com curl `-w "%{time_total}"`)
- [ ] Status muda TRIAL → ATIVA após payload `PAYMENT_CONFIRMED`
- [ ] Status muda TRIAL → INADIMPLENTE após `verificarAssinaturasVencidas` (simular com data no passado)
- [ ] `POST /api/billing/webhook` sem token retorna 200 (degradação graciosa logada)
- [ ] `POST /api/billing/webhook` com token errado retorna 401
- [ ] Logs do Render sem stacktrace Java
- [ ] CSP e CORS sem erro no console do navegador
