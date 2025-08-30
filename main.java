package Project;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.awt.event.*;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.List;


class Movie {
    private String title;
    private String genre;
    private double rating;
    private List<String> actors;

    public Movie(String title, String genre, double rating, List<String> actors) {
        this.title = title;
        this.genre = genre;
        this.rating = rating;
        this.actors = actors;
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public double getRating() {
        return rating;
    }

    public List<String> getActors() {
        return actors;
    }

    @Override
    public String toString() {
        return String.format("%-30s | %-10s | %-4.1f | %s", title, genre, rating, String.join(", ", actors));
    }
}

// Movie Database reading from file
class MovieDatabase {
    private List<Movie> movies; // Declare this in the MovieDatabase class

    public MovieDatabase(String filePath) {
        movies = new ArrayList<>();
        loadMoviesFromFile(filePath);
    }

    private void loadMoviesFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Movies file not found: " + filePath);
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String title = "", genre = "";
            double rating = 0.0;
            List<String> actors = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Title:")) {
                    title = line.substring(7).trim();
                } else if (line.startsWith("Rating:")) {
                    rating = Double.parseDouble(line.substring(8).trim());
                } else if (line.startsWith("Genre:")) {
                    genre = line.substring(7).trim();
                } else if (line.startsWith("Main Actors:")) {
                    actors = Arrays.asList(line.substring(12).trim().split(", "));
                    if (title.isEmpty() || genre.isEmpty() || actors.isEmpty()) {
                        continue; // Skip invalid entries
                    }
                    movies.add(new Movie(title, genre, rating, actors));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading movie database: " + e.getMessage());
        }
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(movies);
    }
}

// User Authentication System
class UserAuth {
    private static final String USERS_FILE = "users.json";
    private static final String WATCHED_MOVIES_FILE = "watched_movies.json";
    private static final Map<String, String> users = new HashMap<>();

    static {
        loadUsers();
    }

    private static void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonContent.append(line);
            }
            JSONObject jsonObject = new JSONObject(jsonContent.toString());
            for (String username : jsonObject.keySet()) {
                users.put(username, jsonObject.getString(username));
            }
        } catch (IOException e) {
            System.out.println("Error loading users: " + e.getMessage());
        }
    }

    private static void saveUsers() {
        JSONObject jsonObject = new JSONObject(users);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE))) {
            bw.write(jsonObject.toString(4));
        } catch (IOException e) {
            System.out.println("Error saving users: " + e.getMessage());
        }
    }

    public static boolean authenticate(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }

    public static void registerUser(String username, String password) {
        if (users.containsKey(username)) {
            JOptionPane.showMessageDialog(null, "Username already exists. Please try another.");
        } else {
            users.put(username, password);
            saveUsers();
            JOptionPane.showMessageDialog(null, "User registered successfully! You can now log in.");
        }
    }

    public static void recordWatchedMovie(String username, String movieTitle) {
        try {
            JSONObject watchedMovies = loadWatchedMovies();
            JSONArray userMovies = watchedMovies.optJSONArray(username);
            if (userMovies == null) {
                userMovies = new JSONArray();
            }
            if (!userMovies.toList().contains(movieTitle)) { // Avoid duplicates
                userMovies.put(movieTitle);
            }
            watchedMovies.put(username, userMovies);
            saveWatchedMovies(watchedMovies);
        } catch (IOException e) {
            System.out.println("Error recording watched movie: " + e.getMessage());
        }
    }

    public static List<String> getWatchedMovies(String username) {
        List<String> watchedMovies = new ArrayList<>();
        try {
            JSONObject watchedMoviesJson = loadWatchedMovies();
            JSONArray userMovies = watchedMoviesJson.optJSONArray(username);
            if (userMovies != null) {
                for (int i = 0; i < userMovies.length(); i++) {
                    watchedMovies.add(userMovies.getString(i));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading watched movies: " + e.getMessage());
        }
        return watchedMovies;
    }

    private static JSONObject loadWatchedMovies() throws IOException {
        File file = new File(WATCHED_MOVIES_FILE);
        if (!file.exists()) {
            return new JSONObject();
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonContent.append(line);
            }
            return new JSONObject(jsonContent.toString());
        }
    }

    private static void saveWatchedMovies(JSONObject watchedMovies) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(WATCHED_MOVIES_FILE))) {
            bw.write(watchedMovies.toString(4));
        }
    }
}

