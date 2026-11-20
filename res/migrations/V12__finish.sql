CREATE TABLE IF NOT EXISTS achievements (
    user        BIGINT      NOT NULL,
    code        VARCHAR(30) NOT NULL,
    unlocked_at BIGINT      NOT NULL,
    PRIMARY KEY (user, code)
);

CREATE TABLE IF NOT EXISTS equipped (
    user      BIGINT      NOT NULL,
    slot      VARCHAR(20) NOT NULL,
    item_code VARCHAR(30) NOT NULL,
    PRIMARY KEY (user, slot)
);

CREATE TABLE IF NOT EXISTS banner_requests (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user           BIGINT       NOT NULL,
    role_id        BIGINT       NOT NULL,
    team_name      VARCHAR(60)  NOT NULL,
    reservation_id VARCHAR(80)  NOT NULL,
    status         VARCHAR(10)  NOT NULL,
    created_at     BIGINT       NOT NULL,
    KEY idx_banner_status (status, created_at)
);

UPDATE shop_items SET category = 'wrap' WHERE code = 'gift-wrap';
UPDATE shop_items SET active = 0 WHERE code = 'banner-club';
