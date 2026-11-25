-- =====================================================================
-- Database Schema: Personal Finance Management System
-- Tech Stack: Core Java, Swing, JDBC, MySQL
-- =====================================================================

-- 1. Create Database if not exists
CREATE DATABASE IF NOT EXISTS personal_finance_db;
USE personal_finance_db;

-- 2. Drop existing tables if re-initializing (Child table first, then parent)
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS users;

-- 3. Create 'users' table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Stored as SHA-256 hashed string
    full_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Create 'transactions' table
CREATE TABLE transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    type ENUM('INCOME', 'EXPENSE') NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_transactions 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Create 'budgets' table for category spending limits & alerts
CREATE TABLE IF NOT EXISTS budgets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    category VARCHAR(50) NOT NULL,
    monthly_limit DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_budget_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    UNIQUE KEY uq_user_category (user_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Helpful Indexes for high query performance
CREATE INDEX idx_user_id ON transactions(user_id);
CREATE INDEX idx_transaction_date ON transactions(transaction_date);
CREATE INDEX idx_tx_type ON transactions(type);

-- 7. Sample Seed Data (Passwords are SHA-256 for 'password123')
-- SHA-256 for 'password123': ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f
INSERT INTO users (username, password, full_name) VALUES 
('john_doe', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'John Doe'),
('sarah_connor', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'Sarah Connor');

-- Sample transactions for user 1 (John Doe)
INSERT INTO transactions (user_id, amount, type, category, description, transaction_date) VALUES 
(1, 4500.00, 'INCOME', 'Salary', 'Monthly software engineering salary', '2026-11-01'),
(1, 1200.00, 'EXPENSE', 'Housing', 'Apartment rent payment', '2026-11-03'),
(1, 350.00, 'EXPENSE', 'Groceries', 'Weekly supermarket haul', '2026-11-05'),
(1, 120.00, 'EXPENSE', 'Utilities', 'Electricity and internet bill', '2026-11-08'),
(1, 600.00, 'INCOME', 'Freelance', 'UI/UX design project payout', '2026-11-12'),
(1, 85.00, 'EXPENSE', 'Entertainment', 'Movie tickets and dinner', '2026-11-14');
