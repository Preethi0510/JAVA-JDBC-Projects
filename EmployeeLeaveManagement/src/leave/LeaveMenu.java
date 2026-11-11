package leave;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class LeaveMenu {
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
                System.out.println("\n===== EMPLOYEE LEAVE MANAGEMENT =====");
                System.out.println("1. Register Employee");
                System.out.println("2. View Employees");
                System.out.println("3. Apply for Leave");
                System.out.println("4. View Leave Requests");
                System.out.println("5. Approve Leave");
                System.out.println("6. Reject Leave");
                System.out.println("7. Check Leave Balance");
                System.out.println("8. View Leave History");
                System.out.println("9. Exit");
                choice = readInt("Enter choice: ");
                switch (choice) {
                    case 1: addEmployee(); break;
                    case 2: viewEmployees(); break;
                    case 3: applyLeave(); break;
                    case 4: viewRequests(); break;
                    case 5: approveLeave(); break;
                    case 6: rejectLeave(); break;
                    case 7: checkBalance(); break;
                    case 8: viewHistory(); break;
                    case 9: System.out.println("Thank you for using the system"); break;
                    default: System.out.println("Invalid choice");
                }
            } while (choice != 9);
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
                if (value > 0) return value;
                System.out.println("Enter a positive number");
            } catch (NumberFormatException e) {
                System.out.println("Enter numbers only");
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

    static LocalDate readDate(String message) {
        while (true) {
            System.out.print(message);
            try {
                return LocalDate.parse(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.println("Enter date in YYYY-MM-DD format");
            }
        }
    }

    static void addEmployee() {
        String name = readText("Enter employee name: ");
        if (!name.matches("[a-zA-Z .'-]{2,100}")) {
            System.out.println("Invalid employee name");
            return;
        }
        String department = readText("Enter department: ");
        String phone;
        while (true) {
            phone = readText("Enter 10-digit phone number: ");
            if (phone.matches("[6-9][0-9]{9}")) break;
            System.out.println("Enter a valid 10-digit phone number");
        }
        String sql = "insert into employees(employee_name,department,phone) values(?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, department);
            ps.setString(3, phone);
            ps.executeUpdate();
            System.out.println("Employee registered successfully");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Phone number already exists");
        } catch (SQLException e) {
            System.out.println("Unable to register employee: " + e.getMessage());
        }
    }

    static void viewEmployees() {
        String sql = "select * from employees order by employee_id";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\nID\tName\t\tDepartment\tPhone\t\tTotal\tUsed");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%d\t%-15s\t%-12s\t%s\t%d\t%d%n", rs.getInt("employee_id"), rs.getString("employee_name"), rs.getString("department"), rs.getString("phone"), rs.getInt("total_leaves"), rs.getInt("used_leaves"));
            }
            if (!found) System.out.println("No employees found");
        } catch (SQLException e) {
            System.out.println("Unable to display employees: " + e.getMessage());
        }
    }

    static void applyLeave() {
        int employeeId = readInt("Enter employee ID: ");
        String leaveType;
        while (true) {
            leaveType = readText("Enter leave type (Casual/Sick/Earned): ");
            if (leaveType.equalsIgnoreCase("Casual") || leaveType.equalsIgnoreCase("Sick") || leaveType.equalsIgnoreCase("Earned")) break;
            System.out.println("Choose Casual, Sick or Earned");
        }
        LocalDate fromDate = readDate("Enter starting date (YYYY-MM-DD): ");
        LocalDate toDate = readDate("Enter ending date (YYYY-MM-DD): ");
        if (fromDate.isBefore(LocalDate.now()) || toDate.isBefore(fromDate)) {
            System.out.println("Invalid leave dates");
            return;
        }
        int days = (int) ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        String reason = readText("Enter reason: ");
        try {
            String employeeSql = "select employee_id,total_leaves,used_leaves from employees where employee_id=?";
            int total;
            int used;
            try (PreparedStatement ps = con.prepareStatement(employeeSql)) {
                ps.setInt(1, employeeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Employee ID not found");
                        return;
                    }
                    total = rs.getInt("total_leaves");
                    used = rs.getInt("used_leaves");
                }
            }
            String overlapSql = "select count(*) from leave_requests where employee_id=? and status in ('Pending','Approved') and from_date<=? and to_date>=?";
            try (PreparedStatement ps = con.prepareStatement(overlapSql)) {
                ps.setInt(1, employeeId);
                ps.setDate(2, Date.valueOf(toDate));
                ps.setDate(3, Date.valueOf(fromDate));
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        System.out.println("Leave dates overlap with an existing request");
                        return;
                    }
                }
            }
            String balanceSql = "select coalesce(sum(leave_days),0) from leave_requests where employee_id=? and status='Pending'";
            int pending;
            try (PreparedStatement ps = con.prepareStatement(balanceSql)) {
                ps.setInt(1, employeeId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    pending = rs.getInt(1);
                }
            }
            if (days > total - used - pending) {
                System.out.println("Insufficient leave balance");
                System.out.println("Available balance: " + (total - used - pending));
                return;
            }
            String sql = "insert into leave_requests(employee_id,leave_type,from_date,to_date,leave_days,reason,status,applied_on) values(?,?,?,?,?,?,'Pending',?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, employeeId);
                ps.setString(2, leaveType);
                ps.setDate(3, Date.valueOf(fromDate));
                ps.setDate(4, Date.valueOf(toDate));
                ps.setInt(5, days);
                ps.setString(6, reason);
                ps.setDate(7, Date.valueOf(LocalDate.now()));
                ps.executeUpdate();
                System.out.println("Leave request submitted successfully");
                System.out.println("Total leave days: " + days);
                System.out.println("Status: Pending");
            }
        } catch (SQLException e) {
            System.out.println("Unable to apply for leave: " + e.getMessage());
        }
    }

    static void viewRequests() {
        String sql = "select lr.request_id,e.employee_name,lr.leave_type,lr.from_date,lr.to_date,lr.leave_days,lr.reason,lr.status from leave_requests lr join employees e on lr.employee_id=e.employee_id order by lr.request_id desc";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\nRequest ID | Employee | Type | From | To | Days | Reason | Status");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%d | %s | %s | %s | %s | %d | %s | %s%n", rs.getInt("request_id"), rs.getString("employee_name"), rs.getString("leave_type"), rs.getDate("from_date"), rs.getDate("to_date"), rs.getInt("leave_days"), rs.getString("reason"), rs.getString("status"));
            }
            if (!found) System.out.println("No leave requests found");
        } catch (SQLException e) {
            System.out.println("Unable to display requests: " + e.getMessage());
        }
    }

    static void approveLeave() {
        int requestId = readInt("Enter request ID: ");
        try {
            con.setAutoCommit(false);
            int employeeId;
            int days;
            try (PreparedStatement ps = con.prepareStatement("select employee_id,leave_days,status from leave_requests where request_id=? for update")) {
                ps.setInt(1, requestId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Request ID not found");
                        con.rollback();
                        return;
                    }
                    if (!rs.getString("status").equals("Pending")) {
                        System.out.println("Only pending requests can be approved");
                        con.rollback();
                        return;
                    }
                    employeeId = rs.getInt("employee_id");
                    days = rs.getInt("leave_days");
                }
            }
            int total;
            int used;
            try (PreparedStatement ps = con.prepareStatement("select total_leaves,used_leaves from employees where employee_id=? for update")) {
                ps.setInt(1, employeeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Employee not found");
                        con.rollback();
                        return;
                    }
                    total = rs.getInt("total_leaves");
                    used = rs.getInt("used_leaves");
                }
            }
            String pendingSql = "select coalesce(sum(leave_days),0) from leave_requests where employee_id=? and status='Pending' and request_id<>?";
            int pending;
            try (PreparedStatement ps = con.prepareStatement(pendingSql)) {
                ps.setInt(1, employeeId);
                ps.setInt(2, requestId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    pending = rs.getInt(1);
                }
            }
            if (days > total - used - pending) {
                System.out.println("Insufficient leave balance for approval");
                con.rollback();
                return;
            }
            try (PreparedStatement ps = con.prepareStatement("update employees set used_leaves=used_leaves+? where employee_id=?")) {
                ps.setInt(1, days);
                ps.setInt(2, employeeId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("update leave_requests set status='Approved' where request_id=?")) {
                ps.setInt(1, requestId);
                ps.executeUpdate();
            }
            con.commit();
            System.out.println("Leave approved successfully");
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.out.println("Rollback failed: " + ex.getMessage());
            }
            System.out.println("Unable to approve leave: " + e.getMessage());
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    static void rejectLeave() {
        int requestId = readInt("Enter request ID: ");
        String sql = "update leave_requests set status='Rejected' where request_id=? and status='Pending'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Leave request rejected successfully");
            else System.out.println("Request not found or already processed");
        } catch (SQLException e) {
            System.out.println("Unable to reject leave: " + e.getMessage());
        }
    }

    static void checkBalance() {
        int employeeId = readInt("Enter employee ID: ");
        String sql = "select employee_name,total_leaves,used_leaves from employees where employee_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total_leaves");
                    int used = rs.getInt("used_leaves");
                    int pending = 0;
                    try (PreparedStatement p = con.prepareStatement("select coalesce(sum(leave_days),0) from leave_requests where employee_id=? and status='Pending'")) {
                        p.setInt(1, employeeId);
                        try (ResultSet r = p.executeQuery()) {
                            r.next();
                            pending = r.getInt(1);
                        }
                    }
                    System.out.println("Employee: " + rs.getString("employee_name"));
                    System.out.println("Total leaves: " + total);
                    System.out.println("Used leaves: " + used);
                    System.out.println("Pending leaves: " + pending);
                    System.out.println("Available leaves: " + (total - used - pending));
                } else {
                    System.out.println("Employee ID not found");
                }
            }
        } catch (SQLException e) {
            System.out.println("Unable to check balance: " + e.getMessage());
        }
    }

    static void viewHistory() {
        int employeeId = readInt("Enter employee ID: ");
        String sql = "select e.employee_name,lr.request_id,lr.leave_type,lr.from_date,lr.to_date,lr.leave_days,lr.reason,lr.status from employees e join leave_requests lr on e.employee_id=lr.employee_id where e.employee_id=? order by lr.request_id desc";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("Request ID: %d | Employee: %s | Type: %s | From: %s | To: %s | Days: %d | Reason: %s | Status: %s%n", rs.getInt("request_id"), rs.getString("employee_name"), rs.getString("leave_type"), rs.getDate("from_date"), rs.getDate("to_date"), rs.getInt("leave_days"), rs.getString("reason"), rs.getString("status"));
                }
                if (!found) System.out.println("No leave history found for this employee");
            }
        } catch (SQLException e) {
            System.out.println("Unable to display leave history: " + e.getMessage());
        }
    }
}