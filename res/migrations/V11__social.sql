CREATE TABLE IF NOT EXISTS duel_ratings (
    user       BIGINT PRIMARY KEY,
    rating     INT    NOT NULL DEFAULT 1000,
    matches    INT    NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS duel_matches (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_a    BIGINT  NOT NULL,
    user_b    BIGINT  NOT NULL,
    stake     INT     NOT NULL,
    winner    BIGINT  NULL,
    ranked    TINYINT NOT NULL,
    played_at BIGINT  NOT NULL,
    KEY idx_duel_pair (user_a, user_b, played_at)
);

CREATE TABLE IF NOT EXISTS bounties (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    poster         BIGINT       NOT NULL,
    description    VARCHAR(200) NOT NULL,
    reward         INT          NOT NULL,
    reservation_id VARCHAR(80)  NOT NULL,
    status         VARCHAR(12)  NOT NULL,
    claimer        BIGINT       NULL,
    created_at     BIGINT       NOT NULL,
    expires_at     BIGINT       NOT NULL,
    KEY idx_bounty_status (status, expires_at)
);

CREATE TABLE IF NOT EXISTS events (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    host       BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    status     VARCHAR(10)  NOT NULL,
    created_at BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS event_participants (
    event_id BIGINT NOT NULL,
    user     BIGINT NOT NULL,
    PRIMARY KEY (event_id, user)
);

CREATE TABLE IF NOT EXISTS quiz_runs (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user      BIGINT      NOT NULL,
    week      INT         NOT NULL,
    mode      VARCHAR(10) NOT NULL,
    correct   INT         NOT NULL,
    total     INT         NOT NULL,
    paid      TINYINT     NOT NULL,
    played_at BIGINT      NOT NULL,
    KEY idx_runs_user (user, week, mode)
);

CREATE TABLE IF NOT EXISTS board_optout (
    user BIGINT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS season_snapshots (
    season INT         NOT NULL,
    board  VARCHAR(20) NOT NULL,
    rank   INT         NOT NULL,
    user   BIGINT      NOT NULL,
    score  INT         NOT NULL,
    PRIMARY KEY (season, board, rank)
);

CREATE TABLE IF NOT EXISTS colour_plans (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user       BIGINT      NOT NULL,
    role_id    BIGINT      NOT NULL,
    colour     VARCHAR(40) NOT NULL,
    target     INT         NOT NULL,
    paid       INT         NOT NULL DEFAULT 0,
    status     VARCHAR(10) NOT NULL,
    created_at BIGINT      NOT NULL,
    KEY idx_plan_user (user, status)
);

ALTER TABLE shop_items ADD COLUMN IF NOT EXISTS base_price INT NOT NULL DEFAULT 0;
ALTER TABLE shop_items ADD COLUMN IF NOT EXISTS prev_price INT NULL;
ALTER TABLE shop_items ADD COLUMN IF NOT EXISTS rotating TINYINT NOT NULL DEFAULT 0;
ALTER TABLE shop_items ADD COLUMN IF NOT EXISTS price_updated_at BIGINT NOT NULL DEFAULT 0;

UPDATE shop_items SET base_price = price WHERE base_price = 0;

INSERT IGNORE INTO shop_items (code, name, category, price, base_price, duration_ms, featured, rotating) VALUES
    ('event-pass', 'Event hosting pass', 'pass', 25, 25, 0, 0, 0),
    ('banner-club', 'Club or team banner (1 week)', 'banner', 45, 45, 604800000, 0, 1);

UPDATE shop_items SET rotating = 1 WHERE code IN ('bundle-weekend', 'title-week', 'badge-month', 'badge-set',
    'victory-fx', 'flair-day');
