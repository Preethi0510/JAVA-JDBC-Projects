package leave;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnections {
    static final String url = "jdbc:mysql://localhost:3306/leave_db";
    static final String user = "root";
    static final String pass = "preethi0510";

    public static Connection getConnection() {
        Connection con = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, pass);
            System.out.println("Database connected successfully");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return con;
    }
}
