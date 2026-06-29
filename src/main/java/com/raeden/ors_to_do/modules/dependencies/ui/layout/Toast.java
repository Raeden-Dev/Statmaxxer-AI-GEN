package com.raeden.ors_to_do.modules.dependencies.ui.layout;

import com.raeden.ors_to_do.TaskTrackerApp;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Lightweight transient notification anchored near the bottom of the main window, optionally with an
 * "Undo" action. Implemented as a {@link Popup} so it overlays without restructuring the scene graph.
 */
public final class Toast {

    private Toast() { }

    /** Shows a toast with an Undo button. {@code onUndo} runs if the user clicks Undo before it fades. */
    public static void showUndo(String message, Runnable onUndo) {
        Stage owner = TaskTrackerApp.MAIN_STAGE;
        if (owner == null || owner.getScene() == null) {
            // No window yet — fall back to running nothing (the action already happened).
            return;
        }

        Popup popup = new Popup();
        popup.setAutoFix(true);

        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 13px;");

        HBox box = new HBox(14);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 14, 10, 16));
        box.setStyle("-fx-background-color: #2D2D30; -fx-background-radius: 8; "
                + "-fx-border-color: #3E3E42; -fx-border-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 12, 0, 0, 4);");

        box.getChildren().add(label);

        if (onUndo != null) {
            Button undoBtn = new Button("Undo");
            undoBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #569CD6; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #569CD6; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 10;");
            undoBtn.setOnAction(e -> { popup.hide(); onUndo.run(); });
            box.getChildren().add(undoBtn);
        }

        Button dismissBtn = new Button("✕");
        dismissBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #858585; -fx-cursor: hand;");
        dismissBtn.setOnAction(e -> popup.hide());
        box.getChildren().add(dismissBtn);

        popup.getContent().add(box);

        // Position bottom-centre of the owner window once we know the toast's real size.
        popup.setOnShown(e -> {
            double w = box.getWidth();
            double x = owner.getX() + (owner.getWidth() - w) / 2.0;
            double y = owner.getY() + owner.getHeight() - box.getHeight() - 80;
            popup.setX(x);
            popup.setY(y);
        });

        popup.show(owner);

        PauseTransition life = new PauseTransition(Duration.seconds(6));
        life.setOnFinished(e -> popup.hide());
        life.play();
    }
}
