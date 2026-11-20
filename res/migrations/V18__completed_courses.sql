CREATE TABLE IF NOT EXISTS completed_courses (
    user BIGINT      NOT NULL,
    code VARCHAR(12) NOT NULL,
    PRIMARY KEY (user, code)
);
