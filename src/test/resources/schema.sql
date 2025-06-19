-- H2-compatible schema for tests
CREATE TABLE IF NOT EXISTS users (
    id BIGINT IDENTITY PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    age INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

-- Create unique constraint on email
ALTER TABLE users ADD CONSTRAINT IF NOT EXISTS uk_email UNIQUE (email);

-- Create indexes for H2
CREATE INDEX IF NOT EXISTS idx_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_name ON users(name);
CREATE INDEX IF NOT EXISTS idx_created_at ON users(created_at);

-- Insert sample data using H2 syntax
MERGE INTO users (email, name, age, created_at) VALUES
('john.doe@example.com', 'John Doe', 30, CURRENT_TIMESTAMP),
('jane.smith@example.com', 'Jane Smith', 25, CURRENT_TIMESTAMP),
('bob.johnson@example.com', 'Bob Johnson', 35, CURRENT_TIMESTAMP); 