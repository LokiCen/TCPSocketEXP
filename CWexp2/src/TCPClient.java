import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class TCPClient extends JFrame {
    private JTextField accountField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JTextArea displayArea;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;

    public TCPClient() {
        super("ATM Client");

        //创建登录界面的UI组件
        accountField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        displayArea = new JTextArea(10, 30);
        displayArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel loginPanel = new JPanel(new GridLayout(3, 2));
        loginPanel.add(new JLabel("Account:"));
        loginPanel.add(accountField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);
        loginPanel.add(new JLabel());
        loginPanel.add(loginButton);

        add(loginPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        //连接登录按钮的监听器
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    login();
                } catch (IOException ex) {
                    displayMessage("Error: " + ex.getMessage());
                }
            }
        });

        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void login() throws IOException {
        String account = accountField.getText();
        String password = new String(passwordField.getPassword());

        Socket clientSocket = new Socket("localhost", 2525);
        outToServer = new DataOutputStream(clientSocket.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        //发送账户
        outToServer.writeBytes("HELO " + account + "\n");
        String response = inFromServer.readLine();
        //displayMessage("FROM SERVER: " + response);

        //检测账户是否存在
        if ("500 AUTH REQUIRED!".equals(response.trim())) {
            // 发送密码
            outToServer.writeBytes("PASS " + password + "\n");


            response = inFromServer.readLine();
            displayMessage("FROM SERVER: " + response);

            // 如果密码正确，进入功能界面
            if ("525 OK!".equals(response.trim())) {
                //displayMessage("Login successfully.");
                showOperations();
            }
            else
            {
                displayMessage("Login failed. Please check your password.");
            }
        }
        else
        {
            displayMessage("Login failed. Please check your account.");
        }
    }

    private void showOperations() {
        //移除登录页面的内容
        getContentPane().removeAll();
        revalidate();

        //添加功能按钮
        JButton balanceButton = new JButton("Check Balance");
        JButton withdrawButton = new JButton("Withdraw");
        JButton logoutButton = new JButton("Logout");

        JPanel operationPanel = new JPanel(new GridLayout(3, 1));
        operationPanel.add(balanceButton);
        operationPanel.add(withdrawButton);
        operationPanel.add(logoutButton);

        add(operationPanel, BorderLayout.NORTH);

        //在功能界面接入按钮的监听器
        balanceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    outToServer.writeBytes("BALA\n");
                    String balance = inFromServer.readLine();
                    //displayMessage("Balance: " + balance);
                    JOptionPane.showMessageDialog(null, balance, "Account Balance", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    //displayMessage("Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Account Balance", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        withdrawButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String amount = JOptionPane.showInputDialog("Enter amount to withdraw:");
                if (amount != null && !amount.isEmpty()) {
                    try {
                        outToServer.writeBytes("WDRA " + amount + "\n");
                        String response = inFromServer.readLine();
                        //displayMessage("FROM SERVER: " + response);
                        if ("525 OK!".equals(response.trim())) {
                            //displayMessage("Withdraw successfully.");
                            //showOperations();
                            JOptionPane.showMessageDialog(null, "Withdraw successfully.", "Account Balance", JOptionPane.INFORMATION_MESSAGE);
                        }
                        else{
                            //displayMessage("Withdraw failed.");
                            JOptionPane.showMessageDialog(null, "Withdraw failed.", "Account Balance", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (IOException ex) {
                        //displayMessage("Error: " + ex.getMessage());
                        JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Account Balance", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });

        logoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    outToServer.writeBytes("BYE\n");
                    String response = inFromServer.readLine();
                    displayMessage("FROM SERVER: " + response);
                    dispose();
                } catch (IOException ex) {
                    displayMessage("Error: " + ex.getMessage());
                }
            }
        });


        setSize(400, 200);
        revalidate();
    }

    private void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            displayArea.append(message + "\n");
        });
    }

    public static void main(String[] args) {
        new TCPClient();
    }
}