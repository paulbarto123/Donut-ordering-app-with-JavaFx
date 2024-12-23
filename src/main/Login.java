package main;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import util.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Login extends Application {

    private TextField emailField;
    private PasswordField passField;
    private Button loginButton;
    private Hyperlink registerLink;

    private BorderPane createLoginLayout(Stage stage) {
        Label titleLabel = new Label("LOGIN");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        GridPane formPane = new GridPane();
        formPane.setAlignment(Pos.CENTER);
        formPane.setHgap(10);
        formPane.setVgap(10);
        formPane.add(new Label("Email:"), 0, 0);
        emailField = new TextField();
        formPane.add(emailField, 1, 0);

        formPane.add(new Label("Password:"), 0, 1);
        passField = new PasswordField();
        formPane.add(passField, 1, 1);

        loginButton = new Button("Login");
        formPane.add(loginButton, 1, 2);

        registerLink = new Hyperlink("Don't have an account? Sign up!");

        VBox contentBox = new VBox(15, titleLabel, formPane, registerLink);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(20));

        BorderPane rootPane = new BorderPane();
        rootPane.setCenter(contentBox);
        rootPane.setPadding(new Insets(10));

        return rootPane;
    }

    private MenuBar createMenu(Stage stage) {
        // Create the menu bar and menu items
        MenuBar menuBar = new MenuBar();
        Menu menu = new Menu("Menu");
        
        MenuItem loginItem = new MenuItem("Login");
        loginItem.setDisable(true);
        loginItem.setOnAction(event -> {
            // Logic for login action
            System.out.println("Login clicked");
            stage.setScene(new Scene(createLoginLayout(stage), 800, 500));  // Switch to login scene
        });

        MenuItem registerItem = new MenuItem("Register");
        registerItem.setOnAction(event -> {
            clearFields();
            redirectToRegister(stage);
        });

        menu.getItems().addAll(loginItem, registerItem);
        menuBar.getMenus().add(menu);
        return menuBar;
    }

    private void configureActions(Stage stage) {
        loginButton.setOnAction(event -> {
            String email = emailField.getText().trim();
            String password = passField.getText();

            if (email.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Login Error", "Email and Password must be filled!");
                return;
            }

            try {
                User user = validateUser(email, password);
                if (user == null) {
                    showAlert(Alert.AlertType.ERROR, "Login Error", "Invalid Email or Password!");
                } else {
                    saveUserSession(email);
                    if (user.getRole().equalsIgnoreCase("Admin")) {
                        redirectToAdminHome(stage, user.getUserId());
                    } else if (user.getRole().equalsIgnoreCase("Customer")) {
                        redirectToUserHome(stage, user.getUserId());
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Login Error", "Unrecognized user role.");
                    }
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while connecting to the database.");
                e.printStackTrace();
            }
        });

        registerLink.setOnAction(event -> {
            clearFields();
            redirectToRegister(stage);
        });
    }

    private User validateUser(String email, String password) throws SQLException {
        String query = "SELECT UserID, Role FROM msuser WHERE email = ? AND password = ?";
        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new User(
                            resultSet.getString("UserID"),
                            resultSet.getString("Role")
                    );
                }
            }
        }
        return null;
    }

    private void saveUserSession(String email) {
        // Placeholder for session logic
        System.out.println("User session saved for: " + email);
    }

    private void redirectToAdminHome(Stage stage, String userId) {
        AdminHome adminHome = new AdminHome(userId); // Pass the userId to UserHome
        try {
            adminHome.start(stage);
        } catch (Exception e) {
            e.printStackTrace();
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

    private void redirectToRegister(Stage stage) {
        Registration registrationPage = new Registration(); // Assuming Registration is implemented
        try {
            registrationPage.start(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        emailField.clear();
        passField.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @Override
    public void start(Stage primaryStage) {
        MenuBar menuBar = createMenu(primaryStage);  // Add the menu bar
        BorderPane loginLayout = createLoginLayout(primaryStage);
        configureActions(primaryStage);

        BorderPane rootLayout = new BorderPane();
        rootLayout.setTop(menuBar);
        rootLayout.setCenter(loginLayout);

        primaryStage.setScene(new Scene(rootLayout, 800, 500));
        primaryStage.setTitle("Login Page");
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    /**
     * Inner class to represent the User
     */
    private static class User {
        private final String userId;
        private final String role;

        public User(String userId, String role) {
            this.userId = userId;
            this.role = role;
        }

        public String getUserId() {
            return userId;
        }

        public String getRole() {
            return role;
        }
    }
    


    public static void main(String[] args) {
        launch(args);
    }

}
