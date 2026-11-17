CREATE TABLE IF NOT EXISTS shop_items (
    code          VARCHAR(30) PRIMARY KEY,
    name          VARCHAR(60) NOT NULL,
    category      VARCHAR(20) NOT NULL,
    price         INT         NOT NULL,
    duration_ms   BIGINT      NOT NULL DEFAULT 0,
    featured      TINYINT     NOT NULL DEFAULT 0,
    active        TINYINT     NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS inventory (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user        BIGINT      NOT NULL,
    item_code   VARCHAR(30) NOT NULL,
    purchase_id VARCHAR(80) NOT NULL,
    expires_at  BIGINT      NOT NULL DEFAULT 0,
    created_at  BIGINT      NOT NULL,
    UNIQUE KEY uq_inventory_purchase (purchase_id),
    KEY idx_inventory_user (user)
);

INSERT IGNORE INTO shop_items (code, name, category, price, duration_ms, featured) VALUES
    ('flair-day', 'Daily profile flair', 'flair', 5, 86400000, 0),
    ('victory-fx', 'Arcade victory effect', 'effect', 5, 86400000, 0),
    ('gift-wrap', 'Gift wrap card', 'effect', 3, 86400000, 0),
    ('bundle-weekend', 'Weekend profile bundle', 'bundle', 16, 259200000, 1),
    ('title-week', 'Weekly title or nameplate', 'title', 15, 604800000, 1),
    ('badge-set', 'Sticker or badge set', 'collectible', 35, 0, 0),
    ('badge-month', 'Monthly collectible badge', 'collectible', 75, 0, 1)
