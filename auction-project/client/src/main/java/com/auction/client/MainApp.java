package com.auction.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(new Label("Hello JavaFX"), 400, 300));
        stage.setTitle("Auction System");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}