-- Create users table
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create email_verification_tokens table
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Create index for better performance
CREATE INDEX idx_email_verification_token ON email_verification_tokens(token);
CREATE INDEX idx_user_email ON users(email);
-- Modify users table
ALTER TABLE users
    MODIFY email VARCHAR(255) NOT NULL,
    MODIFY role VARCHAR(20) NOT NULL
    CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MANAGER'));

-- Modify email_verification_tokens table
ALTER TABLE email_verification_tokens
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN is_used BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for better performance
CREATE INDEX idx_verification_token_user ON email_verification_tokens(user_id);