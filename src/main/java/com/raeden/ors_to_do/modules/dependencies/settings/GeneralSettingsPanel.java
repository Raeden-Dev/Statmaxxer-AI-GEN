package com.raeden.ors_to_do.modules.dependencies.settings;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.services.WindowsStartupManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class GeneralSettingsPanel extends VBox {

    public GeneralSettingsPanel(AppStats appStats, Runnable refreshCallback) {
        super(15);
        setStyle("-fx-border-color: #3E3E42; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5;");

        String extraCss =
                ".slider .track { -fx-background-color: #3E3E42; -fx-background-radius: 5; } " +
                        ".slider .thumb { -fx-background-color: #569CD6; } " +
                        ".slider .thumb:hover { -fx-background-color: #4EC9B0; } " +
                        ".custom-menu-btn { -fx-background-color: #2D2D30; -fx-border-color: #555555; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; } " +
                        ".custom-menu-btn .label { -fx-text-fill: white; } " +
                        ".context-menu { -fx-background-color: #2D2D30; -fx-border-color: #555555; } " +
                        ".menu-item { -fx-background-color: #2D2D30; } " +
                        ".menu-item:hover, .menu-item:focused { -fx-background-color: #3E3E42; } " +
                        ".menu-item .label { -fx-text-fill: white; } " +
                        ".combo-box { -fx-background-color: #2D2D30; -fx-border-color: #555555; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; } " +
                        ".combo-box .list-cell { -fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: transparent; } " +
                        ".combo-box-popup .list-view { -fx-background-color: #2D2D30; -fx-border-color: #555555; } " +
                        ".combo-box-popup .list-view .list-cell { -fx-background-color: #2D2D30; -fx-text-fill: white; -fx-font-weight: normal; } " +
                        ".combo-box-popup .list-view .list-cell:filled:hover, .combo-box-popup .list-view .list-cell:filled:selected { -fx-background-color: #569CD6; -fx-text-fill: white; } " +
                        ".combo-box .arrow-button { -fx-background-color: transparent; } " +
                        ".combo-box .arrow { -fx-background-color: #AAAAAA; } " +
                        ".spinner { -fx-background-color: transparent; } " +
                        ".spinner .text-field { -fx-background-color: #2D2D30; -fx-text-fill: white; -fx-border-color: #555555; -fx-border-width: 1; -fx-border-radius: 3 0 0 3; } " +
                        ".spinner .increment-arrow-button, .spinner .decrement-arrow-button { -fx-body-color: #3E3E42; -fx-background-color: #3E3E42; -fx-border-color: #555555; -fx-border-width: 1; -fx-cursor: hand; } " +
                        ".spinner .increment-arrow-button:hover, .spinner .decrement-arrow-button:hover { -fx-body-color: #569CD6; -fx-background-color: #569CD6; } " +
                        ".spinner .increment-arrow, .spinner .decrement-arrow { -fx-background-color: white; }";

        String b64 = Base64.getEncoder().encodeToString(extraCss.getBytes());
        getStylesheets().add("data:text/css;base64," + b64);

        Label textHeader = new Label("General Configuration");
        textHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

        Label behaviorHeader = new Label("Appearance & Behavior");
        behaviorHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #AAAAAA;");
        VBox.setMargin(behaviorHeader, new Insets(10, 0, 0, 0));

        // --- FIXED: Reduced vertical gap between rows to 8 (was 15) ---
        VBox behaviorList = new VBox(8);

        Spinner<Integer> fontSizeSpinner = new Spinner<>(10, 36, appStats.getTaskFontSize());
        fontSizeSpinner.setEditable(true);

        Slider streakSlider = new Slider(10, 100, appStats.getMinDailyCompletionPercent());
        streakSlider.setMajorTickUnit(10); streakSlider.setMinorTickCount(0); streakSlider.setSnapToTicks(true);
        streakSlider.setShowTickLabels(false); streakSlider.setShowTickMarks(false); streakSlider.setPrefWidth(120);

        Label sliderValueLabel = new Label((int)streakSlider.getValue() + "%");
        sliderValueLabel.setStyle("-fx-text-fill: #4EC9B0; -fx-font-weight: bold; -fx-pref-width: 35px; -fx-alignment: center-right;");
        streakSlider.valueProperty().addListener((obs, oldVal, newVal) -> sliderValueLabel.setText(newVal.intValue() + "%"));
        HBox sliderBox = new HBox(10, streakSlider, sliderValueLabel);
        sliderBox.setAlignment(Pos.CENTER_RIGHT);

        CheckBox runInBackgroundCheck = new CheckBox();
        runInBackgroundCheck.setSelected(appStats.isRunInBackground());

        CheckBox chkStartup = new CheckBox();
        chkStartup.setSelected(appStats.isRunOnStartup());

        CheckBox notificationsCheck = new CheckBox();
        notificationsCheck.setSelected(appStats.isEnableNotifications());

        CheckBox matchRectCheck = new CheckBox();
        matchRectCheck.setSelected(appStats.isMatchDailyRectColor());

        CheckBox matchOutlineCheck = new CheckBox();
        matchOutlineCheck.setSelected(appStats.isMatchPriorityOutline());

        CheckBox matchTitleColorCheck = new CheckBox();
        matchTitleColorCheck.setSelected(appStats.isMatchTitleColor());

        CheckBox alwaysOnTopCheck = new CheckBox();
        alwaysOnTopCheck.setSelected(appStats.isAlwaysOnTop());

        Spinner<Integer> zenSpinner = new Spinner<>(5, 100, appStats.getZenModeThreshold());
        zenSpinner.setEditable(true);

        Button urgeSettingsBtn = new Button("Configure Urge Surfing...");
        urgeSettingsBtn.setStyle("-fx-background-color: #2D2D30; -fx-border-color: #555555; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; -fx-text-fill: white; -fx-font-weight: bold;");
        urgeSettingsBtn.setOnAction(e -> {
            UrgeSettingsDialog.show(appStats, () -> {
                StorageManager.saveStats(appStats);
                refreshCallback.run();
            });
        });

        Spinner<Integer> inactivitySpinner = new Spinner<>(0, 120, appStats.getFocusInactivityThreshold());
        inactivitySpinner.setEditable(true);

        CheckBox chkGlobalStats = new CheckBox();
        chkGlobalStats.setSelected(appStats.isGlobalStatsEnabled());

        CheckBox chkExpandedStats = new CheckBox();
        chkExpandedStats.setSelected(appStats.isExpandStatMiniCards());

        CheckBox chkTextToTask = new CheckBox();
        chkTextToTask.setSelected(appStats.isEnableTextToTask());

        CheckBox chkSidebarCount = new CheckBox();
        chkSidebarCount.setSelected(appStats.isShowSidebarTaskCount());

        ComboBox<String> themeBox = new ComboBox<>();
        themeBox.getItems().addAll("Default", "Dark", "Green", "Blue", "Purple");
        themeBox.setValue(appStats.getCheckboxTheme());

        MenuButton confirmMenu = new MenuButton("Select Sections...");
        confirmMenu.getStyleClass().add("custom-menu-btn");

        Runnable rebuildConfirmMenu = () -> {
            confirmMenu.getItems().clear();
            List<String> selectedConfirm = new ArrayList<>(appStats.getRequireConfirmationSections());
            List<CheckBox> sectionCbs = new ArrayList<>();

            Runnable saveConfirmState = () -> {
                appStats.setRequireConfirmationSections(selectedConfirm);
                StorageManager.saveStats(appStats);
                refreshCallback.run();
            };

            CheckBox allCb = new CheckBox("ALL Sections");
            allCb.setStyle("-fx-text-fill: white;");
            allCb.setSelected(selectedConfirm.contains("ALL"));
            CustomMenuItem allItem = new CustomMenuItem(allCb);
            allItem.setHideOnClick(false);
            confirmMenu.getItems().add(allItem);
            confirmMenu.getItems().add(new SeparatorMenuItem());

            for (SectionConfig sc : appStats.getSections()) {
                if (sc.isNotesPage() || sc.isRewardsPage() || sc.isStatPage() || sc.isPerkPage() || sc.isSeparator()) {
                    continue;
                }

                CheckBox cb = new CheckBox(sc.getName());
                cb.setStyle("-fx-text-fill: white;");
                cb.setSelected(selectedConfirm.contains(sc.getId()) || selectedConfirm.contains("ALL"));
                sectionCbs.add(cb);

                cb.setOnAction(e -> {
                    if (cb.isSelected()) {
                        if (!selectedConfirm.contains(sc.getId())) selectedConfirm.add(sc.getId());
                    } else {
                        selectedConfirm.remove(sc.getId());
                        if (allCb.isSelected()) {
                            allCb.setSelected(false);
                            selectedConfirm.remove("ALL");
                        }
                    }
                    saveConfirmState.run();
                });
                CustomMenuItem item = new CustomMenuItem(cb);
                item.setHideOnClick(false);
                confirmMenu.getItems().add(item);
            }

            allCb.setOnAction(e -> {
                if (allCb.isSelected()) {
                    if (!selectedConfirm.contains("ALL")) selectedConfirm.add("ALL");
                    for (CheckBox cb : sectionCbs) cb.setSelected(true);
                    for (SectionConfig sc : appStats.getSections()) {
                        if (sc.isNotesPage() || sc.isRewardsPage() || sc.isStatPage() || sc.isPerkPage() || sc.isSeparator()) {
                            continue;
                        }
                        if (!selectedConfirm.contains(sc.getId())) selectedConfirm.add(sc.getId());
                    }
                } else {
                    selectedConfirm.clear();
                    for (CheckBox cb : sectionCbs) cb.setSelected(false);
                }
                saveConfirmState.run();
            });
        };

        rebuildConfirmMenu.run();
        confirmMenu.setOnShowing(e -> rebuildConfirmMenu.run());

        behaviorList.getChildren().addAll(
                createSettingRow("Task Font Size", "Adjusts the size of the text across all task cards.", fontSizeSpinner, "#569CD6"),
                createSettingRow("Checkbox Theme", "Changes the visual style and color of the completion checkboxes.", themeBox, "#DCDCAA"),
                createSettingRow("Zen Mode", "Number of active tasks required before Zen Mode becomes available.", zenSpinner, "#FF6666"),
                createSettingRow("Urge Surfing Tool", "Configure the breathing session duration and custom quotes to help resist bad habits.", urgeSettingsBtn, "#4EC9B0"),
                createSettingRow("Require Completion Confirmation", "Prompts for confirmation before completing tasks in selected sections.", confirmMenu, "#FF6666"),
                createSettingRow("Minimum Streak Threshold", "Percentage of completed tasks required to maintain a completion streak.", sliderBox, "#4EC9B0"),
                createSettingRow("Strict Focus Auto-Pause", "Minutes of global keyboard/mouse inactivity before the Focus Hub timer automatically pauses. (0 = Disabled)", inactivitySpinner, "#FF6666"),
                createSettingRow("Enable Desktop Notifications", "Shows alerts for approaching deadlines, inactivity, and resets.", notificationsCheck, "#C586C0"),
                createSettingRow("Run in Background", "Keeps the application running in the system tray when closed.", runInBackgroundCheck, "#569CD6"),
                createSettingRow("Run on Windows Startup", "Automatically launches Task Tracker when your computer boots.", chkStartup, "#569CD6"),
                createSettingRow("Match Prefix Color", "Uses the prefix color for the left-side indicator rectangle on tasks.", matchRectCheck, "#CE9178"),
                createSettingRow("Match Priority Outline", "Applies the priority color to the task card's border.", matchOutlineCheck, "#CE9178"),
                createSettingRow("Match Page Title Color", "Colors the top header text to match the current section's theme.", matchTitleColorCheck, "#CE9178"),
                createSettingRow("Always on Top", "Pins the Task Tracker window above all other applications.", alwaysOnTopCheck, "#C586C0"),
                createSettingRow("Enable 'Text to Task'", "Allows pasting bulk text in the context menu to automatically generate multiple tasks.", chkTextToTask, "#C586C0"),
                createSettingRow("Show Sidebar Active Count", "Displays the number of unfinished tasks directly on the sidebar buttons.", chkSidebarCount, "#C586C0"),
                createSettingRow("Enable Custom Stats", "Turns on the RPG points system across the entire application and tracks them in Analytics.", chkGlobalStats, "#B5CEA8"),
                createSettingRow("Expanded Stats Info", "Shows the full stat name, category (Reward/Cost/Penalty), and custom colors on task cards instead of compact icons.", chkExpandedStats, "#B5CEA8")
        );

        Label navHeader = new Label("Static Sidebar Texts & Colors");
        navHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #AAAAAA;");
        VBox.setMargin(navHeader, new Insets(10, 0, 0, 0));

        // --- FIXED: Reduced vertical gap between rows to 8 (was 15) ---
        VBox navList = new VBox(8);

        TextField focusNavField = new TextField(appStats.getNavFocusText());
        TextField analyticsNavField = new TextField(appStats.getNavAnalyticsText());
        TextField archiveNavField = new TextField(appStats.getNavArchiveText());
        TextField settingsNavField = new TextField(appStats.getNavSettingsText());

        String tfStyle = "-fx-background-color: #2D2D30; -fx-text-fill: white; -fx-border-color: #555555; -fx-border-radius: 3; -fx-pref-width: 120;";
        focusNavField.setStyle(tfStyle); analyticsNavField.setStyle(tfStyle); archiveNavField.setStyle(tfStyle); settingsNavField.setStyle(tfStyle);

        ColorPicker focusColorPicker = new ColorPicker(Color.web(appStats.getNavFocusColor()));
        ColorPicker analyticsColorPicker = new ColorPicker(Color.web(appStats.getNavAnalyticsColor()));
        ColorPicker archiveColorPicker = new ColorPicker(Color.web(appStats.getNavArchiveColor()));
        ColorPicker settingsColorPicker = new ColorPicker(Color.web(appStats.getNavSettingsColor()));

        String cpStyle = "-fx-background-color: #2D2D30; -fx-color-label-visible: false; -fx-border-color: #555555; -fx-border-radius: 3; -fx-pref-width: 45;";
        focusColorPicker.setStyle(cpStyle); analyticsColorPicker.setStyle(cpStyle); archiveColorPicker.setStyle(cpStyle); settingsColorPicker.setStyle(cpStyle);

        navList.getChildren().addAll(
                createSettingRow("Focus Hub", "Sidebar label and theme color for the Pomodoro timer module.", new HBox(5, focusNavField, focusColorPicker), "#FF6666"),
                createSettingRow("Analytics", "Sidebar label and theme color for the charts and statistics module.", new HBox(5, analyticsNavField, analyticsColorPicker), "#FFD700"),
                createSettingRow("Archived", "Sidebar label and theme color for the deleted/archived tasks module.", new HBox(5, archiveNavField, archiveColorPicker), "#C586C0"),
                createSettingRow("Settings", "Sidebar label and theme color for this configuration module.", new HBox(5, settingsNavField, settingsColorPicker), "#AAAAAA")
        );

        Runnable autoSaveTrigger = () -> {
            appStats.setTaskFontSize(fontSizeSpinner.getValue());
            appStats.setMinDailyCompletionPercent((int) streakSlider.getValue());
            appStats.setRunInBackground(runInBackgroundCheck.isSelected());
            appStats.setRunOnStartup(chkStartup.isSelected());
            appStats.setEnableNotifications(notificationsCheck.isSelected());
            appStats.setMatchDailyRectColor(matchRectCheck.isSelected());
            appStats.setMatchPriorityOutline(matchOutlineCheck.isSelected());
            appStats.setMatchTitleColor(matchTitleColorCheck.isSelected());
            appStats.setAlwaysOnTop(alwaysOnTopCheck.isSelected());
            appStats.setZenModeThreshold(zenSpinner.getValue());
            appStats.setGlobalStatsEnabled(chkGlobalStats.isSelected());
            appStats.setEnableTextToTask(chkTextToTask.isSelected());
            appStats.setShowSidebarTaskCount(chkSidebarCount.isSelected());
            appStats.setCheckboxTheme(themeBox.getValue());
            appStats.setFocusInactivityThreshold(inactivitySpinner.getValue());
            appStats.setExpandStatMiniCards(chkExpandedStats.isSelected());

            WindowsStartupManager.setStartupEnabled(chkStartup.isSelected());

            appStats.setNavFocusText(focusNavField.getText().trim().isEmpty() ? "Focus Hub" : focusNavField.getText().trim());
            appStats.setNavArchiveText(archiveNavField.getText().trim().isEmpty() ? "Archived" : archiveNavField.getText().trim());
            appStats.setNavSettingsText(settingsNavField.getText().trim().isEmpty() ? "Settings" : settingsNavField.getText().trim());
            appStats.setNavFocusColor(toHexString(focusColorPicker.getValue()));
            appStats.setNavArchiveColor(toHexString(archiveColorPicker.getValue()));
            appStats.setNavSettingsColor(toHexString(settingsColorPicker.getValue()));
            appStats.setNavAnalyticsText(analyticsNavField.getText().trim().isEmpty() ? "Analytics" : analyticsNavField.getText().trim());
            appStats.setNavAnalyticsColor(toHexString(analyticsColorPicker.getValue()));

            StorageManager.saveStats(appStats);
            refreshCallback.run();
        };

        fontSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> autoSaveTrigger.run());
        zenSpinner.valueProperty().addListener((obs, oldVal, newVal) -> autoSaveTrigger.run());
        streakSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> { if (!isChanging) autoSaveTrigger.run(); });
        streakSlider.setOnMouseReleased(e -> autoSaveTrigger.run());
        runInBackgroundCheck.setOnAction(e -> autoSaveTrigger.run());
        chkStartup.setOnAction(e -> autoSaveTrigger.run());
        notificationsCheck.setOnAction(e -> autoSaveTrigger.run());
        matchRectCheck.setOnAction(e -> autoSaveTrigger.run());
        matchOutlineCheck.setOnAction(e -> autoSaveTrigger.run());
        matchTitleColorCheck.setOnAction(e -> autoSaveTrigger.run());
        chkGlobalStats.setOnAction(e -> autoSaveTrigger.run());
        chkExpandedStats.setOnAction(e -> autoSaveTrigger.run());
        chkTextToTask.setOnAction(e -> autoSaveTrigger.run());
        chkSidebarCount.setOnAction(e -> autoSaveTrigger.run());
        themeBox.setOnAction(e -> autoSaveTrigger.run());

        alwaysOnTopCheck.setOnAction(e -> {
            autoSaveTrigger.run();
            if (getScene() != null && getScene().getWindow() instanceof javafx.stage.Stage) {
                ((javafx.stage.Stage) getScene().getWindow()).setAlwaysOnTop(alwaysOnTopCheck.isSelected());
            }
        });

        for (TextField tf : Arrays.asList(focusNavField, analyticsNavField, archiveNavField, settingsNavField)) {
            tf.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> { if (!isNowFocused) autoSaveTrigger.run(); });
            tf.setOnAction(e -> autoSaveTrigger.run());
        }

        focusColorPicker.setOnAction(e -> autoSaveTrigger.run());
        analyticsColorPicker.setOnAction(e -> autoSaveTrigger.run());
        archiveColorPicker.setOnAction(e -> autoSaveTrigger.run());
        settingsColorPicker.setOnAction(e -> autoSaveTrigger.run());

        getChildren().addAll(
                textHeader, behaviorHeader, behaviorList, new Separator(),
                navHeader, navList
        );
    }

    private HBox createSettingRow(String title, String desc, Node control, String colorHex) {
        // --- FIXED: Tightened row spacing ---
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Rectangle rect = new Rectangle(5, 20, Color.web(colorHex));
        rect.setArcWidth(3);
        rect.setArcHeight(3);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-text-fill: #858585; -fx-font-size: 11px;");
        descLabel.setWrapText(true);

        VBox textVBox = new VBox(0, titleLabel, descLabel);

        HBox leftSide = new HBox(8, rect, textVBox);
        leftSide.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (control instanceof Region && !(control instanceof CheckBox) && !(control instanceof HBox)) {
            ((Region) control).setPrefWidth(170);
            ((Region) control).setMaxWidth(170);
        }

        row.getChildren().addAll(leftSide, spacer, control);
        return row;
    }

    private String toHexString(Color color) {
        if (color == null) return null;
        return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
    }
}