package main;

import java.sql.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import util.DatabaseHelper;

public class CartScene extends Application {

    Scene scene;
    BorderPane borderPaneForm;

    MenuBar menuBar;
    Menu dashboardMenu, logOutMenu;
    MenuItem homeBar, cartBar;
    MenuItem logoutBar;

    Label yourCartTitle, totalPriceLbl;
    TableView<Cart> tableView;
    Button checkOutBtn;

    private ObservableList<Cart> cartItems;
    private String userId; // Variable to store the logged-in user ID
    private String username;
    
    // Constructor to accept the userId
    public CartScene(String userId) {
        this.userId = userId;
    }

    void initialize() {
        borderPaneForm = new BorderPane();

        menuBar = new MenuBar();
        dashboardMenu = new Menu("Dashboard");
        logOutMenu = new Menu("Log Out");

        homeBar = new MenuItem("Home");
        cartBar = new MenuItem("Cart");
        logoutBar = new MenuItem("Logout");
        cartBar.setDisable(true);

        cartItems = FXCollections.observableArrayList();
        loadCartData(); // Load data from the database

        yourCartTitle = new Label("Your Cart");
        yourCartTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        tableView = new TableView<>();
        tableView.setPlaceholder(new Label("No content in table"));
        setTableView();

        totalPriceLbl = new Label("Rp. " + calculateTotalPrice());
        totalPriceLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        checkOutBtn = new Button("Checkout");

        dashboardMenu.getItems().addAll(homeBar, cartBar);
        logOutMenu.getItems().addAll(logoutBar);
        menuBar.getMenus().addAll(dashboardMenu, logOutMenu);
    }

    void setComponent(Stage stage) {
        VBox vbox = new VBox(10, yourCartTitle, tableView, totalPriceLbl, checkOutBtn);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);

        if (cartItems.isEmpty()) {
            totalPriceLbl.setText("Rp. 0");
        } else {
            totalPriceLbl.setText("Rp. " + calculateTotalPrice());
        }
        borderPaneForm.setTop(menuBar);
        borderPaneForm.setCenter(vbox);

        homeBar.setOnAction(e -> redirectToUserHome(stage, userId ));
        logoutBar.setOnAction(e -> logout(stage));
        checkOutBtn.setOnAction(e -> handleCheckout());
        
        
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
    
    private void redirectToUserHome(Stage stage, String userId) {
        CustomerHome userHome = new CustomerHome(userId); // Pass the userId to UserHome
        try {
            userHome.start(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setTableView() {
        TableColumn<Cart, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(200);

        TableColumn<Cart, Integer> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setPrefWidth(100);

        TableColumn<Cart, Integer> quantityColumn = new TableColumn<>("Quantity");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityColumn.setPrefWidth(100);

        TableColumn<Cart, Integer> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalColumn.setPrefWidth(120);

        tableView.setItems(cartItems); // Bind the cartItems list
        tableView.getColumns().addAll(nameColumn, priceColumn, quantityColumn, totalColumn);
    }

    private int calculateTotalPrice() {
        return cartItems.stream().mapToInt(Cart::getTotal).sum();
    }

    private void handleCheckout() {
        Cart selectedItem = tableView.getSelectionModel().getSelectedItem();

        // Validation: Check if an item is selected
        if (selectedItem == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No item selected for checkout.");
            return;
        }

        // Generate Transaction ID
        String transactionId = generateTransactionId();

        try (Connection connection = DatabaseHelper.getConnection()) {
            connection.setAutoCommit(false);

            // Insert into Transaction Header
            String insertHeaderQuery = "INSERT INTO transactionheader (TransactionID, UserID) VALUES (?, ?)";
            try (PreparedStatement headerStmt = connection.prepareStatement(insertHeaderQuery)) {
                headerStmt.setString(1, transactionId);
                headerStmt.setString(2, userId);
                headerStmt.executeUpdate();
            }

            // Get DonutID from selected item
            String donutIdQuery = "SELECT DonutID FROM mddonut WHERE DonutName = ?";
            String donutId;
            try (PreparedStatement donutStmt = connection.prepareStatement(donutIdQuery)) {
                donutStmt.setString(1, selectedItem.getName());
                try (ResultSet rs = donutStmt.executeQuery()) {
                    if (rs.next()) {
                        donutId = rs.getString("DonutID");
                    } else {
                        throw new SQLException("DonutID not found for selected item.");
                    }
                }
            }

            // Insert into Transaction Detail
            String insertDetailQuery = "INSERT INTO transactiondetail (TransactionID, DonutID, Quantity) VALUES (?, ?, ?)";
            try (PreparedStatement detailStmt = connection.prepareStatement(insertDetailQuery)) {
                detailStmt.setString(1, transactionId);
                detailStmt.setString(2, donutId);
                detailStmt.setInt(3, selectedItem.getQuantity());
                detailStmt.executeUpdate();
            }

            // Remove item from Cart table
            String deleteCartQuery = "DELETE FROM cart WHERE UserID = ? AND DonutID = ?";
            try (PreparedStatement deleteCartStmt = connection.prepareStatement(deleteCartQuery)) {
                deleteCartStmt.setString(1, userId);
                deleteCartStmt.setString(2, donutId);
                deleteCartStmt.executeUpdate();
            }

            connection.commit();
            // Remove the item from the TableView and update the total price
            cartItems.remove(selectedItem);
            tableView.refresh();
            totalPriceLbl.setText("Rp. " + calculateTotalPrice());

            // Show success alert
            showAlert(Alert.AlertType.INFORMATION, "Checkout Success", "Transaction ID: " + transactionId);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Checkout Error", "Failed to complete checkout. Please try again.");
        }
    }

    private String generateTransactionId() {
        String query = "SELECT COUNT(*) AS TransactionCount FROM transactionheader";
        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            int transactionIndex = 1; // Default to 1 if no transactions exist
            if (rs.next()) {
                transactionIndex = rs.getInt("TransactionCount") + 1;
            }
            return String.format("TR%03d", transactionIndex);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate transaction ID.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void loadCartData() {
        String query = """
            SELECT mddonut.DonutName, mddonut.DonutPrice, cart.Quantity 
            FROM cart 
            JOIN mddonut ON cart.DonutID = mddonut.DonutID 
            WHERE cart.UserID = ?
        """;

        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                cartItems.clear();
                while (resultSet.next()) {
                    String name = resultSet.getString("DonutName");
                    int price = resultSet.getInt("DonutPrice");
                    int quantity = resultSet.getInt("Quantity");
                    cartItems.add(new Cart(name, price, quantity));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load cart data.");
        }
    }

    public class Cart {
        private String name;
        private Integer price;
        private Integer quantity;
        private Integer total;

        public Cart(String name, Integer price, Integer quantity) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.total = price * quantity;
        }

        public String getName() {
            return name;
        }

        public Integer getPrice() {
            return price;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public Integer getTotal() {
            return total;
        }
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        initialize();
        setComponent(primaryStage);

        scene = new Scene(borderPaneForm, 800, 400);
        primaryStage.setTitle("Dv.CO | Cart");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

}