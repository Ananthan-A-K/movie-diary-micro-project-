import java.awt.*;
import java.sql.*; 
import java.time.LocalDateTime;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;  

// 🎬 Movie Review App with Swing GUI + MySQL (Single File)
public class movie extends JFrame {
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

    // ---------- MAIN ----------
    public static void main(String[] args) {
        // --- MODERN LOOK AND FEEL SETUP ---
        try {
            // Set Nimbus Look and Feel for a cleaner, modern look
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, fall back to default
            System.out.println("Nimbus L&F not found. Using default.");
        }
        // ----------------------------------

        SwingUtilities.invokeLater(() -> {
            try {
                new movie().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ---------- CONSTRUCTOR ----------
    public movie() throws SQLException {
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

    // ---------- UI SETUP (MODIFIED) ----------
    private void setupUI() {
        setTitle("🎬 Movie Review App");
        setSize(1000, 700); // Slightly larger for modern feel
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

    // ---------- LOGIN PANEL (MODIFIED) ----------
    private JPanel createLoginPanel() {
        // Use a container panel with a BorderLayout to center the login box
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(new Color(245, 245, 245)); // Light background

        JPanel panel = new JPanel(new GridLayout(4, 2, 15, 15)); // Increased gap
        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(400, 200)); // Fixed size for the login box

        // Create a central panel to hold the login box
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.add(panel);
        centerPanel.setBackground(new Color(245, 245, 245));


        JTextField userField = new JTextField(20);
        JPasswordField passField = new JPasswordField(20);
        JButton loginBtn = new JButton("Login");
        JButton regBtn = new JButton("Register");

        // Styling for a modern look
        loginBtn.setBackground(new Color(59, 89, 182)); // Facebook blue
        loginBtn.setForeground(Color.WHITE);
        regBtn.setBackground(new Color(66, 170, 70)); // Green for a clear call to action
        regBtn.setForeground(Color.WHITE);
        
        loginBtn.setFocusPainted(false);
        regBtn.setFocusPainted(false);

        panel.add(createStyledLabel("Username:"));
        panel.add(userField);
        panel.add(createStyledLabel("Password:"));
        panel.add(passField);
        panel.add(loginBtn);
        panel.add(regBtn);
        
        container.add(new JLabel("<html><h1 style='color:#333333;'>🎬 CINEMA DIARY</h1></html>", SwingConstants.CENTER), BorderLayout.NORTH);
        container.add(centerPanel, BorderLayout.CENTER);

        // Action Listeners remain the same
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

        return container;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        label.setForeground(new Color(50, 50, 50));
        return label;
    }

    // ---------- DASHBOARD PANEL (MODIFIED) ----------
    private JPanel createDashboardPanel() {
        JPanel dash = new JPanel(new BorderLayout());
        
        // Top Toolbar using BorderLayout for alignment
        JPanel top = new JPanel(new BorderLayout()); 
        top.setBackground(new Color(40, 40, 40)); 
        
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        leftButtons.setOpaque(false); // Inherit background from 'top'

        // Styled Buttons
        JButton browseBtn = createStyledNavButton("Browse Movies");
        JButton addBtn = createStyledNavButton("Add Movie");
        JButton watchlistBtn = createStyledNavButton("My Watchlist");
        JButton logoutBtn = createStyledNavButton("Logout");
        
        logoutBtn.setBackground(new Color(200, 70, 70)); // Red for logout

        leftButtons.add(browseBtn);
        leftButtons.add(addBtn);
        leftButtons.add(watchlistBtn);
        
        top.add(leftButtons, BorderLayout.WEST);
        top.add(logoutBtn, BorderLayout.EAST);
        
        dash.add(top, BorderLayout.NORTH);

        // Content Area
        JPanel content = new JPanel(new CardLayout());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding around content
        
        JPanel moviePanel = createMoviePanel();
        JPanel watchlistPanel = createWatchlistPanel();
        content.add(moviePanel, "movies");
        content.add(watchlistPanel, "watchlist");
        dash.add(content, BorderLayout.CENTER);

        CardLayout innerLayout = (CardLayout) content.getLayout();

        browseBtn.addActionListener(e -> innerLayout.show(content, "movies"));
        watchlistBtn.addActionListener(e -> {
            refreshWatchlist();
            innerLayout.show(content, "watchlist");
        });
        logoutBtn.addActionListener(e -> layout.show(mainPanel, "login"));
        addBtn.addActionListener(e -> addMovieDialog());

        return dash;
    }
    
    private JButton createStyledNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(70, 130, 180)); // Steel blue for a modern color
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15)); // Internal padding
        btn.putClientProperty("JComponent.sizeVariant", "large"); // Nimbus L&F feature for larger components
        return btn;
    }

    // ---------- MOVIE PANEL (MODIFIED) ----------
    private JPanel createMoviePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); // Added gap
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Movie Catalogue", 0, 0, new Font("SansSerif", Font.BOLD, 16)));
        
