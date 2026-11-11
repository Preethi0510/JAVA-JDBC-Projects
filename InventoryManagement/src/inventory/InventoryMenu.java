package inventory;
import java.sql.*;
import java.util.Scanner;

public class InventoryMenu {
    static Scanner sc = new Scanner(System.in);
    static Connection con;

    public static void main(String[] args) {
        try {
            con = DBConnections.getConnection();
            if (con == null) {
                System.out.println("Database connection failed");
                return;
            }
            int choice;
            do {
                System.out.println("\n===== INVENTORY MANAGEMENT =====");
                System.out.println("1. Add Product");
                System.out.println("2. View Products");
                System.out.println("3. Search Product");
                System.out.println("4. Update Product");
                System.out.println("5. Delete Product");
                System.out.println("6. Add Stock");
                System.out.println("7. Reduce Stock");
                System.out.println("8. View Low Stock Products");
                System.out.println("9. View Stock History");
                System.out.println("10. Exit");
                choice = readInt("Enter choice: ");
                switch (choice) {
                    case 1: addProduct(); break;
                    case 2: viewProducts(); break;
                    case 3: searchProduct(); break;
                    case 4: updateProduct(); break;
                    case 5: deleteProduct(); break;
                    case 6: addStock(); break;
                    case 7: reduceStock(); break;
                    case 8: lowStock(); break;
                    case 9: viewHistory(); break;
                    case 10: System.out.println("Thank you for using Inventory Management"); break;
                    default: System.out.println("Invalid choice");
                }
            } while (choice != 10);
        } finally {
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
                if (value >= 0) return value;
                System.out.println("Enter a positive value");
            } catch (NumberFormatException e) {
                System.out.println("Enter numbers only");
            }
        }
    }

    static double readPrice(String message) {
        while (true) {
            System.out.print(message);
            try {
                double value = Double.parseDouble(sc.nextLine().trim());
                if (Double.isFinite(value) && value > 0) return value;
                System.out.println("Price must be greater than zero");
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid price");
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

    static void addProduct() {
        String name = readText("Enter product name: ");
        String category = readText("Enter category: ");
        double price = readPrice("Enter price: ");
        int quantity = readInt("Enter initial quantity: ");
        int minStock = readInt("Enter minimum stock level: ");
        try {
            con.setAutoCommit(false);
            String sql = "insert into products(product_name,category,price,quantity,min_stock) values(?,?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, category);
                ps.setDouble(3, price);
                ps.setInt(4, quantity);
                ps.setInt(5, minStock);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int productId = rs.getInt(1);
                        if (quantity > 0) {
                            try (PreparedStatement hs = con.prepareStatement("insert into stock_history(product_id,transaction_type,quantity) values(?,'Added',?)")) {
                                hs.setInt(1, productId);
                                hs.setInt(2, quantity);
                                hs.executeUpdate();
                            }
                        }
                        con.commit();
                        System.out.println("Product added successfully");
                        System.out.println("Product ID: " + productId);
                    } else {
                        con.rollback();
                        System.out.println("Unable to generate product ID");
                    }
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            rollback();
            System.out.println("Product name already exists");
        } catch (SQLException e) {
            rollback();
            System.out.println("Unable to add product: " + e.getMessage());
        } finally {
            resetAutoCommit();
        }
    }

    static void viewProducts() {
        String sql = "select * from products order by product_id";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\nID | Product | Category | Price | Quantity | Minimum Stock");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%d | %s | %s | %.2f | %d | %d%n", rs.getInt("product_id"), rs.getString("product_name"), rs.getString("category"), rs.getDouble("price"), rs.getInt("quantity"), rs.getInt("min_stock"));
            }
            if (!found) System.out.println("No products found");
        } catch (SQLException e) {
            System.out.println("Unable to display products: " + e.getMessage());
        }
    }

    static void searchProduct() {
        System.out.println("1. Search by Product ID");
        System.out.println("2. Search by Product Name");
        int choice = readInt("Enter choice: ");
        String sql;
        try {
            if (choice == 1) {
                sql = "select * from products where product_id=?";
            } else if (choice == 2) {
                sql = "select * from products where product_name like ?";
            } else {
                System.out.println("Invalid choice");
                return;
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (choice == 1) {
                    ps.setInt(1, readInt("Enter product ID: "));
                } else {
                    ps.setString(1, "%" + readText("Enter product name: ") + "%");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    boolean found = false;
                    while (rs.next()) {
                        found = true;
                        System.out.printf("ID: %d | Name: %s | Category: %s | Price: %.2f | Quantity: %d | Minimum Stock: %d%n", rs.getInt("product_id"), rs.getString("product_name"), rs.getString("category"), rs.getDouble("price"), rs.getInt("quantity"), rs.getInt("min_stock"));
                    }
                    if (!found) System.out.println("Product not found");
                }
            }
        } catch (SQLException e) {
            System.out.println("Unable to search product: " + e.getMessage());
        }
    }

    static void updateProduct() {
        int id = readInt("Enter product ID: ");
        String name = readText("Enter new product name: ");
        String category = readText("Enter new category: ");
        double price = readPrice("Enter new price: ");
        int minStock = readInt("Enter new minimum stock level: ");
        String sql = "update products set product_name=?,category=?,price=?,min_stock=? where product_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.setInt(4, minStock);
            ps.setInt(5, id);
            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Product updated successfully");
            else System.out.println("Product ID not found");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Product name already exists");
        } catch (SQLException e) {
            System.out.println("Unable to update product: " + e.getMessage());
        }
    }

    static void deleteProduct() {
        int id = readInt("Enter product ID: ");
        String sql = "delete from products where product_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Product deleted successfully");
            else System.out.println("Product ID not found");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Product cannot be deleted because stock history exists");
        } catch (SQLException e) {
            System.out.println("Unable to delete product: " + e.getMessage());
        }
    }

    static void addStock() {
        int id = readInt("Enter product ID: ");
        int quantity = readInt("Enter quantity to add: ");
        if (quantity == 0) {
            System.out.println("Quantity must be greater than zero");
            return;
        }
        changeStock(id, quantity, "Stock In");
    }

    static void reduceStock() {
        int id = readInt("Enter product ID: ");
        int quantity = readInt("Enter quantity to reduce: ");
        if (quantity == 0) {
            System.out.println("Quantity must be greater than zero");
            return;
        }
        changeStock(id, quantity, "Stock Out");
    }

    static void changeStock(int id, int quantity, String type) {
        try {
            con.setAutoCommit(false);
            int current;
            try (PreparedStatement ps = con.prepareStatement("select quantity from products where product_id=? for update")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Product ID not found");
                        con.rollback();
                        return;
                    }
                    current = rs.getInt("quantity");
                }
            }
            if (type.equals("Stock Out") && quantity > current) {
                System.out.println("Insufficient stock. Available quantity: " + current);
                con.rollback();
                return;
            }
            int newQuantity;
            try {
                newQuantity = type.equals("Stock In") ? Math.addExact(current, quantity) : current - quantity;
            } catch (ArithmeticException e) {
                System.out.println("Stock quantity exceeds the allowed limit");
                con.rollback();
                return;
            }
            try (PreparedStatement ps = con.prepareStatement("update products set quantity=? where product_id=?")) {
                ps.setInt(1, newQuantity);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("insert into stock_history(product_id,transaction_type,quantity) values(?,?,?)")) {
                ps.setInt(1, id);
                ps.setString(2, type);
                ps.setInt(3, quantity);
                ps.executeUpdate();
            }
            con.commit();
            System.out.println(type + " completed successfully");
            System.out.println("Updated stock: " + newQuantity);
        } catch (SQLException e) {
            rollback();
            System.out.println("Unable to update stock: " + e.getMessage());
        } finally {
            resetAutoCommit();
        }
    }

    static void lowStock() {
        String sql = "select * from products where quantity<=min_stock order by quantity";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\nLow Stock Products");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("ID: %d | Name: %s | Quantity: %d | Minimum Stock: %d%n", rs.getInt("product_id"), rs.getString("product_name"), rs.getInt("quantity"), rs.getInt("min_stock"));
            }
            if (!found) System.out.println("No low stock products found");
        } catch (SQLException e) {
            System.out.println("Unable to display low stock products: " + e.getMessage());
        }
    }

    static void viewHistory() {
        String sql = "select h.history_id,p.product_name,h.transaction_type,h.quantity,h.transaction_date from stock_history h join products p on h.product_id=p.product_id order by h.history_id desc";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\nHistory ID | Product | Transaction | Quantity | Date");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%d | %s | %s | %d | %s%n", rs.getInt("history_id"), rs.getString("product_name"), rs.getString("transaction_type"), rs.getInt("quantity"), rs.getTimestamp("transaction_date"));
            }
            if (!found) System.out.println("No stock history found");
        } catch (SQLException e) {
            System.out.println("Unable to display stock history: " + e.getMessage());
        }
    }

    static void rollback() {
        try {
            con.rollback();
        } catch (SQLException e) {
            System.out.println("Rollback failed: " + e.getMessage());
        }
    }

    static void resetAutoCommit() {
        try {
            con.setAutoCommit(true);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}