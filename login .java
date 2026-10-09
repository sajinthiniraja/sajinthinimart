import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginPage {

    static final String URL =
            "jdbc:mysql://localhost:3306/sajinthini_mart";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "your_mysql_password";

    public static void main(String[] args) {

        JFrame frame = new JFrame("Sajinthini Mart - Login");

        JLabel title = new JLabel("SAJINTHINI MART");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setBounds(100, 30, 250, 40);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(50, 100, 100, 30);

        JTextField userField = new JTextField();
        userField.setBounds(150, 100, 200, 30);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(50, 150, 100, 30);

        JPasswordField passField = new JPasswordField();
        passField.setBounds(150, 150, 200, 30);

        JButton loginButton = new JButton("LOGIN");
        loginButton.setBounds(150, 210, 120, 35);

        frame.add(title);
        frame.add(userLabel);
        frame.add(userField);
        frame.add(passLabel);
        frame.add(passField);
        frame.add(loginButton);

        loginButton.addActionListener(e -> {

            String username = userField.getText();
            String password = new String(passField.getPassword());

            if (checkLogin(username, password)) {

                JOptionPane.showMessageDialog(
                        frame,
                        "Login Successful!"
                );

                frame.dispose();

                // Open Sajinthini Mart
                showMartPage();

            } else {

                JOptionPane.showMessageDialog(
                        frame,
                        "Invalid Username or Password"
                );
            }
        });

        frame.setSize(420, 330);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Check login from MySQL
    static boolean checkLogin(String username, String password) {

        String sql =
                "SELECT * FROM users WHERE username=? AND password=?";

        try {
            Connection con = DriverManager.getConnection(
                    URL,
                    DB_USER,
                    DB_PASSWORD
            );

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            boolean result = rs.next();

            rs.close();
            ps.close();
            con.close();

            return result;

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    null,
                    "Database Error: " + e.getMessage()
            );

            return false;
        }
    }

  
    static void showMartPage() {

        JFrame frame = new JFrame("Sajinthini Mart");

        JLabel title = new JLabel("WELCOME TO SAJINTHINI MART");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setBounds(100, 30, 400, 40);

        JLabel products = new JLabel(
                "Products will appear here..."
        );
        products.setBounds(150, 100, 300, 30);

        frame.add(title);
        frame.add(products);

        frame.setSize(600, 400);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
