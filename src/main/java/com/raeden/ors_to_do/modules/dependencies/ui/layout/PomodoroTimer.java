package com.raeden.ors_to_do.modules.dependencies.ui.layout;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.models.OriginModule;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.services.SystemTrayManager;
import com.raeden.ors_to_do.modules.dependencies.services.GlobalActivityTracker;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PomodoroTimer extends VBox {
    private AppStats appStats;
    private List<TaskItem> globalDatabase;
    private Runnable refreshCallback;
    private Runnable onUrgeSurfing;

    private Timeline timeline;
    private int timeLeft = 25 * 60;
    private int lastTrackedTimeLeft = 25 * 60;
    private boolean isFocusMode = true;

    // --- NEW: Track if the session was interrupted ---
    private boolean sessionWasPaused = false;

    private Label timeDisplay;
    private Label statusLabel;
    private Button startPauseBtn;
    private ComboBox<Integer> timerOptions;
    private Button urgeBtn;

    private TextField taskSearchField;
    private ComboBox<TaskItem> taskSelector;
    private List<TaskItem> allFocusableTasks = new ArrayList<>();

    public PomodoroTimer(AppStats appStats, List<TaskItem> globalDatabase, Runnable refreshCallback, Runnable onUrgeSurfing) {
        super(20);
        this.appStats = appStats;
        this.globalDatabase = globalDatabase;
        this.refreshCallback = refreshCallback;
        this.onUrgeSurfing = onUrgeSurfing;

        setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(this, Priority.ALWAYS);
        setMinWidth(450);

        setStyle("-fx-background-color: #252526; -fx-background-radius: 12; -fx-border-color: #3E3E42; -fx-border-width: 1; -fx-border-radius: 12; -fx-padding: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 5);");

        statusLabel = new Label("FOCUS SESSION");
        statusLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #569CD6; -fx-letter-spacing: 2px;");
        statusLabel.getStyleClass().add("focus-status");
        VBox.setMargin(statusLabel, new Insets(10, 0, 0, 0));

        VBox linkBox = new VBox(8);
        linkBox.setAlignment(Pos.CENTER);
        Label linkLabel = new Label("Link Focus to Task:");
        linkLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 13px; -fx-font-weight: bold;");

        taskSearchField = new TextField();
        taskSearchField.setPromptText("🔍 Search tasks...");
        taskSearchField.setPrefWidth(320);
        taskSearchField.setMaxWidth(320);
        taskSearchField.setStyle("-fx-background-color: #1E1E1E; -fx-text-fill: white; -fx-border-color: #555555; -fx-border-radius: 5; -fx-padding: 8;");

        taskSearchField.textProperty().addListener((obs, oldText, newText) -> {
            applyTaskFilter(newText);
        });

        taskSelector = new ComboBox<>();
        taskSelector.setPrefWidth(320);
        taskSelector.setStyle("-fx-background-color: #3E3E42; -fx-cursor: hand; -fx-border-radius: 5; -fx-background-radius: 5;");

        taskSelector.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(TaskItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("None (Free Focus)");
                } else {
                    setText("[" + getSectionName(item) + "] " + item.getTextContent());
                }
                setStyle("-fx-text-fill: black;");
            }
        });
        taskSelector.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(TaskItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("None (Free Focus)");
                } else {
                    setText("[" + getSectionName(item) + "] " + item.getTextContent());
                }
                setStyle("-fx-text-fill: white;");
            }
        });

        linkBox.getChildren().addAll(linkLabel, taskSearchField, taskSelector);

        timeDisplay = new Label("25:00");
        timeDisplay.setStyle("-fx-font-size: 85px; -fx-font-weight: bold; -fx-text-fill: #569CD6;");

        HBox optionsPanel = new HBox(10);
        optionsPanel.setAlignment(Pos.CENTER);
        Label focusLengthLabel = new Label("Focus Length (m): ");
        focusLengthLabel.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 13px;");

        timerOptions = new ComboBox<>();
        timerOptions.getItems().addAll(1, 10, 25, 30, 40, 60, 90, 120);
        timerOptions.setValue(25);
        timerOptions.setStyle("-fx-background-color: #1E1E1E; -fx-text-fill: white; -fx-border-color: #555555; -fx-border-radius: 3; -fx-cursor: hand;");
        timerOptions.setOnAction(e -> {
            if (timeline == null || !timeline.getStatus().equals(Timeline.Status.RUNNING)) {
                if (isFocusMode) {
                    timeLeft = timerOptions.getValue() * 60;
                    lastTrackedTimeLeft = timeLeft;
                    sessionWasPaused = false; // Reset pause state on manual change
                    updateDisplay();
                }
            }
        });

        // --- NEW: Reward Config Button ---
        Button rewardConfigBtn = new Button("⚙");
        rewardConfigBtn.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: #AAAAAA; -fx-cursor: hand; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 11px;");
        rewardConfigBtn.setOnAction(e -> showRewardConfigDialog());

        optionsPanel.getChildren().addAll(focusLengthLabel, timerOptions, rewardConfigBtn);

        HBox btnPanel = new HBox(20);
        btnPanel.setAlignment(Pos.CENTER);

        startPauseBtn = new Button("START");
        startPauseBtn.setStyle("-fx-background-color: #569CD6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 12 30; -fx-font-size: 14px; -fx-background-radius: 5;");

        Button resetBtn = new Button("RESET");
        resetBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #E06666; -fx-text-fill: #E06666; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 25; -fx-font-size: 14px; -fx-border-radius: 5;");

        btnPanel.getChildren().addAll(startPauseBtn, resetBtn);

        urgeBtn = new Button("🌊 Resist Urge (Breathe)");
        urgeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #4EC9B0; -fx-text-fill: #4EC9B0; -fx-border-radius: 5; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 10 25; -fx-font-size: 13px;");
        urgeBtn.setOnAction(e -> {
            if (timeline != null && timeline.getStatus() == Timeline.Status.RUNNING) {
                startPauseBtn.fire();
            }
            onUrgeSurfing.run();
        });

        getChildren().addAll(statusLabel, linkBox, timeDisplay, optionsPanel, btnPanel, new Separator(), urgeBtn);
        setupTimerLogic();

        startPauseBtn.setOnAction(e -> {
            if (timeline.getStatus() == Timeline.Status.RUNNING) {
                timeline.pause();
                sessionWasPaused = true; // Mark as interrupted

                if (isFocusMode && taskSelector.getValue() != null) {
                    int elapsed = lastTrackedTimeLeft - timeLeft;
                    if (elapsed > 0) taskSelector.getValue().addTimeSpent(elapsed);
                    lastTrackedTimeLeft = timeLeft;
                    StorageManager.saveTasks(globalDatabase);
                    if (refreshCallback != null) refreshCallback.run();
                }

                startPauseBtn.setText("RESUME");
                startPauseBtn.setStyle("-fx-background-color: #FF8C00; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 12 30; -fx-font-size: 14px; -fx-background-radius: 5;");
            } else {
                GlobalActivityTracker.resetActivityTime();
                lastTrackedTimeLeft = timeLeft;
                timeline.play();
                startPauseBtn.setText("PAUSE");
                startPauseBtn.setStyle("-fx-background-color: #569CD6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 12 30; -fx-font-size: 14px; -fx-background-radius: 5;");
            }
        });

        resetBtn.setOnAction(e -> resetTimer(isFocusMode));
    }

    private void showRewardConfigDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Perfect Focus Rewards");
        TaskDialogs.styleDialog(dialog);

        VBox content = new VBox(15);
        content.setPadding(new Insets(15));

        Label infoLabel = new Label("Set rewards for completing a full focus session without stopping or pausing.");
        infoLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-style: italic; -fx-font-size: 12px;");
        infoLabel.setWrapText(true);
        content.getChildren().add(infoLabel);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(10);

        TextField ptsField = new TextField(String.valueOf(appStats.getFocusRewardPoints()));
        ptsField.setPrefWidth(100);
        grid.add(new Label("Global Points Reward:"), 0, 0);
        grid.add(ptsField, 1, 0);

        int r = 1;
        Map<String, TextField> statFields = new HashMap<>();
        for (CustomStat stat : appStats.getCustomStats()) {
            Label statLabel = new Label("+" + stat.getName() + " XP:");
            statLabel.setStyle("-fx-text-fill: " + (stat.getTextColor() != null ? stat.getTextColor() : "#FFFFFF") + ";");
            grid.add(statLabel, 0, r);

            TextField sf = new TextField(String.valueOf(appStats.getFocusStatRewards().getOrDefault(stat.getId(), 0)));
            sf.setPrefWidth(100);
            statFields.put(stat.getId(), sf);
            grid.add(sf, 1, r);
            r++;
        }

        content.getChildren().add(grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try { appStats.setFocusRewardPoints(Math.max(0, Integer.parseInt(ptsField.getText().trim()))); } catch(Exception ignore){}

                Map<String, Integer> newRewards = new HashMap<>();
                for (CustomStat stat : appStats.getCustomStats()) {
                    try {
                        int val = Integer.parseInt(statFields.get(stat.getId()).getText().trim());
                        if (val > 0) newRewards.put(stat.getId(), val);
                    } catch (Exception ignore){}
                }
                appStats.setFocusStatRewards(newRewards);
                StorageManager.saveStats(appStats);
            }
        });
    }

    public void refreshTasks() {
        if (urgeBtn != null) {
            urgeBtn.setVisible(appStats.isEnableUrgeButton());
            urgeBtn.setManaged(appStats.isEnableUrgeButton());
        }

        TaskItem selected = taskSelector.getValue();
        allFocusableTasks.clear();
        allFocusableTasks.add(null);

        for (TaskItem task : globalDatabase) {
            if (!task.isFinished() && !task.isArchived()) {
                boolean canFocus = false;
                if (task.getSectionId() != null) {
                    for (SectionConfig config : appStats.getSections()) {
                        if (config.getId().equals(task.getSectionId()) && config.isTrackTime()) {
                            canFocus = true;
                            break;
                        }
                    }
                } else if (task.getOriginModule() == OriginModule.WORK || task.getOriginModule() == OriginModule.QUICK) {
                    canFocus = true;
                }
                if (canFocus) allFocusableTasks.add(task);
            }
        }

        applyTaskFilter(taskSearchField.getText());

        if (selected != null && taskSelector.getItems().contains(selected)) {
            taskSelector.setValue(selected);
        } else {
            taskSelector.setValue(null);
        }
    }

    private void applyTaskFilter(String query) {
        TaskItem currentlySelected = taskSelector.getValue();
        taskSelector.getItems().clear();

        if (query == null || query.trim().isEmpty()) {
            taskSelector.getItems().addAll(allFocusableTasks);
        } else {
            String lowerQuery = query.toLowerCase();
            for (TaskItem task : allFocusableTasks) {
                if (task == null) {
                    taskSelector.getItems().add(null);
                } else {
                    String taskText = task.getTextContent().toLowerCase();
                    String secName = getSectionName(task).toLowerCase();

                    if (taskText.contains(lowerQuery) || secName.contains(lowerQuery)) {
                        taskSelector.getItems().add(task);
                    }
                }
            }
        }

        if (currentlySelected != null && taskSelector.getItems().contains(currentlySelected)) {
            taskSelector.setValue(currentlySelected);
        } else if (!taskSelector.getItems().isEmpty()) {
            taskSelector.setValue(taskSelector.getItems().get(0));
        }

        // Only auto-open the dropdown while the user is actively narrowing with a query — popping it
        // open on every keystroke (including when the field is cleared) was disruptive.
        if (taskSearchField.isFocused() && query != null && !query.trim().isEmpty() && !taskSelector.getItems().isEmpty()) {
            taskSelector.show();
        }
    }

    private String getSectionName(TaskItem item) {
        if (item == null) return "";
        if (item.getSectionId() != null) {
            for (SectionConfig c : appStats.getSections()) {
                if (c.getId().equals(item.getSectionId())) {
                    return c.getName();
                }
            }
        }
        return "Task";
    }

    private void setupTimerLogic() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {

            if (isFocusMode && appStats.getFocusInactivityThreshold() > 0) {
                long inactiveMillis = System.currentTimeMillis() - GlobalActivityTracker.getLastActivityTime();
                long thresholdMillis = appStats.getFocusInactivityThreshold() * 60 * 1000L;

                if (inactiveMillis > thresholdMillis) {
                    timeline.pause();
                    sessionWasPaused = true; // Mark as interrupted

                    if (taskSelector.getValue() != null) {
                        int elapsed = lastTrackedTimeLeft - timeLeft;
                        if (elapsed > 0) taskSelector.getValue().addTimeSpent(elapsed);
                        lastTrackedTimeLeft = timeLeft;
                        StorageManager.saveTasks(globalDatabase);
                        if (refreshCallback != null) refreshCallback.run();
                    }

                    startPauseBtn.setText("RESUME");
                    startPauseBtn.setStyle("-fx-background-color: #FF8C00; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 12 30; -fx-font-size: 14px; -fx-background-radius: 5;");

                    SystemTrayManager.pushNotification("Focus Paused", "Timer paused due to " + appStats.getFocusInactivityThreshold() + " minutes of inactivity.");
                    GlobalActivityTracker.resetActivityTime();
                    return;
                }
            }

            timeLeft--; updateDisplay();

            if (isFocusMode && taskSelector.getValue() != null) {
                TaskItem activeTask = taskSelector.getValue();
                if (activeTask.getTargetTimeMinutes() > 0 && !activeTask.isFinished()) {
                    int elapsedThisSession = lastTrackedTimeLeft - timeLeft;
                    int totalTracked = activeTask.getTimeSpentSeconds() + elapsedThisSession;

                    if (totalTracked >= activeTask.getTargetTimeMinutes() * 60) {

                        activeTask.addTimeSpent(elapsedThisSession);
                        lastTrackedTimeLeft = timeLeft;

                        SectionConfig taskConfig = appStats.getSections().stream().filter(c -> c.getId().equals(activeTask.getSectionId())).findFirst().orElse(null);
                        // handleTaskCompletion already applies stat rewards (via finalizeCompletion),
                        // honouring any confirmation prompt. We only persist here — re-running
                        // processRPGStats would double-grant XP and apply it even if the prompt was declined.
                        com.raeden.ors_to_do.modules.dependencies.ui.utils.TaskActionHandler.handleTaskCompletion(activeTask, taskConfig, appStats, globalDatabase, refreshCallback, null);
                        if (taskConfig != null && taskConfig.isEnableStatsSystem()) {
                            StorageManager.saveStats(appStats);
                        }

                        SystemTrayManager.pushNotification("Timed Task Complete!", "Target focus time reached for: " + activeTask.getTextContent());
                        taskSelector.setValue(null);
                    }
                }
            }

            if (timeLeft <= 0) {
                timeline.pause();

                // --- NEW: Perfect Focus Reward Logic ---
                if (isFocusMode) {
                    if (!sessionWasPaused) {
                        appStats.setLifetimeFullFocusSessions(appStats.getLifetimeFullFocusSessions() + 1);

                        boolean earnedRewards = false;
                        if (appStats.getFocusRewardPoints() > 0) {
                            appStats.setGlobalScore(appStats.getGlobalScore() + appStats.getFocusRewardPoints());
                            appStats.recordStatChange(com.raeden.ors_to_do.dependencies.models.StatLedgerEntry.GLOBAL_SCORE,
                                    appStats.getFocusRewardPoints(), "pts", "Focus Session");
                            earnedRewards = true;
                        }

                        for (Map.Entry<String, Integer> entry : appStats.getFocusStatRewards().entrySet()) {
                            CustomStat stat = appStats.getCustomStats().stream().filter(s -> s.getId().equals(entry.getKey())).findFirst().orElse(null);
                            if (stat != null && entry.getValue() != null && entry.getValue() != 0) {
                                int applied = stat.gain(entry.getValue(), stat.getEffectiveMaxCap(appStats.getActiveDebuffs()));
                                stat.setLifetimeEarned(stat.getLifetimeEarned() + applied);
                                appStats.recordStatChange(stat.getName(), entry.getValue(), stat.isUseExp() ? "XP" : "pts", "Focus Session");
                                earnedRewards = true;
                            }
                        }

                        StorageManager.saveStats(appStats);
                        if (refreshCallback != null) refreshCallback.run();

                        if (earnedRewards) {
                            SystemTrayManager.pushNotification("Perfect Focus! 🏆", "Session completed without stopping. Rewards granted!");
                        } else {
                            SystemTrayManager.pushNotification("Perfect Focus!", "Session completed without stopping! Great job.");
                        }
                    } else {
                        SystemTrayManager.pushNotification("Pomodoro Session Complete", "Great job! Take a short break.");
                    }
                } else {
                    SystemTrayManager.pushNotification("Pomodoro Break Over", "Back to work!");
                }

                isFocusMode = !isFocusMode;
                resetTimer(isFocusMode);
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void resetTimer(boolean focus) {
        if (timeline != null) timeline.pause();

        if (this.isFocusMode && taskSelector.getValue() != null) {
            int elapsed = lastTrackedTimeLeft - timeLeft;
            if (elapsed > 0) {
                taskSelector.getValue().addTimeSpent(elapsed);
            }
            StorageManager.saveTasks(globalDatabase);
            if (refreshCallback != null) refreshCallback.run();
        }

        isFocusMode = focus;
        timeLeft = isFocusMode ? timerOptions.getValue() * 60 : 5 * 60;
        lastTrackedTimeLeft = timeLeft;
        sessionWasPaused = false; // Reset pause tracker on fresh start

        statusLabel.setText(isFocusMode ? "FOCUS SESSION" : "SHORT BREAK");
        statusLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-letter-spacing: 2px; -fx-text-fill: " + (isFocusMode ? "#569CD6;" : "#4EC9B0;"));

        timeDisplay.setStyle("-fx-font-size: 85px; -fx-font-weight: bold; -fx-text-fill: " + (isFocusMode ? "#569CD6;" : "#4EC9B0;"));

        startPauseBtn.setText("START");
        startPauseBtn.setStyle("-fx-background-color: " + (isFocusMode ? "#569CD6;" : "#4EC9B0;") + " -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 12 30; -fx-font-size: 14px; -fx-background-radius: 5;");

        updateDisplay();
    }

    private void updateDisplay() {
        int minutes = timeLeft / 60; int seconds = timeLeft % 60;
        timeDisplay.setText(String.format("%02d:%02d", minutes, seconds));
    }
}