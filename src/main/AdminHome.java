package main;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import util.DatabaseHelper;

public class AdminHome extends Application {

    private TableView<Donut> tableView;
    private TextField nameField, priceField;
    private TextArea descriptionArea;
    private String username; // Fetched from the database
    private ObservableList<Donut> donutData;
    private String userId; // Variable to store the logged-in user ID

    // Database connection setup
    private Connection connection;

    // Constructor to accept the userId
    public AdminHome(String userId) {
        this.userId = userId;
        try {
            connection = DatabaseHelper.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to connect to the database.");
        }
    }

    @Override
    public void start(Stage primaryStage) {
        fetchUsername();
        donutData = getDonutData();

        // Menu bar setup
        MenuBar menuBar = new MenuBar();
        Menu menu = new Menu("Logout");
        MenuItem logoutMenuItem = new MenuItem("Logout");
        menu.getItems().add(logoutMenuItem);
        menuBar.getMenus().add(menu);

     // Logout event handler
        logoutMenuItem.setOnAction(e -> {
            logout(primaryStage);
        });
        
        // Labels
        Label welcomeLabel = new Label("Hello, " + username);
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-font-weight : bold;");
        Label activeDonutLabel = new Label("Active Donut:");
        Label nameLabel = new Label("Donut Name:");
        Label descriptionLabel = new Label("Donut Description:");
        Label priceLabel = new Label("Donut Price:");

        // TableView
        tableView = new TableView<>();
        TableColumn<Donut, String> idColumn = new TableColumn<>("Donut ID");
        idColumn.setCellValueFactory(data -> data.getValue().idProperty());

        TableColumn<Donut, String> nameColumn = new TableColumn<>("Donut Name");
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<Donut, String> descriptionColumn = new TableColumn<>("Donut Description");
        descriptionColumn.setCellValueFactory(data -> data.getValue().descriptionProperty());

        TableColumn<Donut, String> priceColumn = new TableColumn<>("Donut Price");
        priceColumn.setCellValueFactory(data -> data.getValue().priceProperty());

        tableView.getColumns().addAll(idColumn, nameColumn, descriptionColumn, priceColumn);
        tableView.setItems(donutData);

        // TextFields and TextArea
        nameField = new TextField();
        priceField = new TextField();
        descriptionArea = new TextArea();
        descriptionArea.setPrefHeight(60);

        // Event: Populate fields when a row is selected
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                nameField.setText(newSelection.getName());
                descriptionArea.setText(newSelection.getDescription());
                priceField.setText(newSelection.getPrice());
            }
        });

        // Buttons
        Button addButton = new Button("Add Donut");
        addButton.setOnAction(e -> handleAddDonut());

        Button updateButton = new Button("Update Donut");
        updateButton.setOnAction(e -> handleUpdateDonut());

        Button deleteButton = new Button("Delete Donut");
        deleteButton.setOnAction(e -> handleDeleteDonut());

        // Layout
        GridPane formLayout = new GridPane();
        formLayout.setPadding(new Insets(10));
        formLayout.setHgap(10);
        formLayout.setVgap(10);

        formLayout.add(nameLabel, 0, 0);
        formLayout.add(nameField, 1, 0);
        formLayout.add(descriptionLabel, 0, 1);
        formLayout.add(descriptionArea, 1, 1);
        formLayout.add(priceLabel, 0, 2);
        formLayout.add(priceField, 1, 2);

        HBox buttonLayout = new HBox(10, addButton, updateButton, deleteButton);
        formLayout.add(buttonLayout, 1, 3);

        VBox layout = new VBox(10, menuBar, welcomeLabel, activeDonutLabel, tableView, formLayout);
        layout.setPadding(new Insets(10));

        // Scene
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Home Scene (Admin)");
        primaryStage.show();
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
    
    private void fetchUsername() {
        String query = "SELECT username FROM msuser WHERE UserID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                username = rs.getString("username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to fetch username.");
        }
    }

    private ObservableList<Donut> getDonutData() {
        ObservableList<Donut> data = FXCollections.observableArrayList();
        String query = "SELECT * FROM mddonut";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                data.add(new Donut(
                        rs.getString("DonutID"),
                        rs.getString("DonutName"),
                        rs.getString("DonutDescription"),
                        rs.getString("DonutPrice")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to fetch donuts data.");
        }
        return data;
    }

    private void handleAddDonut() {
        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String price = priceField.getText().trim();

        if (name.isEmpty() || description.isEmpty() || price.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "All fields must be filled.");
            return;
        }

        String newId = generateDonutId();
        String query = "INSERT INTO mddonut (DonutID, DonutName, DonutDescription, DonutPrice) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newId);
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setString(4, price);
            stmt.executeUpdate();
            donutData.add(new Donut(newId, name, description, price));
            showAlert(Alert.AlertType.INFORMATION, "Success", "Donut added successfully!");
            nameField.clear();
            descriptionArea.clear();
            priceField.clear();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add donut.");
        }
    }

    private void handleUpdateDonut() {
        Donut selectedDonut = tableView.getSelectionModel().getSelectedItem();
        if (selectedDonut == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "No donut selected for update.");
            return;
        }

        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String price = priceField.getText().trim();

        if (name.isEmpty() || description.isEmpty() || price.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "All fields must be filled.");
            return;
        }

        String query = "UPDATE mddonut SET DonutName = ?, DonutDescription = ?, DonutPrice = ? WHERE DonutID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setString(3, price);
            stmt.setString(4, selectedDonut.getId());
            stmt.executeUpdate();

            selectedDonut.setName(name);
            selectedDonut.setDescription(description);
            selectedDonut.setPrice(price);

            tableView.refresh();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Donut updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update donut.");
        }
    }

    private void handleDeleteDonut() {
        Donut selectedDonut = tableView.getSelectionModel().getSelectedItem();
        if (selectedDonut == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "No donut selected for deletion.");
            return;
        }

        String query = "DELETE FROM mddonut WHERE DonutID = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, selectedDonut.getId());
            stmt.executeUpdate();
            donutData.remove(selectedDonut);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Donut deleted successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to delete donut.");
        }
    }

    private String generateDonutId() {
        // Query to get the maximum DonutID from the database
        String query = "SELECT MAX(CAST(SUBSTRING(DonutID, 3) AS UNSIGNED)) AS maxId FROM mddonut";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                int nextId = rs.getInt("maxId") + 1; // Get the next ID number
                return String.format("DN%03d", nextId); // Format it to be like DN001, DN002, etc.
            } else {
                // If no rows in the table, start with DN001
                return "DN001";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to generate Donut ID.");
            return "DN001"; // Fallback ID in case of an error
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //Donut class (model)
    public static class Donut {
        private final SimpleStringProperty id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty description;
        private final SimpleStringProperty price;

        public Donut(String id, String name, String description, String price) {
            this.id = new SimpleStringProperty(id);
            this.name = new SimpleStringProperty(name);
            this.description = new SimpleStringProperty(description);
            this.price = new SimpleStringProperty(price);
        }

        public StringProperty idProperty() { return id; }
        public StringProperty nameProperty() { return name; }
        public StringProperty descriptionProperty() { return description; }
        public StringProperty priceProperty() { return price; }

        public String getId() { return id.get(); }
        public String getName() { return name.get(); }
        public String getDescription() { return description.get(); }
        public String getPrice() { return price.get(); }

        public void setName(String name) { this.name.set(name); }
        public void setDescription(String description) { this.description.set(description); }
        public void setPrice(String price) { this.price.set(price); }
    }

    public static void main(String[] args) {
        launch(args);
    }

}