        // Movie Table Setup
        movieTable.setModel(new DefaultTableModel(new String[]{"ID", "Title", "Year"}, 0));
        movieTable.setRowHeight(25); // Taller rows
        movieTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        movieTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(movieTable);
        panel.add(scroll, BorderLayout.CENTER);

        // Bottom Actions Panel
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // Buttons on the right
        JButton viewBtn = new JButton("View Details");
        JButton refreshBtn = new JButton("Refresh List");
        
        viewBtn.putClientProperty("JComponent.sizeVariant", "large");
        refreshBtn.putClientProperty("JComponent.sizeVariant", "large");
        
        bottom.add(refreshBtn);
        bottom.add(viewBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        viewBtn.addActionListener(e -> viewMovieDetails());
        refreshBtn.addActionListener(e -> refreshMovies());

        return panel;
    }

    // ---------- WATCHLIST PANEL (MODIFIED) ----------
    private JPanel createWatchlistPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); // Added gap
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "My Watchlist", 0, 0, new Font("SansSerif", Font.BOLD, 16)));

        // Watchlist Table Setup
        watchlistTable.setModel(new DefaultTableModel(new String[]{"Movie ID", "Title", "Added At"}, 0));
        watchlistTable.setRowHeight(25); // Taller rows
        watchlistTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        JScrollPane scroll = new JScrollPane(watchlistTable);
        panel.add(scroll, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh Watchlist");
        refreshBtn.putClientProperty("JComponent.sizeVariant", "large");
        
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(refreshBtn);
        
        panel.add(bottom, BorderLayout.SOUTH);
        refreshBtn.addActionListener(e -> refreshWatchlist());
        return panel;
    }

    // ---------- HELPER FUNCTIONS (MODIFIED LOGIC) ----------
    private void refreshMovies() {
        try {
            DefaultTableModel model = (DefaultTableModel) movieTable.getModel();
            model.setRowCount(0);
            Statement st = conn.createStatement();
            // --- MODIFICATION: Changed ORDER BY to 'id' ASC (ascending) ---
            ResultSet rs = st.executeQuery("SELECT id, title, year FROM movies ORDER BY id ASC");
            // ------------------------------------------------------------------
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

        String movieTitle = "Movie Details";

        // Main Dialog Content Panel using BorderLayout
        JPanel dialogContent = new JPanel(new BorderLayout(15, 15));
        dialogContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. Details Panel (GridBagLayout for structured metadata)
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Movie Info"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 2. Reviews Panel (ScrollPane for the list of reviews)
        JTextArea reviewArea = new JTextArea(10, 40);
        reviewArea.setEditable(false);
        reviewArea.setLineWrap(true);
        reviewArea.setWrapStyleWord(true);
        reviewArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JScrollPane reviewScroll = new JScrollPane(reviewArea);
        reviewScroll.setBorder(BorderFactory.createTitledBorder("User Reviews (Latest 5)"));
        
        // 3. Description Area (JTextArea, NOW DIRECTLY IN THE DETAILS PANEL)
        JTextArea descriptionArea = new JTextArea(5, 40);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        
        // *** KEY CHANGE 1: Remove border/background to make it look like a regular field ***
        descriptionArea.setBackground(detailsPanel.getBackground()); 
        descriptionArea.setBorder(null);
        
        // Database Fetch and UI Population
        try {
            // Fetch movie details
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM movies WHERE id=?");
            ps.setInt(1, movieId);
            ResultSet rs = ps.executeQuery();
            
            int rowNum = 0; // Start row counter

            if (rs.next()) {
                movieTitle = rs.getString("title");
                descriptionArea.setText(rs.getString("description"));
                descriptionArea.setCaretPosition(0); // Scroll to top

                // Populate detailsPanel (GridBagLayout)
                
                // Row 0: Title
                gbc.gridx = 0; gbc.gridy = rowNum; gbc.weightx = 0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
                detailsPanel.add(new JLabel("Title:"), gbc);
                gbc.gridx = 1; gbc.weightx = 1.0;
                detailsPanel.add(new JLabel("<html><b>" + rs.getString("title") + "</b></html>"), gbc);

                rowNum++;
                // Row 1: Year
                gbc.gridx = 0; gbc.gridy = rowNum; gbc.weightx = 0;
                detailsPanel.add(new JLabel("Year:"), gbc);
                gbc.gridx = 1; gbc.weightx = 1.0;
                detailsPanel.add(new JLabel(String.valueOf(rs.getInt("year"))), gbc);

                rowNum++;
                // Row 2: Starring
                gbc.gridx = 0; gbc.gridy = rowNum; gbc.weightx = 0;
                detailsPanel.add(new JLabel("Starring:"), gbc);
                gbc.gridx = 1; gbc.weightx = 1.0;
                detailsPanel.add(new JLabel("<html><b>" + rs.getString("starring") + "</b></html>"), gbc);
                
                rowNum++;
                // *** KEY CHANGE 2: Add Description Label ***
                gbc.gridx = 0; gbc.gridy = rowNum; gbc.weightx = 0; gbc.gridwidth = 2; // Span two columns
                gbc.anchor = GridBagConstraints.WEST;
                detailsPanel.add(createStyledLabel("Description:"), gbc); 

                rowNum++;
                // *** KEY CHANGE 3: Add Description JTextArea directly ***
                gbc.gridx = 0; gbc.gridy = rowNum; gbc.gridwidth = 2; // Span two columns
                gbc.weightx = 1.0; gbc.weighty = 1.0; // Give it space to grow
                gbc.fill = GridBagConstraints.BOTH; // Fill both horizontally and vertically
                detailsPanel.add(descriptionArea, gbc);
            }
            rs.close();
            ps.close();

            // Fetch reviews
            PreparedStatement reviewPs = conn.prepareStatement(
                "SELECT r.rating, r.comment, u.username FROM reviews r JOIN users u ON r.user_id = u.id WHERE r.movie_id = ? ORDER BY r.created_at DESC LIMIT 5");
            reviewPs.setInt(1, movieId);
            ResultSet reviewRs = reviewPs.executeQuery();
            
            StringBuilder reviewText = new StringBuilder();
            boolean hasReviews = false;
            while(reviewRs.next()) {
                reviewText.append("User: ").append(reviewRs.getString("username")).append("\n");
                reviewText.append("⭐ Rating: ").append(reviewRs.getInt("rating")).append("/10\n");
                reviewText.append("Comment: ").append(reviewRs.getString("comment")).append("\n\n");
                hasReviews = true;
            }
            if (!hasReviews) {
                reviewArea.setText("No reviews yet. Be the first!");
            } else {
                reviewArea.setText(reviewText.toString());
            }
            reviewArea.setCaretPosition(0); // Scroll to top
            reviewRs.close();
            reviewPs.close();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching details: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. Action Buttons Panel
        JButton addReviewBtn = new JButton("Add Review");
        JButton addToWatchlistBtn = new JButton("Add to Watchlist");
        JButton closeBtn = new JButton("Close");
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.add(addReviewBtn);
        btnPanel.add(addToWatchlistBtn);
        btnPanel.add(closeBtn);
        
        // Combine Panels into the main dialogContent
        dialogContent.add(detailsPanel, BorderLayout.NORTH);
        dialogContent.add(reviewScroll, BorderLayout.CENTER);
        dialogContent.add(btnPanel, BorderLayout.SOUTH);

        // Final Dialog Setup
        JDialog detailDialog = new JDialog(this, movieTitle + " Details", true);
        detailDialog.setContentPane(dialogContent);
        detailDialog.setSize(650, 600); // Adjusted size since the panels are stacked differently now
        detailDialog.setLocationRelativeTo(this);
        
        // Add action listeners to custom buttons
        addReviewBtn.addActionListener(e -> {
            detailDialog.dispose();
            addReviewDialog(movieId);
        });
        
        addToWatchlistBtn.addActionListener(e -> {
            detailDialog.dispose();
            addToWatchlist(movieId);
        });
        
        closeBtn.addActionListener(e -> detailDialog.dispose());

        detailDialog.setVisible(true);
    }

    private void addMovieDialog() {
        // 1. Setup Components
        JTextField titleField = new JTextField(30);
        JTextField yearField = new JTextField(10);
        JTextField starsField = new JTextField(30);
        JTextArea descArea = new JTextArea(6, 30);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); // Clean border

        // 2. Setup Dialog and Main Panel
        JDialog addMovieDialog = new JDialog(this, "Add New Movie", true);
        JPanel dialogContent = new JPanel(new BorderLayout(15, 15));
        dialogContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 3. Form Panel using GridBagLayout
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5); // Increased padding
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        
        // Title
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(createStyledLabel("Title:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(titleField, gbc);
        row++;

        // Year
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(createStyledLabel("Year:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(yearField, gbc);
        row++;

        // Starring
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(createStyledLabel("Starring (comma separated):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(starsField, gbc);
        row++;

        // Description 
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weighty = 0;
        formPanel.add(createStyledLabel("Description:"), gbc);
        row++;
        
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH; // Allow the scroll pane to grow
        formPanel.add(descScroll, gbc);
        
        // 4. Action Buttons
        JButton saveBtn = new JButton("Save Movie");
        JButton cancelBtn = new JButton("Cancel");
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        saveBtn.putClientProperty("JComponent.sizeVariant", "large");
        cancelBtn.putClientProperty("JComponent.sizeVariant", "large");
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        // 5. Combine and Display
        dialogContent.add(formPanel, BorderLayout.CENTER);
        dialogContent.add(btnPanel, BorderLayout.SOUTH);
        
        addMovieDialog.setContentPane(dialogContent);
        addMovieDialog.pack();
        addMovieDialog.setMinimumSize(new Dimension(500, 400)); 
        addMovieDialog.setLocationRelativeTo(this);

        // 6. Action Listeners
        saveBtn.addActionListener(e -> {
            try {
                int movieYear = Integer.parseInt(yearField.getText());
                if (movieYear < 1888 || movieYear > LocalDateTime.now().getYear() + 1) {
                    throw new IllegalArgumentException("Invalid year. Must be between 1888 and next year.");
                }
                
                // Database insert
                PreparedStatement ps = conn.prepareStatement("INSERT INTO movies(title,year,starring,description) VALUES(?,?,?,?)");
                ps.setString(1, titleField.getText().trim());
                ps.setInt(2, movieYear);
                ps.setString(3, starsField.getText().trim());
                ps.setString(4, descArea.getText().trim());
                ps.executeUpdate();
                
                JOptionPane.showMessageDialog(addMovieDialog, "Movie added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshMovies();
                addMovieDialog.dispose(); // Close dialog on success
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(addMovieDialog, "Please enter a valid number for the year.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(addMovieDialog, ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(addMovieDialog, "Database Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> addMovieDialog.dispose());

        addMovieDialog.setVisible(true);
    }

    private void addReviewDialog(int movieId) {
        // 1. Setup Dialog and Components
        JDialog reviewDialog = new JDialog(this, "Add Review", true);
        JPanel dialogContent = new JPanel(new BorderLayout(15, 15));
        dialogContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // JSpinner for controlled rating input (1 to 10)
        SpinnerModel ratingModel = new SpinnerNumberModel(10, 1, 10, 1);
        JSpinner ratingSpinner = new JSpinner(ratingModel);
        // Set a preferred size for the spinner field for a uniform look
        ((JSpinner.DefaultEditor) ratingSpinner.getEditor()).getTextField().setColumns(2); 
        ratingSpinner.putClientProperty("JComponent.sizeVariant", "large");

        JTextArea commentArea = new JTextArea(6, 30);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        JScrollPane commentScroll = new JScrollPane(commentArea);
        commentScroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        // 2. Form Panel using GridBagLayout
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        
        // Rating field with the JSpinner
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(createStyledLabel("Rating (1-10):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(ratingSpinner, gbc);
        row++;

        // Comment Label
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(createStyledLabel("Comment:"), gbc);
        row++;

        // Comment Text Area
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(commentScroll, gbc);

        // 3. Action Buttons
        JButton submitBtn = new JButton("Submit Review");
        JButton cancelBtn = new JButton("Cancel");
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        submitBtn.putClientProperty("JComponent.sizeVariant", "large");
        cancelBtn.putClientProperty("JComponent.sizeVariant", "large");
        submitBtn.setBackground(new Color(66, 170, 70));
        submitBtn.setForeground(Color.WHITE);
        btnPanel.add(submitBtn);
        btnPanel.add(cancelBtn);

        // 4. Combine and Display
        dialogContent.add(formPanel, BorderLayout.CENTER);
        dialogContent.add(btnPanel, BorderLayout.SOUTH);
        
        reviewDialog.setContentPane(dialogContent);
        reviewDialog.pack();
        reviewDialog.setMinimumSize(new Dimension(500, 350));
        reviewDialog.setLocationRelativeTo(this);

        // 5. Action Listeners
        submitBtn.addActionListener(e -> {
            try {
                // Get value from JSpinner
                int rating = (int) ratingSpinner.getValue();
                String comment = commentArea.getText().trim();
                
                // Validation (The JSpinner already enforces 1-10, but keep the database logic clean)
                if (rating < 1 || rating > 10) {
                     // This should ideally not happen due to SpinnerModel
                     throw new IllegalArgumentException("Rating must be between 1 and 10.");
                }
                
                PreparedStatement ps = conn.prepareStatement("INSERT INTO reviews(movie_id,user_id,rating,comment,created_at) VALUES(?,?,?,?,?)");
                ps.setInt(1, movieId);
                ps.setInt(2, currentUserId);
                ps.setInt(3, rating);
                ps.setString(4, comment);
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
                
                JOptionPane.showMessageDialog(reviewDialog, "Review submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                reviewDialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(reviewDialog, "Error: " + ex.getMessage(), "Review Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancelBtn.addActionListener(e -> reviewDialog.dispose());

        reviewDialog.setVisible(true);
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
