package com.raeden.ors_to_do.modules.dependencies.settings;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.DeletedHistoryDialog;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

public class DangerZonePanel extends VBox {
    private AppStats appStats;
    private List<TaskItem> globalDatabase;
    private Runnable refreshCallback;
    private final double BUTTON_WIDTH = 200.0;

    /** Buttons that clear individual sections live here, under the "Sections" header. */
    private FlowPane sectionsPane;
    /** Everything else (archive, analytics, stats, history, full reset) lives here, under "Others". */
    private FlowPane othersPane;
    /** Header above the section-wipe buttons; hidden when there are no real sections. */
    private Label sectionsHeader;

    public DangerZonePanel(AppStats appStats, List<TaskItem> globalDatabase, Runnable refreshCallback) {
        super(15);
        this.appStats = appStats;
        this.globalDatabase = globalDatabase;
        this.refreshCallback = refreshCallback;

        setStyle("-fx-border-color: #FF6666; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5;");
        Label dangerLabel = new Label("Danger Zone");
        dangerLabel.setStyle("-fx-text-fill: #FF6666; -fx-font-size: 16px; -fx-font-weight: bold;");

        sectionsHeader = createGroupHeader("Sections");
        sectionsPane = new FlowPane(15, 15);

        Label othersHeader = createGroupHeader("Others");
        othersPane = new FlowPane(15, 15);

        // A little breathing room above the "Others" group so the two clusters read as separate.
        VBox.setMargin(othersHeader, new javafx.geometry.Insets(5, 0, 0, 0));

        getChildren().addAll(dangerLabel, sectionsHeader, sectionsPane, othersHeader, othersPane);
        refreshDangerZone();
    }

    public void refreshDangerZone() {
        sectionsPane.getChildren().clear();
        othersPane.getChildren().clear();

        // --- Group 1: per-section wipe buttons (Keep Red) ---
        for (SectionConfig section : appStats.getSections()) {
            // --- FIXED: Prevent Separators from generating Wipe Buttons ---
            if (!section.isSeparator()) {
                Button wipeBtn = createDangerButton("Wipe " + section.getName(), "#FF6666");
                wipeBtn.setOnAction(e -> wipeList(globalDatabase, section.getId(), refreshCallback));
                sectionsPane.getChildren().add(wipeBtn);
            }
        }

        // Hide the "Sections" header + pane entirely when there are no real sections to wipe.
        boolean hasSections = !sectionsPane.getChildren().isEmpty();
        sectionsHeader.setVisible(hasSections);
        sectionsHeader.setManaged(hasSections);
        sectionsPane.setVisible(hasSections);
        sectionsPane.setManaged(hasSections);

        // --- Group 2: everything else ---
        // Wipe Archive Button (Purple)
        Button wipeArchiveBtn = createDangerButton("Empty Archive", "#C586C0");
        wipeArchiveBtn.setOnAction(e -> wipeList(globalDatabase, "ARCHIVED_FLAG", refreshCallback));
        othersPane.getChildren().add(wipeArchiveBtn);

        // Reset Analytics Button (Yellow)
        Button resetAnalyticsBtn = createDangerButton("Reset Global Analytics", "#FFD700");
        resetAnalyticsBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to permanently reset all analytics?", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText("Reset Analytics");
            TaskDialogs.styleDialog(alert);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    appStats.setGlobalScore(0);
                    appStats.setCurrentStreak(0);
                    appStats.setHighestStreak(0);
                    appStats.setLifetimeDeletedTasks(0);
                    appStats.setLifetimePointsSpent(0);
                    appStats.setRewardsClaimed(0);
                    appStats.getHistoryLog().clear();
                    appStats.getAdvancedHistoryLog().clear();
                    for(TaskItem t : globalDatabase) t.setTimeSpentSeconds(0);
                    StorageManager.saveStats(appStats);
                    StorageManager.saveTasks(globalDatabase);
                    refreshCallback.run();
                }
            });
        });
        othersPane.getChildren().add(resetAnalyticsBtn);

        // 4. Wipe RPG Stats Button (Sea Blue)
        Button wipeStatsBtn = createDangerButton("Reset Stat Progress", "#48D1CC");
        wipeStatsBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to reset all Custom Stat XP and Perk Levels back to 0?", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText("Reset Stat Progress");
            TaskDialogs.styleDialog(alert);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    for (CustomStat stat : appStats.getCustomStats()) {
                        stat.setCurrentAmount(0);
                        stat.setLifetimeEarned(0);
                        stat.setLifetimeLost(0);
                        stat.setMaxLevelReached(0);
                    }
                    appStats.getLastStatGainDates().clear();

                    for (TaskItem t : globalDatabase) {
                        t.setPerkLevel(0);
                        t.setWeeksMaintained(0);
                    }

                    StorageManager.saveStats(appStats);
                    StorageManager.saveTasks(globalDatabase);
                    refreshCallback.run();
                }
            });
        });
        othersPane.getChildren().add(wipeStatsBtn);

        // 5. Deleted Tasks History Button
        Button historyBtn = new Button("View Deleted Tasks History");
        historyBtn.setPrefWidth(BUTTON_WIDTH);
        historyBtn.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #555555; -fx-border-radius: 3;");
        historyBtn.setOnAction(e -> {
            DeletedHistoryDialog.show(appStats, refreshCallback);
        });
        othersPane.getChildren().add(historyBtn);

        // 6. Full App Reset Button
        Button fullResetBtn = new Button("Full App Reset");
        fullResetBtn.setPrefWidth(BUTTON_WIDTH);
        fullResetBtn.setStyle("-fx-background-color: #8B0000; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
        fullResetBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "DANGER: This will permanently delete ALL Tasks, Sections, Custom Stats, Perks, and Settings.\n\nAre you absolutely sure?", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText("FULL APP RESET");
            TaskDialogs.styleDialog(alert);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    globalDatabase.clear();
                    appStats.getSections().clear();
                    appStats.getCustomStats().clear();
                    appStats.getSectionPresets().clear();
                    appStats.getCustomPriorities().clear();
                    appStats.getHistoryLog().clear();
                    appStats.getAdvancedHistoryLog().clear();
                    appStats.getLastStatGainDates().clear();

                    appStats.setGlobalScore(0);
                    appStats.setCurrentStreak(0);
                    appStats.setHighestStreak(0);
                    appStats.setLifetimeDeletedTasks(0);
                    appStats.setLifetimePointsSpent(0);
                    appStats.setRewardsClaimed(0);

                    StorageManager.saveStats(appStats);
                    StorageManager.saveTasks(globalDatabase);
                    refreshCallback.run();
                }
            });
        });
        othersPane.getChildren().add(fullResetBtn);
    }

    /** Small, muted sub-header used to label a group of danger-zone buttons. */
    private Label createGroupHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        return l;
    }

    private Button createDangerButton(String text, String colorHex) {
        Button btn = new Button(text);
        btn.setPrefWidth(BUTTON_WIDTH);
        btn.setStyle("-fx-background-color: #333333; -fx-text-fill: " + colorHex + "; -fx-border-color: " + colorHex + "; -fx-border-radius: 3; -fx-cursor: hand;");
        return btn;
    }

    private void wipeList(List<TaskItem> db, String targetSectionId, Runnable refresh) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to clear this list?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Wipe Section");
        TaskDialogs.styleDialog(alert);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                db.removeIf(task -> {
                    if ("ARCHIVED_FLAG".equals(targetSectionId)) return task.isArchived();
                    return targetSectionId.equals(task.getSectionId()) && !task.isArchived();
                });
                StorageManager.saveTasks(db);
                refresh.run();
            }
        });
    }
}