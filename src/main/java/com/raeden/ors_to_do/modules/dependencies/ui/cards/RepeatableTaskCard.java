package com.raeden.ors_to_do.modules.dependencies.ui.cards;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.Debuff;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.i18n.Lang;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.Design;
import com.raeden.ors_to_do.modules.dependencies.ui.components.SubTaskRenderer;
import com.raeden.ors_to_do.modules.dependencies.ui.components.TaskStatsMiniCard;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import com.raeden.ors_to_do.modules.dependencies.ui.menus.TaskContextMenu;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.TaskActionHandler;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.TaskCardStyleHelper;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.BiConsumer;

import static com.raeden.ors_to_do.modules.dependencies.services.SystemTrayManager.pushNotification;

public class RepeatableTaskCard extends VBox {

    public RepeatableTaskCard(TaskItem task, SectionConfig config, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate, List<Timeline> activeTimelines, BiConsumer<String, String> onReorder) {
        super();
        this.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(TaskDialogs.getCheckboxThemeCss(appStats.getCheckboxTheme()).getBytes()));

        int baseFontSize = appStats.getTaskFontSize();
        int metaFontSize = Math.max(10, baseFontSize - 2);

        VBox primaryCard = new VBox();
        primaryCard.getStyleClass().add("task-row");
        String originalStyle = TaskCardStyleHelper.getBaseStyle(task, config, appStats, false);
        primaryCard.setStyle(originalStyle);
        TaskCardStyleHelper.setupDragAndDrop(this, primaryCard, task, originalStyle, onReorder);

        HBox mainRow = new HBox(10);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        mainRow.setPadding(new Insets(10));

        // --- FIXED: Apply both Time Lock AND Section Completion Lock to double-click ---
        mainRow.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                int lockHours = appStats.getPreventEditingHours();
                boolean isNoteMode = config != null && config.isNotesPage();
                boolean isRewardMode = config != null && config.isRewardsPage();

                boolean isTimeLocked = !(isNoteMode || isRewardMode) && lockHours > 0 && java.time.LocalDateTime.now().isAfter(task.getDateCreated().plusHours(lockHours));
                boolean isCompletionLocked = config != null && config.isLockCompletedTasks() && task.isFinished();

