-- =============================================================================
-- V4__multi_tenant.sql
-- Introduces row-based multi-tenancy: an `empresa` (tenant) table and an
-- `empresa_id` foreign key on every operational aggregate, on users and on
-- the audit log. All pre-existing rows are adopted by a single default
-- "Empresa Padrão" (id=1, explicit) so no data is orphaned. Application code
-- scopes every read/write to the caller's empresa (see TenantResolver).
--
-- Idempotent by design: safe against legacy prod databases that were managed
-- by Hibernate ddl-auto before Flyway (baseline-on-migrate). In particular,
-- old deploys may have left a legacy plural `empresas` table plus
-- Hibernate-generated FKs on `empresa_id` pointing at it — those are dropped
-- before the backfill, otherwise the UPDATEs violate the stale constraint.
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

-- ── legacy cleanup ───────────────────────────────────────────────────────
-- Drop every FK that still points at a legacy Hibernate-era `empresas`
-- table (plural). Names are Hibernate-generated (FKxxxx...), so we discover
-- them via the catalog instead of guessing.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN
        SELECT con.conname, rel.relname AS tabela
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_class ref ON ref.oid = con.confrelid
        WHERE con.contype = 'f'
          AND ref.relname = 'empresas'
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', r.tabela, r.conname);
    END LOOP;
END $$;

-- Drop the legacy plural table itself, but only when it carries no data
-- (the application entity maps to `empresa`, so any legacy rows would be
-- unreachable — we keep the table for manual inspection in that case).
DO $$
BEGIN
    IF to_regclass('public.empresas') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM empresas) THEN
        EXECUTE 'DROP TABLE empresas';
    END IF;
END $$;

-- ── default tenant (explicit id, idempotent) ─────────────────────────────
-- Created BEFORE any backfill so the literal id=1 below is guaranteed valid.
INSERT INTO empresa (id, nome, plano, ativo, data_criacao)
VALUES (1, 'Empresa Padrão', 'TRIAL', TRUE, now())
ON CONFLICT (id) DO NOTHING;

-- Keep the sequence ahead of the explicit insert so future application
-- inserts never collide with id=1.
SELECT setval(pg_get_serial_sequence('empresa', 'id'),
              GREATEST((SELECT COALESCE(MAX(id), 1) FROM empresa), 1));

-- ── add the tenant column ────────────────────────────────────────────────
ALTER TABLE maquinas    ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE pecas       ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE ferramentas ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE usuario     ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE audit_log   ADD COLUMN IF NOT EXISTS empresa_id BIGINT;

-- ── backfill existing rows into the default tenant (id=1, guaranteed) ────
UPDATE maquinas    SET empresa_id = 1 WHERE empresa_id IS NULL;
UPDATE manutencoes SET empresa_id = 1 WHERE empresa_id IS NULL;
UPDATE pecas       SET empresa_id = 1 WHERE empresa_id IS NULL;
UPDATE ferramentas SET empresa_id = 1 WHERE empresa_id IS NULL;
UPDATE usuario     SET empresa_id = 1 WHERE empresa_id IS NULL;
UPDATE audit_log   SET empresa_id = 1 WHERE empresa_id IS NULL;

-- ── enforce NOT NULL on the tenant-owned aggregates + users ──────────────
ALTER TABLE maquinas    ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE manutencoes ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE pecas       ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE ferramentas ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE usuario     ALTER COLUMN empresa_id SET NOT NULL;
-- audit_log stays nullable: some system/auth events have no resolved tenant.

-- ── foreign keys (drop-then-add keeps the script re-runnable) ────────────
ALTER TABLE maquinas    DROP CONSTRAINT IF EXISTS fk_maquina_empresa;
ALTER TABLE manutencoes DROP CONSTRAINT IF EXISTS fk_manutencao_empresa;
ALTER TABLE pecas       DROP CONSTRAINT IF EXISTS fk_peca_empresa;
ALTER TABLE ferramentas DROP CONSTRAINT IF EXISTS fk_ferramenta_empresa;
ALTER TABLE usuario     DROP CONSTRAINT IF EXISTS fk_usuario_empresa;
ALTER TABLE audit_log   DROP CONSTRAINT IF EXISTS fk_audit_empresa;

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
