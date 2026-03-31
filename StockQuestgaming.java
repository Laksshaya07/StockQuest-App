package stockquest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class StockQuestgaming extends JFrame {
    private Connection conn;//database
    private int userId;
    private int currentLevel = 1;
    private JLabel cashLabel, portfolioLabel, scenarioLabel, levelLabel;
    private JComboBox<String> stockCombo;
    private JTextField sharesField;
    private JTextArea historyArea;
    private JButton nextLevelBtn;
    private boolean levelCompleted = false;

    private Map<Integer, String> companyActions = new HashMap<>();
    private double initialCash = 10000;

    public StockQuestgaming() {
        connectDB();
        showLoginScreen();
    }

    private void connectDB() {
        try {
            String url = "jdbc:mysql://localhost:3306/Sqg";
            String user = "root";
            String password = "password";
            conn = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB Connection failed!");
            System.exit(1);
        }
    }

    private void showLoginScreen() {
        JFrame loginFrame = new JFrame("Stock Quest - Login");
        loginFrame.setSize(300, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new GridLayout(4, 2, 10, 10));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");

        loginFrame.add(new JLabel("Username:"));
        loginFrame.add(usernameField);
        loginFrame.add(new JLabel("Password:"));
        loginFrame.add(passwordField);
        loginFrame.add(loginBtn);
        loginFrame.add(registerBtn);

        loginBtn.addActionListener(e -> {
            if (login(usernameField.getText(), new String(passwordField.getPassword()))) {
                loginFrame.dispose();
                showInstructions();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Invalid credentials!");
            }
        });

        registerBtn.addActionListener(e -> {
            if (register(usernameField.getText(), new String(passwordField.getPassword()))) {
                JOptionPane.showMessageDialog(loginFrame, "Registered! Please login.");
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Username exists!");
            }
        });

        loginFrame.setVisible(true);
    }

    private boolean login(String username, String password) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT userId FROM Users WHERE username=? AND password=?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                userId = rs.getInt(1);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean register(String username, String password) {
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO Users(username,password) VALUES(?,?)");
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void showInstructions() {
        JFrame instrFrame = new JFrame("Stock Quest Instructions");
        instrFrame.setSize(500, 400);
        JTextArea instrArea = new JTextArea(
                "Level-based trading game.\n" +
                        "Objective: Grow your wealth by making wise choices.\n" +
                        "You have $10,000 to start.\n" +
                        "Choose a level and trade accordingly.\n" +
                        "Each stock has a unique scenario & volatility per level."
        );
        instrArea.setWrapStyleWord(true);
        instrArea.setLineWrap(true);
        instrArea.setEditable(false);

        JButton startBtn = new JButton("Start Game");
        startBtn.addActionListener(e -> {
            instrFrame.dispose();
            showMainScreen();
        });

        instrFrame.add(instrArea, BorderLayout.CENTER);
        instrFrame.add(startBtn, BorderLayout.SOUTH);
        instrFrame.setVisible(true);
    }

    private void showMainScreen() {
        setTitle("Stock Quest - Level " + currentLevel);
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        levelLabel = new JLabel("Level: " + currentLevel);
        cashLabel = new JLabel();
        portfolioLabel = new JLabel();
        scenarioLabel = new JLabel("Scenario will appear here");

        topPanel.add(levelLabel);
        topPanel.add(cashLabel);
        topPanel.add(portfolioLabel);
        topPanel.add(scenarioLabel);

        add(topPanel, BorderLayout.NORTH);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        stockCombo = new JComboBox<>();
        loadStocks();
        sharesField = new JTextField(5);
        JButton buyBtn = new JButton("Buy");
        JButton sellBtn = new JButton("Sell");
        nextLevelBtn = new JButton("Next Level");
        nextLevelBtn.setVisible(false);

        actionPanel.add(new JLabel("Stock:"));
        actionPanel.add(stockCombo);
        actionPanel.add(new JLabel("Shares:"));
        actionPanel.add(sharesField);
        actionPanel.add(buyBtn);
        actionPanel.add(sellBtn);
        actionPanel.add(nextLevelBtn);

        add(actionPanel, BorderLayout.CENTER);

        historyArea = new JTextArea(10, 50);
        add(new JScrollPane(historyArea), BorderLayout.SOUTH);

        buyBtn.addActionListener(e -> buyStock());
        sellBtn.addActionListener(e -> sellStock());

        nextLevelBtn.addActionListener(e -> {
            double profitPercent = calculateProfitPercent();
            storeProfitPercent(profitPercent);

            JOptionPane.showMessageDialog(this, String.format(
                    "Level %d Completed!\nProfit Percentage: %.2f%%", currentLevel, profitPercent
            ));

            if (currentLevel < 3) {
                currentLevel++;
                levelCompleted = false;
                companyActions.clear();
                nextLevelBtn.setVisible(false);
                levelLabel.setText("Level: " + currentLevel);

                try {
                    PreparedStatement ps = conn.prepareStatement("SELECT cash FROM Users WHERE userId=?");
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        initialCash = rs.getDouble(1);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                updateUI();
            } else {
                JOptionPane.showMessageDialog(this,
                        String.format("🎉 Congratulations! You completed all 3 levels.\nFinal Profit Percentage: %.2f%%", profitPercent));
                resetGame();
            }
        });

        stockCombo.addActionListener(e -> loadScenario());

        try {
            PreparedStatement ps = conn.prepareStatement("SELECT cash FROM Users WHERE userId=?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                initialCash = rs.getDouble(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateUI();
        setVisible(true);
    }

    private void resetGame() {
        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE Users SET cash=10000 WHERE userId=?");
            ps.setInt(1, userId);
            ps.executeUpdate();

            ps = conn.prepareStatement("DELETE FROM Portfolio WHERE userId=?");
            ps.setInt(1, userId);
            ps.executeUpdate();

            ps = conn.prepareStatement("DELETE FROM Transactions WHERE userId=?");
            ps.setInt(1, userId);
            ps.executeUpdate();

            currentLevel = 1;
            levelCompleted = false;
            companyActions.clear();
            nextLevelBtn.setVisible(false);
            levelLabel.setText("Level: " + currentLevel);
            initialCash = 10000;
            updateUI();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadStocks() {
        try {
            stockCombo.removeAllItems();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT stockId, company_name, price FROM Stocks");
            while (rs.next()) {
                stockCombo.addItem(rs.getInt(1) + " - " + rs.getString(2) + " ($" + rs.getDouble(3) + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadScenario() {
        try {
            if (stockCombo.getSelectedItem() == null) return;
            int stockId = Integer.parseInt(stockCombo.getSelectedItem().toString().split(" - ")[0]);
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT scenario, volatility FROM Simulations WHERE level=? AND stockId=?"
            );
            ps.setInt(1, currentLevel);
            ps.setInt(2, stockId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                scenarioLabel.setText("Scenario: " + rs.getString("scenario") + " | Volatility: " + rs.getString("volatility"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buyStock() {
        try {
            int stockId = Integer.parseInt(stockCombo.getSelectedItem().toString().split(" - ")[0]);

            if (companyActions.containsKey(stockId) && companyActions.get(stockId).equals("SELL")) {
                JOptionPane.showMessageDialog(this, "You cannot buy in this company after selling in the same level!");
                return;
            }

            companyActions.put(stockId, "BUY");

            int shares = Integer.parseInt(sharesField.getText());
            PreparedStatement ps = conn.prepareStatement("SELECT price FROM Stocks WHERE stockId=?");
            ps.setInt(1, stockId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            double price = rs.getDouble(1);
            double cost = price * shares;

            ps = conn.prepareStatement("SELECT cash FROM Users WHERE userId=?");
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            rs.next();
            double cash = rs.getDouble(1);

            if (cash >= cost) {
                conn.setAutoCommit(false);
                ps = conn.prepareStatement("UPDATE Users SET cash=cash-? WHERE userId=?");
                ps.setDouble(1, cost);
                ps.setInt(2, userId);
                ps.executeUpdate();

                ps = conn.prepareStatement(
                        "INSERT INTO Portfolio(userId,stockId,shares) VALUES(?,?,?) " +
                                "ON DUPLICATE KEY UPDATE shares=shares+?"
                );
                ps.setInt(1, userId);
                ps.setInt(2, stockId);
                ps.setInt(3, shares);
                ps.setInt(4, shares);
                ps.executeUpdate();

                ps = conn.prepareStatement(
                        "INSERT INTO Transactions(userId,stockId,type,shares,price) VALUES(?,?,?,?,?)"
                );
                ps.setInt(1, userId);
                ps.setInt(2, stockId);
                ps.setString(3, "BUY");
                ps.setInt(4, shares);
                ps.setDouble(5, price);
                ps.executeUpdate();

                conn.commit();

                levelCompleted = true;
                nextLevelBtn.setVisible(true);
                updateUI();
            } else {
                JOptionPane.showMessageDialog(this, "Not enough cash!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try { conn.rollback(); } catch (Exception ex) {}
        }
    }

    private void sellStock() {
        try {
            int stockId = Integer.parseInt(stockCombo.getSelectedItem().toString().split(" - ")[0]);

            if (companyActions.containsKey(stockId) && companyActions.get(stockId).equals("BUY")) {
                JOptionPane.showMessageDialog(this, "You cannot sell in this company after buying in the same level!");
                return;
            }

            companyActions.put(stockId, "SELL");

            int shares = Integer.parseInt(sharesField.getText());
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT shares FROM Portfolio WHERE userId=? AND stockId=?"
            );
            ps.setInt(1, userId);
            ps.setInt(2, stockId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next() || rs.getInt(1) < shares) {
                JOptionPane.showMessageDialog(this, "Not enough shares!");
                return;
            }

            ps = conn.prepareStatement("SELECT price FROM Stocks WHERE stockId=?");
            ps.setInt(1, stockId);
            rs = ps.executeQuery();
            rs.next();
            double price = rs.getDouble(1);
            double gain = price * shares;

            conn.setAutoCommit(false);
            ps = conn.prepareStatement("UPDATE Users SET cash=cash+? WHERE userId=?");
            ps.setDouble(1, gain);
            ps.setInt(2, userId);
            ps.executeUpdate();

            ps = conn.prepareStatement(
                    "UPDATE Portfolio SET shares=shares-? WHERE userId=? AND stockId=?"
            );
            ps.setInt(1, shares);
            ps.setInt(2, userId);
            ps.setInt(3, stockId);
            ps.executeUpdate();

            ps = conn.prepareStatement("DELETE FROM Portfolio WHERE shares=0");
            ps.executeUpdate();

            ps = conn.prepareStatement(
                    "INSERT INTO Transactions(userId,stockId,type,shares,price) VALUES(?,?,?,?,?)"
            );
            ps.setInt(1, userId);
            ps.setInt(2, stockId);
            ps.setString(3, "SELL");
            ps.setInt(4, shares);
            ps.setDouble(5, price);
            ps.executeUpdate();

            conn.commit();
            System.out.println("SELL successful: " + shares + " shares of stock ID " + stockId + " at $" + price);
            levelCompleted = true;
            nextLevelBtn.setVisible(true);

            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
            try { conn.rollback(); } catch (Exception ex) {}
        }
    }

    private void updateUI() {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT cash FROM Users WHERE userId=?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                cashLabel.setText("Cash: $" + rs.getDouble(1));
            }

            levelLabel.setText("Level: " + currentLevel);

            StringBuilder sb = new StringBuilder("<html>Portfolio: ");
            ps = conn.prepareStatement(
                    "SELECT s.company_name, p.shares FROM Portfolio p " +
                            "JOIN Stocks s ON p.stockId=s.stockId WHERE userId=?"
            );
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString(1)).append(" x ").append(rs.getInt(2)).append(" | ");
            }
            portfolioLabel.setText(sb.toString() + "</html>");

            historyArea.setText("");
            Statement st = conn.createStatement();
            rs = st.executeQuery("SELECT * FROM Transactions WHERE userId=" + userId + " ORDER BY timestamp DESC");
            while (rs.next()) {
                historyArea.append(
                        rs.getString("type") + " " + rs.getInt("shares") +
                                " of " + rs.getInt("stockId") +
                                " at $" + rs.getDouble("price") + " on " + rs.getString("timestamp") + "\n"
                );
            }

            loadScenario();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double calculateProfitPercent() {
        double cash = 0, portfolioValue = 0;
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT cash FROM Users WHERE userId=?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                cash = rs.getDouble(1);
            }

            ps = conn.prepareStatement(
                    "SELECT p.shares, s.price FROM Portfolio p JOIN Stocks s ON p.stockId=s.stockId WHERE p.userId=?"
            );
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                portfolioValue += rs.getInt(1) * rs.getDouble(2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((cash + portfolioValue - initialCash) / initialCash) * 100;
    }

    private void storeProfitPercent(double profitPercent) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO LevelPerformance(userId, level, profitPercent) VALUES(?,?,?)"
            );
            ps.setInt(1, userId);
            ps.setInt(2, currentLevel);
            ps.setDouble(3, profitPercent);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StockQuestgaming::new);
    }
}