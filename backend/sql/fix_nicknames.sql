SET NAMES utf8mb4;
UPDATE sys_user SET nickname='系统管理员' WHERE username='admin';
INSERT IGNORE INTO sys_user (username, password, nickname, role) VALUES
('docadmin', '$2a$10$hfNeTGBkfEINldI.Q5ydY.TNM/4zcsNjowivh6HQCyOVk2T/diE/O', '文档管理员', 'DOC_ADMIN');
UPDATE sys_user SET nickname='文档管理员' WHERE username='docadmin';
