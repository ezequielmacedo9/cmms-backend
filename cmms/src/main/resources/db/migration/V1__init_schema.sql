-- =============================================================================
-- V1__init_schema.sql
-- Initial schema for CMMS — derived from the JPA entities as of v0.0.1.
--
-- Targets PostgreSQL. Flyway runs ONLY in the `prod` profile (see
-- application-prod.properties); H2 in dev keeps using Hibernate ddl-auto.
--
-- Existing prod databases were previously managed by Hibernate ddl-auto with
-- the same physical names, so `baseline-on-migrate=true` lets Flyway adopt
-- the live schema as the V0 baseline. This file becomes the canonical source
-- of truth for new environments.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Roles
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id   BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    CONSTRAINT uk_roles_nome UNIQUE (nome)
);

-- -----------------------------------------------------------------------------
-- Usuario (entity has no explicit @Table → physical name = `usuario`)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario (
    id                     BIGSERIAL PRIMARY KEY,
    email                  VARCHAR(255) NOT NULL,
    senha                  VARCHAR(255) NOT NULL,
    nome                   VARCHAR(255),
    telefone               VARCHAR(20),
    cargo                  VARCHAR(100),
    departamento           VARCHAR(100),
    avatar_base64          TEXT,
    google_id              VARCHAR(255),
    totp_secret            VARCHAR(40),
    totp_enabled           BOOLEAN DEFAULT FALSE,
    failed_login_attempts  INTEGER DEFAULT 0,
    locked_until           TIMESTAMP,
    ultimo_login           TIMESTAMP,
    ativo                  BOOLEAN,
    data_criacao           TIMESTAMP,
    role_id                BIGINT,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_role  FOREIGN KEY (role_id) REFERENCES roles (id)
);
CREATE INDEX IF NOT EXISTS idx_usuario_email ON usuario (email);

-- -----------------------------------------------------------------------------
-- Refresh token (entity has no @Table → `refresh_token`)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_token (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(255) NOT NULL,
    usuario_id BIGINT,
    expiracao  TIMESTAMP NOT NULL,
    CONSTRAINT uk_refresh_token_token UNIQUE (token),
    CONSTRAINT fk_refresh_token_user  FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_token (usuario_id);

-- -----------------------------------------------------------------------------
-- Password reset token
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_token (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(64) NOT NULL,
    usuario_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    usado      BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em  TIMESTAMP,
    CONSTRAINT uk_prt_token   UNIQUE (token),
    CONSTRAINT fk_prt_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
CREATE INDEX IF NOT EXISTS idx_prt_user ON password_reset_token (usuario_id);

-- -----------------------------------------------------------------------------
-- Audit log
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id         BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255),
    user_id    BIGINT,
    acao       VARCHAR(100) NOT NULL,
    recurso    VARCHAR(100),
    recurso_id BIGINT,
    detalhes   TEXT,
    ip         VARCHAR(50),
    timestamp  TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_log (user_email);
CREATE INDEX IF NOT EXISTS idx_audit_ts   ON audit_log (timestamp);

-- -----------------------------------------------------------------------------
-- System configuration (key/value store)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configuracao_sistema (
    chave     VARCHAR(100) PRIMARY KEY,
    valor     TEXT,
    grupo     VARCHAR(50),
    tipo      VARCHAR(20),
    descricao VARCHAR(200)
);

-- -----------------------------------------------------------------------------
-- Maquinas
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS maquinas (
    id                          BIGSERIAL PRIMARY KEY,
    nome                        VARCHAR(255) NOT NULL,
    setor                       VARCHAR(255) NOT NULL,
    data_ultima_manutencao      DATE,
    status                      VARCHAR(255) NOT NULL,
    intervalo_preventiva_dias   INTEGER,
    prioridade                  VARCHAR(20)
);
CREATE INDEX IF NOT EXISTS idx_maquina_status     ON maquinas (status);
CREATE INDEX IF NOT EXISTS idx_maquina_prioridade ON maquinas (prioridade);

-- -----------------------------------------------------------------------------
-- Manutencoes
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS manutencoes (
    id              BIGSERIAL PRIMARY KEY,
    tipo            VARCHAR(255) NOT NULL,
    data_manutencao DATE,
    prioridade      VARCHAR(20),
    status          VARCHAR(20),
    maquina_id      BIGINT NOT NULL,
    tecnico         VARCHAR(255) NOT NULL,
    descricao       VARCHAR(500),
    CONSTRAINT fk_manutencao_maquina FOREIGN KEY (maquina_id) REFERENCES maquinas (id)
);
CREATE INDEX IF NOT EXISTS idx_manutencao_tipo    ON manutencoes (tipo);
CREATE INDEX IF NOT EXISTS idx_manutencao_data    ON manutencoes (data_manutencao);
CREATE INDEX IF NOT EXISTS idx_manutencao_maquina ON manutencoes (maquina_id);
CREATE INDEX IF NOT EXISTS idx_manutencao_status  ON manutencoes (status);

-- -----------------------------------------------------------------------------
-- Pecas
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pecas (
    id                     BIGSERIAL PRIMARY KEY,
    nome                   VARCHAR(255) NOT NULL,
    codigo                 VARCHAR(255) NOT NULL,
    vida_util_horas        INTEGER NOT NULL,
    quantidade_em_estoque  INTEGER NOT NULL,
    custo_unitario         DOUBLE PRECISION NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_peca_codigo ON pecas (codigo);

-- -----------------------------------------------------------------------------
-- Ferramentas
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ferramentas (
    id                      BIGSERIAL PRIMARY KEY,
    nome                    VARCHAR(255) NOT NULL,
    codigo                  VARCHAR(255),
    status                  VARCHAR(255),
    localizacao             VARCHAR(255),
    responsavel             VARCHAR(255),
    data_ultima_manutencao  DATE
);
CREATE INDEX IF NOT EXISTS idx_ferramenta_codigo ON ferramentas (codigo);

-- -----------------------------------------------------------------------------
-- Join table: manutencao_pecas (many-to-many)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS manutencao_pecas (
    manutencao_id BIGINT NOT NULL,
    peca_id       BIGINT NOT NULL,
    CONSTRAINT fk_mp_manutencao FOREIGN KEY (manutencao_id) REFERENCES manutencoes (id),
    CONSTRAINT fk_mp_peca       FOREIGN KEY (peca_id)       REFERENCES pecas (id)
);
CREATE INDEX IF NOT EXISTS idx_mp_manutencao ON manutencao_pecas (manutencao_id);
CREATE INDEX IF NOT EXISTS idx_mp_peca       ON manutencao_pecas (peca_id);

-- -----------------------------------------------------------------------------
-- Join table: manutencao_ferramentas (many-to-many)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS manutencao_ferramentas (
    manutencao_id BIGINT NOT NULL,
    ferramenta_id BIGINT NOT NULL,
    CONSTRAINT fk_mf_manutencao FOREIGN KEY (manutencao_id) REFERENCES manutencoes (id),
    CONSTRAINT fk_mf_ferramenta FOREIGN KEY (ferramenta_id) REFERENCES ferramentas (id)
);
CREATE INDEX IF NOT EXISTS idx_mf_manutencao ON manutencao_ferramentas (manutencao_id);
CREATE INDEX IF NOT EXISTS idx_mf_ferramenta ON manutencao_ferramentas (ferramenta_id);
