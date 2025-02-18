package shop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/furniture_shop_db?zeroDateTimeBehavior=CONVERT_TO_NULL&useSSL=false&serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

public static Connection getConnection() {
    Connection conn = null;
    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
        conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        System.out.println("Connected to Database!");
    } catch (ClassNotFoundException e) {
        JOptionPane.showMessageDialog(null, "JDBC Driver is incorrect!", "Database Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Failed to connect to the database!\nPlease check MySQL Server, Database, Username, and Password.", "Database Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
    return conn;
}

}
