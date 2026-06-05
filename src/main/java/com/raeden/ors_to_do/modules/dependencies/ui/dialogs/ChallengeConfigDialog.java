package com.raeden.ors_to_do.modules.dependencies.ui.dialogs;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.i18n.Lang;
import com.raeden.ors_to_do.modules.dependencies.ui.components.DependencyMenuBuilder;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.ColorUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The "Configure Challenge" editor dialog, extracted from {@code ChallengeCard} so that the
 * card class stays a focused view (and below the 500-line "god class" threshold). All copy is
 * routed through {@link Lang}; styling is shared via {@link TaskDialogs#styleDialog}.
 */
public final class ChallengeConfigDialog {

    private ChallengeConfigDialog() { }

    public static void open(TaskItem challengeTask, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Lang.DLG_CONFIGURE_CHALLENGE_TITLE.get());
        TaskDialogs.styleDialog(dialog);

        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        TextField nameInput = new TextField(challengeTask.getTextContent());
        nameInput.setPromptText(Lang.FIELD_CHALLENGE_NAME_PROMPT.get());
        TextArea descInput = new TextArea(challengeTask.getPerkDescription() != null ? challengeTask.getPerkDescription() : "");
        descInput.setPromptText(Lang.FIELD_CHALLENGE_DESC_PROMPT.get());
        descInput.setPrefRowCount(3);
        content.getChildren().addAll(new Label(Lang.LBL_CHALLENGE_NAME.get()), nameInput, new Label(Lang.LBL_DESC_AND_RULES.get()), descInput);

        // --- Deadline Config ---
        content.getChildren().add(new Separator());
        Label timeLabel = new Label(Lang.LBL_CHALLENGE_TIMELINE.get());
        timeLabel.setStyle("-fx-text-fill: #E0E0E0; -fx-font-weight: bold;");
        content.getChildren().add(timeLabel);

        GridPane timeGrid = new GridPane();
        timeGrid.setHgap(15); timeGrid.setVgap(10);

        DatePicker datePicker = new DatePicker();
        datePicker.setMaxWidth(Double.MAX_VALUE);
        if (challengeTask.getDeadline() != null) datePicker.setValue(challengeTask.getDeadline().toLocalDate());

        TextField timePicker = new TextField();
        timePicker.setMaxWidth(Double.MAX_VALUE);
        timePicker.setPromptText(Lang.LBL_TIME_PROMPT.get());
        if (challengeTask.getDeadline() != null) timePicker.setText(challengeTask.getDeadline().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));

        timePicker.setDisable(datePicker.getValue() == null);
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> timePicker.setDisable(newVal == null));

        timeGrid.add(new Label(Lang.LBL_DEADLINE_DATE.get()), 0, 0);
        timeGrid.add(datePicker, 1, 0);
        timeGrid.add(new Label(Lang.LBL_EXACT_TIME.get()), 0, 1);
        timeGrid.add(timePicker, 1, 1);
        content.getChildren().add(timeGrid);

        content.getChildren().add(new Separator());
        Label styleLabel = new Label(Lang.LBL_APPEARANCE_STYLING.get());
        styleLabel.setStyle("-fx-text-fill: #569CD6; -fx-font-weight: bold;");
        content.getChildren().add(styleLabel);

        GridPane styleGrid = new GridPane();
        styleGrid.setHgap(15); styleGrid.setVgap(10);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(150);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setHgrow(Priority.ALWAYS);
        styleGrid.getColumnConstraints().addAll(col1, col2);

        ComboBox<String> iconBox = new ComboBox<>();
        iconBox.getItems().addAll(TaskDialogs.ICON_LIST);
        iconBox.setValue(challengeTask.getIconSymbol() != null ? challengeTask.getIconSymbol() : "None");
        iconBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(iconBox, Priority.ALWAYS);

        ColorPicker iconColorPicker = new ColorPicker(Color.web(challengeTask.getIconColor() != null ? challengeTask.getIconColor() : "#FFFFFF"));
        iconColorPicker.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(iconColorPicker, Priority.ALWAYS);

        ColorPicker bgColorPicker = new ColorPicker(Color.web(challengeTask.getColorHex() != null && !challengeTask.getColorHex().equals("transparent") ? challengeTask.getColorHex() : "#2D2D30"));
        bgColorPicker.setMaxWidth(Double.MAX_VALUE);

        ColorPicker outlinePicker = new ColorPicker(Color.web(challengeTask.getCustomOutlineColor() != null && !challengeTask.getCustomOutlineColor().equals("transparent") ? challengeTask.getCustomOutlineColor() : "#FF8C00"));
        outlinePicker.setMaxWidth(Double.MAX_VALUE);

        HBox iconRow = new HBox(10, iconBox, iconColorPicker);

        styleGrid.add(new Label(Lang.LBL_ICON_AND_COLOR.get()), 0, 0); styleGrid.add(iconRow, 1, 0);
        styleGrid.add(new Label(Lang.LBL_BACKGROUND_COLOR.get()), 0, 1); styleGrid.add(bgColorPicker, 1, 1);
        styleGrid.add(new Label(Lang.LBL_OUTLINE_COLOR.get()), 0, 2); styleGrid.add(outlinePicker, 1, 2);

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

        content.getChildren().add(new Separator());
        Label lootLabel = new Label(Lang.LBL_CHALLENGE_LOOT.get());
        lootLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold;");
        content.getChildren().add(lootLabel);

        GridPane lootGrid = new GridPane();
        lootGrid.setHgap(10); lootGrid.setVgap(10);
        ColumnConstraints lcol1 = new ColumnConstraints();
        lcol1.setMinWidth(150);
        ColumnConstraints lcol2 = new ColumnConstraints(); lcol2.setHgrow(Priority.ALWAYS);
        ColumnConstraints lcol3 = new ColumnConstraints(); lcol3.setHgrow(Priority.ALWAYS);
        lootGrid.getColumnConstraints().addAll(lcol1, lcol2, lcol3);

        TextField globalPtsField = new TextField(String.valueOf(challengeTask.getRewardPoints()));
        globalPtsField.setMaxWidth(Double.MAX_VALUE);
        lootGrid.add(new Label(Lang.LBL_GLOBAL_POINTS.get()), 0, 0);
        lootGrid.add(globalPtsField, 1, 0, 2, 1);

        lootGrid.add(new Label(Lang.LBL_COL_STAT.get()), 0, 1);
        lootGrid.add(new Label(Lang.LBL_COL_XP_REWARD.get()), 1, 1);
        lootGrid.add(new Label(Lang.LBL_COL_MAX_CAP.get()), 2, 1);

        int r = 2;
        Map<String, TextField> rewardFields = new HashMap<>();
        Map<String, TextField> capFields = new HashMap<>();

        for (CustomStat stat : appStats.getCustomStats()) {
            Label statNameLabel = new Label(stat.getName());
            statNameLabel.setStyle("-fx-text-fill: " + (stat.getTextColor() != null ? stat.getTextColor() : "white") + ";");
            lootGrid.add(statNameLabel, 0, r);

            TextField rF = new TextField(); rF.setMaxWidth(Double.MAX_VALUE);
            if (challengeTask.getStatRewards().containsKey(stat.getId())) rF.setText(String.valueOf(challengeTask.getStatRewards().get(stat.getId())));
            rewardFields.put(stat.getId(), rF);
            lootGrid.add(rF, 1, r);

            TextField cF = new TextField(); cF.setMaxWidth(Double.MAX_VALUE);
            if (challengeTask.getStatCapRewards().containsKey(stat.getId())) cF.setText(String.valueOf(challengeTask.getStatCapRewards().get(stat.getId())));
            capFields.put(stat.getId(), cF);
            lootGrid.add(cF, 2, r);
            r++;
        }
        content.getChildren().add(lootGrid);

        content.getChildren().add(new Separator());
        Label hookLabel = new Label(Lang.LBL_UNLOCK_REQUIREMENTS.get());
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
            for (Map.Entry<String, Integer> req : challengeTask.getStatRequirements().entrySet()) {
                CustomStat s = appStats.getCustomStats().stream().filter(x -> x.getId().equals(req.getKey())).findFirst().orElse(null);
                if (s != null) {
                    HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
                    Label l = new Label(Lang.REQ_LINE_BULLET.get(req.getValue(), s.getName())); l.setStyle("-fx-text-fill: #E0E0E0;");
                    Button removeBtn = new Button("❌");
                    removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #FF6666; -fx-cursor: hand;");
                    removeBtn.setOnAction(e -> { challengeTask.getStatRequirements().remove(req.getKey()); refreshReqs[0].run(); });
                    row.getChildren().addAll(l, removeBtn);
                    activeReqsBox.getChildren().add(row);
                }
            }
        };
        refreshReqs[0].run();

        addStatBtn.setOnAction(e -> {
            if (statBox.getValue() != null) {
                challengeTask.getStatRequirements().put(statBox.getValue().getId(), amountSpinner.getValue());
                refreshReqs[0].run();
            }
        });
        content.getChildren().add(activeReqsBox);

        content.getChildren().add(new Separator());
        Label depLabel = new Label(Lang.LBL_HOOK_TASKS.get());
        depLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold;");
        content.getChildren().add(depLabel);

        List<String> selectedDeps = new ArrayList<>(challengeTask.getDependsOnTaskIds());
        MenuButton dependenciesMenu = DependencyMenuBuilder.build(challengeTask, appStats, globalDatabase, selectedDeps);
        content.getChildren().add(dependenciesMenu);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(550, 700);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");
        scrollPane.setBorder(Border.EMPTY);

        String scrollCss = ".scroll-bar:vertical, .scroll-bar:horizontal { -fx-background-color: transparent; } " +
                ".scroll-bar:vertical .track, .scroll-bar:horizontal .track { -fx-background-color: #1E1E1E; -fx-border-color: transparent; } " +
                ".scroll-bar:vertical .thumb, .scroll-bar:horizontal .thumb { -fx-background-color: #555555; -fx-background-radius: 5; }";
        scrollPane.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(scrollCss.getBytes()));

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                challengeTask.setTextContent(nameInput.getText().trim());
                challengeTask.setPerkDescription(descInput.getText().trim());
                challengeTask.setIconSymbol(iconBox.getValue());
                challengeTask.setIconColor(ColorUtil.toHexOrTransparent(iconColorPicker.getValue()));
                challengeTask.setColorHex(ColorUtil.toHexOrTransparent(bgColorPicker.getValue()));
                challengeTask.setCustomOutlineColor(ColorUtil.toHexOrTransparent(outlinePicker.getValue()));
                challengeTask.setDependsOnTaskIds(selectedDeps);

                if (datePicker.getValue() != null) {
                    try {
                        LocalTime time = LocalTime.MIDNIGHT;
                        if (!timePicker.getText().trim().isEmpty()) time = LocalTime.parse(timePicker.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
                        challengeTask.setDeadline(LocalDateTime.of(datePicker.getValue(), time));
                    } catch (Exception ex) { challengeTask.setDeadline(LocalDateTime.of(datePicker.getValue(), LocalTime.MIDNIGHT)); }
                } else {
                    challengeTask.setDeadline(null);
                }

                try { challengeTask.setRewardPoints(Math.max(0, Integer.parseInt(globalPtsField.getText().trim()))); } catch(Exception ignore){}

                Map<String, Integer> newRewards = new HashMap<>();
                Map<String, Integer> newCaps = new HashMap<>();

                for (CustomStat stat : appStats.getCustomStats()) {
                    try {
                        int rewardVal = Integer.parseInt(rewardFields.get(stat.getId()).getText().trim());
                        if (rewardVal > 0) newRewards.put(stat.getId(), rewardVal);
                    } catch(Exception ignore){}

                    try {
                        int capVal = Integer.parseInt(capFields.get(stat.getId()).getText().trim());
                        if (capVal > 0) newCaps.put(stat.getId(), capVal);
                    } catch(Exception ignore){}
                }
                challengeTask.setStatRewards(newRewards);
                challengeTask.setStatCapRewards(newCaps);

                StorageManager.saveTasks(globalDatabase);
                onUpdate.run();
            }
        });
    }
}
