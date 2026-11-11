package library;
import java.sql.Connection;

public class TestConnection {
    public static void main(String[] args) {
        Connection con = DBConnections.getConnection();
        if (con != null) {
            System.out.println("JDBC connection successful");
        }
    }
}