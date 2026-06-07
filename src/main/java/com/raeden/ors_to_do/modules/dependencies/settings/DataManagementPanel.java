package com.raeden.ors_to_do.modules.dependencies.settings;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.i18n.Lang;
import com.raeden.ors_to_do.modules.dependencies.services.BackupManager;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.Design;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManagementPanel extends VBox {

    public DataManagementPanel(AppStats appStats, List<TaskItem> globalDatabase, Runnable refreshCallback) {
        super(15);
        setStyle("-fx-border-color: #2E8B57; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5;");

        Label header = new Label("Data Management (Backup & Restore)");
        header.setStyle("-fx-text-fill: #4EC9B0; -fx-font-size: 16px; -fx-font-weight: bold;");

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button exportBtn = new Button("Export All Data");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(exportBtn, Priority.ALWAYS);
        exportBtn.setStyle("-fx-background-color: #1a4d33; -fx-text-fill: #4EC9B0; -fx-border-color: #4EC9B0; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; -fx-padding: 8 15;");

        Button customExportBtn = new Button("Export Custom Data");
        customExportBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(customExportBtn, Priority.ALWAYS);
        customExportBtn.setStyle("-fx-background-color: #1a4d33; -fx-text-fill: #FFD700; -fx-border-color: #FFD700; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; -fx-padding: 8 15;");

        Button importBtn = new Button("Import Backup Data");
        importBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(importBtn, Priority.ALWAYS);
        importBtn.setStyle("-fx-background-color: #1a4d33; -fx-text-fill: #4EC9B0; -fx-border-color: #4EC9B0; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; -fx-padding: 8 15;");

        Button openFolderBtn = new Button(Lang.OPEN_DATA_FOLDER_BTN.get());
        openFolderBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(openFolderBtn, Priority.ALWAYS);
        openFolderBtn.setStyle("-fx-background-color: #1a4d33; -fx-text-fill: #C586C0; -fx-border-color: #C586C0; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; -fx-padding: 8 15;");
        openFolderBtn.setTooltip(new Tooltip(Lang.OPEN_DATA_FOLDER_TOOLTIP.get()));

        exportBtn.setOnAction(e -> BackupManager.exportData(appStats, globalDatabase));
        importBtn.setOnAction(e -> BackupManager.importData(appStats, globalDatabase, refreshCallback));
        customExportBtn.setOnAction(e -> showCustomExportDialog(appStats, globalDatabase));
        openFolderBtn.setOnAction(e -> openDataFolder());

        buttonBox.getChildren().addAll(exportBtn, customExportBtn, importBtn, openFolderBtn);
        getChildren().addAll(header, buttonBox);
    }

    /**
     * Opens the directory where tasks.json / stats.json / backups live in the OS file explorer.
     * Done off the JavaFX thread because {@link java.awt.Desktop#open} can block briefly on
     * Windows while it spawns the shell handler.
     */
    private void openDataFolder() {
        java.io.File dir = StorageManager.getDataDirectory();
        if (!dir.exists()) dir.mkdirs();   // first launch may not have written anything yet
        new Thread(() -> {
            try {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    java.awt.Desktop.getDesktop().open(dir);
                } else {
                    javafx.application.Platform.runLater(() ->
                            Design.error(Lang.OPEN_DATA_FOLDER_ERROR_HEADER, Lang.OPEN_DATA_FOLDER_ERROR_BODY, dir.getAbsolutePath()));
                }
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() ->
                        Design.error(Lang.OPEN_DATA_FOLDER_ERROR_HEADER, Lang.OPEN_DATA_FOLDER_ERROR_BODY,
                                dir.getAbsolutePath() + "\n\n" + ex.getMessage()));
            }
        }, "open-data-folder").start();
    }

    private void showCustomExportDialog(AppStats appStats, List<TaskItem> globalDatabase) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Export Custom Data");
        TaskDialogs.styleDialog(dialog);

        VBox content = new VBox(15);
        content.setPadding(new Insets(15));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(400, 500);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");

        // --- 1. Analytics Group ---
        CheckBox analyticsMaster = styleCheckBox(new CheckBox("Analytics (All)"), true);
        CheckBox globalAnalyticsCb = styleCheckBox(new CheckBox("Global Analytics"), false);
        CheckBox currentStatsCb = styleCheckBox(new CheckBox("Current Stats"), false);
        CheckBox sectionAnalyticsCb = styleCheckBox(new CheckBox("Section stuff"), false);

        VBox analyticsBox = new VBox(8, analyticsMaster, indent(globalAnalyticsCb), indent(currentStatsCb), indent(sectionAnalyticsCb));
        linkMasterCheckbox(analyticsMaster, globalAnalyticsCb, currentStatsCb, sectionAnalyticsCb);

        // --- 2. Archived Group ---
        CheckBox archivedCb = styleCheckBox(new CheckBox("Archived Tasks"), true);

        // --- 3. Settings Group ---
        CheckBox settingsMaster = styleCheckBox(new CheckBox("Settings (All)"), true);
        CheckBox dynSectionsCb = styleCheckBox(new CheckBox("Dynamic Sections"), false);
        CheckBox genConfigCb = styleCheckBox(new CheckBox("General Configuration"), false);
        CheckBox autoGenCb = styleCheckBox(new CheckBox("Auto Generating Tasks"), false);
        CheckBox statsConfigCb = styleCheckBox(new CheckBox("Stats Configuration"), false);
        CheckBox prioritiesCb = styleCheckBox(new CheckBox("Priorities"), false);

        VBox settingsBox = new VBox(8, settingsMaster, indent(dynSectionsCb), indent(genConfigCb), indent(autoGenCb), indent(statsConfigCb), indent(prioritiesCb));
        linkMasterCheckbox(settingsMaster, dynSectionsCb, genConfigCb, autoGenCb, statsConfigCb, prioritiesCb);

        // --- 4. Dynamic Sections Data Group ---
        CheckBox sectionDataMaster = styleCheckBox(new CheckBox("Section Tasks Data (All)"), true);
        VBox sectionDataBox = new VBox(8, sectionDataMaster);
        List<CheckBox> sectionCbs = new ArrayList<>();

        for (SectionConfig sec : appStats.getSections()) {
            CheckBox cb = styleCheckBox(new CheckBox(sec.getName() + " Data"), false);
            sectionCbs.add(cb);
            sectionDataBox.getChildren().add(indent(cb));
        }
        linkMasterCheckbox(sectionDataMaster, sectionCbs.toArray(new CheckBox[0]));

        content.getChildren().addAll(analyticsBox, new Separator(), archivedCb, new Separator(), settingsBox, new Separator(), sectionDataBox);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                // Package the selections into a Map
                Map<String, Boolean> exportFlags = new HashMap<>();
                exportFlags.put("globalAnalytics", globalAnalyticsCb.isSelected());
                exportFlags.put("currentStats", currentStatsCb.isSelected());
                exportFlags.put("sectionAnalytics", sectionAnalyticsCb.isSelected());
                exportFlags.put("archived", archivedCb.isSelected());
                exportFlags.put("dynamicSectionsConfig", dynSectionsCb.isSelected());
                exportFlags.put("generalConfig", genConfigCb.isSelected());
                exportFlags.put("autoGenTasks", autoGenCb.isSelected());
                exportFlags.put("statsConfig", statsConfigCb.isSelected());
                exportFlags.put("prioritiesConfig", prioritiesCb.isSelected());

                List<String> selectedSectionIdsToExport = new ArrayList<>();
                for (int i = 0; i < appStats.getSections().size(); i++) {
                    if (sectionCbs.get(i).isSelected()) {
                        selectedSectionIdsToExport.add(appStats.getSections().get(i).getId());
                    }
                }

                // Call the new targeted export method
                BackupManager.exportCustomData(appStats, globalDatabase, exportFlags, selectedSectionIdsToExport);
            }
        });
    }

    private CheckBox styleCheckBox(CheckBox cb, boolean isBold) {
        cb.setStyle("-fx-text-fill: #E0E0E0;" + (isBold ? " -fx-font-weight: bold;" : ""));
        return cb;
    }

    private HBox indent(CheckBox cb) {
        HBox box = new HBox(cb);
        box.setPadding(new Insets(0, 0, 0, 25)); // 25px indent
        return box;
    }

    private void linkMasterCheckbox(CheckBox master, CheckBox... children) {
        master.setOnAction(e -> {
            boolean isSelected = master.isSelected();
            for (CheckBox child : children) {
                child.setSelected(isSelected);
            }
        });
    }
}