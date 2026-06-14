-- =============================================================================
-- V5__assinatura.sql
-- One subscription per empresa. Every existing empresa is backfilled with a
-- TRIAL/STARTER subscription so the billing page works for legacy tenants.
-- PostgreSQL only (prod); dev/H2 uses ddl-auto.
-- =============================================================================

CREATE TABLE IF NOT EXISTS assinatura (
    id                     BIGSERIAL PRIMARY KEY,
    empresa_id             BIGINT NOT NULL,
    plano                  VARCHAR(30) NOT NULL DEFAULT 'STARTER',
    status                 VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    data_inicio            DATE,
    trial_fim              DATE,
    data_proxima_cobranca  DATE,
    CONSTRAINT uk_assinatura_empresa UNIQUE (empresa_id),
    CONSTRAINT fk_assinatura_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

-- Backfill: a 14-day TRIAL on STARTER for every empresa without a subscription.
INSERT INTO assinatura (empresa_id, plano, status, data_inicio, trial_fim, data_proxima_cobranca)
SELECT e.id, 'STARTER', 'TRIAL', CURRENT_DATE, CURRENT_DATE + 14, CURRENT_DATE + 14
FROM empresa e
WHERE NOT EXISTS (SELECT 1 FROM assinatura a WHERE a.empresa_id = e.id);
