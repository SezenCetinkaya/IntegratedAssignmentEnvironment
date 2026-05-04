package com.iae;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // TODO: FXML yüklemesi Uğur Emin Baynal tarafından eklenecek
        primaryStage.setTitle("IAE — Integrated Assignment Environment");
        primaryStage.setWidth(900);
        primaryStage.setHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
