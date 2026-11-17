CREATE TABLE IF NOT EXISTS community_goals (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(120) NOT NULL,
    target     INT          NOT NULL,
    raised     INT          NOT NULL DEFAULT 0,
    status     VARCHAR(12)  NOT NULL,
    created_at BIGINT       NOT NULL,
    reached_at BIGINT       NULL
);

CREATE TABLE IF NOT EXISTS goal_contributions (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id    BIGINT      NOT NULL,
    user       BIGINT      NOT NULL,
    amount     INT         NOT NULL,
    op_id      VARCHAR(80) NOT NULL,
    reversed   TINYINT     NOT NULL DEFAULT 0,
    created_at BIGINT      NOT NULL,
    KEY idx_contrib_goal (goal_id)
);
