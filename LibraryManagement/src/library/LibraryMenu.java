package library;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class LibraryMenu {
    static Scanner sc = new Scanner(System.in);
    static Connection con;

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DBConnections.getConnection();

            if (con == null) {
                System.out.println("Database connection failed");
                return;
            }

            int choice;
            do {
                System.out.println("\n===== LIBRARY MANAGEMENT =====");
                System.out.println("1. Add Book");
                System.out.println("2. View Books");
                System.out.println("3. Search Book");
                System.out.println("4. Update Book");
                System.out.println("5. Delete Book");
                System.out.println("6. Register Member");
                System.out.println("7. View Members");
                System.out.println("8. Issue Book");
                System.out.println("9. Return Book");
                System.out.println("10. View Issue History");
                System.out.println("11. Exit");
                choice = readInt("Enter choice: ");

                switch (choice) {
                    case 1: addBook(); break;
                    case 2: viewBooks(); break;
                    case 3: searchBook(); break;
                    case 4: updateBook(); break;
                    case 5: deleteBook(); break;
                    case 6: addMember(); break;
                    case 7: viewMembers(); break;
                    case 8: issueBook(); break;
                    case 9: returnBook(); break;
                    case 10: viewIssueHistory(); break;
                    case 11: System.out.println("Thank you for using the library system"); break;
                    default: System.out.println("Invalid choice");
                }
            } while (choice != 11);

        } catch (ClassNotFoundException e) {
            System.out.println("MySQL driver not found: " + e.getMessage());
        } 
         finally {
            try {
                if (con != null) con.close();
                sc.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    static int readInt(String message) {
        while (true) {
            System.out.print(message);
            try {
                int value = Integer.parseInt(sc.nextLine().trim());
                if (value > 0) return value;
                System.out.println("Enter a valid positive number");
            } catch (NumberFormatException e) {
                System.out.println("Please enter numbers only");
            }
        }
    }

    static String readText(String message) {
        while (true) {
            System.out.print(message);
            String value = sc.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("This field cannot be empty");
        }
    }

    static void addBook() {
        try {
            String name = readText("Enter book name: ");
            String author = readText("Enter author name: ");

            String sql = "insert into books(book_name,author,available) values(?,?,true)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, author);
                int rows = ps.executeUpdate();

                if (rows > 0) System.out.println("Book added successfully");
            }
        } catch (SQLException e) {
            System.out.println("Unable to add book: " + e.getMessage());
        }
    }

    static void viewBooks() {
        String sql = "select * from books order by book_id";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\nID\tBook Name\t\tAuthor\t\tAvailable");
            System.out.println("------------------------------------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%d\t%-20s\t%-18s\t%s%n",
                    rs.getInt("book_id"),
                    rs.getString("book_name"),
                    rs.getString("author"),
                    rs.getBoolean("available") ? "Yes" : "No");
            }

            if (!found) System.out.println("No books found");

        } catch (SQLException e) {
            System.out.println("Unable to display books: " + e.getMessage());
        }
    }

    static void searchBook() {
        String search = readText("Enter book name or author: ");
        String sql = "select * from books where book_name like ? or author like ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + search + "%");
            ps.setString(2, "%" + search + "%");

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.println("\nBook ID: " + rs.getInt("book_id"));
                    System.out.println("Book Name: " + rs.getString("book_name"));
                    System.out.println("Author: " + rs.getString("author"));
                    System.out.println("Available: " + (rs.getBoolean("available") ? "Yes" : "No"));
                }

                if (!found) System.out.println("No matching books found");
            }
        } catch (SQLException e) {
            System.out.println("Search failed: " + e.getMessage());
        }
    }

    static void updateBook() {
        int id = readInt("Enter book ID: ");
        String name = readText("Enter new book name: ");
        String author = readText("Enter new author name: ");

        String sql = "update books set book_name=?, author=? where book_id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, author);
            ps.setInt(3, id);

            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Book updated successfully" : "Book ID not found");

        } catch (SQLException e) {
            System.out.println("Unable to update book: " + e.getMessage());
        }
    }

    static void deleteBook() {
        int id = readInt("Enter book ID: ");
        String sql = "delete from books where book_id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();

            if (rows > 0) System.out.println("Book deleted successfully");
            else System.out.println("Book ID not found");

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("This book has issue history and cannot be deleted");
        } catch (SQLException e) {
            System.out.println("Unable to delete book: " + e.getMessage());
        }
    }

    static void addMember() {
        String name = readText("Enter member name: ");
        String phone;

        while (true) {
            phone = readText("Enter 10-digit phone number: ");
            if (phone.matches("[6-9][0-9]{9}")) break;
            System.out.println("Enter a valid 10-digit Indian mobile number");
        }

        String sql = "insert into members(member_name,phone) values(?,?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);

            if (ps.executeUpdate() > 0)
                System.out.println("Member registered successfully");

        } catch (SQLException e) {
            System.out.println("Unable to register member: " + e.getMessage());
        }
    }

    static void viewMembers() {
        String sql = "select * from members order by member_id";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\nID\tMember Name\t\tPhone");
            System.out.println("---------------------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%d\t%-20s\t%s%n",
                    rs.getInt("member_id"),
                    rs.getString("member_name"),
                    rs.getString("phone"));
            }

            if (!found) System.out.println("No members found");

        } catch (SQLException e) {
            System.out.println("Unable to display members: " + e.getMessage());
        }
    }

    static void issueBook() {
        int bookId = readInt("Enter book ID: ");
        int memberId = readInt("Enter member ID: ");

        try {
            con.setAutoCommit(false);

            String bookSql = "select available from books where book_id=? for update";
            boolean available = false;
            boolean bookFound = false;

            try (PreparedStatement ps = con.prepareStatement(bookSql)) {
                ps.setInt(1, bookId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        bookFound = true;
                        available = rs.getBoolean("available");
                    }
                }
            }

            if (!bookFound) {
                System.out.println("Book ID not found");
                con.rollback();
                return;
            }

            if (!available) {
                System.out.println("Book is already issued");
                con.rollback();
                return;
            }

            String memberSql = "select member_id from members where member_id=?";
            try (PreparedStatement ps = con.prepareStatement(memberSql)) {
                ps.setInt(1, memberId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Member ID not found");
                        con.rollback();
                        return;
                    }
                }
            }

            LocalDate issueDate = LocalDate.now();
            LocalDate dueDate = issueDate.plusDays(14);

            String insertSql = "insert into book_issues(book_id,member_id,issue_date,due_date) values(?,?,?,?)";

            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.setDate(3, Date.valueOf(issueDate));
                ps.setDate(4, Date.valueOf(dueDate));
                ps.executeUpdate();
            }

            String updateSql = "update books set available=false where book_id=?";
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            con.commit();
            System.out.println("Book issued successfully");
            System.out.println("Due date: " + dueDate);

        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.out.println("Rollback failed: " + ex.getMessage());
            }
            System.out.println("Book issue failed: " + e.getMessage());
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    static void returnBook() {
        int issueId = readInt("Enter issue ID: ");

        try {
            con.setAutoCommit(false);

            String sql = "select book_id,due_date,return_date from book_issues where issue_id=? for update";
            int bookId;
            LocalDate dueDate;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, issueId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Issue record not found");
                        con.rollback();
                        return;
                    }

                    if (rs.getDate("return_date") != null) {
                        System.out.println("This book has already been returned");
                        con.rollback();
                        return;
                    }

                    bookId = rs.getInt("book_id");
                    dueDate = rs.getDate("due_date").toLocalDate();
                }
            }

            LocalDate returnDate = LocalDate.now();
            long lateDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, returnDate));
            double fine = lateDays * 5.0;

            String updateIssue = "update book_issues set return_date=?,fine_amount=? where issue_id=?";
            try (PreparedStatement ps = con.prepareStatement(updateIssue)) {
                ps.setDate(1, Date.valueOf(returnDate));
                ps.setDouble(2, fine);
                ps.setInt(3, issueId);
                ps.executeUpdate();
            }

            String updateBook = "update books set available=true where book_id=?";
            try (PreparedStatement ps = con.prepareStatement(updateBook)) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            con.commit();
            System.out.println("Book returned successfully");
            System.out.println("Late days: " + lateDays);
            System.out.printf("Fine amount: Rs. %.2f%n", fine);

        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.out.println("Rollback failed: " + ex.getMessage());
            }
            System.out.println("Book return failed: " + e.getMessage());
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    static void viewIssueHistory() {
        String sql = "select bi.issue_id,b.book_name,m.member_name,bi.issue_date,bi.due_date,bi.return_date,bi.fine_amount from book_issues bi join books b on bi.book_id=b.book_id join members m on bi.member_id=m.member_id order by bi.issue_id desc";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\nIssue ID | Book | Member | Issue Date | Due Date | Return Date | Fine");
            System.out.println("--------------------------------------------------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%d | %s | %s | %s | %s | %s | Rs. %.2f%n",
                    rs.getInt("issue_id"),
                    rs.getString("book_name"),
                    rs.getString("member_name"),
                    rs.getDate("issue_date"),
                    rs.getDate("due_date"),
                    rs.getDate("return_date"),
                    rs.getDouble("fine_amount"));
            }

            if (!found) System.out.println("No issue records found");

        } catch (SQLException e) {
            System.out.println("Unable to display issue history: " + e.getMessage());
        }
    }
}
