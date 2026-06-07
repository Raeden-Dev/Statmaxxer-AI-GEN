package com.raeden.ors_to_do.modules.dependencies.ui.components;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomPriority;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;

public class DynamicInputPanel extends HBox {

    private TextField inputField;
    private TextField prefixField;
    private ComboBox<CustomPriority> priorityBox;

    private SectionConfig config;
    private AppStats appStats;
    private List<TaskItem> globalDatabase;
    private FilterSortHeader filterSortHeader;
    private Runnable refreshListAction;

    public DynamicInputPanel(SectionConfig config, AppStats appStats, List<TaskItem> globalDatabase, FilterSortHeader filterSortHeader, Runnable refreshListAction) {
        super(10);
        this.config = config;
        this.appStats = appStats;
        this.globalDatabase = globalDatabase;
        this.filterSortHeader = filterSortHeader;
        this.refreshListAction = refreshListAction;

        setAlignment(Pos.CENTER);
        setPadding(new Insets(15, 0, 0, 0));

        if (config.isShowPrefix() && !config.isNotesPage()) {
            prefixField = new TextField();
            prefixField.setPromptText("[PREFIX]");
            prefixField.setPrefWidth(80);
            prefixField.getStyleClass().add("input-field");
            getChildren().add(prefixField);
        }

        inputField = new TextField();
        if (config.isNotesPage()) inputField.setPromptText("Enter new note for " + config.getName() + "...");
        else if (config.isRewardsPage()) inputField.setPromptText("Enter new reward...");
        else if (config.isPerkPage()) inputField.setPromptText("Enter new perk name...");
        else inputField.setPromptText("Enter new task for " + config.getName() + "...");

        inputField.getStyleClass().add("input-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        getChildren().add(inputField);

        if (config.isShowPriority() && !config.isNotesPage() && !config.isPerkPage()) {
            priorityBox = new ComboBox<>();
            priorityBox.getItems().addAll(appStats.getCustomPriorities());
            // Default to the second priority ("medium" in the seeded set), but fall back to the
            // first if the user has only one priority defined. Without this guard, deleting
            // priorities down to a single entry crashes the section with IndexOutOfBoundsException.
            int prioSize = appStats.getCustomPriorities().size();
            if (prioSize > 0) {
                priorityBox.setValue(appStats.getCustomPriorities().get(Math.min(1, prioSize - 1)));
            }
            TaskDialogs.setupPriorityBoxColors(priorityBox);
            getChildren().add(priorityBox);
        }

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("action-btn");
        Button clearBtn = new Button("Clear");
        getChildren().addAll(addBtn, clearBtn);

        addBtn.setOnAction(e -> addTask());
        inputField.setOnAction(e -> addTask());
        clearBtn.setOnAction(e -> { inputField.clear(); if (prefixField != null) prefixField.clear(); });
    }

    private void addTask() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        CustomPriority defaultPrio = null;
        if (config.isShowPriority() && !config.isNotesPage() && !config.isPerkPage() && priorityBox != null) defaultPrio = priorityBox.getValue();
        else if (config.isShowPriority() && !config.isNotesPage() && !config.isPerkPage() && !appStats.getCustomPriorities().isEmpty()) defaultPrio = appStats.getCustomPriorities().get(0);

        TaskItem newTask = new TaskItem(text, defaultPrio, config.getId());

        if (config.isShowPrefix() && !config.isNotesPage() && prefixField != null) {
            String pText = prefixField.getText().trim();
            if (!pText.isEmpty()) {
                if (!pText.startsWith("[")) pText = "[" + pText;
                if (!pText.endsWith("]")) pText = pText + "]";
                newTask.setPrefix(pText.toUpperCase());
                newTask.setPrefixColor("#4EC9B0");
            }
        }
        if (config.isShowTaskType() && !config.isNotesPage() && !config.isPerkPage()) newTask.setTaskType("General");

        // Flag challenge-page tasks here so we never rely on "last item in the global database",
        // which races with other sections that might be appending tasks concurrently.
        if (config.isChallengePage()) newTask.setChallengeCard(true);

        globalDatabase.add(newTask);

        if (filterSortHeader.getSortMode().equals("Most Recent")) refreshListAction.run();
        else { filterSortHeader.forceSortMode("Most Recent"); refreshListAction.run(); }

        inputField.clear();
        if (prefixField != null) prefixField.clear();
        StorageManager.saveTasks(globalDatabase);
    }
}