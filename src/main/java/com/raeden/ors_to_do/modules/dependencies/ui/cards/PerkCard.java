package com.raeden.ors_to_do.modules.dependencies.ui.cards;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.i18n.Lang;
import com.raeden.ors_to_do.modules.dependencies.services.ProgressionService;
import com.raeden.ors_to_do.modules.dependencies.ui.components.DependencyMenuBuilder;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.Design;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.ColorUtil;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.TaskLinkUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PerkCard extends VBox {
    private boolean isExpanded = false;

    public PerkCard(TaskItem perkTask, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate) {
        super(10);

        boolean meetsRequirements = true;
        VBox requirementsBox = new VBox(5);
        requirementsBox.setPadding(new Insets(10, 0, 0, 0));

        // 1. Check Stat Requirements
        for (Map.Entry<String, Integer> req : perkTask.getStatRequirements().entrySet()) {
            CustomStat foundStat = appStats.getCustomStats().stream().filter(s -> s.getId().equals(req.getKey())).findFirst().orElse(null);
            if (foundStat != null) {
                if (foundStat.getCurrentAmount() < req.getValue()) {
                    meetsRequirements = false;
                    Label l = new Label(Lang.REQ_STAT_UNMET.get(req.getValue(), foundStat.getName(), foundStat.getCurrentAmount()));
                    l.setStyle("-fx-text-fill: #FF6666; -fx-font-size: 12px;");
                    requirementsBox.getChildren().add(l);
                } else {
                    Label l = new Label(Lang.REQ_STAT_MET.get(req.getValue(), foundStat.getName()));
                    l.setStyle("-fx-text-fill: #4EC9B0; -fx-font-size: 12px;");
                    requirementsBox.getChildren().add(l);
                }
            }
        }

        // 2. Check Perk (Task) Dependencies
        if (perkTask.getDependsOnTaskIds() != null && !perkTask.getDependsOnTaskIds().isEmpty()) {
            for (String depId : perkTask.getDependsOnTaskIds()) {
                TaskItem depTask = globalDatabase.stream().filter(t -> t.getId().equals(depId)).findFirst().orElse(null);
                if (depTask != null) {
                    boolean isDepUnlocked = TaskLinkUtil.isDependencyUnlocked(depTask);

                    if (!isDepUnlocked) {
                        meetsRequirements = false;
                        // A counter card that isn't at max gets a clearer "x/y" message.
                        Label l = depTask.isCounterMode()
                                ? new Label(Lang.REQ_DEP_COUNTER_UNMET.get(depTask.getTextContent(), depTask.getCurrentCount(), depTask.getMaxCount()))
                                : new Label(Lang.REQ_DEP_PERK_UNMET.get(depTask.getTextContent()));
                        l.setStyle("-fx-text-fill: #FF6666; -fx-font-size: 12px;");
                        requirementsBox.getChildren().add(l);
                    } else {
                        Label l = new Label(Lang.DEP_PERK_HOOKED.get(depTask.getTextContent()));
                        l.setStyle("-fx-text-fill: #4EC9B0; -fx-font-size: 12px;");
                        requirementsBox.getChildren().add(l);
                    }
                }
            }
        }

        // --- 15-Minute Setup Phase Logic ---
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime creationTime = perkTask.getDateCreated();
        boolean isSetupPhase = ProgressionService.isInSetupPhase(perkTask, now);
        boolean isLocked = !meetsRequirements || isSetupPhase;

        if (isSetupPhase) {
            long minsLeft = Duration.between(now, creationTime.plusMinutes(ProgressionService.SETUP_PHASE_MINUTES)).toMinutes();
            Label setupLbl = new Label(Lang.SETUP_PHASE_INACTIVE.get(minsLeft + 1));
            setupLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12px; -fx-font-weight: bold;");
            requirementsBox.getChildren().add(0, setupLbl);
        }

        // --- Tracking Unlock and Lost Dates (pure transition lives in ProgressionService) ---
        boolean stateChanged = ProgressionService.recomputePerkState(perkTask, meetsRequirements, isSetupPhase, now);

        if (stateChanged) {
            StorageManager.saveTasks(globalDatabase);
        }

        // --- Display Dates in UI ---
        if (perkTask.getPerkUnlockedDate() != null) {
            Label unlockedLbl = new Label(Lang.PERK_UNLOCKED_ON.get(perkTask.getPerkUnlockedDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))));
            unlockedLbl.setStyle("-fx-text-fill: #4EC9B0; -fx-font-size: 11px;");
            requirementsBox.getChildren().add(unlockedLbl);
        }
        if (perkTask.getPerkLostDate() != null) {
            Label lostLbl = new Label(Lang.PERK_LOST_ON.get(perkTask.getPerkLostDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))));
            lostLbl.setStyle("-fx-text-fill: #E06666; -fx-font-size: 11px;");
            requirementsBox.getChildren().add(lostLbl);
        }

        // Dynamic Custom Colors
        String bgColor = perkTask.getColorHex() != null && !perkTask.getColorHex().equals("transparent") ? perkTask.getColorHex() : "#2D2D30";
        String outlineColor = perkTask.getCustomOutlineColor() != null && !perkTask.getCustomOutlineColor().equals("transparent") ? perkTask.getCustomOutlineColor() : "#569CD6";
        String iconColor = perkTask.getIconColor() != null && !perkTask.getIconColor().equals("transparent") ? perkTask.getIconColor() : "#FFFFFF";
        String iconStr = (perkTask.getIconSymbol() != null && !perkTask.getIconSymbol().equals("None")) ? perkTask.getIconSymbol() + " " : "✨ ";

        // Visual Styling & Level Glow based on custom colors
        if (!isLocked && perkTask.getPerkLevel() > 0) {
            int glowSpread = perkTask.getPerkLevel() * 8; // Max level 5 = spread 40
            setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 15; -fx-background-radius: 5; -fx-border-color: " + outlineColor + "; -fx-border-radius: 5; -fx-effect: dropshadow(three-pass-box, " + outlineColor + ", " + glowSpread + ", 0.3, 0, 0);");
        } else if (!isLocked) {
            setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 15; -fx-background-radius: 5; -fx-border-color: " + outlineColor + "; -fx-border-radius: 5;");
        } else {
            setStyle("-fx-background-color: #1E1E1E; -fx-padding: 15; -fx-background-radius: 5; -fx-border-color: #3E3E42; -fx-border-radius: 5; -fx-opacity: 0.7;");
        }

        // --- HEADER ---
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label((isLocked ? "🔒 " : iconStr) + perkTask.getTextContent());
        nameLabel.setStyle("-fx-text-fill: " + (isLocked ? "#858585" : iconColor) + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label levelLabel = new Label(Lang.LEVEL_LABEL.get(perkTask.getPerkLevel()));
        levelLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #332B00; -fx-padding: 3 8; -fx-background-radius: 10;");
        if (isLocked) levelLabel.setVisible(false);

        Button editBtn = new Button("⚙");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #AAAAAA; -fx-cursor: hand;");
        editBtn.setOnAction(e -> openPerkConfigDialog(perkTask, appStats, globalDatabase, onUpdate));

        header.getChildren().addAll(nameLabel, spacer, levelLabel, editBtn);

        // --- DESCRIPTION (Expandable) ---
        VBox descBox = new VBox(5);
        descBox.setVisible(false);
        descBox.setManaged(false);
        Label descLabel = new Label(perkTask.getPerkDescription() == null || perkTask.getPerkDescription().isEmpty() ? Lang.NO_DESCRIPTION.get() : perkTask.getPerkDescription());
        descLabel.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 13px; -fx-font-style: italic;");
        descLabel.setWrapText(true);
        descBox.getChildren().add(descLabel);

        Button toggleDescBtn = new Button(Lang.TOGGLE_SHOW_DETAILS.get());
        toggleDescBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + outlineColor + "; -fx-cursor: hand; -fx-padding: 0;");
        toggleDescBtn.setOnAction(e -> {
            isExpanded = !isExpanded;
            descBox.setVisible(isExpanded);
            descBox.setManaged(isExpanded);
            toggleDescBtn.setText(isExpanded ? Lang.TOGGLE_HIDE_DETAILS.get() : Lang.TOGGLE_SHOW_DETAILS.get());
        });

        getChildren().addAll(header, toggleDescBtn, descBox);
        if (!requirementsBox.getChildren().isEmpty()) {
            getChildren().addAll(new Separator(), requirementsBox);
        }

        // --- FIXED: Context Menu for Editing & Deletion ---
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem(Lang.MENU_EDIT_PERK.get());
        editItem.setOnAction(e -> openPerkConfigDialog(perkTask, appStats, globalDatabase, onUpdate));

        MenuItem deleteItem = new MenuItem(Lang.MENU_DELETE_PERK.get());
        deleteItem.setStyle("-fx-text-fill: #FF6666; -fx-font-weight: bold;");
        deleteItem.setOnAction(e -> {
            if (Design.confirmedYes(Lang.CONFIRM_DELETE_PERK_HEADER, Lang.CONFIRM_DELETE_PERK_BODY, perkTask.getTextContent())) {
                globalDatabase.remove(perkTask);
                StorageManager.saveTasks(globalDatabase);
                onUpdate.run();
            }
        });
        contextMenu.getItems().add(editItem);

        // "Move to Category" — only when this perk's page has categories enabled.
        com.raeden.ors_to_do.dependencies.models.SectionConfig perkSection = appStats.findSection(perkTask.getSectionId());
        Menu categoryMenu = com.raeden.ors_to_do.modules.dependencies.ui.menus.TaskContextMenu
                .buildMoveToCategoryMenu(perkTask, perkSection, globalDatabase, onUpdate);
        if (categoryMenu != null) contextMenu.getItems().add(categoryMenu);

        contextMenu.getItems().addAll(new SeparatorMenuItem(), deleteItem);

        this.setOnContextMenuRequested(e -> {
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    private void openPerkConfigDialog(TaskItem perkTask, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Lang.DLG_CONFIGURE_PERK_TITLE.get(perkTask.getTextContent()));
        TaskDialogs.styleDialog(dialog);

        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        // 1. Name & Description
        TextField nameInput = new TextField(perkTask.getTextContent());
        nameInput.setPromptText(Lang.FIELD_PERK_NAME_PROMPT.get());

        TextArea descInput = new TextArea(perkTask.getPerkDescription() != null ? perkTask.getPerkDescription() : "");
        descInput.setPromptText(Lang.FIELD_PERK_DESC_PROMPT.get());
        descInput.setPrefRowCount(3);

        content.getChildren().addAll(new Label(Lang.LBL_PERK_NAME.get()), nameInput, new Label(Lang.LBL_PERK_EFFECT.get()), descInput);

        // 2. Styling & Icon Section
        content.getChildren().add(new Separator());
        Label styleLabel = new Label(Lang.LBL_PERK_APPEARANCE_STYLING.get());
        styleLabel.setStyle("-fx-text-fill: #569CD6; -fx-font-weight: bold;");
        content.getChildren().add(styleLabel);

        GridPane styleGrid = new GridPane();
        styleGrid.setHgap(15); styleGrid.setVgap(10);
        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints(); col2.setHgrow(Priority.ALWAYS);
        styleGrid.getColumnConstraints().addAll(col1, col2);

        ComboBox<String> iconBox = new ComboBox<>();
        iconBox.getItems().addAll(TaskDialogs.ICON_LIST);
        iconBox.setValue(perkTask.getIconSymbol() != null ? perkTask.getIconSymbol() : "None");
        iconBox.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(iconBox, Priority.ALWAYS);

        ColorPicker iconColorPicker = new ColorPicker(Color.web(perkTask.getIconColor() != null ? perkTask.getIconColor() : "#FFFFFF"));
        iconColorPicker.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(iconColorPicker, Priority.ALWAYS);

        ColorPicker bgColorPicker = new ColorPicker(Color.web(perkTask.getColorHex() != null && !perkTask.getColorHex().equals("transparent") ? perkTask.getColorHex() : "#2D2D30"));
        ColorPicker outlinePicker = new ColorPicker(Color.web(perkTask.getCustomOutlineColor() != null && !perkTask.getCustomOutlineColor().equals("transparent") ? perkTask.getCustomOutlineColor() : "#569CD6"));

        bgColorPicker.setMaxWidth(Double.MAX_VALUE);
        outlinePicker.setMaxWidth(Double.MAX_VALUE);

        styleGrid.add(new Label(Lang.LBL_ICON_AND_COLOR.get()), 0, 0);
        styleGrid.add(new HBox(10, iconBox, iconColorPicker), 1, 0);

        styleGrid.add(new Label(Lang.LBL_BACKGROUND_COLOR.get()), 0, 1);
        styleGrid.add(bgColorPicker, 1, 1);

        styleGrid.add(new Label(Lang.LBL_OUTLINE_GLOW_COLOR.get()), 0, 2);
        styleGrid.add(outlinePicker, 1, 2);

        Button randomBtn = new Button(Lang.BTN_RANDOMIZE_STYLE.get());
        randomBtn.setMaxWidth(Double.MAX_VALUE);
        randomBtn.setOnAction(e -> {
            java.util.Random rand = new java.util.Random();
            double hue = rand.nextDouble() * 360.0;
            iconBox.setValue(TaskDialogs.ICON_LIST[rand.nextInt(TaskDialogs.ICON_LIST.length - 1) + 1]);
            iconColorPicker.setValue(Color.hsb(hue, 0.5, 0.95));
            bgColorPicker.setValue(Color.hsb(hue, 0.8, 0.2));
            outlinePicker.setValue(Color.hsb(hue, 0.8, 0.8));
        });
        styleGrid.add(randomBtn, 1, 3);
        content.getChildren().add(styleGrid);

        // 3. Stat Requirements
        content.getChildren().add(new Separator());
        Label hookLabel = new Label(Lang.LBL_HOOK_STAT_REQUIREMENTS.get());
        hookLabel.setStyle("-fx-text-fill: #4EC9B0; -fx-font-weight: bold;");
        content.getChildren().add(hookLabel);

        HBox statInputBox = new HBox(10);
        ComboBox<CustomStat> statBox = new ComboBox<>();
        statBox.getItems().addAll(appStats.getCustomStats());
        statBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(CustomStat item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        statBox.setButtonCell(statBox.getCellFactory().call(null));
        statBox.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(statBox, Priority.ALWAYS);

        Spinner<Integer> amountSpinner = new Spinner<>(1, 99999, 100);
        amountSpinner.setEditable(true);
        amountSpinner.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(amountSpinner, Priority.ALWAYS);

        Button addStatBtn = new Button(Lang.BTN_ADD_HOOK.get());
        addStatBtn.setStyle("-fx-background-color: #0E639C; -fx-text-fill: white;");

        statInputBox.getChildren().addAll(statBox, amountSpinner, addStatBtn);
        content.getChildren().add(statInputBox);

        VBox activeReqsBox = new VBox(5);

        Runnable[] refreshReqs = new Runnable[1];
        refreshReqs[0] = () -> {
            activeReqsBox.getChildren().clear();
            for (Map.Entry<String, Integer> req : perkTask.getStatRequirements().entrySet()) {
                CustomStat s = appStats.getCustomStats().stream().filter(x -> x.getId().equals(req.getKey())).findFirst().orElse(null);
                if (s != null) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label l = new Label(Lang.REQ_LINE_BULLET.get(req.getValue(), s.getName()));
                    l.setStyle("-fx-text-fill: #E0E0E0;");

                    Button removeBtn = new Button("❌");
                    removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #FF6666; -fx-cursor: hand;");
                    removeBtn.setOnAction(e -> {
                        perkTask.getStatRequirements().remove(req.getKey());
                        refreshReqs[0].run();
                    });

                    row.getChildren().addAll(l, removeBtn);
                    activeReqsBox.getChildren().add(row);
                }
            }
        };
        refreshReqs[0].run(); // Initial population

        addStatBtn.setOnAction(e -> {
            if (statBox.getValue() != null) {
                perkTask.getStatRequirements().put(statBox.getValue().getId(), amountSpinner.getValue());
                refreshReqs[0].run();
            }
        });

        content.getChildren().add(activeReqsBox);

        // --- Category field (only when the owning section opts in via "Enable Categories") ---
        com.raeden.ors_to_do.dependencies.models.SectionConfig owningSection = appStats.findSection(perkTask.getSectionId());
        final TextField perkCategoryField;
        if (owningSection != null && owningSection.isEnableCategories()) {
            content.getChildren().add(new Separator());
            Label catLabel = new Label(Lang.CATEGORY_LABEL.get());
            catLabel.setStyle("-fx-text-fill: #DCDCAA; -fx-font-weight: bold;");
            content.getChildren().add(catLabel);

            perkCategoryField = new TextField(perkTask.getCategoryName() != null ? perkTask.getCategoryName() : "");
            perkCategoryField.setPromptText(Lang.CATEGORY_PROMPT.get());

            java.util.List<String> existing = new java.util.ArrayList<>();
            for (TaskItem other : globalDatabase) {
                if (other == perkTask) continue;
                if (other.getSectionId() == null || !other.getSectionId().equals(owningSection.getId())) continue;
                String c = other.getCategoryName();
                if (c != null && !c.trim().isEmpty() && !existing.contains(c)) existing.add(c);
            }
            javafx.scene.layout.FlowPane chips = new javafx.scene.layout.FlowPane(5, 5);
            for (String c : existing) {
                Button chip = new Button(c);
                chip.setStyle("-fx-background-color: #2D2D30; -fx-text-fill: #DCDCAA; -fx-border-color: #555555; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 1 8;");
                chip.setOnAction(e -> perkCategoryField.setText(c));
                chips.getChildren().add(chip);
            }
            content.getChildren().addAll(perkCategoryField, chips);
        } else {
            perkCategoryField = null;
        }

        // 4. Hook Tasks / Challenges
        content.getChildren().add(new Separator());
        Label depLabel = new Label(Lang.LBL_HOOK_TASKS.get());
        depLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold;");
        content.getChildren().add(depLabel);

        List<String> selectedDeps = new ArrayList<>(perkTask.getDependsOnTaskIds());
        MenuButton dependenciesMenu = DependencyMenuBuilder.build(perkTask, appStats, globalDatabase, selectedDeps);
        content.getChildren().add(dependenciesMenu);

        // Wrap in ScrollPane to ensure it never gets cramped
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(500, 650);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");
        scrollPane.setBorder(Border.EMPTY);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                perkTask.setTextContent(nameInput.getText().trim());
                perkTask.setPerkDescription(descInput.getText().trim());

                perkTask.setIconSymbol(iconBox.getValue());
                perkTask.setIconColor(ColorUtil.toHexOrTransparent(iconColorPicker.getValue()));
                perkTask.setColorHex(ColorUtil.toHexOrTransparent(bgColorPicker.getValue()));
                perkTask.setCustomOutlineColor(ColorUtil.toHexOrTransparent(outlinePicker.getValue()));

                perkTask.setDependsOnTaskIds(DependencyMenuBuilder.stripStale(selectedDeps, globalDatabase));

                if (perkCategoryField != null) {
                    String typed = perkCategoryField.getText() != null ? perkCategoryField.getText().trim() : "";
                    perkTask.setCategoryName(typed.isEmpty() ? null : typed);
                }

                StorageManager.saveTasks(globalDatabase);
                onUpdate.run();
            }
        });
    }
}