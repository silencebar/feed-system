CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    token TEXT,
    refresh_token VARCHAR(255),
    avatar_url VARCHAR(512),
    bio VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_accounts_username (username),
    KEY idx_accounts_refresh_token (refresh_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
