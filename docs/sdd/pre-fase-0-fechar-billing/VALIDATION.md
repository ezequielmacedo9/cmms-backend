# Validation: Pré-Fase 0 — Fechar Billing

## Variáveis de Ambiente (configurar manualmente no Render Dashboard)

| Variável | Descrição | Onde obter |
|---|---|---|
| `ASAAS_API_KEY` | API key do Asaas sandbox | https://sandbox.asaas.com → Integrações → API Key |
| `ASAAS_WEBHOOK_TOKEN` | Token para validar webhooks | Criar qualquer string aleatória; registrar no Asaas em Integrações → Webhooks |
| `DATABASE_URL` | URL do PostgreSQL Render | Render Dashboard → Database → Connection String |
| `JWT_SECRET` | Segredo JWT (≥32 chars) | Gerar com `openssl rand -hex 32` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP para alertas | Gmail App Password |
| `GOOGLE_CLIENT_ID` | OAuth Google | Google Cloud Console |
| `FRONTEND_URL` | URL do frontend Vercel | https://seu-dominio.vercel.app |
| `CORS_ALLOWED_ORIGINS` | Mesma URL do frontend | Idem |

## Comandos de Smoke Test

### 1. Healthcheck
```bash
curl https://cmms-backend-8y7h.onrender.com/actuator/health
# esperado: {"status":"UP"}
```

### 2. Login e obter token
```bash
TOKEN=$(curl -s -X POST https://cmms-backend-8y7h.onrender.com/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@email.com","senha":"123456"}' | jq -r '.accessToken')
echo $TOKEN
```

### 3. Ver assinatura atual
```bash
curl -s https://cmms-backend-8y7h.onrender.com/api/billing/minha \
  -H "Authorization: Bearer $TOKEN" | jq .
# esperado: status: "TRIAL"
```

### 4. Simular webhook PAYMENT_CONFIRMED
```bash
curl -s -X POST https://cmms-backend-8y7h.onrender.com/api/billing/webhook \
  -H 'Content-Type: application/json' \
  -H 'asaas-access-token: SEU_ASAAS_WEBHOOK_TOKEN' \
  -w "\nHTTP %{http_code} — %{time_total}s\n" \
  -d '{
    "event": "PAYMENT_CONFIRMED",
    "payment": {
      "subscription": "GATEWAY_SUB_ID_AQUI",
      "value": 297.00,
      "status": "CONFIRMED"
    }
  }'
# esperado: HTTP 200 — <0.5s
```

### 5. Simular webhook com token inválido
```bash
curl -s -X POST https://cmms-backend-8y7h.onrender.com/api/billing/webhook \
  -H 'Content-Type: application/json' \
  -H 'asaas-access-token: token-errado' \
  -w "\nHTTP %{http_code}\n" \
  -d '{"event":"PAYMENT_CONFIRMED","payment":{"subscription":"x"}}'
# esperado: HTTP 401
```

### 6. Verificar status após webhook
```bash
curl -s https://cmms-backend-8y7h.onrender.com/api/billing/minha \
  -H "Authorization: Bearer $TOKEN" | jq '.status'
# esperado: "ATIVA"
```

## Checklist de Produção

- [ ] Webhook responde 200 em < 500ms
- [ ] Webhook com token errado retorna 401
- [ ] Status muda TRIAL → ATIVA após PAYMENT_CONFIRMED
- [ ] Logs do Render: sem stacktrace nas últimas 100 linhas
- [ ] Console do navegador: sem erros de CORS
- [ ] `securityheaders.com` para o frontend: nota mínima B (após Fase 1 deve ser A+)
