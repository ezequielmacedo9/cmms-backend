# Design: Pré-Fase 0 — Fechar Billing e Publicar em Produção

## Arquitetura

```
[Asaas sandbox] --POST /api/billing/webhook + header asaas-access-token-->
  [BillingController] --> valida token --> [BillingService.processarWebhook]
    --> [AssinaturaRepository.findByGatewayAssinaturaId]
    --> update status/dataProximaCobranca
    --> [EmpresaRepository.save] (atualiza plano da empresa)
```

## Decisões e Trade-offs (ADR)

### ADR-001: Validação do webhook token
**Decisão:** Validar via `@Value("${asaas.webhook.token:}")` comparando com header `asaas-access-token`.  
**Motivo:** Sem validação, qualquer ator pode forjar eventos de pagamento e ativar assinaturas sem pagar.  
**Trade-off:** Se o token não estiver configurado no Render, o endpoint aceita qualquer requisição com degradação logada como WARNING.

### ADR-002: render.yaml versionado
**Decisão:** Criar `render.yaml` na raiz do repo para ter a infra declarativa.  
**Motivo:** Evitar configuração manual e tornar a infra auditável via git.  
**Trade-off:** Variáveis secretas (API keys) ficam com `sync: false` — precisam ser configuradas manualmente no dashboard Render na primeira vez.

### ADR-003: Sem `ASAAS_ENV` na app
**Decisão:** A URL base do Asaas (`api.asaas.com` vs `sandbox.asaas.com`) é determinada pela própria API key (sandbox keys começam com `$aact_` prefixo diferente).  
**Motivo:** Simplifica configuração — o ambiente é implícito na key.  
**Trade-off:** Documentar isso claramente para o operador.

## Modelos de Dados

```
assinaturas
  id                    BIGINT PK
  empresa_id            BIGINT FK → empresas.id UNIQUE
  plano                 VARCHAR (STARTER|PRO|BUSINESS|ENTERPRISE)
  valor_mensal          DECIMAL(10,2)
  data_inicio           DATE
  data_proxima_cobranca DATE
  status                VARCHAR (TRIAL|ATIVA|INADIMPLENTE|CANCELADA)
  dias_trial            INT DEFAULT 14
  gateway_cliente_id    VARCHAR(100)
  gateway_assinatura_id VARCHAR(100)
  data_criacao          TIMESTAMP
```

## Endpoints / Contratos

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/api/billing/planos` | JWT | Lista planos e preços |
| GET | `/api/billing/minha` | JWT + empresa | Status da assinatura da empresa |
| POST | `/api/billing/checkout` | JWT + ADMIN | Inicia checkout Asaas |
| PUT | `/api/billing/upgrade` | JWT + ADMIN | Muda plano |
| POST | `/api/billing/cancelar` | JWT + ADMIN | Cancela assinatura |
| POST | `/api/billing/webhook` | Token header | Callback Asaas |

### Payload webhook (Asaas → nosso sistema)
```json
{
  "event": "PAYMENT_CONFIRMED",
  "payment": {
    "subscription": "sub_abc123",
    "value": 297.00,
    "status": "CONFIRMED"
  }
}
```

## Segurança
- Header `asaas-access-token` validado contra `${asaas.webhook.token}`.
- Endpoint `/api/billing/webhook` está em `permitAll` no SecurityConfig (sem JWT).
- Token ausente → aceita mas loga WARNING (não rejeita para evitar perda de evento em misconfiguration).
- Token presente e inválido → rejeita com `401`.

## Observabilidade
- Log `INFO` em cada mudança de status de assinatura.
- Log `WARN` em pagamento em atraso.
- Log `ERROR` em falha de parsing de webhook.
- Métrica `billing.webhook.received` via Micrometer (Fase 6).

## Plano de Rollback
- Qualquer commit pode ser revertido sem migração destrutiva: tabela `assinaturas` é aditiva.
- Se o billing quebrar, desabilitar `BillingService` via feature flag (Fase 5) ou remover a rota temporariamente.
- Backup do banco Render antes do push de produção.
