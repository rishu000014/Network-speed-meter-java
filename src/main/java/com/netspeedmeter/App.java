package com.netspeedmeter;

import com.netspeedmeter.ui.DashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private DashboardView dashboard;

    @Override
    public void start(Stage stage) {
        dashboard = new DashboardView();
        Scene scene = new Scene(dashboard.getRoot(), 1280, 820);
        var css = getClass().getResource("/css/theme.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setTitle("NetSpeed Meter — Windows Network Monitor");
        stage.setMinWidth(1080);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (dashboard != null) {
                dashboard.shutdown();
            }
        });
        stage.show();
    }

    @Override
    public void stop() {
        if (dashboard != null) {
            dashboard.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
