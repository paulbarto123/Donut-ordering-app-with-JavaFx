/**
 * 
 */
/**
 * 
 */
module DavidCo {
	requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    exports main;
 // Use transitive to make javafx.graphics accessible to other modules
    requires transitive javafx.graphics;

    opens main to javafx.fxml;
}