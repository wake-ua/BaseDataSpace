DROP TYPE IF EXISTS role_enum CASCADE;
CREATE TYPE role_enum AS ENUM ('DEFAULT', 'WEB', 'SEARCH');

-- =========================
-- Providers table
-- =========================
CREATE TABLE IF NOT EXISTS providers (
    provider_id VARCHAR(36) PRIMARY KEY,
    password VARCHAR(255),
    share_token VARCHAR(255)
);

-- =========================
-- Admins table
-- =========================
CREATE TABLE IF NOT EXISTS admins (
    id VARCHAR(36) PRIMARY KEY,
    token VARCHAR(255),
    role role_enum NOT NULL DEFAULT 'DEFAULT'
);

-- =========================
-- Sample table
-- =========================
CREATE TABLE IF NOT EXISTS samples (
    id VARCHAR(36) PRIMARY KEY,
    asset_id VARCHAR(255),
    path VARCHAR(255),

    provider_id VARCHAR(36) NOT NULL,

    CONSTRAINT fk_provider
        FOREIGN KEY (provider_id)
        REFERENCES providers(provider_id)
        ON DELETE CASCADE
);
