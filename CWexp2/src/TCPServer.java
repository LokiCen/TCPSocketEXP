import java.io.*;
import java.net.*;
import java.sql.*;

class TCPServer {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/atmserver?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "111111xiaomc";

    static {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("Error loading JDBC Driver:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String argv[]) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(2525);

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());


            String clientSentence = inFromClient.readLine();
            String[] userInfo = clientSentence.split(" ");
            if (isValidUserID(clientSentence)) {

                String response = "500 AUTH REQUIRED!";
                outToClient.writeBytes(response + "\n");
                System.out.println("User ID CHECK");
                clientSentence = inFromClient.readLine();
                if (isValidPassword(clientSentence,userInfo[1])) {
                    response = "525 OK!";
                    outToClient.writeBytes(response + "\n");
                    System.out.println("User PASS CHECK");
                    boolean done = false;
                    while (!done) {
                        clientSentence = inFromClient.readLine();
                        if (clientSentence != null) {
                            String[] parts = clientSentence.split(" ");
                            switch (parts[0].toUpperCase()) {
                                case "BALA":
                                    double balance = getBalance(userInfo[1]);
                                    response = "ATM: " + balance;
                                    System.out.println("User BALA");
                                    break;
                                case "WDRA":
                                    if (parts.length == 2) {
                                        double amount = Double.parseDouble(parts[1]);
                                        if (withdrawAmount(amount,userInfo[1])) {
                                            response = "525 OK!";
                                            System.out.println("User WDRA OK");
                                        } else {
                                            response = "401 ERROR!";
                                            System.out.println("User WDRA ERROR");
                                        }
                                    } else {
                                        response = "Invalid WDRA command format.";
                                    }
                                    break;
                                case "BYE":
                                    response = "BYE";
                                    done = true;
                                    break;
                                default:
                                    response = "Unknown Command";
                                    break;
                            }
                            outToClient.writeBytes(response + "\n");
                        }
                    }
                } else {
                    response = "401 ERROR!";
                    outToClient.writeBytes(response + "\n");
                }
            } else {
                String response = "Invalid user ID";
                outToClient.writeBytes(response + "\n");
            }
        }
    }

    private static Connection getConnection() throws SQLException {
        System.out.println("Attempting to connect to the database...");
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        System.out.println("Connected to the database successfully.");
        return conn;
    }

    private static boolean isValidUserID(String userId) {
        String[] parts = userId.split(" ");
        if (parts.length == 2 && parts[0].equals("HELO")) {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM customers1 WHERE UserId = ?")) {
                stmt.setString(1, parts[1]);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private static boolean isValidPassword(String password, String userInfo1) {
        String[] parts = password.split(" ");
        if (parts.length == 2 && parts[0].equals("PASS")) {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM customers1 WHERE UserId = ? AND Password = ?")) {
                stmt.setString(1, userInfo1);
                stmt.setString(2, parts[1]);
                ResultSet rs = stmt.executeQuery();
                return rs.next(); 
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }


    private static double getBalance(String userInfo2) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT Balance FROM customers1 WHERE UserId = ?")) {
            stmt.setString(1, userInfo2); // 设置用户ID作为参数
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("Balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0; // 如果检索失败，则返回默认余额
    }

    private static boolean withdrawAmount(double amount, String userInfo3) {
        try (Connection conn = getConnection();
             PreparedStatement updateStmt = conn.prepareStatement("UPDATE customers1 SET Balance = ? WHERE UserId = ?");
             PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO log (UserId, WithdrawMoney) VALUES (?, ?)")) {
            double currentBalance = getBalance(userInfo3);
            if (currentBalance >= amount) {
                double newBalance = currentBalance - amount;
                updateStmt.setDouble(1, newBalance);
                updateStmt.setString(2, userInfo3);
                int updated = updateStmt.executeUpdate();
                if (updated > 0) {
                    // 插入记录到 Log 表
                    insertStmt.setString(1, userInfo3);
                    insertStmt.setDouble(2, amount);
                    int inserted = insertStmt.executeUpdate();
                    if (inserted > 0) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
