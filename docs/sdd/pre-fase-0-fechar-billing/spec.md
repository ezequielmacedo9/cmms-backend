# Spec: Pré-Fase 0 — Fechar Billing e Publicar em Produção

## Objetivo
Levar os commits das Fases 1–3 (multi-tenancy + billing Asaas) para produção com zero downtime, validar os 4 estados de assinatura e garantir que o webhook Asaas chega, é autenticado e processa corretamente.

## User Stories
- **US-01** Como admin de uma empresa, quero me cadastrar no trial de 14 dias sem cartão.
- **US-02** Como admin, quero escolher um plano e ser redirecionado ao link de checkout Asaas.
- **US-03** Como sistema, quando o Asaas confirmar o pagamento via webhook, quero que a assinatura mude para `ATIVA` automaticamente.
- **US-04** Como admin, quero cancelar minha assinatura e receber confirmação.
- **US-05** Como operador, se o trial expirar sem pagamento, quero que o status mude para `INADIMPLENTE`.

## Critérios de Aceitação (mensuráveis)
- `GET /api/billing/minha` retorna `status: TRIAL` para empresa recém-criada.
- `POST /api/billing/checkout` retorna `linkPagamento` (URL Asaas sandbox) ou mensagem de configuração pendente.
- `POST /api/billing/webhook` com header `asaas-access-token: <ASAAS_WEBHOOK_TOKEN>` + payload `PAYMENT_CONFIRMED` muda status para `ATIVA` e retorna `200` em < 500ms.
- `POST /api/billing/webhook` sem token válido retorna `401`.
- Job `verificarAssinaturasVencidas` expira trials com `dataProximaCobranca < hoje`.
- Smoke test em produção: fluxo completo sem stacktrace nos logs do Render.

## Fora de Escopo
- Integração com Asaas produção (fica em sandbox).
- Interface de admin de planos/preços.
- Cobrança proporcional (prorate) no upgrade.

## Riscos
| Risco | Probabilidade | Mitigação |
|---|---|---|
| Render cold-start atrasa webhook | Média | Keep-alive ping a cada 14min (já existe no frontend via wakeup.service) |
| Asaas sandbox instável | Baixa | Teste offline com `curl` simulando payload |
| Token não configurado no Render | Alta | Webhook retorna 200 sem processar (degradação graciosa documentada) |
