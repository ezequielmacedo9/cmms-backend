-- =============================================================================
-- V2__performance_indexes.sql
-- Indexes used by hot-path queries identified after the FASE 2C profiling:
--
--   * Auth flow: every login looks up refresh_token by usuario_id (delete-and-replace).
--   * Estoque  : peca.codigo and ferramenta.codigo are surfaced in search/scan.
--   * Reset    : password_reset_token.usuario_id is used during cleanup of expired rows.
--   * Audit    : (acao, timestamp) speeds up the common "X actions last week" reports.
--   * Maquinas : preventive scanner filters by intervalo_preventiva_dias > 0.
--
-- All indexes are created `IF NOT EXISTS` so re-runs against a baseline schema
-- never fail.
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_refresh_token_usuario_id
    ON refresh_token (usuario_id);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_usuario_id
    ON password_reset_token (usuario_id);

CREATE INDEX IF NOT EXISTS idx_peca_codigo
    ON pecas (codigo);

CREATE INDEX IF NOT EXISTS idx_ferramenta_codigo
    ON ferramentas (codigo);

CREATE INDEX IF NOT EXISTS idx_audit_acao_ts
    ON audit_log (acao, "timestamp");

-- Speeds up the hourly scheduler that scans for overdue preventives.
CREATE INDEX IF NOT EXISTS idx_maquina_intervalo
    ON maquinas (intervalo_preventiva_dias)
    WHERE intervalo_preventiva_dias IS NOT NULL AND intervalo_preventiva_dias > 0;