                if (isTimeLocked) {
                    Design.warn(Lang.EDIT_LOCKED_HEADER, Lang.EDIT_LOCKED_TASK_BODY, lockHours);
                } else if (isCompletionLocked) {
                    Design.warn(Lang.EDIT_LOCKED_HEADER, Lang.EDIT_LOCKED_COMPLETED_BODY);
                } else {
                    TaskDialogs.showEditDialog(task, config, appStats, globalDatabase, onUpdate);
                }
            }
        });

        HBox metaBox = new HBox(7);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        if (config != null && config.isEnableIcons() && task.getIconSymbol() != null && !task.getIconSymbol().equals("None")) {
            Label icon = new Label(task.getIconSymbol());
            icon.setMinWidth(Region.USE_PREF_SIZE);
            icon.setStyle("-fx-text-fill: " + (task.getIconColor() != null ? task.getIconColor() : "#FFFFFF") + "; -fx-font-size: " + (baseFontSize + 2) + "px;");
            metaBox.getChildren().add(icon);
        }

        Region sideRect = new Region();
        sideRect.setMinWidth(5); sideRect.setPrefWidth(5);
        sideRect.setPrefHeight(25); sideRect.setMaxHeight(25);

        String fillColor = "#FFFFFF";
        if (config != null && config.isEnableTaskStyling() && task.getCustomSideboxColor() != null && !task.getCustomSideboxColor().equals("transparent")) fillColor = task.getCustomSideboxColor();
        else if (config != null && config.isShowPriority() && task.getPriority() != null && task.getPriority().getColorHex() != null) fillColor = task.getPriority().getColorHex();
        else if (config != null && config.isShowPrefix() && appStats.isMatchDailyRectColor() && task.getPrefixColor() != null) fillColor = task.getPrefixColor();

        sideRect.setStyle("-fx-background-color: " + fillColor + "; -fx-background-radius: 3;");
        metaBox.getChildren().add(sideRect);

        Label textLabel = new Label(task.getTextContent());
        textLabel.setStyle("-fx-font-size: " + baseFontSize + "px; -fx-text-fill: #E0E0E0;");

        textLabel.setWrapText(true);
        textLabel.setMinWidth(10);
        textLabel.setMinHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(textLabel, Priority.ALWAYS);
        textLabel.setMaxWidth(Double.MAX_VALUE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TaskStatsMiniCard statsMiniCard = new TaskStatsMiniCard(task, config, appStats, false);
        Button eyeBtn = null;
        if (statsMiniCard.hasAnyStats()) {
            eyeBtn = new Button("✦");
            eyeBtn.setMinWidth(Region.USE_PREF_SIZE);
            eyeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: " + baseFontSize + "px; -fx-padding: 0 10 0 0; -fx-text-fill: " + (task.isStatsExpanded() ? "#569CD6" : "#AAAAAA") + ";");

            Button finalEyeBtn = eyeBtn;
            eyeBtn.setOnAction(e -> {
                task.setStatsExpanded(!task.isStatsExpanded());
                statsMiniCard.setVisible(task.isStatsExpanded());
                statsMiniCard.setManaged(task.isStatsExpanded());
                finalEyeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: " + baseFontSize + "px; -padding: 0 10 0 0; -fx-text-fill: " + (task.isStatsExpanded() ? "#569CD6" : "#AAAAAA") + ";");
                StorageManager.saveTasks(globalDatabase);
            });
        }

        Label counterLbl = new Label(String.valueOf(task.getCurrentCount()));
        counterLbl.setMinWidth(Region.USE_PREF_SIZE);
        counterLbl.setStyle("-fx-text-fill: #4EC9B0; -fx-font-size: " + metaFontSize + "px; -fx-font-weight: bold; -fx-background-color: #1A332E; -fx-padding: 3 8; -fx-background-radius: 5;");

        Label repeatIcon = new Label("🔁");
        repeatIcon.setMinWidth(Region.USE_PREF_SIZE);
        repeatIcon.setStyle("-fx-text-fill: #569CD6; -fx-font-size: " + (baseFontSize + 2) + "px;");

        Button incBtn = new Button("+");
        incBtn.setMinWidth(Region.USE_PREF_SIZE);
        incBtn.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-border-radius: 3; -fx-background-radius: 3;");

        incBtn.setOnAction(e -> {
            task.setCurrentCount(task.getCurrentCount() + 1);

            TaskActionHandler.applyInflictedDebuffs(task, appStats);

            if (appStats.getActiveDebuffs() != null && !appStats.getActiveDebuffs().isEmpty()) {
                boolean cleansed = false;
                java.util.Iterator<Debuff> it = appStats.getActiveDebuffs().iterator();
                while (it.hasNext()) {
                    Debuff d = it.next();
                    if (d.getRequiredTaskCompletions() > 0) {
                        d.setCurrentTaskCompletions(d.getCurrentTaskCompletions() + 1);
                        if (d.getCurrentTaskCompletions() >= d.getRequiredTaskCompletions()) {
                            it.remove();
                            cleansed = true;
                            pushNotification(Lang.NOTIFY_DEBUFF_CLEANSED_TITLE.get(), Lang.NOTIFY_DEBUFF_CLEANSED_BODY.get(d.getName()));
                        }
                    }
                }
                if (cleansed) StorageManager.saveStats(appStats);
            }

            if (config != null && config.isEnableStatsSystem() && (task.getRewardPoints() > 0 || !task.getStatRewards().isEmpty() || !task.getStatCapRewards().isEmpty() || !task.getStatCosts().isEmpty())) {
                TaskActionHandler.processRPGStats(task, appStats, true);
                appStats.setGlobalScore(appStats.getGlobalScore() + task.getRewardPoints());
            }

            StorageManager.saveStats(appStats);
            StorageManager.saveTasks(globalDatabase);
            onUpdate.run();
        });

        mainRow.getChildren().addAll(metaBox, textLabel, spacer);
        if (eyeBtn != null) mainRow.getChildren().add(eyeBtn);
        mainRow.getChildren().addAll(counterLbl, repeatIcon, incBtn);

        SubTaskRenderer subTaskBox = new SubTaskRenderer(task, config, appStats, globalDatabase, onUpdate);

        primaryCard.getChildren().addAll(mainRow, subTaskBox);
        getChildren().addAll(primaryCard, statsMiniCard);

        ContextMenu contextMenu = TaskContextMenu.build(task, config, appStats, globalDatabase, onUpdate, null);

        MenuItem resetItem = new MenuItem(Lang.MENU_RESET_PROGRESS.get());
        resetItem.setOnAction(e -> {
            task.setCurrentCount(0);
            StorageManager.saveTasks(globalDatabase);
            onUpdate.run();
        });
        contextMenu.getItems().add(1, resetItem);

        this.setOnContextMenuRequested(e -> {
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }
}