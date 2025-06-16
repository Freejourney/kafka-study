-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    age INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    INDEX idx_email (email),
    INDEX idx_name (name),
    INDEX idx_created_at (created_at)
);

-- Insert some sample data
INSERT IGNORE INTO users (email, name, age, created_at) VALUES
('john.doe@example.com', 'John Doe', 30, NOW()),
('jane.smith@example.com', 'Jane Smith', 25, NOW()),
('bob.johnson@example.com', 'Bob Johnson', 35, NOW()); 