// Button Renderer for JTable
class ButtonRenderer extends JButton implements TableCellRenderer {
    public ButtonRenderer() {
        setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText((value == null) ? "" : value.toString());
        return this;
    }
}


class ButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String label;
    private boolean clicked;
    private List<Movie> movies;
    private int row;
    private String username; // Add username field

    public ButtonEditor(JCheckBox checkBox, List<Movie> movies, String username) {
        super(checkBox);
        this.movies = movies;
        this.username = username; // Initialize username
        button = new JButton();
        button.setOpaque(true);

        button.addActionListener(e -> {
            clicked = true;
            fireEditingStopped();
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.row = row;
        label = (value == null) ? "" : value.toString();
        button.setText(label);
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        if (clicked) {
            Movie movie = movies.get(row);
            UserAuth.recordWatchedMovie(username, movie.getTitle()); // Use the logged-in username
            JOptionPane.showMessageDialog(button, "Marked \"" + movie.getTitle() + "\" as watched!");
        }
        clicked = false;
        return label;
    }

    @Override
    public boolean stopCellEditing() {
        clicked = false;
        return super.stopCellEditing();
    }
}

// Main Class
public class Main {
    private static JFrame frame;
    private static MovieDatabase database = new MovieDatabase("movies.txt");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Movie Recommendation System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);
            showLoginScreen();
            frame.setVisible(true);
        });
    }

    private static void showLoginScreen() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField();
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        panel.add(userLabel);
        panel.add(userField);
        panel.add(passLabel);
        panel.add(passField);
        panel.add(loginButton);
        panel.add(registerButton);

        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();

        loginButton.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());
            if (UserAuth.authenticate(username, password)) {
                JOptionPane.showMessageDialog(frame, "Login successful!");
                showMainMenu(username);
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid credentials! Try again.");
            }
        });

        registerButton.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());
            UserAuth.registerUser(username, password);
        });
    }

    private static void showMainMenu(String username) {
        JPanel panel = new JPanel(new GridLayout(8, 1, 10, 10));
        JLabel welcomeLabel = new JLabel("Welcome, " + username + "!", SwingConstants.CENTER);
        JButton genreButton = new JButton("Search by Genre");
        JButton actorButton = new JButton("Search by Actor");
        JButton ratingButton = new JButton("Search by Rating");
        JButton topRatedButton = new JButton("Show Top-Rated Movies");
        JButton recordWatchedButton = new JButton("Record Watched Movie");
        JButton viewWatchedButton = new JButton("View Watched Movies");
        JButton logoutButton = new JButton("Logout");

        panel.add(welcomeLabel);
        panel.add(genreButton);
        panel.add(actorButton);
        panel.add(ratingButton);
        panel.add(topRatedButton);
        panel.add(recordWatchedButton);
        panel.add(viewWatchedButton);
        panel.add(logoutButton);

        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();

        genreButton.addActionListener(e -> searchByGenre(username)); // Pass username
        actorButton.addActionListener(e -> searchByActor());
        ratingButton.addActionListener(e -> searchByRating());
        topRatedButton.addActionListener(e -> displayTopRatedMovies());
        recordWatchedButton.addActionListener(e -> recordWatchedMovie(username));
        viewWatchedButton.addActionListener(e -> viewWatchedMovies(username));
        logoutButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                System.exit(0); // Close the entire program
            }
        });
    }

    private static void searchByGenre(String username) { // Accept username as a parameter
        String genre = JOptionPane.showInputDialog(frame, "Enter preferred genre:");
        if (genre == null || genre.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Genre cannot be empty!");
            return;
        }

        // Get the list of watched movies for the user
        List<String> watchedMovies = UserAuth.getWatchedMovies(username);

        // Filter movies by genre and exclude watched movies
        List<Movie> movies = database.getMovies().stream()
                .filter(movie -> movie.getGenre().equalsIgnoreCase(genre) && !watchedMovies.contains(movie.getTitle()))
                .collect(Collectors.toList());

        if (movies.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No movies found matching your criteria.");
            return;
        }

        // Create a table to display movies with a "Watched" button
        String[] columnNames = {"Title", "Genre", "Rating", "Actors", "Action"};
        Object[][] data = new Object[movies.size()][5];

        for (int i = 0; i < movies.size(); i++) {
            Movie movie = movies.get(i);
            data[i][0] = movie.getTitle();
            data[i][1] = movie.getGenre();
            data[i][2] = movie.getRating();
            data[i][3] = String.join(", ", movie.getActors());
            data[i][4] = "Watched"; // Button label
        }

        JTable table = new JTable(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Only the "Watched" column is editable
            }
        };

        table.getColumn("Action").setCellRenderer(new ButtonRenderer());
        table.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox(), movies, username)); // Pass username

        JScrollPane scrollPane = new JScrollPane(table);
        JOptionPane.showMessageDialog(frame, scrollPane, "Movies by Genre", JOptionPane.PLAIN_MESSAGE);
    }

    private static void searchByActor() {
        String actor = JOptionPane.showInputDialog(frame, "Enter actor name:");
        if (actor != null) {
            List<Movie> movies = database.getMovies().stream()
                    .filter(movie -> movie.getActors().contains(actor))
                    .collect(Collectors.toList());
            displayMovies(movies);
        }
    }

    private static void searchByRating() {
        String ratingStr = JOptionPane.showInputDialog(frame, "Enter minimum rating:");
        if (ratingStr != null) {
            try {
                double rating = Double.parseDouble(ratingStr);
                List<Movie> movies = database.getMovies().stream()
                        .filter(movie -> movie.getRating() >= rating)
                        .collect(Collectors.toList());
                displayMovies(movies);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Invalid rating input!");
            }
        }
    }

    private static void displayTopRatedMovies() {
        List<Movie> movies = database.getMovies().stream()
                .sorted((m1, m2) -> Double.compare(m2.getRating(), m1.getRating()))
                .limit(5)
                .collect(Collectors.toList());
        displayMovies(movies);
    }

    private static void recordWatchedMovie(String username) {
        String movieTitle = JOptionPane.showInputDialog(frame, "Enter the title of the movie you watched:");
        if (movieTitle == null || movieTitle.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Movie title cannot be empty!");
            return;
        }
        UserAuth.recordWatchedMovie(username, movieTitle);
        JOptionPane.showMessageDialog(frame, "Movie recorded successfully!");
    }

    private static void viewWatchedMovies(String username) {
        List<String> watchedMovies = UserAuth.getWatchedMovies(username);
        if (watchedMovies.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "You haven't recorded any watched movies yet.");
            return;
        }

        StringBuilder movieList = new StringBuilder("Movies you have watched:\n");
        for (String movie : watchedMovies) {
            movieList.append("- ").append(movie).append("\n");
        }
        JOptionPane.showMessageDialog(frame, movieList.toString());
    }

    private static void displayMovies(List<Movie> movies) {
        if (movies.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No movies found matching your criteria.");
            return;
        }

        StringBuilder movieList = new StringBuilder("<html><body><table border='1'><tr><th>Title</th><th>Genre</th><th>Rating</th><th>Actors</th></tr>");
        for (Movie movie : new ArrayList<>(movies)) {
            movieList.append("<tr><td>")
                    .append(movie.getTitle())
                    .append("</td><td>")
                    .append(movie.getGenre())
                    .append("</td><td>")
                    .append(movie.getRating())
                    .append("</td><td>")
                    .append(String.join(", ", movie.getActors()))
                    .append("</td></tr>");
        }
        movieList.append("</table></body></html>");

        JOptionPane.showMessageDialog(frame, new JLabel(movieList.toString()), "Movies", JOptionPane.INFORMATION_MESSAGE);
    }
}
