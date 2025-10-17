import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;

// 🎬 Movie Review App with Swing GUI + MySQL (Single File)
public class MovieReviewAppGUI extends JFrame {
    // ---------- CONFIG ----------
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/moviereviewdb";
    private static final String JDBC_USER = "root"; // change as needed
    private static final String JDBC_PASS = "root"; // change as needed

    private static Connection conn;
    private int currentUserId = -1;
    private String currentUsername = null;

    // ---------- UI STATE ----------
    private CardLayout layout = new CardLayout();
    private JPanel mainPanel = new JPanel(layout);
    private JTable movieTable = new JTable();
    private JTable watchlistTable = new JTable();
    private JTextArea movieDetailsArea = new JTextArea();

    // ---------- MAIN ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MovieReviewAppGUI().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ---------- CONSTRUCTOR ----------
    public MovieReviewAppGUI() throws SQLException {
        connectDB();
        setupDB();
        setupUI();
    }

    private void connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "DB Connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void setupDB() throws SQLException {
        Statement st = conn.createStatement();
        st.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) UNIQUE NOT NULL, password VARCHAR(100) NOT NULL)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS movies (id INT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(200), year INT, starring VARCHAR(300), description TEXT)");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS reviews (id INT AUTO_INCREMENT PRIMARY KEY, movie_id INT, user_id INT, rating INT, comment TEXT, created_at DATETIME, FOREIGN KEY(movie_id) REFERENCES movies(id), FOREIGN KEY(user_id) REFERENCES users(id))");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS watchlist (id INT AUTO_INCREMENT PRIMARY KEY, user_id INT, movie_id INT, added_at DATETIME, UNIQUE(user_id, movie_id), FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(movie_id) REFERENCES movies(id))");
        st.close();
    }

    // ---------- UI SETUP ----------
    private void setupUI() {
        setTitle("🎬 Movie Review App");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panels
        JPanel loginPanel = createLoginPanel();
        JPanel dashboardPanel = createDashboardPanel();

        mainPanel.add(loginPanel, "login");
        mainPanel.add(dashboardPanel, "dashboard");

        add(mainPanel);
        layout.show(mainPanel, "login");
    }

    // ---------- LOGIN PANEL ----------
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 200, 50, 200));

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton loginBtn = new JButton("Login");
        JButton regBtn = new JButton("Register");

        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        panel.add(loginBtn);
        panel.add(regBtn);

        loginBtn.addActionListener(e -> {
            try {
                String u = userField.getText();
                String p = new String(passField.getPassword());
                PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username=? AND password=?");
                ps.setString(1, u);
                ps.setString(2, p);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    currentUserId = rs.getInt("id");
                    currentUsername = u;
                    JOptionPane.showMessageDialog(this, "Welcome " + u + "!");
                    refreshMovies();
                    refreshWatchlist();
                    layout.show(mainPanel, "dashboard");
                } else JOptionPane.showMessageDialog(this, "Invalid credentials");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        regBtn.addActionListener(e -> {
            try {
                String u = userField.getText();
                String p = new String(passField.getPassword());
                PreparedStatement ps = conn.prepareStatement("INSERT INTO users(username,password) VALUES(?,?)");
                ps.setString(1, u);
                ps.setString(2, p);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Registered successfully!");
            } catch (SQLException ex) {
                if (ex.getMessage().toLowerCase().contains("duplicate"))
                    JOptionPane.showMessageDialog(this, "Username already exists!");
                else JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        return panel;
    }

    // ---------- DASHBOARD PANEL ----------
    private JPanel createDashboardPanel() {
        JPanel dash = new JPanel(new BorderLayout());
        JPanel top = new JPanel();
        JButton browseBtn = new JButton("Browse Movies");
        JButton addBtn = new JButton("Add Movie");
        JButton watchlistBtn = new JButton("My Watchlist");
        JButton logoutBtn = new JButton("Logout");

        top.add(browseBtn);
        top.add(addBtn);
        top.add(watchlistBtn);
        top.add(logoutBtn);
        dash.add(top, BorderLayout.NORTH);

        JPanel content = new JPanel(new CardLayout());
        JPanel moviePanel = createMoviePanel();
        JPanel watchlistPanel = createWatchlistPanel();
        content.add(moviePanel, "movies");
        content.add(watchlistPanel, "watchlist");
        dash.add(content, BorderLayout.CENTER);

        CardLayout innerLayout = (CardLayout) content.getLayout();

        browseBtn.addActionListener(e -> innerLayout.show(content, "movies"));
        watchlistBtn.addActionListener(e -> innerLayout.show(content, "watchlist"));
        logoutBtn.addActionListener(e -> layout.show(mainPanel, "login"));
        addBtn.addActionListener(e -> addMovieDialog());

        return dash;
    }

    // ---------- MOVIE PANEL ----------
    private JPanel createMoviePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        movieTable.setModel(new DefaultTableModel(new String[]{"ID", "Title", "Year"}, 0));
        JScrollPane scroll = new JScrollPane(movieTable);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton viewBtn = new JButton("View Details");
        JButton refreshBtn = new JButton("Refresh");
        bottom.add(viewBtn);
        bottom.add(refreshBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        viewBtn.addActionListener(e -> viewMovieDetails());
        refreshBtn.addActionListener(e -> refreshMovies());

        return panel;
    }

    // ---------- WATCHLIST PANEL ----------
    private JPanel createWatchlistPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        watchlistTable.setModel(new DefaultTableModel(new String[]{"Movie ID", "Title", "Added At"}, 0));
        JScrollPane scroll = new JScrollPane(watchlistTable);
        panel.add(scroll, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh");
        panel.add(refreshBtn, BorderLayout.SOUTH);
        refreshBtn.addActionListener(e -> refreshWatchlist());
        return panel;
    }

    // ---------- HELPER FUNCTIONS ----------
    private void refreshMovies() {
        try {
            DefaultTableModel model = (DefaultTableModel) movieTable.getModel();
            model.setRowCount(0);
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT id, title, year FROM movies ORDER BY title");
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getInt("year")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void refreshWatchlist() {
        try {
            DefaultTableModel model = (DefaultTableModel) watchlistTable.getModel();
            model.setRowCount(0);
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT m.id, m.title, w.added_at FROM watchlist w JOIN movies m ON w.movie_id=m.id WHERE w.user_id=? ORDER BY w.added_at DESC");
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getTimestamp("added_at")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void viewMovieDetails() {
        int row = movieTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a movie first.");
            return;
        }
        int movieId = (int) movieTable.getValueAt(row, 0);

        JTextArea details = new JTextArea(15, 50);
        details.setEditable(false);
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM movies WHERE id=?");
            ps.setInt(1, movieId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                details.append("Title: " + rs.getString("title") + "\n");
                details.append("Year: " + rs.getInt("year") + "\n");
                details.append("Starring: " + rs.getString("starring") + "\n\n");
                details.append("Description: " + rs.getString("description") + "\n\n");
            }
        } catch (SQLException e) {
            details.append("Error: " + e.getMessage());
        }

        int opt = JOptionPane.showOptionDialog(this, new JScrollPane(details),
                "Movie Details", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new String[]{"Add Review", "Add to Watchlist", "Close"}, "Close");

        if (opt == 0) addReviewDialog(movieId);
        if (opt == 1) addToWatchlist(movieId);
    }

    private void addMovieDialog() {
        JTextField title = new JTextField();
        JTextField year = new JTextField();
        JTextField stars = new JTextField();
        JTextArea desc = new JTextArea(5, 20);

        JPanel p = new JPanel(new GridLayout(0, 1));
        p.add(new JLabel("Title:")); p.add(title);
        p.add(new JLabel("Year:")); p.add(year);
        p.add(new JLabel("Starring:")); p.add(stars);
        p.add(new JLabel("Description:")); p.add(new JScrollPane(desc));

        int ok = JOptionPane.showConfirmDialog(this, p, "Add Movie", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO movies(title,year,starring,description) VALUES(?,?,?,?)");
                ps.setString(1, title.getText());
                ps.setInt(2, Integer.parseInt(year.getText()));
                ps.setString(3, stars.getText());
                ps.setString(4, desc.getText());
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Movie added!");
                refreshMovies();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void addReviewDialog(int movieId) {
        String ratingStr = JOptionPane.showInputDialog("Rating (1–10):");
        if (ratingStr == null) return;
        String comment = JOptionPane.showInputDialog("Comment:");
        if (comment == null) return;
        try {
            int rating = Integer.parseInt(ratingStr);
            PreparedStatement ps = conn.prepareStatement("INSERT INTO reviews(movie_id,user_id,rating,comment,created_at) VALUES(?,?,?,?,?)");
            ps.setInt(1, movieId);
            ps.setInt(2, currentUserId);
            ps.setInt(3, rating);
            ps.setString(4, comment);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Review added!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void addToWatchlist(int movieId) {
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO watchlist(user_id,movie_id,added_at) VALUES(?,?,NOW())");
            ps.setInt(1, currentUserId);
            ps.setInt(2, movieId);
            int n = ps.executeUpdate();
            JOptionPane.showMessageDialog(this, n > 0 ? "Added to watchlist!" : "Already in watchlist!");
            refreshWatchlist();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
}
