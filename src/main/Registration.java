package main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

public class Registration extends Application {
    private BorderPane borderPane;
    private GridPane gridPane;
    private VBox vbox;
    private Label titlelbl;
    private TextField usernameTf, emailTf, phoneTf;
    private PasswordField passPf, confPassPf;
    private Spinner<Integer> age;
    private RadioButton male, female;
    private ComboBox<String> country;
    private CheckBox checkBox;
    private Button regisBtn;
    private MenuBar menuBar;
    private Hyperlink loginLink;
    private ToggleGroup toggleGroup;
    private Scene registerScene;

    private void initialize(Stage stage) {
        borderPane = new BorderPane();
        gridPane = new GridPane();
        vbox = new VBox(10);

        // Labels
        titlelbl = new Label("Register");
        titlelbl.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        // Text fields
        usernameTf = new TextField();
        emailTf = new TextField();
        phoneTf = new TextField();

        // Password fields
        passPf = new PasswordField();
        confPassPf = new PasswordField();

        // Spinner 
        age = new Spinner<>();
        age.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(14, 100, 14));

        // Radio buttons for gender
        male = new RadioButton("Male");
        female = new RadioButton("Female");
        toggleGroup = new ToggleGroup();
        male.setToggleGroup(toggleGroup);
        female.setToggleGroup(toggleGroup);

        // ComboBox for country
        country = new ComboBox<>();
        country.setPromptText("Select a country");
        country.getItems().addAll("Indonesia", "Malaysia", "Singapore", "Thailand");

        // CheckBox for terms
        checkBox = new CheckBox("Agree to the terms and conditions");

        // Register button
        regisBtn = new Button("Register");

        // Menu bar
        menuBar = new MenuBar();
        Menu menu = new Menu("Menu");
        MenuItem loginMenuItem = new MenuItem("Login");
        MenuItem registerMenuItem = new MenuItem("Register");
        menu.getItems().addAll(loginMenuItem, registerMenuItem);
        menuBar.getMenus().add(menu);

        // Hyperlink for login
        loginLink = new Hyperlink("Already have an account? Sign in!");

        registerMenuItem.setDisable(true);
        // Login action
        loginMenuItem.setOnAction(event -> {
            Login loginPage = new Login();
            try {
                loginPage.start(stage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setComponent() {
        borderPane.setTop(menuBar);
        borderPane.setCenter(vbox);

        vbox.getChildren().addAll(titlelbl, gridPane, loginLink);

        gridPane.add(new Label("Username:"), 0, 0);
        gridPane.add(usernameTf, 1, 0);

        gridPane.add(new Label("Email:"), 0, 1);
        gridPane.add(emailTf, 1, 1);

        gridPane.add(new Label("Password:"), 0, 2);
        gridPane.add(passPf, 1, 2);

        gridPane.add(new Label("Confirm Password:"), 0, 3);
        gridPane.add(confPassPf, 1, 3);

        gridPane.add(new Label("Age:"), 0, 4);
        gridPane.add(age, 1, 4);

        gridPane.add(new Label("Gender:"), 0, 5);
        gridPane.add(male, 1, 5);
        gridPane.add(female, 2, 5);

        gridPane.add(new Label("Country:"), 0, 6);
        gridPane.add(country, 1, 6);

        gridPane.add(new Label("Phone Number:"), 0, 7);
        gridPane.add(phoneTf, 1, 7);

        gridPane.add(checkBox, 1, 8);
        gridPane.add(regisBtn, 1, 9);

        loginLink.setOnAction(event -> {
            showLoginPage((Stage) registerScene.getWindow());
        });
    }

    private void arrangeComponent() {
        vbox.setAlignment(Pos.CENTER);
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setVgap(10);
        gridPane.setHgap(10);
        BorderPane.setMargin(menuBar, new Insets(10));
        BorderPane.setAlignment(loginLink, Pos.BOTTOM_CENTER);
        BorderPane.setMargin(titlelbl, new Insets(10));
    }

    private void validate(Stage stage) {
        regisBtn.setOnAction(event -> {
            String username = usernameTf.getText().trim();
            String email = emailTf.getText().trim();
            String password = passPf.getText();
            String confirmPassword = confPassPf.getText();
            Integer userAge = age.getValue();
            String selectedGender = toggleGroup.getSelectedToggle() == null ? "" : ((RadioButton) toggleGroup.getSelectedToggle()).getText();
            String selectedCountry = country.getValue();
            String phoneNumber = phoneTf.getText().trim();
            boolean isAgreed = checkBox.isSelected();

            StringBuilder errors = new StringBuilder();

            if (username.isEmpty() || username.length() < 3 || username.length() > 15) {
                errors.append("- Username must be between 3-15 characters long.\n");
            }

            if (!email.endsWith("@gmail.com")) {
                errors.append("- Email must end with '@gmail.com'.\n");
            }

            if (!password.matches("[a-zA-Z0-9]+")) {
                errors.append("- Password must be alphanumeric.\n");
            }

            if (!password.equals(confirmPassword)) {
                errors.append("- Passwords do not match.\n");
            }

            if (userAge <= 13) {
                errors.append("- Age must be older than 13 years.\n");
            }

            if (phoneNumber.isEmpty() || phoneNumber.length() > 15) {
                errors.append("- Phone Number must be less than 15 characters long.\n");
            }

            if (!isAgreed) {
                errors.append("- You must agree to the terms and conditions.\n");
            }

            if (errors.length() > 0) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Validation Error");
                alert.setHeaderText("Please fix the following errors:");
                alert.setContentText(errors.toString());
                alert.showAndWait();
            } else {
                String userId = generateUserId();
                saveToDatabase(userId, username, email, password, userAge, selectedGender, selectedCountry, phoneNumber);
                showLoginPage(stage);
                
            }
        });
    }

    private String generateUserId() {
        String userId = "US001"; // Default ID if no users exist
        try (Connection connection = DatabaseHelper.getConnection()) {
            String sql = "SELECT MAX(CAST(SUBSTRING(UserID, 3, LENGTH(UserID) - 2) AS UNSIGNED)) AS MaxIndex FROM msuser";
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next() && resultSet.getInt("MaxIndex") > 0) {
                int newIndex = resultSet.getInt("MaxIndex") + 1;
                userId = String.format("US%03d", newIndex); // Zero-padded format
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userId;
    }
    
    private void saveToDatabase(String userId, String username, String email, String password, int age, String gender, String country, String phone) {
        try (Connection connection = DatabaseHelper.getConnection()) {
            String sql = "INSERT INTO msuser (UserID, Username, Email, Password, Age, Gender, Country, PhoneNumber, Role) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, username);
            statement.setString(3, email);
            statement.setString(4, password);
            statement.setInt(5, age);
            statement.setString(6, gender);
            statement.setString(7, country);
            statement.setString(8, phone);
            statement.setString(9, "Customer");

            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Registration completed successfully!\nUser ID: " + userId);
                alert.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("An error occurred while saving the data.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }


    public void showLoginPage(Stage stage) {
        Login loginPage = new Login();
        try {
            loginPage.start(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

      
    @Override
    public void start(Stage primaryStage) {
        try {
            initialize(primaryStage);
            setComponent();
            arrangeComponent();
            validate(primaryStage);

            // Initialize the Scene with the layout
            registerScene = new Scene(borderPane, 800, 500); // Set appropriate width and height
            primaryStage.setScene(registerScene);
            primaryStage.setResizable(false);
            primaryStage.setTitle("Dc.VO || Register");
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }

}