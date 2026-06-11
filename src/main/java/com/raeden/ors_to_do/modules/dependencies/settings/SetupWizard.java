package com.raeden.ors_to_do.modules.dependencies.settings;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.storage.ProfileManager;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.services.WindowsStartupManager;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.FontManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

/**
 * First-launch setup wizard. Lets the user name their profile, choose whether to keep the starter
 * sections, pick appearance (theme + font), and set personal/behavior preferences. Runs once; on
 * finish it marks {@code setupCompleted} so it never reappears.
 */
public final class SetupWizard {

    private SetupWizard() { }

    public static void show(AppStats appStats, Runnable onFinish) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Welcome to Statmaxxer");
        TaskDialogs.styleDialog(dialog);

        VBox content = new VBox(16);
        content.setPadding(new Insets(16));
        content.setPrefWidth(520);

        Label welcome = new Label("Let's set things up");
        welcome.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #569CD6;");
        Label sub = new Label("You can change any of this later in Settings.");
        sub.setStyle("-fx-text-fill: #858585; -fx-font-size: 12px;");
        content.getChildren().addAll(welcome, sub);

        // --- 1. Profile / display name ---
        TextField nameField = new TextField(appStats.getUserDisplayName());
        nameField.setPromptText("Your name / profile name");
        content.getChildren().add(section("1 · Your Profile",
                "Names your active profile and how the app greets you.",
                labeled("Display name:", nameField)));

        // --- 2. Starter sections ---
        ToggleGroup sectionsGroup = new ToggleGroup();
        RadioButton keepStarter = new RadioButton("Keep starter sections (Quick, Daily, Work)");
        keepStarter.setToggleGroup(sectionsGroup); keepStarter.setSelected(true); keepStarter.setStyle("-fx-text-fill: white;");
        RadioButton startEmpty = new RadioButton("Start empty — I'll create my own");
        startEmpty.setToggleGroup(sectionsGroup); startEmpty.setStyle("-fx-text-fill: white;");
        content.getChildren().add(section("2 · Sections",
                "Pick a starting point for your pages.",
                new VBox(6, keepStarter, startEmpty)));

        // --- 3. Appearance ---
        ComboBox<String> themeBox = new ComboBox<>();
        themeBox.getItems().addAll("Default", "Dark", "Green", "Blue", "Purple");
        themeBox.setValue(appStats.getCheckboxTheme());
        ComboBox<String> fontBox = new ComboBox<>();
        fontBox.getItems().addAll(FontManager.options());
        fontBox.setValue(FontManager.normalize(appStats.getTaskFontFamily()));
        content.getChildren().add(section("3 · Appearance",
                "Checkbox theme and the app-wide font (includes Retro, Pixel Art, Matrix).",
                new VBox(8, labeled("Checkbox theme:", themeBox), labeled("Font style:", fontBox))));

        // --- 4. Personal & behavior ---
        DatePicker birthday = new DatePicker(appStats.getUserBirthDate());
        Spinner<Integer> targetAge = new Spinner<>(1, 150, appStats.getUserTargetAge());
        targetAge.setEditable(true);
        CheckBox startup = new CheckBox("Launch on Windows startup"); startup.setSelected(appStats.isRunOnStartup()); startup.setStyle("-fx-text-fill: white;");
        CheckBox notifications = new CheckBox("Enable desktop notifications"); notifications.setSelected(appStats.isEnableNotifications()); notifications.setStyle("-fx-text-fill: white;");
        content.getChildren().add(section("4 · Personal & Behavior",
                "Used by the age analytics and app behavior.",
                new VBox(8,
                        labeled("Birthday:", birthday),
                        labeled("Target age:", targetAge),
                        startup, notifications)));

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPrefSize(560, 620);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");
        sp.setBorder(Border.EMPTY);

        dialog.getDialogPane().setContent(sp);
        ButtonType finishType = new ButtonType("Finish Setup", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(finishType);

        dialog.showAndWait();

        // Apply (the wizard cannot be cancelled — it always completes setup).
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        appStats.setUserDisplayName(name);
        if (!name.isEmpty()) ProfileManager.renameProfile(ProfileManager.getActiveId(), name);

        if (startEmpty.isSelected()) appStats.getSections().clear();

        appStats.setCheckboxTheme(themeBox.getValue());
        appStats.setTaskFontFamily(fontBox.getValue());

        if (birthday.getValue() != null) appStats.setUserBirthDate(birthday.getValue());
        else appStats.setUserBirthDate(LocalDate.of(2000, 1, 1));
        appStats.setUserTargetAge(targetAge.getValue());
        appStats.setRunOnStartup(startup.isSelected());
        appStats.setEnableNotifications(notifications.isSelected());
        WindowsStartupManager.setStartupEnabled(startup.isSelected());

        appStats.setSetupCompleted(true);
        StorageManager.saveStats(appStats);

        if (onFinish != null) onFinish.run();
    }

    private static VBox section(String title, String desc, javafx.scene.Node body) {
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #DCDCAA;");
        Label d = new Label(desc);
        d.setStyle("-fx-text-fill: #858585; -fx-font-size: 11px;"); d.setWrapText(true);
        VBox box = new VBox(8, t, d, body);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-border-color: #3E3E42; -fx-border-radius: 6; -fx-background-color: #252526; -fx-background-radius: 6;");
        return box;
    }

    private static HBox labeled(String label, javafx.scene.control.Control control) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #DDDDDD;");
        l.setMinWidth(120);
        HBox row = new HBox(10, l, control);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }
}
