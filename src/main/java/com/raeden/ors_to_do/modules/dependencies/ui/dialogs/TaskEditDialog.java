package com.raeden.ors_to_do.modules.dependencies.ui.dialogs;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.ui.forms.TaskMetaForm;
import com.raeden.ors_to_do.modules.dependencies.ui.forms.TaskRPGForm;
import com.raeden.ors_to_do.modules.dependencies.ui.forms.TaskStyleForm;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Border;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs.styleDialog;

public class TaskEditDialog {

    public static void showEditDialog(TaskItem task, SectionConfig config, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(config != null && config.isNotesPage() ? "Edit Note" : (config != null && config.isRewardsPage() ? "Edit Reward" : "Edit Task"));
        styleDialog(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(10);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(170);
        col1.setPrefWidth(170);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        AtomicInteger rowIdx = new AtomicInteger(0);

        // 1. Core Text & Link Controls
        TextArea contentField = new TextArea(task.getTextContent() != null ? task.getTextContent() : "");
        contentField.setMaxWidth(Double.MAX_VALUE);
        contentField.setWrapText(true);
        contentField.setPrefRowCount(config != null && config.isNotesPage() ? 6 : 2);

        Label contentLabel = new Label(config != null && config.isNotesPage() ? "Note Text:" : (config != null && config.isRewardsPage() ? "Reward Name:" : "Content:"));
        GridPane.setValignment(contentLabel, javafx.geometry.VPos.TOP);
        grid.add(contentLabel, 0, rowIdx.get());
        grid.add(contentField, 1, rowIdx.getAndIncrement());

        CheckBox linkCardCheck = new CheckBox("Is Link Card?");
        linkCardCheck.setStyle("-fx-text-fill: white;");

        boolean hasSubTasks = task.getSubTasks() != null && !task.getSubTasks().isEmpty();
        boolean sectionAllowsLinks = config == null || config.isEnableLinkCards();

        // Link cards may now coexist with sub-tasks / sub-task links, so sub-tasks no longer
        // disable the toggle — only sections that forbid link cards or optional tasks do.
        if (!sectionAllowsLinks || task.isOptional()) {
            linkCardCheck.setDisable(true);
            linkCardCheck.setSelected(false);
            linkCardCheck.setText("Is Link Card? (Disabled)");
        } else {
            linkCardCheck.setSelected(task.isLinkCard());
        }

        TextField linkPathField = new TextField(task.getLinkActionPath() != null ? task.getLinkActionPath() : "");
        linkPathField.setPromptText("Enter URL, Folder path, or App path (.exe)");
        linkPathField.setMaxWidth(Double.MAX_VALUE);
        linkPathField.setDisable(!linkCardCheck.isSelected());
        linkCardCheck.setOnAction(e -> linkPathField.setDisable(!linkCardCheck.isSelected()));

        grid.add(linkCardCheck, 0, rowIdx.get());
        grid.add(linkPathField, 1, rowIdx.getAndIncrement());

        // --- NEW: Repeating Task Toggle & Repetition Spinner ---
        CheckBox repeatingTaskCheck = new CheckBox("Make Repeating Task?");
        repeatingTaskCheck.setStyle("-fx-text-fill: #4EC9B0; -fx-font-weight: bold;");

        Spinner<Integer> repCounterSpinner = new Spinner<>(0, 999999, Math.max(0, task.getRepetitionCount()));
        repCounterSpinner.setEditable(true);
        repCounterSpinner.setPrefWidth(100);

        HBox repBox = new HBox(10, new Label("Set Counter:"), repCounterSpinner);
        repBox.setAlignment(Pos.CENTER_LEFT);

        boolean sectionAllowsRepeating = config != null && config.isAllowRepeatingTasks();

        if (!sectionAllowsRepeating || task.isLinkCard() || hasSubTasks) {
            repeatingTaskCheck.setDisable(true);
            repeatingTaskCheck.setSelected(false);
            repeatingTaskCheck.setText("Make Repeating Task? (Disabled)");
            repBox.setVisible(false);
        } else {
            repeatingTaskCheck.setSelected(task.isRepeatingMode());
            repBox.setVisible(task.isRepeatingMode());
        }

        grid.add(repeatingTaskCheck, 0, rowIdx.get());
        grid.add(repBox, 1, rowIdx.getAndIncrement());

        // --- NEW: Description Card toggle + copied content (only when the section opts in) ---
        boolean sectionAllowsDescription = config != null && config.isEnableDescriptionCards();
        CheckBox descCardCheck = new CheckBox("Is Description Card?");
        descCardCheck.setStyle("-fx-text-fill: #DCDCAA; -fx-font-weight: bold;");
        TextArea descContentField = new TextArea(task.getDescriptionContent());
        descContentField.setWrapText(true);
        descContentField.setPrefRowCount(4);
        descContentField.setMaxWidth(Double.MAX_VALUE);
        descContentField.setPromptText("Text copied to the clipboard when the card's Copy button is clicked.");

        if (task.isLinkCard() || hasSubTasks || task.isRepeatingMode()) {
            descCardCheck.setDisable(true);
            descCardCheck.setSelected(false);
        } else {
            descCardCheck.setSelected(task.isDescriptionCard());
        }
        descContentField.setDisable(!descCardCheck.isSelected());

        if (sectionAllowsDescription) {
            GridPane.setValignment(descCardCheck, javafx.geometry.VPos.TOP);
            grid.add(descCardCheck, 0, rowIdx.get());
            grid.add(descContentField, 1, rowIdx.getAndIncrement());
            // If the task is already a description card, the mutually-exclusive modes start locked.
            if (descCardCheck.isSelected()) {
                linkCardCheck.setSelected(false); linkCardCheck.setDisable(true);
                repeatingTaskCheck.setSelected(false); repeatingTaskCheck.setDisable(true); repBox.setVisible(false);
            }
        }

        // --- NEW: Notes Card toggle (only when the section opts in via "Allow Notes") ---
        boolean sectionAllowsNotes = config != null && config.isAllowNotes() && !config.isNotesPage();
        CheckBox noteCardCheck = new CheckBox("Is Notes Card?");
        noteCardCheck.setStyle("-fx-text-fill: #C586C0; -fx-font-weight: bold;");
        noteCardCheck.setSelected(sectionAllowsNotes && task.isNoteCard());
        if (sectionAllowsNotes) {
            Label noteHint = new Label("Behaves like a note: a pin replaces the checkbox and it never counts as completed.");
            noteHint.setWrapText(true);
            noteHint.setStyle("-fx-text-fill: #858585; -fx-font-size: 11px;");
            GridPane.setValignment(noteCardCheck, javafx.geometry.VPos.TOP);
            grid.add(noteCardCheck, 0, rowIdx.get());
            grid.add(noteHint, 1, rowIdx.getAndIncrement());
            // A note card starts with the mutually-exclusive modes locked off.
            if (noteCardCheck.isSelected()) {
                linkCardCheck.setSelected(false); linkCardCheck.setDisable(true);
                repeatingTaskCheck.setSelected(false); repeatingTaskCheck.setDisable(true); repBox.setVisible(false);
                descCardCheck.setSelected(false); descCardCheck.setDisable(true);
            }
        }

        // --- NEW: Category field (only shown when the section opts in via "Enable Categories") ---
        TextField categoryField = null;
        if (config != null && config.isEnableCategories()) {
            categoryField = new TextField(task.getCategoryName() != null ? task.getCategoryName() : "");
            categoryField.setPromptText(com.raeden.ors_to_do.i18n.Lang.CATEGORY_PROMPT.get());

            // Build a small suggestions row so users don't have to retype existing categories.
            java.util.List<String> existing = new java.util.ArrayList<>();
            for (TaskItem other : globalDatabase) {
                if (other == task) continue;
                if (other.getSectionId() == null || !other.getSectionId().equals(config.getId())) continue;
                String c = other.getCategoryName();
                if (c != null && !c.trim().isEmpty() && !existing.contains(c)) existing.add(c);
            }
            javafx.scene.layout.FlowPane suggestionPane = new javafx.scene.layout.FlowPane(5, 5);
            for (String c : existing) {
                Button chip = new Button(c);
                chip.setStyle("-fx-background-color: #2D2D30; -fx-text-fill: #DCDCAA; -fx-border-color: #555555; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 1 8;");
                TextField finalCatField = categoryField;
                chip.setOnAction(e -> finalCatField.setText(c));
                suggestionPane.getChildren().add(chip);
            }

            javafx.scene.layout.VBox catBox = new javafx.scene.layout.VBox(4, categoryField, suggestionPane);
            grid.add(new Label(com.raeden.ors_to_do.i18n.Lang.CATEGORY_LABEL.get()), 0, rowIdx.get());
            grid.add(catBox, 1, rowIdx.getAndIncrement());
        }
        final TextField categoryFieldFinal = categoryField;

        // Ensure Mutually Exclusive Link vs Repeating vs Description vs Notes Logic
        linkCardCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                repeatingTaskCheck.setSelected(false);
                repeatingTaskCheck.setDisable(true);
                repBox.setVisible(false);
                descCardCheck.setSelected(false);
                descCardCheck.setDisable(true);
                noteCardCheck.setSelected(false);
                noteCardCheck.setDisable(true);
            } else {
                if (sectionAllowsRepeating && !hasSubTasks) repeatingTaskCheck.setDisable(false);
                if (sectionAllowsDescription && !hasSubTasks) descCardCheck.setDisable(false);
                if (sectionAllowsNotes) noteCardCheck.setDisable(false);
            }
        });

        repeatingTaskCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            repBox.setVisible(newVal);
            if (newVal) {
                linkCardCheck.setSelected(false);
                linkCardCheck.setDisable(true);
                descCardCheck.setSelected(false);
                descCardCheck.setDisable(true);
                noteCardCheck.setSelected(false);
                noteCardCheck.setDisable(true);
            } else {
                if (sectionAllowsLinks && !task.isOptional()) linkCardCheck.setDisable(false);
                if (sectionAllowsDescription && !hasSubTasks) descCardCheck.setDisable(false);
                if (sectionAllowsNotes) noteCardCheck.setDisable(false);
            }
        });

        descCardCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            descContentField.setDisable(!newVal);
            if (newVal) {
                linkCardCheck.setSelected(false); linkCardCheck.setDisable(true);
                repeatingTaskCheck.setSelected(false); repeatingTaskCheck.setDisable(true); repBox.setVisible(false);
                noteCardCheck.setSelected(false); noteCardCheck.setDisable(true);
            } else {
                if (sectionAllowsLinks && !task.isOptional()) linkCardCheck.setDisable(false);
                if (sectionAllowsRepeating && !hasSubTasks) repeatingTaskCheck.setDisable(false);
                if (sectionAllowsNotes) noteCardCheck.setDisable(false);
            }
        });

        noteCardCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                linkCardCheck.setSelected(false); linkCardCheck.setDisable(true);
                repeatingTaskCheck.setSelected(false); repeatingTaskCheck.setDisable(true); repBox.setVisible(false);
                descCardCheck.setSelected(false); descCardCheck.setDisable(true);
            } else {
                if (sectionAllowsLinks && !task.isOptional()) linkCardCheck.setDisable(false);
                if (sectionAllowsRepeating && !hasSubTasks) repeatingTaskCheck.setDisable(false);
                if (sectionAllowsDescription && !hasSubTasks) descCardCheck.setDisable(false);
            }
        });

        // 2. Initialize Helper Forms
        TaskStyleForm styleForm = new TaskStyleForm();
        TaskMetaForm metaForm = new TaskMetaForm();
        TaskRPGForm rpgForm = new TaskRPGForm();

        // 3. Delegate UI Building
        styleForm.buildUI(grid, rowIdx, task, config);

        if (config == null || !config.isNotesPage()) {
            metaForm.buildUI(grid, rowIdx, task, config, appStats, globalDatabase);
            rpgForm.buildUI(grid, rowIdx, task, config, appStats);
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        TaskDialogs.styleScrollPane(scrollPane, 600, 550);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TaskDialogs.installConfirmCancelShortcuts(dialog);

        // 4. Handle Save Execution
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {

                String typedContent = contentField.getText() != null ? contentField.getText().trim() : "";
                if (typedContent.isEmpty() && linkCardCheck.isSelected() && !linkPathField.getText().trim().isEmpty()) {
                    typedContent = linkPathField.getText().trim();
                }
                task.setTextContent(typedContent);

                if (linkCardCheck.isSelected()) {
                    task.setLinkCard(true);
                    task.setLinkActionPath(linkPathField.getText().trim());
                } else {
                    task.setLinkCard(false);
                    task.setLinkActionPath("");
                }

                task.setRepeatingMode(repeatingTaskCheck.isSelected());

                if (repeatingTaskCheck.isSelected()) {
                    task.setRepetitionCount(repCounterSpinner.getValue());
                }

                if (sectionAllowsDescription) {
                    task.setDescriptionCard(descCardCheck.isSelected());
                    task.setDescriptionContent(descContentField.getText() == null ? "" : descContentField.getText());
                }

                if (sectionAllowsNotes) {
                    task.setNoteCard(noteCardCheck.isSelected());
                }

                if (categoryFieldFinal != null) {
                    String typed = categoryFieldFinal.getText() != null ? categoryFieldFinal.getText().trim() : "";
                    task.setCategoryName(typed.isEmpty() ? null : typed);
                }

                styleForm.applyTo(task);

                if (config == null || !config.isNotesPage()) {
                    metaForm.applyTo(task);
                    rpgForm.applyTo(task);
                }

                StorageManager.saveTasks(globalDatabase);
                onUpdate.run();
            }
        });
    }
}