CREATE TABLE IF NOT EXISTS grade_items (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user    BIGINT        NOT NULL,
    code    VARCHAR(12)   NOT NULL,
    name    VARCHAR(40)   NOT NULL,
    weight  DECIMAL(5,2)  NOT NULL,
    mark    DECIMAL(5,2)  NULL,
    created BIGINT        NOT NULL,
    UNIQUE KEY one_name (user, code, name),
    INDEX by_user (user, code)
);
