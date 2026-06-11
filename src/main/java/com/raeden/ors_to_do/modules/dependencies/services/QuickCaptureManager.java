package com.raeden.ors_to_do.modules.dependencies.services;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomPriority;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

public class QuickCaptureManager implements NativeKeyListener {
    private AppStats appStats;
    private List<TaskItem> taskDatabase;
    private Runnable onCaptureComplete;

    public QuickCaptureManager(AppStats appStats, List<TaskItem> taskDatabase, Runnable onCaptureComplete) {
        this.appStats = appStats;
        this.taskDatabase = taskDatabase;
        this.onCaptureComplete = onCaptureComplete;
    }

    /**
     * Swaps the data this manager captures into without touching the native hook. Used on profile
     * switch — re-registering the JNativeHook would reject events because its dispatch pool can't be
     * restarted once {@code unregisterNativeHook()} has terminated it.
     */
    public void updateContext(AppStats appStats, List<TaskItem> taskDatabase, Runnable onCaptureComplete) {
        this.appStats = appStats;
        this.taskDatabase = taskDatabase;
        this.onCaptureComplete = onCaptureComplete;
    }

    public void register() {
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException ex) {
            ex.printStackTrace();
        }
    }

    public void unregister() {
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // CTRL + SHIFT + SPACE
        if (e.getKeyCode() == NativeKeyEvent.VC_SPACE
                && (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0
                && (e.getModifiers() & NativeKeyEvent.SHIFT_MASK) != 0) {
            Platform.runLater(this::showQuickCaptureOverlay);
        }
    }

    private void showQuickCaptureOverlay() {
        Stage captureStage = new Stage();
        captureStage.initStyle(StageStyle.TRANSPARENT);
        captureStage.setAlwaysOnTop(true);

        TextField captureField = new TextField();
        captureField.setPromptText("Quick Capture...");
        captureField.setStyle("-fx-background-color: #2D2D30; -fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 15px; -fx-border-color: #569CD6; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
        captureField.setPrefWidth(400);

        captureField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !captureField.getText().trim().isEmpty()) {
                CustomPriority defaultPrio = appStats.getCustomPriorities().isEmpty() ? null : appStats.getCustomPriorities().get(0);
                String fallbackId = appStats.getSections().isEmpty() ? "QUICK" : appStats.getSections().get(0).getId();

                TaskItem newTask = new TaskItem(captureField.getText().trim(), defaultPrio, fallbackId);
                taskDatabase.add(newTask);
                StorageManager.saveTasks(taskDatabase);

                if (onCaptureComplete != null) onCaptureComplete.run();
                captureStage.close();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                captureStage.close();
            }
        });

        // Close it automatically if they click away
        captureField.focusedProperty().addListener((obs, oldV, newV) -> { if (!newV) captureStage.close(); });

        VBox layout = new VBox(captureField);
        layout.setStyle("-fx-background-color: transparent; -fx-padding: 10;");
        Scene scene = new Scene(layout);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        captureStage.setScene(scene);
        captureStage.show();
        captureField.requestFocus();
    }
}