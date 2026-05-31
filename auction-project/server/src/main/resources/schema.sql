-- CREATE TABLES
CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(64) PRIMARY KEY,
    full_name     VARCHAR(255) NOT NULL,
    username      VARCHAR(255) UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone_number  VARCHAR(32),
    gender        VARCHAR(32),
    date_of_birth VARCHAR(16),
    created_at    VARCHAR(32) NOT NULL,
    active        TINYINT NOT NULL DEFAULT 1,
    role          VARCHAR(16) NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_details (
    user_id     VARCHAR(64) PRIMARY KEY,
    admin_notes TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS seller_details (
    user_id      VARCHAR(64) PRIMARY KEY,
    shop_name    VARCHAR(255) NOT NULL,
    citizen_id   VARCHAR(32) NOT NULL UNIQUE,
    balance      DOUBLE NOT NULL DEFAULT 0.0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bidder_details (
    user_id           VARCHAR(64) PRIMARY KEY,
    deposit_balance   DOUBLE NOT NULL DEFAULT 0.0,
    frozen_balance    DOUBLE NOT NULL DEFAULT 0.0,
    shipping_address  TEXT,
    total_bids_placed INT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS items (
    id            VARCHAR(64) PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    base_price    DOUBLE NOT NULL,
    min_increment DOUBLE NOT NULL,
    seller_id     VARCHAR(64) NOT NULL,
    category      VARCHAR(32) NOT NULL,
    image_url     TEXT,
    available     TINYINT NOT NULL DEFAULT 1,
    listed_at     VARCHAR(32) NOT NULL,
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS electronics_details (
    item_id          VARCHAR(64) PRIMARY KEY,
    brand            VARCHAR(255) NOT NULL,
    model            VARCHAR(255) NOT NULL,
    warranty_months  INT NOT NULL DEFAULT 0,
    condition_type   VARCHAR(64) NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS art_details (
    item_id          VARCHAR(64) PRIMARY KEY,
    artist_name      VARCHAR(255) NOT NULL,
    creation_year    INT DEFAULT 0,
    medium           VARCHAR(255) NOT NULL,
    authenticated    TINYINT NOT NULL DEFAULT 0,
    certificate_id   VARCHAR(255),
    dimensions       VARCHAR(255),
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS vehicle_details (
    item_id            VARCHAR(64) PRIMARY KEY,
    vehicle_type       VARCHAR(64) NOT NULL,
    make               VARCHAR(255) NOT NULL,
    model              VARCHAR(255) NOT NULL,
    year               INT NOT NULL,
    mileage            DOUBLE NOT NULL DEFAULT -1,
    fuel_type          VARCHAR(64) NOT NULL,
    transmission       VARCHAR(64) NOT NULL,
    color              VARCHAR(64),
    license_plate      VARCHAR(64),
    has_valid_registry TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auction_sessions (
    id                    VARCHAR(64) PRIMARY KEY,
    item_id               VARCHAR(64) NOT NULL,
    seller_id             VARCHAR(64) NOT NULL,
    seller_name           VARCHAR(255) NOT NULL,
    current_price         DOUBLE NOT NULL,
    current_winner_id     VARCHAR(64),
    current_winner_name   VARCHAR(255),
    status                VARCHAR(32) NOT NULL,
    start_time            VARCHAR(32) NOT NULL,
    end_time              VARCHAR(32) NOT NULL,
    actual_end_time       VARCHAR(32) NOT NULL,
    created_at            VARCHAR(32) NOT NULL,
    anti_sniping_seconds  INT NOT NULL DEFAULT 30,
    approved_by_admin_id  VARCHAR(64),
    admin_note            TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bids (
    id                  VARCHAR(64) PRIMARY KEY,
    auction_session_id  VARCHAR(64) NOT NULL,
    bidder_id           VARCHAR(64),
    bidder_name         VARCHAR(255) NOT NULL,
    amount              DOUBLE NOT NULL,
    max_auto_bid        DOUBLE,
    timestamp           VARCHAR(32) NOT NULL,
    bid_type            VARCHAR(32) NOT NULL,
    FOREIGN KEY (auction_session_id) REFERENCES auction_sessions(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);
