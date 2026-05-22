-- H2 Test Schema for file-service
-- Note: H2 uses different syntax than MySQL

-- file_info table
CREATE TABLE IF NOT EXISTS file_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_uuid       VARCHAR(36) UNIQUE NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    original_name   VARCHAR(255) NOT NULL,
    file_type       VARCHAR(50) DEFAULT NULL,
    file_size       BIGINT DEFAULT 0,
    storage_path    VARCHAR(500) NOT NULL,
    summary         CLOB DEFAULT NULL,
    keywords        VARCHAR(1000) DEFAULT NULL,
    category        VARCHAR(100) DEFAULT '其他',
    version         INT DEFAULT 1,
    version_history VARCHAR(2000) DEFAULT NULL,
    indexed         BOOLEAN DEFAULT TRUE,
    vector_id       VARCHAR(100) DEFAULT NULL,
    index_time      TIMESTAMP DEFAULT NULL,
    process_status  VARCHAR(20) DEFAULT 'PENDING',
    process_time    TIMESTAMP DEFAULT NULL,
    retry_count     INT DEFAULT 0,
    user_id         BIGINT NOT NULL,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    delete_time     TIMESTAMP DEFAULT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- file_share table
CREATE TABLE IF NOT EXISTS file_share (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_uuid       VARCHAR(36) NOT NULL,
    owner_id        BIGINT NOT NULL,
    share_to_id     BIGINT NOT NULL,
    permission      VARCHAR(20) DEFAULT 'VIEW',
    expire_time     TIMESTAMP DEFAULT NULL,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- public_share table
CREATE TABLE IF NOT EXISTS public_share (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    share_code      VARCHAR(32) UNIQUE NOT NULL,
    file_uuid       VARCHAR(36) NOT NULL,
    owner_id        BIGINT NOT NULL,
    password        VARCHAR(100) DEFAULT NULL,
    download_limit  INT DEFAULT -1,
    download_count  INT DEFAULT 0,
    expire_time     TIMESTAMP DEFAULT NULL,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- sys_user table (minimal for foreign key references)
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50) NOT NULL,
    password        VARCHAR(100) NOT NULL,
    email           VARCHAR(100) DEFAULT NULL,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert a test user
INSERT INTO sys_user (id, username, password, email, status) VALUES (999, 'testuser', 'testpass', 'test@test.com', 'ACTIVE');