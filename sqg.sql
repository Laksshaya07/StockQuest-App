CREATE DATABASE Sqg;
USE sqg;
CREATE TABLE Users (
    userId INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    password VARCHAR(30) NOT NULL,
    cash DOUBLE DEFAULT 10000
);
CREATE TABLE Stocks (
    stockId INT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(50) NOT NULL,
    ticker VARCHAR(10),
    price DOUBLE NOT NULL,
    risk_level VARCHAR(10)
);

INSERT INTO Stocks (company_name, ticker, price, risk_level) VALUES
('TechTrend Innovations', 'TTI', 50, 'High'),
('StableFoods Co.', 'SFC', 100, 'Low'),
('GreenEnergy Solutions', 'GES', 25, 'High');
CREATE TABLE Portfolio (
    portfolioId INT AUTO_INCREMENT PRIMARY KEY,
    userId INT,
    stockId INT,
    shares INT,
    FOREIGN KEY (userId) REFERENCES Users(userId),
    FOREIGN KEY (stockId) REFERENCES Stocks(stockId),
    UNIQUE(userId, stockId)
);
CREATE TABLE Transactions (
    transactionId INT AUTO_INCREMENT PRIMARY KEY,
    userId INT,
    stockId INT,
    type VARCHAR(10),        -- BUY or SELL
    shares INT,
    price DOUBLE,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userId) REFERENCES Users(userId),
    FOREIGN KEY (stockId) REFERENCES Stocks(stockId)
);

CREATE TABLE Simulations (
    simulationId INT AUTO_INCREMENT PRIMARY KEY,
    level INT,
    stockId INT,
    scenario VARCHAR(255),
    volatility VARCHAR(10),  -- High or Low
    FOREIGN KEY (stockId) REFERENCES Stocks(stockId),
    UNIQUE(level, stockId)
);
CREATE TABLE LevelPerformance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    userId INT,
    level INT,
    profitPercent DOUBLE,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMPusers
);


-- Example entries
INSERT INTO Simulations (level, stockId, scenario, volatility) VALUES
(1, 1, 'Tech gadgets are trending this month.', 'High'),
(1, 2, 'Food sales are steady.', 'Low'),
(1, 3, 'Renewable energy incentives increase.', 'High'),
(2, 1, 'Competition increases, tech stock dips.', 'High'),
(2, 2, 'Food prices slightly rise.', 'Low'),
(2, 3, 'Energy startup gets new investors.', 'High'),
(3, 1, 'Tech breakthrough boosts stock.', 'High'),
(3, 2, 'Food company faces supply issues.', 'Low'),
(3, 3, 'Energy regulations cause volatility.', 'High');

