CREATE TABLE IF NOT EXISTS economy_ledger (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    op_id         VARCHAR(80) NOT NULL,
    user          BIGINT      NOT NULL,
    delta         INT         NOT NULL,
    balance_after INT         NOT NULL,
    reason        VARCHAR(40) NOT NULL,
    ref           VARCHAR(80) NULL,
    created_at    BIGINT      NOT NULL,
    UNIQUE KEY uq_ledger_op (op_id, user),
    KEY idx_ledger_user (user, id)
);

CREATE TABLE IF NOT EXISTS economy_reservations (
    reservation_id VARCHAR(80) PRIMARY KEY,
    user           BIGINT      NOT NULL,
    amount         INT         NOT NULL,
    reason         VARCHAR(40) NOT NULL,
    ref            VARCHAR(80) NULL,
    created_at     BIGINT      NOT NULL,
    KEY idx_reservation_user (user)
);
