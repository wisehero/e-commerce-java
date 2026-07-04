CREATE DATABASE IF NOT EXISTS commerce_pg
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

GRANT ALL PRIVILEGES ON commerce_pg.* TO 'application'@'%';
