CREATE TABLE IF NOT EXISTS commerce_pg.payments (
  transaction_key VARCHAR(255) NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  order_id VARCHAR(255) NOT NULL,
  card_type VARCHAR(32) NOT NULL,
  card_no VARCHAR(32) NOT NULL,
  amount BIGINT NOT NULL,
  refunded_amount BIGINT NOT NULL DEFAULT 0,
  callback_url VARCHAR(2048) NOT NULL,
  status VARCHAR(32) NOT NULL,
  reason VARCHAR(255) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (transaction_key),
  KEY idx_user_transaction (user_id, transaction_key),
  KEY idx_user_order (user_id, order_id),
  UNIQUE KEY idx_unique_user_order_transaction (user_id, order_id, transaction_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
