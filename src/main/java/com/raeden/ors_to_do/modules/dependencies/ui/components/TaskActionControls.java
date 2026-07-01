package com.raeden.ors_to_do.modules.dependencies.ui.components;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomPriority;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.TaskActionHandler;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.List;

public class TaskActionControls extends HBox {

    public TaskActionControls(TaskItem task, SectionConfig config, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate, int baseFontSize, int metaFontSize, boolean isNoteMode) {
        super(5);
        setAlignment(Pos.CENTER);

        // 1. Time Tracked
        if (config != null && config.isTrackTime() && !isNoteMode) {
            int mins = task.getTimeSpentSeconds() / 60;

            String timeText = task.getTargetTimeMinutes() > 0 ? "⏱ " + mins + "m / " + task.getTargetTimeMinutes() + "m" : "⏱ " + mins + "m";
            Label timeLabel = new Label(timeText);
            timeLabel.setPadding(new Insets(0, 10, 0, 0));

            if (task.getTargetTimeMinutes() > 0 && mins >= task.getTargetTimeMinutes()) {
                timeLabel.setStyle("-fx-text-fill: #4EC9B0; -fx-font-weight: bold; -fx-font-size: " + metaFontSize + "px;");
            } else {
                timeLabel.setStyle("-fx-text-fill: " + (mins > 0 ? "#E06666" : "#858585") + "; -fx-font-weight: bold; -fx-font-size: " + metaFontSize + "px;");
            }
            getChildren().add(timeLabel);
        }

        // 2. Priority Dropdown
        if (config != null && config.isShowPriority() && !isNoteMode && !task.isOptional()) {
            ComboBox<CustomPriority> prioBox = new ComboBox<>();
            prioBox.getItems().addAll(appStats.getCustomPriorities());
            prioBox.setValue(task.getPriority());
            TaskDialogs.setupPriorityBoxColors(prioBox);
            prioBox.setOnAction(e -> {
                task.setPriority(prioBox.getValue());
                StorageManager.saveTasks(globalDatabase);
                onUpdate.run();
            });
            getChildren().add(prioBox);
        }

        // 3. Action Logic (Pin / Lock / Buy / Counter / Checkbox)
        boolean hasUnfinishedSubTasks = !task.isFinished() && config != null && config.isEnableSubTasks() && !task.getSubTasks().isEmpty() && task.getSubTasks().stream().anyMatch(sub -> !sub.isFinished());

        if (task.isDescriptionCard()) {
            // Description cards replace the checkbox with a Copy button that copies the card's
            // stored description text to the system clipboard.
            Button copyBtn = new Button("📋 Copy");
            copyBtn.setStyle("-fx-background-color: #2D2D30; -fx-text-fill: #DCDCAA; -fx-border-color: #DCDCAA; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: " + metaFontSize + "px; -fx-padding: 4 10;");
            copyBtn.setOnAction(e -> {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(task.getDescriptionContent());
                Clipboard.getSystemClipboard().setContent(cc);
                copyBtn.setText("✓ Copied");
                PauseTransition revert = new PauseTransition(Duration.seconds(1.2));
                revert.setOnFinished(ev -> copyBtn.setText("📋 Copy"));
                revert.play();
            });
            getChildren().add(copyBtn);

        } else if (isNoteMode) {
            Button pinBtn = new Button("📌");
            pinBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: " + baseFontSize + "px; -fx-text-fill: " + (task.isPinned() ? "#FF6666" : "#FFFFFF") + "; -fx-opacity: " + (task.isPinned() ? 1.0 : 0.5) + ";");
            pinBtn.setOnAction(e -> { task.setPinned(!task.isPinned()); StorageManager.saveTasks(globalDatabase); onUpdate.run(); });
            getChildren().add(pinBtn);

        } else if (hasUnfinishedSubTasks) {
            Label subLockIcon = new Label("🔒");
            subLockIcon.setStyle("-fx-text-fill: #FF6666; -fx-font-size: " + baseFontSize + "px; -fx-cursor: help;");
            Tooltip.install(subLockIcon, new Tooltip("Complete all sub-tasks to unlock!"));
            getChildren().add(subLockIcon);

        } else if (config != null && config.isRewardsPage()) {
            Button buyBtn = new Button(task.isCounterMode() ? "Buy (" + task.getCurrentCount() + "/" + task.getMaxCount() + ")" : "Buy");
            buyBtn.setStyle("-fx-background-color: #0E639C; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
            if (task.isFinished()) buyBtn.setDisable(true);

            buyBtn.setOnAction(e -> TaskActionHandler.handleRewardPurchase(task, config, appStats, globalDatabase, onUpdate));

            getChildren().add(buyBtn);

        } else if (task.isCounterMode()) {
            Button minusBtn = new Button("-");
            Button plusBtn = new Button("+");
            String btnStyle = "-fx-background-color: #3E3E42; -fx-text-fill: white; -fx-cursor: hand;";
            minusBtn.setStyle(btnStyle); plusBtn.setStyle(btnStyle);

            // --- FIXED: Apply Section-Level Completion Lock to Counters ---
            boolean isSectionLocked = config != null && config.isLockCompletedTasks();
            boolean isLocked = task.isFinished() && (task.isPermaLock() || isSectionLocked);

            if (isLocked) {
                minusBtn.setDisable(true);
            }
            if (task.isFinished() || isLocked) {
                plusBtn.setDisable(true);
            }

            Label countLabel = new Label(task.getCurrentCount() + (task.getMaxCount() > 0 ? " / " + task.getMaxCount() : ""));
            countLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 0 5 0 5;");

            minusBtn.setOnAction(e -> {
                if (task.getCurrentCount() > 0) {
                    if (task.isPointsClaimed()) {
                        int scoreBefore = appStats.getGlobalScore();
                        int scoreAfter = Math.max(0, scoreBefore - task.getRewardPoints());
                        appStats.setGlobalScore(scoreAfter);
                        int scoreDelta = scoreAfter - scoreBefore;
                        if (scoreDelta != 0) {
                            appStats.recordStatChange(com.raeden.ors_to_do.dependencies.models.StatLedgerEntry.GLOBAL_SCORE,
                                    scoreDelta, "pts",
                                    "Reverted: " + (task.getTextContent() == null || task.getTextContent().isBlank() ? "Task" : task.getTextContent()));
                        }
                        if (config != null && config.isEnableStatsSystem()) {
                            TaskActionHandler.processRPGStats(task, appStats, false);
                        }
                        task.setPointsClaimed(false);
                        StorageManager.saveStats(appStats);
                    }

                    task.setCurrentCount(task.getCurrentCount() - 1);
                    task.setFinished(false);
                    StorageManager.saveTasks(globalDatabase); onUpdate.run();
                }
            });

            plusBtn.setOnAction(e -> {
                if (!task.isFinished()) {
                    task.setCurrentCount(task.getCurrentCount() + 1);
                    if (task.getMaxCount() > 0 && task.getCurrentCount() >= task.getMaxCount()) {
                        TaskActionHandler.handleTaskCompletion(task, config, appStats, globalDatabase, onUpdate, null);
                        if (config != null && config.isEnableStatsSystem()) { StorageManager.saveStats(appStats); }
                    } else {
                        StorageManager.saveTasks(globalDatabase); onUpdate.run();
                    }
                }
            });
            getChildren().addAll(minusBtn, countLabel, plusBtn);

        } else {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(task.isFinished());

            boolean timeLocked = task.getTargetTimeMinutes() > 0 && (task.getTimeSpentSeconds() / 60) < task.getTargetTimeMinutes();

            // --- FIXED: Apply Section-Level Completion Lock to Checkboxes ---
            boolean isSectionLocked = config != null && config.isLockCompletedTasks();
            boolean isLocked = (task.isFinished() && (task.isPermaLock() || isSectionLocked)) || timeLocked;

            if (isLocked) {
                checkBox.setDisable(true);
            }

            if (timeLocked && !task.isFinished()) {
                Tooltip t = new Tooltip("Requires " + task.getTargetTimeMinutes() + "m of focus time to complete!");
                t.setStyle("-fx-background-color: #1E1E1E; -fx-text-fill: #FF6666; -fx-border-color: #FF6666; -fx-font-size: 12px;");
                Tooltip.install(checkBox, t);
            }

            checkBox.setOnAction(e -> {
                if (checkBox.isSelected()) {
                    TaskActionHandler.handleTaskCompletion(task, config, appStats, globalDatabase, onUpdate, checkBox);
                    if (config != null && config.isEnableStatsSystem()) { StorageManager.saveStats(appStats); }
                } else {
                    if (task.isPointsClaimed()) {
                        int scoreBefore = appStats.getGlobalScore();
                        int scoreAfter = Math.max(0, scoreBefore - task.getRewardPoints());
                        appStats.setGlobalScore(scoreAfter);
                        int scoreDelta = scoreAfter - scoreBefore;
                        if (scoreDelta != 0) {
                            appStats.recordStatChange(com.raeden.ors_to_do.dependencies.models.StatLedgerEntry.GLOBAL_SCORE,
                                    scoreDelta, "pts",
                                    "Reverted: " + (task.getTextContent() == null || task.getTextContent().isBlank() ? "Task" : task.getTextContent()));
                        }
                        if (config != null && config.isEnableStatsSystem()) {
                            TaskActionHandler.processRPGStats(task, appStats, false);
                        }
                        task.setPointsClaimed(false);
                        StorageManager.saveStats(appStats);
                    }

                    task.setFinished(false); StorageManager.saveTasks(globalDatabase); onUpdate.run();
                }
            });
            getChildren().add(checkBox);
        }
    }
}