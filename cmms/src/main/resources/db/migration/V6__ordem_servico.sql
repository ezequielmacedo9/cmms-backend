-- =============================================================================
-- V6__ordem_servico.sql
-- Enriches manutenções into work orders: responsible user, labor time/cost,
-- open/close dates (for MTTR) and a parts-consumption table that drives stock
-- deduction and cost-per-asset reporting. PostgreSQL only (prod).
-- =============================================================================

ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS tecnico_id              BIGINT;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS tempo_execucao_minutos  INTEGER;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS custo_mao_obra          DOUBLE PRECISION;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS data_abertura           DATE;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS data_conclusao          DATE;

-- Backfill: existing maintenances are considered opened on their maintenance date.
UPDATE manutencoes SET data_abertura = data_manutencao WHERE data_abertura IS NULL;

CREATE TABLE IF NOT EXISTS manutencao_peca (
    id             BIGSERIAL PRIMARY KEY,
    empresa_id     BIGINT NOT NULL,
    manutencao_id  BIGINT NOT NULL,
    peca_id        BIGINT NOT NULL,
    peca_nome      VARCHAR(255),
    quantidade     INTEGER NOT NULL,
    custo_unitario DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_mpeca_empresa    FOREIGN KEY (empresa_id)    REFERENCES empresa (id),
    CONSTRAINT fk_mpeca_manutencao FOREIGN KEY (manutencao_id) REFERENCES manutencoes (id),
    CONSTRAINT fk_mpeca_peca       FOREIGN KEY (peca_id)       REFERENCES pecas (id)
);
CREATE INDEX IF NOT EXISTS idx_mpeca_manutencao ON manutencao_peca (manutencao_id);
CREATE INDEX IF NOT EXISTS idx_mpeca_empresa    ON manutencao_peca (empresa_id);
