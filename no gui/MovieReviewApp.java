import java.sql.*;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MovieReviewApp {
    // ---------- CONFIG ----------
    // Use MySQL. Ensure MySQL JDBC driver (mysql-connector-java) is added to classpath.
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/moviereviewdb";
    private static final String JDBC_USER = "root"; // change as needed
    private static final String JDBC_PASS = "root"; // change as needed

    // ---------- STATE ----------
    private static Connection conn;
    private static Scanner sc = new Scanner(System.in);
    private static Integer currentUserId = null;
    private static String currentUsername = null;

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
            conn.setAutoCommit(true);
            setupDB();
            showWelcome();
        } catch (Exception e) {
            System.err.println("Fatal error connecting to DB: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }

    // Create tables if not exist
    private static void setupDB() throws SQLException {
        Statement st = conn.createStatement();
        st.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) UNIQUE NOT NULL, password VARCHAR(100) NOT NULL)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS movies (id INT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(200) NOT NULL, year INT, starring VARCHAR(300), description TEXT)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS reviews (id INT AUTO_INCREMENT PRIMARY KEY, movie_id INT NOT NULL, user_id INT NOT NULL, rating INT NOT NULL CHECK(rating >= 1 AND rating <= 10), comment TEXT, created_at DATETIME, FOREIGN KEY(movie_id) REFERENCES movies(id), FOREIGN KEY(user_id) REFERENCES users(id))");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS watchlist (id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, movie_id INT NOT NULL, added_at DATETIME, UNIQUE(user_id, movie_id), FOREIGN KEY(movie_id) REFERENCES movies(id), FOREIGN KEY(user_id) REFERENCES users(id))");
        st.close();
    }

    private static void showWelcome() throws SQLException {
        while (true) {
            System.out.println("\n=== Movie Review Console App ===");
            System.out.println("1) Register");
            System.out.println("2) Login");
            System.out.println("3) Browse movies (no login required)");
            System.out.println("4) Exit");
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1": register(); break;
                case "2": if (login()) userMenu(); break;
                case "3": browseMovies(); break;
                case "4": System.out.println("Bye!"); return;
                default: System.out.println("Invalid choice");
            }
        }
    }

    private static void register() {
        try {
            System.out.print("Choose username: ");
            String username = sc.nextLine().trim();
            System.out.print("Choose password: ");
            String password = sc.nextLine().trim();
            PreparedStatement ps = conn.prepareStatement("INSERT INTO users(username, password) VALUES(?,?)");
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            ps.close();
            System.out.println("Registered. You can log in now.");
        } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("duplicate")) {
                System.out.println("Username already exists. Try a different one.");
            } else {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static boolean login() {
        try {
            System.out.print("Username: ");
            String username = sc.nextLine().trim();
            System.out.print("Password: ");
            String password = sc.nextLine().trim();
            PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username = ? AND password = ?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentUserId = rs.getInt("id");
                currentUsername = username;
                System.out.println("Welcome, " + username + "!");
                rs.close(); ps.close();
                return true;
            } else {
                System.out.println("Invalid credentials.");
                rs.close(); ps.close();
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
            return false;
        }
    }

    private static void userMenu() throws SQLException {
        while (true) {
            System.out.println("\n--- User: " + currentUsername + " ---");
            System.out.println("1) Add movie");
            System.out.println("2) Browse movies");
            System.out.println("3) View movie details / add review");
            System.out.println("4) My watchlist");
            System.out.println("5) Logout");
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1": addMovie(); break;
                case "2": browseMovies(); break;
                case "3": viewAndReviewMovie(); break;
                case "4": viewWatchlist(); break;
                case "5": currentUserId = null; currentUsername = null; return;
                default: System.out.println("Invalid choice");
            }
        }
    }

    private static void addMovie() {
        try {
            System.out.print("Title: ");
            String title = sc.nextLine().trim();
            System.out.print("Year (optional): ");
            String y = sc.nextLine().trim();
            Integer year = null;
            if (!y.isEmpty()) try { year = Integer.parseInt(y); } catch (NumberFormatException ignored) {}
            System.out.print("Starring (comma separated): ");
            String starring = sc.nextLine().trim();
            System.out.print("Short description: ");
            String desc = sc.nextLine().trim();

            PreparedStatement ps = conn.prepareStatement("INSERT INTO movies(title, year, starring, description) VALUES(?,?,?,?)");
            ps.setString(1, title);
            if (year == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, year);
            ps.setString(3, starring);
            ps.setString(4, desc);
            ps.executeUpdate();
            ps.close();
            System.out.println("Movie added.");
        } catch (SQLException e) {
            System.out.println("Error adding movie: " + e.getMessage());
        }
    }

    private static void browseMovies() {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT id, title, year FROM movies ORDER BY title");
            ResultSet rs = ps.executeQuery();
            System.out.println("\nMovies:");
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                int year = rs.getInt("year");
                String yearStr = rs.wasNull() ? "" : (" (" + year + ")");
                System.out.println(id + ": " + title + yearStr);
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            System.out.println("Error listing movies: " + e.getMessage());
        }
    }

    private static void viewAndReviewMovie() {
        try {
            System.out.print("Enter movie id to view details: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM movies WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("Movie not found."); rs.close(); ps.close(); return;
            }
            System.out.println("\nTitle: " + rs.getString("title"));
            int year = rs.getInt("year"); if (!rs.wasNull()) System.out.println("Year: " + year);
            System.out.println("Starring: " + rs.getString("starring"));
            System.out.println("Description: " + rs.getString("description"));
            rs.close(); ps.close();

            PreparedStatement ps2 = conn.prepareStatement("SELECT AVG(rating) as avgR, COUNT(*) as cnt FROM reviews WHERE movie_id = ?");
            ps2.setInt(1, id);
            ResultSet r2 = ps2.executeQuery();
            if (r2.next()) {
                double avg = r2.getDouble("avgR");
                int cnt = r2.getInt("cnt");
                if (cnt > 0) System.out.printf("Average rating: %.2f (%d reviews)\n", avg, cnt);
                else System.out.println("No reviews yet.");
            }
            r2.close(); ps2.close();

            PreparedStatement ps3 = conn.prepareStatement("SELECT r.rating, r.comment, r.created_at, u.username FROM reviews r JOIN users u ON r.user_id = u.id WHERE r.movie_id = ? ORDER BY r.id DESC LIMIT 5");
            ps3.setInt(1, id);
            ResultSet r3 = ps3.executeQuery();
            System.out.println("\nRecent reviews:");
            boolean any = false;
            while (r3.next()) {
                any = true;
                System.out.println("- " + r3.getString("username") + " (" + r3.getInt("rating") + ") at " + r3.getTimestamp("created_at") );
                System.out.println("  " + r3.getString("comment"));
            }
            if (!any) System.out.println("(none)");
            r3.close(); ps3.close();

            while (true) {
                System.out.println("\nActions: 1) Add review  2) Add to watchlist  3) Back");
                System.out.print("Choose: ");
                String act = sc.nextLine().trim();
                if (act.equals("1")) addReview(id);
                else if (act.equals("2")) addToWatchlist(id);
                else break;
            }

        } catch (NumberFormatException nfe) {
            System.out.println("Invalid id.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void addReview(int movieId) {
        if (currentUserId == null) {
            System.out.println("Must be logged in to add review.");
            return;
        }
        try {
            System.out.print("Rating (1-10): ");
            int rating = Integer.parseInt(sc.nextLine().trim());
            if (rating < 1 || rating > 10) { System.out.println("Invalid rating"); return; }
            System.out.print("Comment: ");
            String comment = sc.nextLine().trim();
            PreparedStatement ps = conn.prepareStatement("INSERT INTO reviews(movie_id, user_id, rating, comment, created_at) VALUES(?,?,?,?,?)");
            ps.setInt(1, movieId);
            ps.setInt(2, currentUserId);
            ps.setInt(3, rating);
            ps.setString(4, comment);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate(); ps.close();
            System.out.println("Review added.");
        } catch (NumberFormatException nfe) {
            System.out.println("Rating must be a number.");
        } catch (SQLException e) {
            System.out.println("Error adding review: " + e.getMessage());
        }
    }

    private static void addToWatchlist(int movieId) {
        if (currentUserId == null) { System.out.println("Must be logged in to add to watchlist."); return; }
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO watchlist(user_id, movie_id, added_at) VALUES(?,?,?)");
            ps.setInt(1, currentUserId);
            ps.setInt(2, movieId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            int changed = ps.executeUpdate(); ps.close();
            if (changed == 0) System.out.println("Already in watchlist."); else System.out.println("Added to watchlist.");
        } catch (SQLException e) {
            System.out.println("Error adding to watchlist: " + e.getMessage());
        }
    }

    private static void viewWatchlist() {
        if (currentUserId == null) { System.out.println("Must be logged in to see watchlist."); return; }
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT w.id, m.id as mid, m.title, w.added_at FROM watchlist w JOIN movies m ON w.movie_id = m.id WHERE w.user_id = ? ORDER BY w.added_at DESC");
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            System.out.println("\nYour watchlist:");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.println(rs.getInt("mid") + ": " + rs.getString("title") + " (added: " + rs.getTimestamp("added_at") + ")");
            }
            if (!any) System.out.println("(empty)");
            rs.close(); ps.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
