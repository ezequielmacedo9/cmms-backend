-- =============================================================================
-- V7__checklist_anexos.sql
-- Work-order checklist items and attachments (files stored as base64 for now).
-- PostgreSQL only (prod); dev/H2 uses ddl-auto.
-- =============================================================================

CREATE TABLE IF NOT EXISTS manutencao_checklist (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT NOT NULL,
    manutencao_id BIGINT NOT NULL,
    descricao     VARCHAR(300) NOT NULL,
    concluido     BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_checklist_empresa    FOREIGN KEY (empresa_id)    REFERENCES empresa (id),
    CONSTRAINT fk_checklist_manutencao FOREIGN KEY (manutencao_id) REFERENCES manutencoes (id)
);
CREATE INDEX IF NOT EXISTS idx_checklist_manutencao ON manutencao_checklist (manutencao_id);
CREATE INDEX IF NOT EXISTS idx_checklist_empresa    ON manutencao_checklist (empresa_id);

CREATE TABLE IF NOT EXISTS manutencao_anexo (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT NOT NULL,
    manutencao_id BIGINT NOT NULL,
    nome          VARCHAR(255) NOT NULL,
    content_type  VARCHAR(120),
    tamanho       INTEGER,
    dados_base64  TEXT NOT NULL,
    criado_em     TIMESTAMP,
    CONSTRAINT fk_anexo_empresa    FOREIGN KEY (empresa_id)    REFERENCES empresa (id),
    CONSTRAINT fk_anexo_manutencao FOREIGN KEY (manutencao_id) REFERENCES manutencoes (id)
);
CREATE INDEX IF NOT EXISTS idx_anexo_manutencao ON manutencao_anexo (manutencao_id);
CREATE INDEX IF NOT EXISTS idx_anexo_empresa    ON manutencao_anexo (empresa_id);
