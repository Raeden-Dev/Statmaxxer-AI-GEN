package com.raeden.ors_to_do.modules.dependencies.settings;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomPriority;
import com.raeden.ors_to_do.dependencies.models.DailyTemplate;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.SubTask;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.services.SystemTrayManager;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TemplateManagerPanel extends VBox {
    private AppStats appStats;
    private List<TaskItem> globalDatabase;
    private Runnable refreshCallback;
    private ComboBox<SectionConfig> sectionBox;
    private VBox templateList;

    public TemplateManagerPanel(AppStats appStats, List<TaskItem> globalDatabase, Runnable refreshCallback) {
        super(15);
        this.appStats = appStats;
        this.globalDatabase = globalDatabase;
        this.refreshCallback = refreshCallback;
        setStyle("-fx-border-color: #3E3E42; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5;");

        Label header = new Label("Auto-Generating Tasks (Templates)");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

        HBox controlBox = new HBox(10);
        controlBox.setAlignment(Pos.CENTER_LEFT);

        sectionBox = new ComboBox<>();
        setupSectionBox();
        sectionBox.setOnAction(e -> refreshList());

        Button addBtn = new Button("Add Template");
        addBtn.setStyle("-fx-background-color: #0E639C; -fx-text-fill: white; -fx-cursor: hand;");
        addBtn.setOnAction(e -> {
            if (sectionBox.getValue() != null) {
                TemplateEditDialog.show(null, sectionBox.getValue(), appStats, this::saveAndRefresh);
            }
        });

        Button generateBtn = new Button("Generate Tasks Now");
        generateBtn.setStyle("-fx-background-color: #22543D; -fx-text-fill: white; -fx-cursor: hand;");
        generateBtn.setOnAction(e -> generateTasks());

        HBox.setHgrow(sectionBox, Priority.ALWAYS);
        sectionBox.setMaxWidth(Double.MAX_VALUE);

        controlBox.getChildren().addAll(new Label("Select Section:"), sectionBox, addBtn, generateBtn);

        templateList = new VBox(10);
        getChildren().addAll(header, controlBox, templateList);
        refreshList();
    }

    public void refreshSectionSelector() {
        SectionConfig current = sectionBox.getValue();
        setupSectionBox();
        // Preserve the current selection if it still exists; otherwise default to "(Hide)" (null)
        // so the panel reopens collapsed rather than auto-expanding a section's template list.
        if (current != null && sectionBox.getItems().contains(current)) sectionBox.setValue(current);
        else sectionBox.setValue(null);
        refreshList();
    }

    private void setupSectionBox() {
        sectionBox.getItems().clear();
        // "(Hide)" sentinel (null) lets the user collapse this panel so it isn't cramped with a
        // long list of templates from a previously-selected section.
        sectionBox.getItems().add(null);
        for (SectionConfig config : appStats.getSections()) {
            if (config.getResetIntervalHours() > 0) sectionBox.getItems().add(config);
        }
        sectionBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(SectionConfig item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "(Hide)" : item.getName()));
            }
        });
        sectionBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(SectionConfig item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "(Hide)" : item.getName()));
            }
        });
    }

    private void refreshList() {
        templateList.getChildren().clear();
        SectionConfig selected = sectionBox.getValue();

        // "(Hide)" / nothing selected → keep the panel collapsed (empty) so it doesn't take space.
        if (selected == null) { return; }
        if (selected.getAutoAddTemplates().isEmpty()) { templateList.getChildren().add(new Label("No templates for this section.")); return; }

        List<DailyTemplate> templates = selected.getAutoAddTemplates();
        for (int i = 0; i < templates.size(); i++) {
            templateList.getChildren().add(new TemplateRow(templates.get(i), i, templates, selected, appStats, this::saveAndRefresh));
        }
    }

    private void generateTasks() {
        SectionConfig selected = sectionBox.getValue();
        if (selected == null || selected.getAutoAddTemplates().isEmpty()) return;

        CustomPriority fallbackPrio = appStats.getCustomPriorities().isEmpty() ? null : appStats.getCustomPriorities().get(0);

        for (DailyTemplate template : selected.getAutoAddTemplates()) {
            TaskItem newTask = new TaskItem(template.getText(), fallbackPrio, selected.getId());

            // --- FIXED: Formats prefix with brackets and safely injects properties ---
            if (selected.isEnableIcons() && template.getIconSymbol() != null && !template.getIconSymbol().equals("None")) {
                newTask.setIconSymbol(template.getIconSymbol());
                newTask.setIconColor(template.getIconColor());
            }
            if (selected.isShowPrefix() && template.getPrefix() != null && !template.getPrefix().isEmpty()) {
                String pText = template.getPrefix().trim();
                if (!pText.isEmpty()) {
                    if (!pText.startsWith("[")) pText = "[" + pText;
                    if (!pText.endsWith("]")) pText = pText + "]";
                    newTask.setPrefix(pText.toUpperCase());
                    newTask.setPrefixColor(template.getPrefixColor());
                }
            }
            if (selected.isShowPriority() && template.getPriorityName() != null && !template.isOptional()) {
                appStats.getCustomPriorities().stream().filter(p -> p.getName().equals(template.getPriorityName())).findFirst().ifPresent(newTask::setPriority);
            }
            if (selected.isShowTaskType() && template.getTaskType() != null) newTask.setTaskType(template.getTaskType());
            if (selected.isEnableScore()) {
                newTask.setRewardPoints(template.getRewardPoints());
                newTask.setPenaltyPoints(template.getPenaltyPoints());
            }
            if (selected.isEnableSubTasks() && template.getSubTaskLines() != null) {
                for (String st : template.getSubTaskLines()) {
                    if (!st.trim().isEmpty()) newTask.getSubTasks().add(new SubTask(st.trim()));
                }
            }

            if (template.getBgColor() != null) newTask.setColorHex(template.getBgColor());
            if (template.getCustomOutlineColor() != null) newTask.setCustomOutlineColor(template.getCustomOutlineColor());
            if (template.getCustomSideboxColor() != null) newTask.setCustomSideboxColor(template.getCustomSideboxColor());

            newTask.setRepeatingMode(template.isRepeatingMode());
            newTask.setRepetitionCount(template.getRepetitionCount());
            newTask.setOptional(template.isOptional());

            if (selected.isEnableStatsSystem()) {
                if (template.getStatRewards() != null) newTask.setStatRewards(new HashMap<>(template.getStatRewards()));
                if (template.getStatCapRewards() != null) newTask.setStatCapRewards(new HashMap<>(template.getStatCapRewards()));
                if (template.getStatCosts() != null) newTask.setStatCosts(new HashMap<>(template.getStatCosts()));
                if (template.getStatPenalties() != null) newTask.setStatPenalties(new HashMap<>(template.getStatPenalties()));
                if (template.getStatRequirements() != null) newTask.setStatRequirements(new HashMap<>(template.getStatRequirements()));
                if (template.getInflictedDebuffIds() != null) newTask.setInflictedDebuffIds(new ArrayList<>(template.getInflictedDebuffIds()));
            }

            globalDatabase.add(newTask);
        }

        StorageManager.saveTasks(globalDatabase);
        refreshCallback.run();
        SystemTrayManager.pushNotification("Tasks Generated", "Manually generated " + selected.getAutoAddTemplates().size() + " tasks for " + selected.getName());
    }

    private void saveAndRefresh() {
        StorageManager.saveStats(appStats);
        refreshList();
    }
}