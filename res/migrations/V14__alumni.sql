CREATE TABLE IF NOT EXISTS alumni (
    user       BIGINT       PRIMARY KEY,
    granted_by BIGINT       NOT NULL,
    note       VARCHAR(200) NULL,
    granted_at BIGINT       NOT NULL
);
