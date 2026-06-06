-- =============================================================================
-- V4__multi_tenant.sql
-- Introduces row-based multi-tenancy: an `empresa` (tenant) table and an
-- `empresa_id` foreign key on every operational aggregate, on users and on
-- the audit log. All pre-existing rows are adopted by a single default
-- "Empresa Padrão" so no data is orphaned. Application code scopes every
-- read/write to the caller's empresa (see TenantResolver).
--
-- PostgreSQL only (Flyway runs in the prod profile; dev/H2 uses ddl-auto).
-- =============================================================================

CREATE TABLE IF NOT EXISTS empresa (
    id           BIGSERIAL PRIMARY KEY,
    nome         VARCHAR(150) NOT NULL,
    cnpj         VARCHAR(20),
    plano        VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    ativo        BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_empresa_cnpj ON empresa (cnpj);

-- Default tenant that adopts every pre-existing row.
INSERT INTO empresa (nome, plano, ativo, data_criacao)
VALUES ('Empresa Padrão', 'TRIAL', TRUE, now());

-- ── add the tenant column ────────────────────────────────────────────────
ALTER TABLE maquinas    ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE pecas       ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE ferramentas ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE usuario     ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE audit_log   ADD COLUMN IF NOT EXISTS empresa_id BIGINT;

-- ── backfill existing rows into the default tenant ───────────────────────
UPDATE maquinas    SET empresa_id = (SELECT id FROM empresa ORDER BY id LIMIT 1) WHERE empresa_id IS NULL;
UPDATE manutencoes SET empresa_id = (SELECT id FROM empresa ORDER BY id LIMIT 1) WHERE empresa_id IS NULL;
UPDATE pecas       SET empresa_id = (SELECT id FROM empresa ORDER BY id LIMIT 1) WHERE empresa_id IS NULL;
UPDATE ferramentas SET empresa_id = (SELECT id FROM empresa ORDER BY id LIMIT 1) WHERE empresa_id IS NULL;
UPDATE usuario     SET empresa_id = (SELECT id FROM empresa ORDER BY id LIMIT 1) WHERE empresa_id IS NULL;
UPDATE audit_log   SET empresa_id = (SELECT id FROM empresa ORDER BY id LIMIT 1) WHERE empresa_id IS NULL;

-- ── enforce NOT NULL on the tenant-owned aggregates + users ──────────────
ALTER TABLE maquinas    ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE manutencoes ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE pecas       ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE ferramentas ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE usuario     ALTER COLUMN empresa_id SET NOT NULL;
-- audit_log stays nullable: some system/auth events have no resolved tenant.

-- ── foreign keys ─────────────────────────────────────────────────────────
ALTER TABLE maquinas    ADD CONSTRAINT fk_maquina_empresa    FOREIGN KEY (empresa_id) REFERENCES empresa (id);
ALTER TABLE manutencoes ADD CONSTRAINT fk_manutencao_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id);
ALTER TABLE pecas       ADD CONSTRAINT fk_peca_empresa       FOREIGN KEY (empresa_id) REFERENCES empresa (id);
ALTER TABLE ferramentas ADD CONSTRAINT fk_ferramenta_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id);
ALTER TABLE usuario     ADD CONSTRAINT fk_usuario_empresa    FOREIGN KEY (empresa_id) REFERENCES empresa (id);
ALTER TABLE audit_log   ADD CONSTRAINT fk_audit_empresa      FOREIGN KEY (empresa_id) REFERENCES empresa (id);

-- ── indexes for tenant-scoped queries ────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_maquina_empresa    ON maquinas (empresa_id);
CREATE INDEX IF NOT EXISTS idx_manutencao_empresa ON manutencoes (empresa_id);
CREATE INDEX IF NOT EXISTS idx_peca_empresa       ON pecas (empresa_id);
CREATE INDEX IF NOT EXISTS idx_ferramenta_empresa ON ferramentas (empresa_id);
CREATE INDEX IF NOT EXISTS idx_usuario_empresa    ON usuario (empresa_id);
CREATE INDEX IF NOT EXISTS idx_audit_empresa      ON audit_log (empresa_id);
