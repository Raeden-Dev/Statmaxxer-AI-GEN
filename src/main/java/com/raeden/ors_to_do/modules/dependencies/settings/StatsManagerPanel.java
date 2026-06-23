package com.raeden.ors_to_do.modules.dependencies.settings;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.models.Debuff;
import com.raeden.ors_to_do.dependencies.models.StatThreshold;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsManagerPanel extends VBox {
    private VBox existingStatsBox;
    private AppStats appStats;
    private Runnable refreshCallback;
    private final double BUTTON_WIDTH = 200.0;

    private Label descLabel;

    public StatsManagerPanel(AppStats appStats, Runnable refreshCallback) {
        super(15);
        this.appStats = appStats;
        this.refreshCallback = refreshCallback;

        setStyle("-fx-border-color: #B5CEA8; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5;");
        Label headerLabel = new Label("Stats Configuration");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #B5CEA8;");

        // "Base Stats" sits in the header row so it lands just before the section's "Hide" toggle
        // (which is pinned to the panel's top-right corner by the collapsible wrapper).
        Button baseStatsBtn = new Button("⚖ Base Stats");
        baseStatsBtn.setStyle("-fx-background-color: #2D2D30; -fx-border-color: #B5CEA8; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #B5CEA8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 10;");
        baseStatsBtn.setTooltip(new Tooltip("Set a baseline value per stat and reset stats back to it."));
        baseStatsBtn.setOnAction(e -> showBaseStatsDialog());

        HBox header = new HBox(12, headerLabel, baseStatsBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        descLabel = new Label();

        existingStatsBox = new VBox(10);
        renderExistingStats();

        Button createStatBtn = new Button("+ Create New Stat");
        createStatBtn.setPrefWidth(BUTTON_WIDTH);
        createStatBtn.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        createStatBtn.setOnAction(e -> showStatDialog(null));

        getChildren().addAll(header, descLabel, existingStatsBox, new Separator(), createStatBtn);

        refreshState();
    }

    public void refreshState() {
        if (!appStats.isGlobalStatsEnabled()) {
            this.setDisable(true);
            descLabel.setText("⚠️ Custom Stats are disabled. Turn them on in General Configuration to use this feature.");
            descLabel.setStyle("-fx-text-fill: #FF6666; -fx-font-weight: bold; -fx-font-size: 12px;");
        } else {
            this.setDisable(false);
            descLabel.setText("Create custom stats (Strength, Focus, etc.) to attach to your tasks.");
            descLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12px;");
        }
    }

    private void renderExistingStats() {
        existingStatsBox.getChildren().clear();

        for (int i = 0; i < appStats.getCustomStats().size(); i++) {
            CustomStat stat = appStats.getCustomStats().get(i);
            int index = i;

            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #2D2D30; -fx-padding: 10; -fx-border-color: #3E3E42; -fx-border-radius: 5;");

            Label badgePreview = new Label((stat.getIconSymbol() != null && !stat.getIconSymbol().equals("None") ? stat.getIconSymbol() + " " : "") + stat.getName());
            String bgColor = stat.getBackgroundColor() != null ? stat.getBackgroundColor() : "#333333";
            String txtColor = stat.getTextColor() != null ? stat.getTextColor() : "#FFFFFF";
            badgePreview.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + txtColor + "; -fx-padding: 3 8; -fx-background-radius: 3; -fx-font-weight: bold;");
            badgePreview.setPrefWidth(150);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox btnBox = new HBox(5);

            Button upBtn = new Button("▲");
            upBtn.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: white; -fx-cursor: hand;");
            upBtn.setDisable(index == 0);
            upBtn.setOnAction(e -> {
                Collections.swap(appStats.getCustomStats(), index, index - 1);
                StorageManager.saveStats(appStats);
                renderExistingStats();
            });

            Button downBtn = new Button("▼");
            downBtn.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: white; -fx-cursor: hand;");
            downBtn.setDisable(index == appStats.getCustomStats().size() - 1);
            downBtn.setOnAction(e -> {
                Collections.swap(appStats.getCustomStats(), index, index + 1);
                StorageManager.saveStats(appStats);
                renderExistingStats();
            });

            Button resetAtrophyBtn = new Button("⏳");
            resetAtrophyBtn.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: white; -fx-cursor: hand;");
            boolean atrophyOff = stat.getAtrophyDays() <= 0;
            resetAtrophyBtn.setDisable(atrophyOff);
            resetAtrophyBtn.setTooltip(new Tooltip(atrophyOff
                    ? "Atrophy disabled for this stat."
                    : "Reset this stat's atrophy timer (counts inactivity from today)."));
            resetAtrophyBtn.setOnAction(e -> {
                appStats.getLastStatGainDates().put(stat.getId(), java.time.LocalDate.now());
                StorageManager.saveStats(appStats);
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Atrophy timer for '" + stat.getName() + "' has been reset.", ButtonType.OK);
                a.setHeaderText("Atrophy Timer Reset");
                TaskDialogs.styleDialog(a);
                a.show();
            });

            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: #0E639C; -fx-text-fill: white; -fx-cursor: hand;");
            editBtn.setOnAction(e -> showStatDialog(stat));

            Button removeBtn = new Button("❌");
            removeBtn.setStyle("-fx-background-color: #8B0000; -fx-text-fill: white; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete stat '" + stat.getName() + "'? This will remove it from future use.", ButtonType.YES, ButtonType.NO);
                alert.setHeaderText("Delete Custom Stat");
                TaskDialogs.styleDialog(alert);
                alert.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        appStats.getCustomStats().remove(stat);
                        StorageManager.saveStats(appStats);
                        renderExistingStats();
                    }
                });
            });

            btnBox.getChildren().addAll(upBtn, downBtn, resetAtrophyBtn, editBtn, removeBtn);

            // Atrophy countdown is shown only when this stat actually decays; otherwise it's hidden.
            Label atrophyStatus = buildAtrophyStatus(stat);
            if (atrophyStatus != null) {
                row.getChildren().addAll(badgePreview, atrophyStatus, spacer, btnBox);
            } else {
                row.getChildren().addAll(badgePreview, spacer, btnBox);
            }
            existingStatsBox.getChildren().add(row);
        }
    }

    /**
     * Builds the per-stat atrophy countdown label: how long until this stat starts decaying, or its
     * current status. Returns {@code null} when the stat has no atrophy configured, so the caller can
     * omit the label entirely.
     */
    private Label buildAtrophyStatus(CustomStat stat) {
        if (stat.getAtrophyDays() <= 0) return null;

        String text;
        String color;
        if (stat.getCurrentAmount() <= 0) {
            // Atrophy only drains a stat above zero — nothing to decay here.
            text = "⏳ No decay (stat at 0)";
            color = "#858585";
        } else {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate lastGain = appStats.getLastStatGainDates().getOrDefault(stat.getId(), today);
            long daysSinceGain = java.time.temporal.ChronoUnit.DAYS.between(lastGain, today);
            long remaining = stat.getAtrophyDays() - daysSinceGain;

            if (remaining <= 0) {
                text = "⚠ Atrophy due (decays on next daily reset)";
                color = "#FF6666";
            } else if (remaining == 1) {
                text = "⏳ Atrophies tomorrow";
                color = "#DCDCAA";
            } else {
                text = "⏳ Atrophies in " + remaining + " days";
                color = remaining <= 2 ? "#DCDCAA" : "#6A9955";
            }
        }

        Label lbl = new Label(text);
        lbl.setMinWidth(Region.USE_PREF_SIZE);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
        Tooltip.install(lbl, new Tooltip("Loses value after " + stat.getAtrophyDays()
                + " day(s) of inactivity.\nComplete a task that grants this stat to reset the timer."));
        return lbl;
    }

    /**
     * "Base Stats" dialog: set a baseline value per stat and snap any stat back to its baseline.
     * Base values are persisted on each {@link CustomStat}; the per-row "Reset to Base" button
     * immediately sets the stat's current value to its (possibly just-edited) baseline.
     */
    private void showBaseStatsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Base Stats");
        TaskDialogs.styleDialog(dialog);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label info = new Label("Set a baseline value for each stat. \"Reset to Base\" snaps that stat's current value back to its baseline.");
        info.setWrapText(true);
        info.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12px;");
        content.getChildren().add(info);

        if (appStats.getCustomStats().isEmpty()) {
            Label none = new Label("No stats defined yet. Create a stat first.");
            none.setStyle("-fx-text-fill: #858585;");
            content.getChildren().add(none);
        }

        Map<String, Spinner<Integer>> spinners = new HashMap<>();
        for (CustomStat stat : appStats.getCustomStats()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #2D2D30; -fx-padding: 8; -fx-background-radius: 4; -fx-border-color: #3E3E42; -fx-border-radius: 4;");

            Label name = new Label((stat.getIconSymbol() != null && !stat.getIconSymbol().equals("None") ? stat.getIconSymbol() + " " : "") + stat.getName());
            name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            name.setPrefWidth(130);

            Label currentLbl = new Label("Current: " + stat.getCurrentAmount());
            currentLbl.setStyle("-fx-text-fill: #858585; -fx-font-size: 11px;");
            currentLbl.setPrefWidth(95);

            Label baseLbl = new Label("Base:");
            baseLbl.setStyle("-fx-text-fill: #B5CEA8;");
            Spinner<Integer> baseSpinner = new Spinner<>(0, 999999999, Math.max(0, stat.getBaseValue()));
            baseSpinner.setEditable(true);
            baseSpinner.setPrefWidth(120);
            spinners.put(stat.getId(), baseSpinner);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button resetBtn = new Button("↩ Reset to Base");
            resetBtn.setStyle("-fx-background-color: #0E639C; -fx-text-fill: white; -fx-cursor: hand;");
            resetBtn.setOnAction(e -> {
                int base = baseSpinner.getValue();
                stat.setBaseValue(base);
                int cap = stat.getEffectiveMaxCap(appStats.getActiveDebuffs());
                int target = (cap > 0) ? Math.min(base, cap) : base;
                stat.setCurrentAmount(Math.max(0, target));
                stat.setCurrentExp(0);
                if (stat.getCurrentAmount() > stat.getMaxLevelReached()) stat.setMaxLevelReached(stat.getCurrentAmount());
                StorageManager.saveStats(appStats);
                currentLbl.setText("Current: " + stat.getCurrentAmount());
            });

            row.getChildren().addAll(name, currentLbl, baseLbl, baseSpinner, spacer, resetBtn);
            content.getChildren().add(row);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(580, 480);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");
        scrollPane.setBorder(Border.EMPTY);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                for (CustomStat stat : appStats.getCustomStats()) {
                    Spinner<Integer> spn = spinners.get(stat.getId());
                    if (spn != null) stat.setBaseValue(spn.getValue());
                }
                StorageManager.saveStats(appStats);
                renderExistingStats();
            }
        });
    }

    private void showStatDialog(CustomStat stat) {
        boolean isNew = (stat == null);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Create Custom Stat" : "Edit Stat: " + stat.getName());
        TaskDialogs.styleDialog(dialog);

        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(10));

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        int rowIdx = 0;

        TextField nameField = new TextField(isNew ? "" : stat.getName());
        nameField.setPromptText("e.g. Strength, Intellect");
        nameField.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label("Stat Name:"), 0, rowIdx);
        grid.add(nameField, 1, rowIdx++);

        TextArea descArea = new TextArea(isNew ? "" : stat.getDescription());
        descArea.setPromptText("Brief lore or description...");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label("Description:"), 0, rowIdx);
        grid.add(descArea, 1, rowIdx++);

        // --- FIXED: Increased max limit to 999,999,999 ---
        Spinner<Integer> capSpinner = new Spinner<>(0, 999999999, isNew ? 10000000 : stat.getMaxCap());
        capSpinner.setEditable(true);
        capSpinner.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label("Max Cap\n(0 = Infinite):"), 0, rowIdx);
        grid.add(capSpinner, 1, rowIdx++);

        Spinner<Integer> atrophySpinner = new Spinner<>(0, 365, isNew ? 0 : stat.getAtrophyDays());
        atrophySpinner.setEditable(true);
        atrophySpinner.setMaxWidth(Double.MAX_VALUE);
        Label atrophyLabel = new Label("Atrophy Decay\n(Days):");
        Tooltip atrophyTip = new Tooltip("Days of inactivity before stat drops.\n0 = Never decays.");
        atrophyLabel.setTooltip(atrophyTip);
        grid.add(atrophyLabel, 0, rowIdx);
        grid.add(atrophySpinner, 1, rowIdx++);

        // --- FIXED: Increased max limit to 999,999,999 ---
        Spinner<Integer> startingAmountSpinner = new Spinner<>(0, 999999999, isNew ? 0 : stat.getCurrentAmount());
        startingAmountSpinner.setEditable(true);
        startingAmountSpinner.setMaxWidth(Double.MAX_VALUE);
        Label startingAmountLabel = new Label(com.raeden.ors_to_do.i18n.Lang.STAT_CURRENT_LEVEL.get());
        grid.add(startingAmountLabel, 0, rowIdx);
        grid.add(startingAmountSpinner, 1, rowIdx++);

        // --- EXP leveling (opt-in) ---
        CheckBox useExpCheck = new CheckBox(com.raeden.ors_to_do.i18n.Lang.STAT_USE_EXP.get());
        useExpCheck.setSelected(!isNew && stat.isUseExp());
        useExpCheck.setStyle("-fx-text-fill: white;");
        Tooltip.install(useExpCheck, new Tooltip(com.raeden.ors_to_do.i18n.Lang.STAT_EXP_TOOLTIP.get()));
        grid.add(new Label("EXP System:"), 0, rowIdx);
        grid.add(useExpCheck, 1, rowIdx++);

        Spinner<Integer> expPerLevelSpinner = new Spinner<>(1, 999999999, !isNew && stat.getExpPerLevel() > 0 ? stat.getExpPerLevel() : 100);
        expPerLevelSpinner.setEditable(true);
        expPerLevelSpinner.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label(com.raeden.ors_to_do.i18n.Lang.STAT_EXP_PER_LEVEL.get()), 0, rowIdx);
        grid.add(expPerLevelSpinner, 1, rowIdx++);

        Spinner<Integer> currentExpSpinner = new Spinner<>(0, 999999999, isNew ? 0 : stat.getCurrentExp());
        currentExpSpinner.setEditable(true);
        currentExpSpinner.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label("Current EXP:"), 0, rowIdx);
        grid.add(currentExpSpinner, 1, rowIdx++);

        Runnable syncExpEnable = () -> {
            boolean on = useExpCheck.isSelected();
            expPerLevelSpinner.setDisable(!on);
            currentExpSpinner.setDisable(!on);
        };
        syncExpEnable.run();
        useExpCheck.setOnAction(e -> syncExpEnable.run());

        ComboBox<String> iconBox = new ComboBox<>();
        iconBox.getItems().addAll(TaskDialogs.ICON_LIST);
        iconBox.setValue(!isNew && stat.getIconSymbol() != null ? stat.getIconSymbol() : "None");
        iconBox.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label("Icon Symbol:"), 0, rowIdx);
        grid.add(iconBox, 1, rowIdx++);

        ColorPicker bgColorPicker = new ColorPicker(Color.web(!isNew && stat.getBackgroundColor() != null ? stat.getBackgroundColor() : "#333333"));
        bgColorPicker.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label("Background Color:"), 0, rowIdx);
        grid.add(bgColorPicker, 1, rowIdx++);

        ColorPicker textColorPicker = new ColorPicker(Color.web(!isNew && stat.getTextColor() != null ? stat.getTextColor() : "#FFFFFF"));
        textColorPicker.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label("Text Color:"), 0, rowIdx);
        grid.add(textColorPicker, 1, rowIdx++);

        Button randomBtn = new Button("🎲 Randomize Style");
        randomBtn.setMaxWidth(Double.MAX_VALUE);
        randomBtn.setOnAction(e -> {
            java.util.Random rand = new java.util.Random();
            double hue = rand.nextDouble() * 360.0;
            iconBox.setValue(TaskDialogs.ICON_LIST[rand.nextInt(TaskDialogs.ICON_LIST.length - 1) + 1]);
            bgColorPicker.setValue(Color.hsb(hue, 1.0, 0.2));
            textColorPicker.setValue(Color.hsb(hue, 0.6, 0.95));
        });
        grid.add(randomBtn, 1, rowIdx++);

        mainContent.getChildren().add(grid);

        // ==========================================
        // NEW: AURA THRESHOLDS (PERMANENT DEBUFFS)
        // ==========================================
        mainContent.getChildren().add(new Separator());
        Label thresholdHeader = new Label("Aura Thresholds (Permanent Debuffs)");
        thresholdHeader.setStyle("-fx-text-fill: #FF6666; -fx-font-weight: bold;");
        mainContent.getChildren().add(thresholdHeader);

        List<StatThreshold> tempThresholds = new ArrayList<>();
        if (!isNew && stat.getThresholds() != null) {
            tempThresholds.addAll(stat.getThresholds());
        }

        VBox thresholdsList = new VBox(5);
        Runnable renderThresholds = () -> {
            thresholdsList.getChildren().clear();
            for (StatThreshold t : tempThresholds) {
                HBox tRow = new HBox(10);
                tRow.setAlignment(Pos.CENTER_LEFT);
                String condition = t.isUpperThreshold() ? ">= " : "<= ";

                Debuff d = null;
                if (appStats.getDebuffTemplates() != null) {
                    d = appStats.getDebuffTemplates().stream().filter(db -> db.getId().equals(t.getDebuffId())).findFirst().orElse(null);
                }
                String dName = d != null ? d.getName() : "Unknown Debuff";

                Label lbl = new Label("If stat " + condition + t.getThresholdValue() + "  ➔  Inflict " + dName);
                lbl.setStyle("-fx-text-fill: white; -fx-background-color: #2D2D30; -fx-padding: 4 8; -fx-background-radius: 3; -fx-border-color: #3E3E42; -fx-border-radius: 3;");

                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

                Button delBtn = new Button("❌");
                delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #FF6666; -fx-cursor: hand;");
                delBtn.setOnAction(e -> {
                    tempThresholds.remove(t);
                    // Also trigger a refresh logic trick via java variable scoping
                });

                // Real lambda capture trick
                Button finalDelBtn = delBtn;
                finalDelBtn.setOnAction(e -> {
                    tempThresholds.remove(t);
                    thresholdsList.getChildren().remove(tRow);
                });

                tRow.getChildren().addAll(lbl, spacer, finalDelBtn);
                thresholdsList.getChildren().add(tRow);
            }
        };
        renderThresholds.run();

        HBox addThreshBox = new HBox(10);
        addThreshBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> conditionBox = new ComboBox<>();
        conditionBox.getItems().addAll(">=", "<=");
        conditionBox.setValue(">=");
        conditionBox.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: white;");

        Spinner<Integer> valSpinner = new Spinner<>(-99999, 99999, 0);
        valSpinner.setEditable(true);
        valSpinner.setPrefWidth(90);

        ComboBox<Debuff> debuffBox = new ComboBox<>();
        if (appStats.getDebuffTemplates() != null) debuffBox.getItems().addAll(appStats.getDebuffTemplates());
        debuffBox.setPromptText("Select Aura...");
        debuffBox.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: white;");
        debuffBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Debuff item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        debuffBox.setButtonCell(debuffBox.getCellFactory().call(null));

        Button addThreshBtn = new Button("Add");
        addThreshBtn.setStyle("-fx-background-color: #0E639C; -fx-text-fill: white; -fx-font-weight: bold;");
        addThreshBtn.setOnAction(e -> {
            if (debuffBox.getValue() != null) {
                boolean isUpper = conditionBox.getValue().equals(">=");
                StatThreshold newT = new StatThreshold(valSpinner.getValue(), isUpper, debuffBox.getValue().getId());
                tempThresholds.add(newT);
                renderThresholds.run();
            } else {
                Alert a = new Alert(Alert.AlertType.WARNING, "Please select a Debuff Template to inflict as an Aura.");
                TaskDialogs.styleDialog(a); a.show();
            }
        });

        Label ifLbl = new Label("If stat"); ifLbl.setStyle("-fx-text-fill: #AAAAAA;");
        Label arrowLbl = new Label("➔"); arrowLbl.setStyle("-fx-text-fill: #AAAAAA;");
        addThreshBox.getChildren().addAll(ifLbl, conditionBox, valSpinner, arrowLbl, debuffBox, addThreshBtn);

        mainContent.getChildren().addAll(thresholdsList, addThreshBox);

        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(580, 600);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");
        scrollPane.setBorder(Border.EMPTY);

        String scrollCss = ".scroll-bar:vertical, .scroll-bar:horizontal { -fx-background-color: transparent; } " +
                ".scroll-bar:vertical .track, .scroll-bar:horizontal .track { -fx-background-color: #1E1E1E; -fx-border-color: transparent; } " +
                ".scroll-bar:vertical .thumb, .scroll-bar:horizontal .thumb { -fx-background-color: #555555; -fx-background-radius: 5; }";
        scrollPane.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(scrollCss.getBytes()));

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK && !nameField.getText().trim().isEmpty()) {
                CustomStat target = isNew ? new CustomStat() : stat;

                target.setName(nameField.getText().trim());
                target.setIconSymbol(iconBox.getValue());
                target.setBackgroundColor(toHexString(bgColorPicker.getValue()));
                target.setTextColor(toHexString(textColorPicker.getValue()));
                target.setDescription(descArea.getText().trim());
                target.setMaxCap(capSpinner.getValue());
                target.setAtrophyDays(atrophySpinner.getValue());
                target.setThresholds(new ArrayList<>(tempThresholds)); // Save Thresholds!

                int startingAmt = startingAmountSpinner.getValue();
                target.setCurrentAmount(startingAmt);

                target.setUseExp(useExpCheck.isSelected());
                target.setExpPerLevel(expPerLevelSpinner.getValue());
                target.setCurrentExp(currentExpSpinner.getValue());

                if (isNew) {
                    target.setLifetimeEarned(startingAmt);
                    target.setMaxLevelReached(startingAmt);
                    appStats.getCustomStats().add(target);
                }

                StorageManager.saveStats(appStats);
                renderExistingStats();

                com.raeden.ors_to_do.modules.dependencies.ui.utils.TaskActionHandler.evaluateThresholdDebuffs(appStats);
            }
        });
    }

    private String toHexString(Color color) {
        if (color == null || color.getOpacity() == 0.0) return "transparent";
        return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
    }
}