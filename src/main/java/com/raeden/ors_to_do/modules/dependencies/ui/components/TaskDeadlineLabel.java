package com.raeden.ors_to_do.modules.dependencies.ui.components;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.TaskActionHandler;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.List;

import static com.raeden.ors_to_do.modules.dependencies.services.SystemTrayManager.pushNotification;

public class TaskDeadlineLabel extends Label {

    public TaskDeadlineLabel(TaskItem task, SectionConfig config, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate, List<Timeline> activeTimelines, int metaFontSize) {
        setPadding(new Insets(0, 10, 0, 0));
        setStyle("-fx-text-fill: #FF6666; -fx-font-weight: bold; -fx-font-size: " + metaFontSize + "px;");

        Runnable updateLabel = () -> {
            java.time.Duration dur = java.time.Duration.between(LocalDateTime.now(), task.getDeadline());
            if (dur.isNegative() || dur.isZero()) {
                setText("🚨 OVERDUE");

                // Shared "missed task" penalty (penalty points + the task's Stat Penalties map).
                if (TaskActionHandler.applyMissPenalty(task, appStats, config)) {
                    TaskActionHandler.evaluateThresholdDebuffs(appStats);
                    StorageManager.saveStats(appStats);
                    StorageManager.saveTasks(globalDatabase);
                    Platform.runLater(() -> {
                        onUpdate.run();
                        pushNotification("Deadline Missed!", "Penalties applied for task: " + task.getTextContent());
                    });
                }
            } else {
                long totalSecs = dur.getSeconds();
                long days = totalSecs / 86400;
                long hours = (totalSecs % 86400) / 3600;
                long mins = (totalSecs % 3600) / 60;
                long secs = totalSecs % 60;
                if (days > 0) setText(String.format("⏳ %dd %02d:%02d:%02d", days, hours, mins, secs));
                else setText(String.format("⏳ %02d:%02d:%02d", hours, mins, secs));
            }
        };

        updateLabel.run();
        Timeline deadlineTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateLabel.run()));
        deadlineTimer.setCycleCount(Animation.INDEFINITE);
        deadlineTimer.play();
        activeTimelines.add(deadlineTimer);
    }
}