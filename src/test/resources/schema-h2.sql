-- Java 迁移初版；MySQL 8.0.16+（推荐 8.4 LTS）。在已选定的空数据库中执行。
-- 不创建/删除数据库；连接时区统一 UTC；业务展示使用 Asia/Shanghai。


CREATE TABLE app_user (
 id BIGINT NOT NULL AUTO_INCREMENT,
 public_id VARCHAR(64) NOT NULL,
 display_name VARCHAR(100) NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY (id), UNIQUE KEY uk_user_public (public_id)
);

CREATE TABLE trip_plan (
 id BIGINT NOT NULL AUTO_INCREMENT,
 public_id VARCHAR(64) NOT NULL,
 user_id BIGINT NOT NULL,
 title VARCHAR(255) NOT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY (id), UNIQUE KEY uk_plan_public (public_id),
 KEY idx_plan_user (user_id, created_at, id),
 CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE trip_task (
 id BIGINT NOT NULL AUTO_INCREMENT,
 public_id VARCHAR(64) NOT NULL,
 plan_id BIGINT NOT NULL,
 idempotency_key VARCHAR(64) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'processing',
 stage VARCHAR(40) NOT NULL DEFAULT 'submitted',
 progress TINYINT NOT NULL DEFAULT 0,
 request_json MEDIUMTEXT NOT NULL,
 message VARCHAR(1000) NULL,
 error_code VARCHAR(64) NULL,
 error_message TEXT NULL,
 lock_version BIGINT NOT NULL DEFAULT 0,
 started_at DATETIME(3) NULL,
 finished_at DATETIME(3) NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY (id), UNIQUE KEY uk_task_public (public_id),
 UNIQUE KEY uk_task_idempotency (plan_id, idempotency_key),
 KEY idx_task_plan (plan_id, created_at, id),
 KEY idx_task_recovery (status, updated_at, id),
 CONSTRAINT fk_task_plan FOREIGN KEY (plan_id) REFERENCES trip_plan(id),
 CONSTRAINT ck_task_status CHECK (status IN ('processing','completed','failed')),
 CONSTRAINT ck_task_progress CHECK (progress <= 100)
);

CREATE TABLE trip_plan_version (
 id BIGINT NOT NULL AUTO_INCREMENT,
 plan_id BIGINT NOT NULL,
 task_id BIGINT NOT NULL,
 version_no INT NOT NULL,
 main_city VARCHAR(128) NOT NULL,
 start_date DATE NOT NULL,
 end_date DATE NOT NULL,
 travel_days SMALLINT NOT NULL,
 language VARCHAR(16) NOT NULL DEFAULT 'zh',
 total_budget DECIMAL(12,2) NULL,
 currency CHAR(3) NOT NULL DEFAULT 'CNY',
 plan_json MEDIUMTEXT NOT NULL,
 schema_version INT NOT NULL DEFAULT 1,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY (id), UNIQUE KEY uk_plan_version (plan_id, version_no),
 UNIQUE KEY uk_version_task (task_id),
 CONSTRAINT fk_version_plan FOREIGN KEY (plan_id) REFERENCES trip_plan(id),
 CONSTRAINT fk_version_task FOREIGN KEY (task_id) REFERENCES trip_task(id),
 CONSTRAINT ck_version_number CHECK (version_no >= 1),
 CONSTRAINT ck_version_days CHECK (travel_days BETWEEN 1 AND 30),
 CONSTRAINT ck_version_dates CHECK (end_date >= start_date),
 CONSTRAINT ck_version_budget CHECK (total_budget IS NULL OR total_budget >= 0)
);

CREATE TABLE user_memory (
 id BIGINT NOT NULL AUTO_INCREMENT,
 public_id VARCHAR(64) NOT NULL,
 user_id BIGINT NOT NULL,
 content TEXT NOT NULL,
 content_hash BINARY(32) NOT NULL COMMENT '应用对 trim 后 UTF-8 内容计算 SHA-256',
 source VARCHAR(16) NOT NULL,
 weight DECIMAL(10,6) NOT NULL,
 last_decay_at DATETIME(3) NOT NULL,
 last_access_at DATETIME(3) NOT NULL,
 lock_version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY (id), UNIQUE KEY uk_memory_public (public_id),
 UNIQUE KEY uk_memory_content (user_id, content_hash),
 CONSTRAINT fk_memory_user FOREIGN KEY (user_id) REFERENCES app_user(id),
 CONSTRAINT ck_memory_source CHECK (source IN ('explicit','implicit')),
 CONSTRAINT ck_memory_weight CHECK (weight BETWEEN 0 AND 10)
);

-- 以下两张表：需要后端保存对话时启用；当前 Python 版由请求传入历史。
CREATE TABLE chat_session (
 id BIGINT NOT NULL AUTO_INCREMENT,
 public_id VARCHAR(64) NOT NULL,
 plan_version_id BIGINT NOT NULL,
 next_sequence BIGINT NOT NULL DEFAULT 1,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY (id), UNIQUE KEY uk_chat_public (public_id),
 KEY idx_chat_version (plan_version_id, id),
 CONSTRAINT fk_chat_version FOREIGN KEY (plan_version_id) REFERENCES trip_plan_version(id)
);

CREATE TABLE chat_message (
 id BIGINT NOT NULL AUTO_INCREMENT,
 session_id BIGINT NOT NULL,
 sequence_no BIGINT NOT NULL,
 role VARCHAR(16) NOT NULL,
 content MEDIUMTEXT NOT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY (id), UNIQUE KEY uk_message_sequence (session_id, sequence_no),
 CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES chat_session(id),
 CONSTRAINT ck_message_role CHECK (role IN ('user','assistant')),
 CONSTRAINT ck_message_sequence CHECK (sequence_no >= 1)
);
