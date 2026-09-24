-- 匿名注册只生成申请；既有绑定无法判断来源，保留并由农场主线下复核。
CREATE TABLE farm_join_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    requested_role VARCHAR(32) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1已批准 2已拒绝 3已撤销',
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    review_reason VARCHAR(500) NULL,
    revoked_by BIGINT NULL,
    revoked_at DATETIME NULL,
    revoke_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_farm_join_user (user_id),
    KEY idx_farm_join_owner_status (owner_user_id, status, id),
    CONSTRAINT fk_farm_join_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT chk_farm_join_role CHECK (requested_role IN ('user', 'technician')),
    CONSTRAINT chk_farm_join_status CHECK (status IN (0, 1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
