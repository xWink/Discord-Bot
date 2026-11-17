CREATE TABLE IF NOT EXISTS blackjack_games (
    user           BIGINT       PRIMARY KEY,
    bet            INT          NOT NULL,
    reservation_id VARCHAR(80)  NOT NULL,
    player_hand    VARCHAR(255) NOT NULL,
    dealer_hand    VARCHAR(255) NOT NULL,
    deck           TEXT         NOT NULL,
    channel_id     BIGINT       NOT NULL,
    last_action    BIGINT       NOT NULL,
    created_at     BIGINT       NOT NULL
);
