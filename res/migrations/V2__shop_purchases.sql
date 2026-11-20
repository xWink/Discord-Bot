CREATE TABLE IF NOT EXISTS shop_purchases (
    purchase_id   VARCHAR(80) PRIMARY KEY,
    user          BIGINT      NOT NULL,
    listing       VARCHAR(80) NOT NULL,
    cost          INT         NOT NULL,
    role_id       BIGINT      NOT NULL,
    duration_days INT         NOT NULL,
    status        VARCHAR(16) NOT NULL,
    created_at    BIGINT      NOT NULL,
    updated_at    BIGINT      NOT NULL,
    KEY idx_purchase_status (status)
);

ALTER TABLE economy ADD COLUMN IF NOT EXISTS role_id BIGINT NULL;
