-- =============================================================================
-- V8__configuracao_por_empresa.sql
-- configuracao_sistema becomes per-tenant: surrogate PK + empresa_id, with the
-- old global rows replicated to every existing empresa (then removed).
-- =============================================================================

-- 1) Drop the old natural PK (chave) and add the new columns.
ALTER TABLE configuracao_sistema DROP CONSTRAINT IF EXISTS configuracao_sistema_pkey;
ALTER TABLE configuracao_sistema ADD COLUMN IF NOT EXISTS id BIGSERIAL;
ALTER TABLE configuracao_sistema ADD COLUMN IF NOT EXISTS empresa_id BIGINT;

-- 2) Replicate the current global rows to every empresa.
INSERT INTO configuracao_sistema (chave, valor, grupo, tipo, descricao, empresa_id)
SELECT c.chave, c.valor, c.grupo, c.tipo, c.descricao, e.id
FROM configuracao_sistema c
CROSS JOIN empresa e
WHERE c.empresa_id IS NULL;

-- 3) Remove the old global rows and lock the schema down.
DELETE FROM configuracao_sistema WHERE empresa_id IS NULL;

ALTER TABLE configuracao_sistema ALTER COLUMN empresa_id SET NOT NULL;
ALTER TABLE configuracao_sistema ALTER COLUMN chave SET NOT NULL;
ALTER TABLE configuracao_sistema ADD CONSTRAINT configuracao_sistema_pkey PRIMARY KEY (id);
ALTER TABLE configuracao_sistema
    ADD CONSTRAINT uq_config_empresa_chave UNIQUE (empresa_id, chave);
ALTER TABLE configuracao_sistema
    ADD CONSTRAINT fk_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id);
CREATE INDEX IF NOT EXISTS idx_config_empresa ON configuracao_sistema (empresa_id);
