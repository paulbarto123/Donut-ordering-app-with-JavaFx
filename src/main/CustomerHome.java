package main;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import util.DatabaseHelper;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CustomerHome extends Application {

	private final Map<String, Integer> cart = new HashMap<>(); // Cart to store donutID (String) and quantity (Integer)
	private String username = ""; // Username fetched from the database during login
    private String userId; // Variable to store the logged-in user ID

    // Constructor to accept the userId
    public CustomerHome(String userId) {
        this.userId = userId;
    }

    @Override
    public void start(Stage primaryStage) {
        username = getLoggedInUsername(userId); // Use the userId passed to the constructor
        BorderPane root = createHomeLayout(primaryStage);

        primaryStage.setScene(new Scene(root, 800, 500));
        primaryStage.setTitle("Dv.CO | Home (User)");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private BorderPane createHomeLayout(Stage primaryStage) {
        // Top Menu Bar
        MenuBar menuBar = new MenuBar();
        Menu menu = new Menu("Dashboard");
        Menu logout = new Menu("Logout");
        MenuItem homeItem = new MenuItem("Home");
        MenuItem cartItem = new MenuItem("Cart");
        MenuItem logoutItem = new MenuItem("Logout");
        menu.getItems().addAll(homeItem, cartItem);
        logout.getItems().addAll(logoutItem);
        menuBar.getMenus().addAll(menu, logout);
        
        homeItem.setDisable(true);

        // Event Handlers for MenuItems
        cartItem.setOnAction(event -> showCartScene(primaryStage, userId));
        logoutItem.setOnAction(event -> logout(primaryStage)); // Placeholder for logout functionality

        // Left Section: List of Donuts
        ListView<String> donutListView = new ListView<>();
        populateDonutList(donutListView);
        donutListView.setPrefWidth(200);

        // Right Section: Details
        VBox detailsBox = new VBox(10);
        detailsBox.setPadding(new Insets(10));

        Label donutNameLabel = new Label();
        donutNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        Label donutDescriptionLabel = new Label();
        donutDescriptionLabel.setWrapText(true);
        Label donutPriceLabel = new Label();

        Spinner<Integer> quantitySpinner = new Spinner<>(1, 999, 1);
        Button addToCartButton = new Button("Add to cart");
        addToCartButton.setVisible(false);

        detailsBox.getChildren().addAll(donutNameLabel, donutDescriptionLabel, donutPriceLabel, quantitySpinner, addToCartButton);
        detailsBox.setVisible(false);

        // Center Layout
        HBox centerBox = new HBox(10, donutListView, detailsBox);
        centerBox.setPadding(new Insets(10));

        // Welcome Section
        Label welcomeLabel = new Label("Hello, " + username);
        welcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        VBox topBox = new VBox(10, menuBar, welcomeLabel);
        topBox.setPadding(new Insets(10));

        // Main Layout
        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(centerBox);

        // Event Handlers
        donutListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Donut donut = getDonutDetails(newValue);
                if (donut != null) {
                    donutNameLabel.setText(donut.getDonutName());
                    donutDescriptionLabel.setText(donut.getDonutDescription());
                    donutPriceLabel.setText("Price: Rp. " + donut.getDonutPrice());
                    detailsBox.setVisible(true);
                    addToCartButton.setVisible(true);
                }
            } else {
                detailsBox.setVisible(false);
                addToCartButton.setVisible(false);
            }
        });
        
        // Inside the addToCartButton event handler
        addToCartButton.setOnAction(event -> {
            String selectedDonut = donutListView.getSelectionModel().getSelectedItem();
            int quantity = quantitySpinner.getValue();

            if (selectedDonut != null) {
                Donut donut = getDonutDetails(selectedDonut);
                if (donut != null) {
                    saveToCart(userId, donut.getDonutID(), quantity); // Save to cart in the database

                    // Change to use DonutID as the key (String)
                    cart.merge(donut.getDonutID(), quantity, Integer::sum); // Merge by summing the quantities
                    showAlert(Alert.AlertType.INFORMATION, "Added to Cart", "You have added " + quantity + " " + selectedDonut + "(s) to your cart.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Selection Error", "Please select a donut to add to the cart.");
            }
        });

        return root;
    }

    private void logout(Stage primaryStage) {
        // Clear the logged-in user data
        this.userId = null;
        this.username = null;

        // Clear the current scene by setting a new empty scene
        primaryStage.setScene(new Scene(new StackPane(), 800, 600)); // Empty scene to clear the UI

        // Redirect to Login scene
        Login loginScene = new Login(); // Assuming you have a Login class
        try {
            loginScene.start(primaryStage);
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load login screen.");
        }
    }
    
    private void populateDonutList(ListView<String> listView) {
        try (Connection connection = DatabaseHelper.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DonutName FROM mddonut")) {

            while (resultSet.next()) {
                listView.getItems().add(resultSet.getString("DonutName"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Donut getDonutDetails(String name) {
        String query = "SELECT DonutID, DonutName, DonutDescription, DonutPrice FROM mddonut WHERE DonutName = ?";

        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, name);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new Donut(
                            resultSet.getString("DonutID"),
                            resultSet.getString("DonutName"),
                            resultSet.getString("DonutDescription"),
                            resultSet.getDouble("DonutPrice")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void saveToCart(String userId, String donutId, int quantity) {
        String query = "INSERT INTO cart (UserID, DonutID, Quantity) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE Quantity = Quantity + ?";

        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, donutId);
            preparedStatement.setInt(3, quantity);
            preparedStatement.setInt(4, quantity);
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getLoggedInUsername(String userId) {
        String query = "SELECT Username FROM msuser WHERE UserID = ?";

        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("Username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "Guest"; // Default username if not found
    }

    private void showCartScene(Stage stage, String userId) {
    	CartScene userHome = new CartScene(userId); // Pass the userId to UserHome
         try {
             userHome.start(stage);
         } catch (Exception e) {
             e.printStackTrace();
         }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class Donut {
        private final String donutID;
        private final String donutName;
        private final String donutDescription;
        private final double donutPrice;

        public Donut(String donutID, String donutName, String donutDescription, double donutPrice) {
            this.donutID = donutID;
            this.donutName = donutName;
            this.donutDescription = donutDescription;
            this.donutPrice = donutPrice;
        }

        public String getDonutID() {
            return donutID;
        }

        public String getDonutName() {
            return donutName;
        }

        public String getDonutDescription() {
            return donutDescription;
        }

        public double getDonutPrice() {
            return donutPrice;
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}