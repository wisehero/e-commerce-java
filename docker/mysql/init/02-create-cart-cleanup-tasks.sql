USE commerce;

CREATE TABLE IF NOT EXISTS cart_cleanup_tasks (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  cart_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  attempt_count INT NOT NULL,
  next_attempt_at DATETIME(6) NOT NULL,
  last_error VARCHAR(1000) NULL,
  completed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_cart_cleanup_tasks_order_id UNIQUE (order_id),
  INDEX idx_cart_cleanup_tasks_ready (status, next_attempt_at),
  INDEX idx_cart_cleanup_tasks_member_status (member_id, status)
);
