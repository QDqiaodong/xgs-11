SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS todo_app DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE todo_app;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 任务表： tasks
CREATE TABLE IF NOT EXISTS tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    assignee_id INT DEFAULT NULL,
    text VARCHAR(255) NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    priority ENUM('high', 'medium', 'low') DEFAULT 'medium',
    due_date DATETIME DEFAULT NULL,
    overdue_reason TEXT DEFAULT NULL,
    completed_at DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_missing('users', 'created_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP');
CALL add_column_if_missing('tasks', 'user_id', 'INT NULL AFTER `id`');
CALL add_column_if_missing('tasks', 'assignee_id', 'INT DEFAULT NULL AFTER `user_id`');
CALL add_column_if_missing('tasks', 'priority', 'ENUM(''high'', ''medium'', ''low'') DEFAULT ''medium'' AFTER `completed`');
CALL add_column_if_missing('tasks', 'due_date', 'DATETIME DEFAULT NULL AFTER `priority`');
CALL add_column_if_missing('tasks', 'overdue_reason', 'TEXT DEFAULT NULL AFTER `due_date`');
CALL add_column_if_missing('tasks', 'completed_at', 'DATETIME DEFAULT NULL AFTER `overdue_reason`');
CALL add_column_if_missing('tasks', 'created_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP AFTER `completed_at`');
CALL add_column_if_missing('tasks', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`');

DROP PROCEDURE IF EXISTS add_column_if_missing;

INSERT INTO users (username, password) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e'),
('user1', 'e10adc3949ba59abbe56e057f20f883e')
ON DUPLICATE KEY UPDATE username = VALUES(username);

SET @admin_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1);
SET @user1_id = (SELECT id FROM users WHERE username = 'user1' LIMIT 1);

UPDATE tasks
SET user_id = @admin_id
WHERE user_id IS NULL;

ALTER TABLE tasks MODIFY COLUMN user_id INT NOT NULL;

INSERT INTO tasks (user_id, assignee_id, text, completed, priority, due_date)
SELECT @admin_id, @admin_id, '完成 Web 开发期末作业', false, 'high', DATE_ADD(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM tasks WHERE user_id = @admin_id AND text = '完成 Web 开发期末作业'
);

INSERT INTO tasks (user_id, assignee_id, text, completed, priority, due_date)
SELECT @admin_id, @user1_id, '设计数据库架构', true, 'medium', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tasks WHERE user_id = @admin_id AND text = '设计数据库架构'
);

INSERT INTO tasks (user_id, assignee_id, text, completed, priority, due_date)
SELECT @user1_id, @user1_id, '编写前端组件', false, 'medium', DATE_ADD(NOW(), INTERVAL 2 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM tasks WHERE user_id = @user1_id AND text = '编写前端组件'
);

INSERT INTO tasks (user_id, assignee_id, text, completed, priority, due_date, overdue_reason, completed_at)
SELECT @admin_id, @admin_id, '修复登录页样式错位问题', true, 'high', DATE_SUB(NOW(), INTERVAL 2 DAY), '需求评审延后启动，开发排期被迫后移，导致跨过原定截止日期才完成', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tasks WHERE user_id = @admin_id AND text = '修复登录页样式错位问题'
);
