-- =============================================================================
-- V3__soft_delete.sql
-- Adds the `deleted_at` column to user-facing aggregates so deletes never
-- destroy data — auditing, compliance and historical reports rely on the
-- ability to recover removed rows.
--
-- Convention:
--   deleted_at IS NULL  -> active row (default)
--   deleted_at = ts     -> soft-deleted at `ts`
--
-- Hibernate uses @SQLRestriction("deleted_at IS NULL") on the entities, so
-- every find/repository query filters automatically. The reports and audit
-- services bypass the restriction with native queries when needed.
-- =============================================================================

ALTER TABLE usuario     ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE maquinas    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Partial indexes so queries on active rows stay sargable without paying
-- the price of indexing thousands of dead rows.
CREATE INDEX IF NOT EXISTS idx_usuario_active
    ON usuario (id)     WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_maquinas_active
    ON maquinas (id)    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_manutencoes_active
    ON manutencoes (id) WHERE deleted_at IS NULL;
