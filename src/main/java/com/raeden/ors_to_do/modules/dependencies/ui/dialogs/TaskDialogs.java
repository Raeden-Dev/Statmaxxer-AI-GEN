package com.raeden.ors_to_do.modules.dependencies.ui.dialogs;

import com.raeden.ors_to_do.TaskTrackerApp;
import com.raeden.ors_to_do.dependencies.models.*;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.i18n.Lang;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.util.List;

import static com.raeden.ors_to_do.TaskTrackerApp.APP_VERSION;

public class TaskDialogs {

    public static final String[] ICON_LIST = {
            "None", "★", "☆", "⚡", "⚠", "⚙", "✉", "✎", "✔", "✖", "✚", "♫", "⚑", "⚐", "✂", "⌛", "⌚", "❀", "☾", "☁", "☂", "☃", "♛", "♚", "♞", "☯", "♦", "♣", "♠", "♥", "●", "■", "▲", "▼", "◆", "▶", "◀", "✦", "✧", "❂", "❖", "➤", "➥", "✓", "✗", "🔥", "🚀", "💡", "📌", "🏆"
    };

    public static void showLinkDialog(TaskItem task, TaskLink existingLink, List<TaskItem> globalDatabase, Runnable onUpdate) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existingLink == null ? "Add Link" : "Edit Link");
        styleDialog(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField nameField = new TextField(existingLink == null ? "" : existingLink.getName());
        TextField urlField = new TextField(existingLink == null ? "" : existingLink.getUrl());
        grid.add(new Label("Link Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("URL:"), 0, 1); grid.add(urlField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK && !urlField.getText().trim().isEmpty()) {
                String name = nameField.getText().trim().isEmpty() ? urlField.getText().trim() : nameField.getText().trim();
                if (existingLink == null) task.getTaskLinks().add(new TaskLink(name, urlField.getText().trim()));
                else { existingLink.setName(name); existingLink.setUrl(urlField.getText().trim()); }
                task.setExpanded(true); StorageManager.saveTasks(globalDatabase); onUpdate.run();
            }
        });
    }

    public static void showCreditsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Lang.ABOUT_CREDITS_TITLE.get(APP_VERSION));
        alert.setHeaderText(Lang.APP_NAME.get());
        alert.setContentText(Lang.ABOUT_CREDITS_BODY.get());
        styleDialog(alert);
        alert.show();
    }

    public static void showHelpDialog(AppStats stats) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Lang.HELP_TITLE.get());
        dialog.setHeaderText(Lang.HELP_HEADER.get(APP_VERSION));
        styleDialog(dialog);

        VBox contentBox = new VBox(15);
        contentBox.setStyle("-fx-padding: 10;");
        contentBox.setPrefWidth(450);

        ScrollPane scroll = new ScrollPane(contentBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Pulled string out into ThemeConstants
        scroll.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(ThemeConstants.TRANSPARENT_SCROLL_CSS.getBytes()));

        contentBox.getChildren().addAll(
                createHelpCard("🏆" + " Complete Control", "You are in charge of what you want to turn this application into. Whether a simple to-do app or a full rpg game system where the game is your life.", "yellow"),
                createHelpCard("♠" + " Stylized UI", "If you fancy nice and sleek looks, you can use your creativity to bring some color into the tasks or this application.", "aqua"),
                createHelpCard("⚡" + " Gamifying", "Gain skills or perks through completed challenges or achieving a certain level of a stat. You have to work hard to maintain your perks as well!", "purple"),
                createHelpCard("🔥" + " Streak System", "If you want to see the results of your consistency.", "orange"),
                createHelpCard("📊 Analytics", "Track completion streaks, total tasks done, and productivity statistics in the main dashboard.", "lightgreen"),
                createHelpCard("☯ Zen Mode", "Access Zen Mode from a section dashboard to focus on a single high-priority task. ", "red"),
                createHelpCard("⏱ Focus Hub", "Designed to help you focus on tasks, track time for a task and quickly add your sudden ideas to a scratchpad.", "orange"),
                createHelpCard("⚙" + " Custom Stat Creation", "Create and track your own personalized stat that you wish to improve on.", "pink"),
                createHelpCard("✦" + " Sub-Tasks", "Create sub-tasks for any type of task. If a task has sub-tasks, the main completion button will lock until all sub-tasks are completed.", "navy"),
                createHelpCard("✧" + " Optional Tasks", "Create Optional tasks that are only generated through templates. Ensure your optional tasks are meaningful.", "goldenrod"),
                createHelpCard("📌" + " Notes & Links page", "Don't want tasks? Create your own page for notes or links.", "darkmagenta"),
                createHelpCard("💎 Rewards Page", "Turn any section into a 'Rewards Page' via Section Manager. Assign cost points to items and 'Buy' them using your global score.", "#FFD700")
        );

        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    private static VBox createHelpCard(String title, String description, String titleColor) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: #2D2D30; -fx-padding: 10; -fx-border-color: #3E3E42; -fx-border-radius: 5; -fx-background-radius: 5;");
        Label tLabel = new Label(title);
        tLabel.setStyle("-fx-text-fill: " + titleColor + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label dLabel = new Label(description);
        dLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 13px;");
        dLabel.setWrapText(true);
        card.getChildren().addAll(tLabel, dLabel);
        return card;
    }

    public static void styleDialog(Dialog<?> dialog) {
        if (TaskTrackerApp.MAIN_STAGE != null) {
            dialog.initOwner(TaskTrackerApp.MAIN_STAGE);
        }

        // Pulled massive CSS string out into ThemeConstants
        String b64 = java.util.Base64.getEncoder().encodeToString(ThemeConstants.DIALOG_BASE_CSS.getBytes());
        dialog.getDialogPane().getStylesheets().add("data:text/css;base64," + b64);
        dialog.getDialogPane().setStyle("-fx-background-color: #1E1E1E;");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o, oldWin, newWin) -> {
                    if (newWin instanceof javafx.stage.Stage) {
                        ((javafx.stage.Stage) newWin).setAlwaysOnTop(true);
                    }
                });
            }
        });

        if (dialogPane.getScene() != null && dialogPane.getScene().getWindow() instanceof javafx.stage.Stage) {
            ((javafx.stage.Stage) dialogPane.getScene().getWindow()).setAlwaysOnTop(true);
        }
    }

    public static void setupPriorityBoxColors(ComboBox<CustomPriority> box) {
        // Pulled string out into ThemeConstants
        String b64 = java.util.Base64.getEncoder().encodeToString(ThemeConstants.PRIORITY_COMBO_CSS.getBytes());
        box.getStylesheets().add("data:text/css;base64," + b64);

        box.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(CustomPriority item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else { setText(item.getName()); setStyle("-fx-text-fill: " + item.getColorHex() + "; -fx-font-weight: bold;"); }
            }
        });
        box.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(CustomPriority item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else { setText(item.getName()); setStyle("-fx-text-fill: " + item.getColorHex() + "; -fx-font-weight: bold;"); }
            }
        });
    }

    public static String getCheckboxThemeCss(String theme) {
        if ("Dark".equals(theme)) {
            return ".check-box .box { -fx-background-color: #1E1E1E; -fx-border-color: #3E3E42; } .check-box:selected .mark { -fx-background-color: #858585; }";
        } else if ("Green".equals(theme)) {
            return ".check-box .box { -fx-background-color: #2D2D30; -fx-border-color: #4EC9B0; } .check-box:selected .mark { -fx-background-color: #4EC9B0; }";
        } else if ("Blue".equals(theme)) {
            return ".check-box .box { -fx-background-color: #2D2D30; -fx-border-color: #569CD6; } .check-box:selected .mark { -fx-background-color: #569CD6; }";
        } else if ("Purple".equals(theme)) {
            return ".check-box .box { -fx-background-color: #2D2D30; -fx-border-color: #C586C0; } .check-box:selected .mark { -fx-background-color: #C586C0; }";
        }
        return ".check-box .box { -fx-background-color: #2D2D30; -fx-border-color: #555555; } .check-box:selected .mark { -fx-background-color: white; }";
    }

    public static String toHexString(Color color) {
        if (color == null) return null;
        return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
    }

    public static void showAddSubTaskDialog(TaskItem task, List<TaskItem> globalDatabase, Runnable onUpdate) {
        SubTaskDialogs.showAddSubTaskDialog(task, globalDatabase, onUpdate);
    }

    public static void showEditSubTaskDialog(SubTask subTask, List<TaskItem> globalDatabase, Runnable onUpdate) {
        SubTaskDialogs.showEditSubTaskDialog(subTask, globalDatabase, onUpdate);
    }

    public static void showTextToTaskDialog(TaskItem sourceTask, List<TaskItem> globalDatabase, Runnable onUpdate) {
        SubTaskDialogs.showTextToTaskDialog(sourceTask, globalDatabase, onUpdate);
    }

    public static void showEditDialog(TaskItem task, SectionConfig config, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate) {
        TaskEditDialog.showEditDialog(task, config, appStats, globalDatabase, onUpdate);
    }
}