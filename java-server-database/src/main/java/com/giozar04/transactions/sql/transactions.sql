-- Tabla principal de transacciones (versión compatible con MySQL)
CREATE TABLE transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    payment_method VARCHAR(20) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    title VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    comments TEXT,
    date DATETIME NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    tags TEXT -- Almacenará las etiquetas como texto separado por comas
);

-- Índices para mejorar el rendimiento
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_date ON transactions(date);
CREATE INDEX idx_transactions_category ON transactions(category);