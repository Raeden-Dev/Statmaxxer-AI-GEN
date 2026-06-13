package com.raeden.ors_to_do.modules.dependencies.settings;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.ColorUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.UUID;

public class SectionEditDialog {

    public static void show(SectionConfig config, boolean isNew, AppStats appStats, Runnable onSave) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Create New Section" : "Edit Section: " + config.getName());
        TaskDialogs.styleDialog(dialog);

        VBox content = new VBox(15);
        content.setPadding(new Insets(10));
        content.setPrefWidth(700);

        // --- Presets Row ---
        HBox presetRow = new HBox(10);
        presetRow.setAlignment(Pos.CENTER_LEFT);
        presetRow.setStyle("-fx-background-color: #2D2D30; -fx-padding: 10; -fx-border-color: #3E3E42; -fx-border-radius: 5; -fx-background-radius: 5;");
        Label presetLabel = new Label("Load Preset:"); presetLabel.setStyle("-fx-text-fill: #569CD6; -fx-font-weight: bold;");
        ComboBox<SectionConfig> presetBox = new ComboBox<>();
        presetBox.setStyle("-fx-background-color: #3E3E42; -fx-cursor: hand;"); presetBox.setPrefWidth(200);
        presetBox.getItems().add(null); presetBox.getItems().addAll(appStats.getSectionPresets());
        presetBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(SectionConfig item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select Preset..." : item.getName());
                setStyle("-fx-text-fill: " + (empty || item == null ? "#AAAAAA" : "black") + ";");
            }
        });
        presetBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(SectionConfig item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select Preset..." : item.getName());
                setStyle("-fx-text-fill: white;");
            }
        });
        Button savePresetBtn = new Button("💾 Save Current Config as Preset");
        savePresetBtn.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: white; -fx-cursor: hand;");
        Region presetSpacer = new Region(); HBox.setHgrow(presetSpacer, Priority.ALWAYS);
        presetRow.getChildren().addAll(presetLabel, presetBox, presetSpacer, savePresetBtn);
        content.getChildren().add(presetRow);

        // --- Basic Info Row ---
        HBox basicInfoRow = new HBox(15); basicInfoRow.setAlignment(Pos.CENTER_LEFT);
        VBox nameBox = new VBox(5, new Label("Name:"), new TextField(config.getName()));
        ((TextField)nameBox.getChildren().get(1)).setPrefWidth(300);
        ColorPicker colorPicker = new ColorPicker(Color.web(config.getSidebarColor() != null ? config.getSidebarColor() : "#FFFFFF"));
        colorPicker.setStyle("-fx-color-label-visible: false;");
        VBox colorBox = new VBox(5, new Label("Theme Color:"), colorPicker);
        Spinner<Integer> intervalSpinner = new Spinner<>(0, 8760, config.getResetIntervalHours());
        intervalSpinner.setEditable(true); intervalSpinner.setPrefWidth(80);
        intervalSpinner.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            try { intervalSpinner.getValueFactory().setValue(Integer.parseInt(newText.trim())); } catch (NumberFormatException ignored) {}
        });
        intervalSpinner.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                try { intervalSpinner.getValueFactory().setValue(Integer.parseInt(intervalSpinner.getEditor().getText().trim())); }
                catch (NumberFormatException e) { intervalSpinner.getEditor().setText(String.valueOf(intervalSpinner.getValue())); }
            }
        });
        VBox intervalBox = new VBox(5, new Label("Reset Interval (Hours):"), intervalSpinner);

        // --- Per-section "Prevent Editing (Hours)" (formerly a global setting). 0 = disabled. ---
        Spinner<Integer> preventEditingSpinner = new Spinner<>(0, 8760, config.getPreventEditingHours());
        preventEditingSpinner.setEditable(true); preventEditingSpinner.setPrefWidth(80);
        preventEditingSpinner.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                try { preventEditingSpinner.getValueFactory().setValue(Integer.parseInt(preventEditingSpinner.getEditor().getText().trim())); }
                catch (NumberFormatException e) { preventEditingSpinner.getEditor().setText(String.valueOf(preventEditingSpinner.getValue())); }
            }
        });
        Label preventEditingLabel = new Label("Prevent Editing After (Hours):");
        Tooltip.install(preventEditingLabel, new Tooltip("Hours after creation before tasks in this section are permanently locked from being edited. 0 disables the lock."));
        VBox preventEditingBox = new VBox(5, preventEditingLabel, preventEditingSpinner);

        basicInfoRow.getChildren().addAll(nameBox, colorBox, intervalBox, preventEditingBox);
        content.getChildren().addAll(basicInfoRow, new Separator());

        // --- Special Modes Box (moved up: sits directly below Name / Theme Color / etc. and
        // above the feature checkboxes, so the page "type" is chosen before its options). ---
        VBox specialModesBox = new VBox(10);
        specialModesBox.setStyle("-fx-border-color: #555555; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #252526; -fx-background-radius: 5;");
        Label modeHeader = new Label("Special Page Modes (Select up to One):"); modeHeader.setStyle("-fx-text-fill: #AAAAAA; -fx-font-style: italic; -fx-font-size: 12px;");
        FlowPane modeToggles = new FlowPane(20, 10);

        CheckBox notesPageCheck = new CheckBox("Notes Page"); notesPageCheck.setSelected(config.isNotesPage()); notesPageCheck.setStyle("-fx-text-fill: #4EC9B0; -fx-font-weight: bold;");
        CheckBox statPageCheck = new CheckBox("Stat Page"); statPageCheck.setSelected(config.isStatPage()); statPageCheck.setStyle("-fx-text-fill: #FF6666; -fx-font-weight: bold;");
        CheckBox perkPageCheck = new CheckBox("Perk Page"); perkPageCheck.setSelected(config.isPerkPage()); perkPageCheck.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold;");
        CheckBox rewardsPageCheck = new CheckBox("Rewards Shop"); rewardsPageCheck.setSelected(config.isRewardsPage()); rewardsPageCheck.setStyle("-fx-text-fill: #569CD6; -fx-font-weight: bold;");
        CheckBox challengePageCheck = new CheckBox("Challenge Page"); challengePageCheck.setSelected(config.isChallengePage()); challengePageCheck.setStyle("-fx-text-fill: #FF8C00; -fx-font-weight: bold;");
        CheckBox calendarPageCheck = new CheckBox("Calendar Page"); calendarPageCheck.setSelected(config.isCalendarPage()); calendarPageCheck.setStyle("-fx-text-fill: #C586C0; -fx-font-weight: bold;");

        modeToggles.getChildren().addAll(notesPageCheck, statPageCheck, perkPageCheck, rewardsPageCheck, challengePageCheck, calendarPageCheck);
        specialModesBox.getChildren().addAll(modeHeader, modeToggles);
        content.getChildren().addAll(specialModesBox, new Separator());

        // --- Calendar-only options (display style, manipulation of past days, XP grant). Shown only
        // when Calendar Page is selected; wired up after the master state engine below. ---
        VBox calendarOptionsBox = new VBox(8);
        calendarOptionsBox.setStyle("-fx-border-color: #C586C0; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #2A2330; -fx-background-radius: 5;");
        Label calHeader = new Label(com.raeden.ors_to_do.i18n.Lang.SEC_CAL_OPTIONS_HEADER.get()); calHeader.setStyle("-fx-text-fill: #C586C0; -fx-font-weight: bold; -fx-font-size: 12px;");
        CheckBox calSegmentsCheck = new CheckBox(com.raeden.ors_to_do.i18n.Lang.SEC_CAL_SEGMENTS.get()); calSegmentsCheck.setSelected(config.isCalendarShowSegments()); calSegmentsCheck.setStyle("-fx-text-fill: white;");
        CheckBox calDotsCheck = new CheckBox(com.raeden.ors_to_do.i18n.Lang.SEC_CAL_DOTS.get()); calDotsCheck.setSelected(config.isCalendarShowDots()); calDotsCheck.setStyle("-fx-text-fill: white;");
        CheckBox calManipulationCheck = new CheckBox(com.raeden.ors_to_do.i18n.Lang.SEC_CAL_MANIPULATION.get()); calManipulationCheck.setSelected(config.isAllowCalendarManipulation()); calManipulationCheck.setStyle("-fx-text-fill: white;");
        CheckBox calGrantXpCheck = new CheckBox(com.raeden.ors_to_do.i18n.Lang.SEC_CAL_GRANT.get()); calGrantXpCheck.setSelected(config.isCalendarGrantsXp()); calGrantXpCheck.setStyle("-fx-text-fill: white;");
        CheckBox calJournalCheck = new CheckBox(com.raeden.ors_to_do.i18n.Lang.SEC_CAL_JOURNAL.get()); calJournalCheck.setSelected(config.isCalendarJournalEnabled()); calJournalCheck.setStyle("-fx-text-fill: white;");
        CheckBox calJournalOnlyCheck = new CheckBox(com.raeden.ors_to_do.i18n.Lang.SEC_CAL_JOURNAL_ONLY.get()); calJournalOnlyCheck.setSelected(config.isCalendarJournalOnly()); calJournalOnlyCheck.setStyle("-fx-text-fill: white;");
        // Journal-only implies journal enabled; keep the two checkboxes consistent.
        calJournalOnlyCheck.selectedProperty().addListener((o, was, on) -> { if (on) calJournalCheck.setSelected(true); });
        calJournalCheck.selectedProperty().addListener((o, was, on) -> { if (!on) calJournalOnlyCheck.setSelected(false); });
        Label calDesc = new Label(com.raeden.ors_to_do.i18n.Lang.SEC_CAL_DESC.get());
        calDesc.setStyle("-fx-text-fill: #858585; -fx-font-size: 11px;"); calDesc.setWrapText(true);
        calendarOptionsBox.getChildren().addAll(calHeader, calSegmentsCheck, calDotsCheck, calManipulationCheck, calGrantXpCheck, calJournalCheck, calJournalOnlyCheck, calDesc);
        content.getChildren().add(calendarOptionsBox);

        // --- Features Grid ---
        GridPane featuresGrid = new GridPane(); featuresGrid.setHgap(20); featuresGrid.setVgap(15);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        featuresGrid.getColumnConstraints().addAll(col1, col2);

        CheckBox allowManualArchiveCheck = new CheckBox("Allow Manual Archiving"); allowManualArchiveCheck.setSelected(config.isAllowManualArchiving());
        CheckBox enableSubTasksCheck = new CheckBox("Enable Sub-Tasks"); enableSubTasksCheck.setSelected(config.isEnableSubTasks());
        CheckBox showDateCheck = new CheckBox("Show Creation Date"); showDateCheck.setSelected(config.isShowDate());
        CheckBox showPrefixCheck = new CheckBox("Enable Custom Prefixes"); showPrefixCheck.setSelected(config.isShowPrefix());
        CheckBox showTagsCheck = new CheckBox("Enable Dynamic Filter Tags"); showTagsCheck.setSelected(config.isShowTags());
        CheckBox enableScoreCheck = new CheckBox("Enable Point System"); enableScoreCheck.setSelected(config.isEnableScore());
        CheckBox enableLinksCheck = new CheckBox("Enable Sub-Task Links"); enableLinksCheck.setSelected(config.isEnableLinks());
        CheckBox enableStatsSystemCheck = new CheckBox("Enable Custom Stats"); enableStatsSystemCheck.setSelected(config.isEnableStatsSystem());
        CheckBox enableZenModeCheck = new CheckBox("Allow Zen Mode"); enableZenModeCheck.setSelected(config.isEnableZenMode());
        CheckBox enableTaskStylingCheck = new CheckBox("Enable Task Styling"); enableTaskStylingCheck.setSelected(config.isEnableTaskStyling());
        CheckBox enableTimedTasksCheck = new CheckBox("Allow Timed Tasks"); enableTimedTasksCheck.setSelected(config.isEnableTimedTasks());

        CheckBox streakCheck = new CheckBox("Enable Streak System"); streakCheck.setSelected(config.isHasStreak());
        CheckBox autoArchiveCheck = new CheckBox("Auto-Archive Completed"); autoArchiveCheck.setSelected(config.isAutoArchive());
        CheckBox showPriorityCheck = new CheckBox("Show Priority Toggles"); showPriorityCheck.setSelected(config.isShowPriority());
        CheckBox trackTimeCheck = new CheckBox("Track Focus Time"); trackTimeCheck.setSelected(config.isTrackTime());
        CheckBox showTaskTypeCheck = new CheckBox("Enable Work Types"); showTaskTypeCheck.setSelected(config.isShowTaskType());
        CheckBox favoriteCheck = new CheckBox("Enable Favorite System"); favoriteCheck.setSelected(config.isAllowFavorite());
        CheckBox showAnalyticsCheck = new CheckBox("Show Analytics Export"); showAnalyticsCheck.setSelected(config.isShowAnalytics());
        CheckBox enableIconsCheck = new CheckBox("Enable Task Icons"); enableIconsCheck.setSelected(config.isEnableIcons());
        CheckBox enableOptionalTasksCheck = new CheckBox("Enable Optional Tasks"); enableOptionalTasksCheck.setSelected(config.isEnableOptionalTasks());
        CheckBox enableLinkCardsCheck = new CheckBox("Enable Link Cards"); enableLinkCardsCheck.setSelected(config.isEnableLinkCards());
        CheckBox enableDescriptionCardsCheck = new CheckBox("Enable Description Cards"); enableDescriptionCardsCheck.setSelected(config.isEnableDescriptionCards());
        CheckBox allowRepeatingTasksCheck = new CheckBox("Allow Repeating Tasks"); allowRepeatingTasksCheck.setSelected(config.isAllowRepeatingTasks());

        // --- NEW: Lock Task Checkbox ---
        CheckBox lockCompletedCheck = new CheckBox("Lock Task After Completion"); lockCompletedCheck.setSelected(config.isLockCompletedTasks());

        // --- NEW: Categories toggle ---
        CheckBox enableCategoriesCheck = new CheckBox(com.raeden.ors_to_do.i18n.Lang.CATEGORY_ENABLE_TOGGLE.get());
        enableCategoriesCheck.setSelected(config.isEnableCategories());

        featuresGrid.add(createToggle(allowManualArchiveCheck, "Enables right-click to send tasks to Archive."), 0, 0);
        featuresGrid.add(createToggle(enableSubTasksCheck, "Allows creating nested to-do items inside a card."), 0, 1);
        featuresGrid.add(createToggle(showDateCheck, "Displays the exact date the task was generated."), 0, 2);
        featuresGrid.add(createToggle(showPrefixCheck, "Allows prefixing tags like [GYM] with custom colors."), 0, 3);
        featuresGrid.add(createToggle(showTagsCheck, "Auto-generates clickable sorting buttons at the top of the page."), 0, 4);
        featuresGrid.add(createToggle(enableScoreCheck, "Allows adding and earning score points for tasks."), 0, 5);
        featuresGrid.add(createToggle(enableLinksCheck, "Allows attaching clickable URLs to sub-tasks."), 0, 6);
        featuresGrid.add(createToggle(enableStatsSystemCheck, "Allows tasks to grant XP towards your Custom RPG Stats."), 0, 7);
        featuresGrid.add(createToggle(enableZenModeCheck, "Adds a focus mode button that unlocks when threshold is met."), 0, 8);
        featuresGrid.add(createToggle(enableTaskStylingCheck, "Allows custom background and outline colors for individual tasks."), 0, 9);
        featuresGrid.add(createToggle(enableTimedTasksCheck, "Tasks require specific focus time to complete."), 0, 10);
        featuresGrid.add(createToggle(lockCompletedCheck, "Disables un-checking or editing tasks once completed."), 0, 11);
        featuresGrid.add(createToggle(enableDescriptionCardsCheck, "Allows cards that show a Copy button (instead of a checkbox) which copies their description text."), 0, 12);

        featuresGrid.add(createToggle(streakCheck, "Tracks consecutive completions. Requires a reset interval."), 1, 0);
        featuresGrid.add(createToggle(autoArchiveCheck, "Tasks are sent to archive the moment they are checked off."), 1, 1);
        featuresGrid.add(createToggle(showPriorityCheck, "Adds a priority ranking dropdown to each task."), 1, 2);
        featuresGrid.add(createToggle(trackTimeCheck, "Links tasks to the Pomodoro Focus Hub timer."), 1, 3);
        featuresGrid.add(createToggle(showTaskTypeCheck, "Displays an editable string box for categorization."), 1, 4);
        featuresGrid.add(createToggle(favoriteCheck, "Allows starring tasks for a golden border override."), 1, 5);
        featuresGrid.add(createToggle(showAnalyticsCheck, "Displays a button to export an HTML graph of completed tasks."), 1, 6);
        featuresGrid.add(createToggle(enableIconsCheck, "Allows attaching custom color-coded symbols to tasks."), 1, 7);
        featuresGrid.add(createToggle(enableOptionalTasksCheck, "Allows tasks that grant bonus points but do not count to totals."), 1, 8);
        featuresGrid.add(createToggle(enableLinkCardsCheck, "Allows creating tasks that act purely as clickable shortcuts."), 1, 9);
        featuresGrid.add(createToggle(allowRepeatingTasksCheck, "Turns cards into unlimited clickers to farm stats/points."), 1, 10);
        featuresGrid.add(createToggle(enableCategoriesCheck, com.raeden.ors_to_do.i18n.Lang.CATEGORY_ENABLE_DESC.get()), 1, 11);

        content.getChildren().addAll(featuresGrid, new Separator());

        // --- Master State Engine ---
        Runnable updateUIState = () -> {
            boolean isNotes = notesPageCheck.isSelected(); boolean isStat = statPageCheck.isSelected();
            boolean isPerk = perkPageCheck.isSelected(); boolean isReward = rewardsPageCheck.isSelected();
            boolean isChallenge = challengePageCheck.isSelected();
            boolean isCalendar = calendarPageCheck.isSelected();
            boolean isSpecial = isNotes || isStat || isPerk || isReward || isChallenge || isCalendar;

            calendarOptionsBox.setVisible(isCalendar);
            calendarOptionsBox.setManaged(isCalendar);

            if (isStat || isPerk || isReward || isChallenge || isCalendar) {
                intervalSpinner.setDisable(true); if (intervalSpinner.getValue() != 0) intervalSpinner.getValueFactory().setValue(0);
                autoArchiveCheck.setDisable(true); autoArchiveCheck.setSelected(false);
            } else {
                intervalSpinner.setDisable(false); autoArchiveCheck.setDisable(false);
            }
            boolean hasInterval = intervalSpinner.getValue() != null && intervalSpinner.getValue() > 0;

            if (isSpecial) {
                enableSubTasksCheck.setDisable(true); enableSubTasksCheck.setSelected(false);
                if(!isNotes) { enableLinkCardsCheck.setDisable(true); enableLinkCardsCheck.setSelected(false); }
                trackTimeCheck.setDisable(true); trackTimeCheck.setSelected(false);
                enableZenModeCheck.setDisable(true); enableZenModeCheck.setSelected(false);
                showPriorityCheck.setDisable(!isReward); if (!isReward) showPriorityCheck.setSelected(false);
                enableTimedTasksCheck.setDisable(true); enableTimedTasksCheck.setSelected(false);
                allowRepeatingTasksCheck.setDisable(true); allowRepeatingTasksCheck.setSelected(false);
            } else {
                boolean subTasks = enableSubTasksCheck.isSelected(); boolean links = enableLinkCardsCheck.isSelected(); boolean focus = trackTimeCheck.isSelected();
                enableSubTasksCheck.setDisable(links); enableLinkCardsCheck.setDisable(subTasks || focus); trackTimeCheck.setDisable(links);
                enableZenModeCheck.setDisable(false); showPriorityCheck.setDisable(false);
                enableTimedTasksCheck.setDisable(false); allowRepeatingTasksCheck.setDisable(false);
            }

            // --- Page-type compatibility matrix ---------------------------------------
            // Reset the matrix-managed toggles each pass, then disable whatever the selected
            // page type can't use, so incompatible settings can't be left on silently.
            CheckBox[] matrixToggles = { allowManualArchiveCheck, showDateCheck, showPrefixCheck, showTagsCheck,
                    enableScoreCheck, enableLinksCheck, enableStatsSystemCheck, enableTaskStylingCheck,
                    lockCompletedCheck, showTaskTypeCheck, favoriteCheck, showAnalyticsCheck,
                    enableIconsCheck, enableCategoriesCheck, enableDescriptionCardsCheck };
            for (CheckBox cb : matrixToggles) cb.setDisable(false);
            preventEditingSpinner.setDisable(false);

            java.util.function.Consumer<CheckBox> off = cb -> { cb.setDisable(true); cb.setSelected(false); };

            // Description cards replace the checkbox with a Copy button on a normal task card, so
            // they're incompatible with pages that don't render a normal checkbox: rewards (Buy
            // button), perk/challenge (specialized cards), and calendar/stat (no task cards). They
            // are allowed on normal pages and notes pages.
            if (isReward || isPerk || isChallenge) off.accept(enableDescriptionCardsCheck);

            if (isCalendar || isStat) {
                // Calendar and Stat pages render no task cards — every task feature is inert.
                for (CheckBox cb : matrixToggles) off.accept(cb);
                preventEditingSpinner.setDisable(true);
                if (preventEditingSpinner.getValue() != 0) preventEditingSpinner.getValueFactory().setValue(0);
            } else if (isPerk || isChallenge) {
                // Perk/Challenge cards configure icons, colours and rewards in their own dialogs,
                // never render tag pills / creation dates / favorites, and lock themselves on
                // completion — so those section toggles would be dead switches here.
                off.accept(showTagsCheck); off.accept(enableScoreCheck); off.accept(enableStatsSystemCheck);
                off.accept(showDateCheck); off.accept(favoriteCheck); off.accept(enableIconsCheck);
                off.accept(enableTaskStylingCheck); off.accept(allowManualArchiveCheck);
                off.accept(lockCompletedCheck); off.accept(showTaskTypeCheck); off.accept(enableLinksCheck);
            } else if (isNotes) {
                // Notes never complete, so completion-driven features have nothing to act on.
                off.accept(enableScoreCheck); off.accept(enableStatsSystemCheck);
                off.accept(showTaskTypeCheck); off.accept(lockCompletedCheck); off.accept(enableLinksCheck);
            } else if (!enableSubTasksCheck.isSelected()) {
                // Regular / rewards pages: sub-task links require sub-tasks to exist.
                off.accept(enableLinksCheck);
            }

            streakCheck.setDisable(!hasInterval); if (!hasInterval) streakCheck.setSelected(false);
            boolean hasPoints = enableScoreCheck.isSelected() || enableStatsSystemCheck.isSelected();
            enableOptionalTasksCheck.setDisable(!hasInterval || !hasPoints); if (!hasInterval || !hasPoints) enableOptionalTasksCheck.setSelected(false);
        };

        intervalSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateUIState.run());
        enableSubTasksCheck.setOnAction(e -> updateUIState.run());
        enableLinkCardsCheck.setOnAction(e -> updateUIState.run());
        trackTimeCheck.setOnAction(e -> updateUIState.run());
        enableScoreCheck.setOnAction(e -> updateUIState.run());
        enableStatsSystemCheck.setOnAction(e -> updateUIState.run());

        rewardsPageCheck.setOnAction(e -> { if(rewardsPageCheck.isSelected()) { notesPageCheck.setSelected(false); statPageCheck.setSelected(false); perkPageCheck.setSelected(false); challengePageCheck.setSelected(false); calendarPageCheck.setSelected(false); } updateUIState.run(); });
        notesPageCheck.setOnAction(e -> { if(notesPageCheck.isSelected()) { rewardsPageCheck.setSelected(false); statPageCheck.setSelected(false); perkPageCheck.setSelected(false); challengePageCheck.setSelected(false); calendarPageCheck.setSelected(false); } updateUIState.run(); });
        statPageCheck.setOnAction(e -> { if(statPageCheck.isSelected()) { rewardsPageCheck.setSelected(false); notesPageCheck.setSelected(false); perkPageCheck.setSelected(false); challengePageCheck.setSelected(false); calendarPageCheck.setSelected(false); } updateUIState.run(); });
        perkPageCheck.setOnAction(e -> { if(perkPageCheck.isSelected()) { rewardsPageCheck.setSelected(false); notesPageCheck.setSelected(false); statPageCheck.setSelected(false); challengePageCheck.setSelected(false); calendarPageCheck.setSelected(false); } updateUIState.run(); });
        challengePageCheck.setOnAction(e -> { if(challengePageCheck.isSelected()) { rewardsPageCheck.setSelected(false); notesPageCheck.setSelected(false); statPageCheck.setSelected(false); perkPageCheck.setSelected(false); calendarPageCheck.setSelected(false); } updateUIState.run(); });
        calendarPageCheck.setOnAction(e -> { if(calendarPageCheck.isSelected()) { rewardsPageCheck.setSelected(false); notesPageCheck.setSelected(false); statPageCheck.setSelected(false); perkPageCheck.setSelected(false); challengePageCheck.setSelected(false); } updateUIState.run(); });

        presetBox.setOnAction(e -> {
            SectionConfig p = presetBox.getValue();
            if (p != null) {
                intervalSpinner.getValueFactory().setValue(p.getResetIntervalHours()); streakCheck.setSelected(p.isHasStreak());
                allowManualArchiveCheck.setSelected(p.isAllowManualArchiving()); enableSubTasksCheck.setSelected(p.isEnableSubTasks());
                showDateCheck.setSelected(p.isShowDate()); showPrefixCheck.setSelected(p.isShowPrefix()); showTagsCheck.setSelected(p.isShowTags());
                enableScoreCheck.setSelected(p.isEnableScore()); enableLinksCheck.setSelected(p.isEnableLinks());
                rewardsPageCheck.setSelected(p.isRewardsPage()); autoArchiveCheck.setSelected(p.isAutoArchive());
                showPriorityCheck.setSelected(p.isShowPriority()); trackTimeCheck.setSelected(p.isTrackTime());
                showTaskTypeCheck.setSelected(p.isShowTaskType()); favoriteCheck.setSelected(p.isAllowFavorite());
                showAnalyticsCheck.setSelected(p.isShowAnalytics()); enableIconsCheck.setSelected(p.isEnableIcons());
                enableZenModeCheck.setSelected(p.isEnableZenMode()); enableStatsSystemCheck.setSelected(p.isEnableStatsSystem());
                enableLinkCardsCheck.setSelected(p.isEnableLinkCards()); enableDescriptionCardsCheck.setSelected(p.isEnableDescriptionCards());
                notesPageCheck.setSelected(p.isNotesPage());
                statPageCheck.setSelected(p.isStatPage()); perkPageCheck.setSelected(p.isPerkPage());
                challengePageCheck.setSelected(p.isChallengePage());
                calendarPageCheck.setSelected(p.isCalendarPage());
                calSegmentsCheck.setSelected(p.isCalendarShowSegments()); calDotsCheck.setSelected(p.isCalendarShowDots());
                calManipulationCheck.setSelected(p.isAllowCalendarManipulation()); calGrantXpCheck.setSelected(p.isCalendarGrantsXp());
                calJournalCheck.setSelected(p.isCalendarJournalEnabled()); calJournalOnlyCheck.setSelected(p.isCalendarJournalOnly());
                enableOptionalTasksCheck.setSelected(p.isEnableOptionalTasks()); enableTaskStylingCheck.setSelected(p.isEnableTaskStyling());
                enableTimedTasksCheck.setSelected(p.isEnableTimedTasks());
                allowRepeatingTasksCheck.setSelected(p.isAllowRepeatingTasks());

                // --- NEW: Load preset lock state ---
                lockCompletedCheck.setSelected(p.isLockCompletedTasks());
                enableCategoriesCheck.setSelected(p.isEnableCategories());
                preventEditingSpinner.getValueFactory().setValue(p.getPreventEditingHours());

                updateUIState.run();
            }
        });

        savePresetBtn.setOnAction(e -> {
            TextInputDialog nameDialog = new TextInputDialog("Custom Preset");
            nameDialog.setTitle("Save Preset"); nameDialog.setHeaderText("Enter a name for this preset configuration:");
            TaskDialogs.styleDialog(nameDialog);
            nameDialog.showAndWait().ifPresent(presetName -> {
                SectionConfig newPreset = new SectionConfig(UUID.randomUUID().toString(), presetName);
                newPreset.setResetIntervalHours(intervalSpinner.getValue()); newPreset.setHasStreak(streakCheck.isSelected());
                newPreset.setAllowManualArchiving(allowManualArchiveCheck.isSelected()); newPreset.setEnableSubTasks(enableSubTasksCheck.isSelected());
                newPreset.setShowDate(showDateCheck.isSelected()); newPreset.setShowPrefix(showPrefixCheck.isSelected()); newPreset.setShowTags(showTagsCheck.isSelected());
                newPreset.setEnableScore(enableScoreCheck.isSelected()); newPreset.setEnableLinks(enableLinksCheck.isSelected());
                newPreset.setRewardsPage(rewardsPageCheck.isSelected()); newPreset.setAutoArchive(autoArchiveCheck.isSelected());
                newPreset.setShowPriority(showPriorityCheck.isSelected()); newPreset.setTrackTime(trackTimeCheck.isSelected());
                newPreset.setShowTaskType(showTaskTypeCheck.isSelected()); newPreset.setAllowFavorite(favoriteCheck.isSelected());
                newPreset.setShowAnalytics(showAnalyticsCheck.isSelected()); newPreset.setEnableIcons(enableIconsCheck.isSelected());
                newPreset.setEnableZenMode(enableZenModeCheck.isSelected()); newPreset.setEnableStatsSystem(enableStatsSystemCheck.isSelected());
                newPreset.setEnableLinkCards(enableLinkCardsCheck.isSelected()); newPreset.setEnableDescriptionCards(enableDescriptionCardsCheck.isSelected());
                newPreset.setNotesPage(notesPageCheck.isSelected());
                newPreset.setStatPage(statPageCheck.isSelected()); newPreset.setPerkPage(perkPageCheck.isSelected());
                newPreset.setChallengePage(challengePageCheck.isSelected());
                newPreset.setCalendarPage(calendarPageCheck.isSelected());
                newPreset.setCalendarShowSegments(calSegmentsCheck.isSelected()); newPreset.setCalendarShowDots(calDotsCheck.isSelected());
                newPreset.setAllowCalendarManipulation(calManipulationCheck.isSelected()); newPreset.setCalendarGrantsXp(calGrantXpCheck.isSelected());
                newPreset.setCalendarJournalEnabled(calJournalCheck.isSelected()); newPreset.setCalendarJournalOnly(calJournalOnlyCheck.isSelected());
                newPreset.setEnableOptionalTasks(enableOptionalTasksCheck.isSelected()); newPreset.setEnableTaskStyling(enableTaskStylingCheck.isSelected());
                newPreset.setEnableTimedTasks(enableTimedTasksCheck.isSelected());
                newPreset.setAllowRepeatingTasks(allowRepeatingTasksCheck.isSelected());

                // --- NEW: Save preset lock state ---
                newPreset.setLockCompletedTasks(lockCompletedCheck.isSelected());
                newPreset.setEnableCategories(enableCategoriesCheck.isSelected());
                newPreset.setPreventEditingHours(preventEditingSpinner.getValue());

                appStats.getSectionPresets().add(newPreset); presetBox.getItems().add(newPreset); presetBox.setValue(newPreset);
                StorageManager.saveStats(appStats);
            });
        });

        updateUIState.run();

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(720, 600);
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
                String typedName = ((TextField)nameBox.getChildren().get(1)).getText().trim();
                if (isNew && typedName.isEmpty()) return;
                config.setName(typedName.isEmpty() ? "Unnamed Section" : typedName);
                config.setSidebarColor(ColorUtil.toHex(colorPicker.getValue()));
                config.setResetIntervalHours(intervalSpinner.getValue());
                config.setHasStreak(streakCheck.isSelected()); config.setAllowManualArchiving(allowManualArchiveCheck.isSelected());
                config.setEnableSubTasks(enableSubTasksCheck.isSelected()); config.setShowDate(showDateCheck.isSelected());
                config.setShowPrefix(showPrefixCheck.isSelected()); config.setShowTags(showTagsCheck.isSelected());
                config.setEnableScore(enableScoreCheck.isSelected()); config.setEnableLinks(enableLinksCheck.isSelected());
                config.setRewardsPage(rewardsPageCheck.isSelected()); config.setStatPage(statPageCheck.isSelected());
                config.setPerkPage(perkPageCheck.isSelected()); config.setChallengePage(challengePageCheck.isSelected());
                config.setCalendarPage(calendarPageCheck.isSelected());
                config.setCalendarShowSegments(calSegmentsCheck.isSelected()); config.setCalendarShowDots(calDotsCheck.isSelected());
                config.setAllowCalendarManipulation(calManipulationCheck.isSelected()); config.setCalendarGrantsXp(calGrantXpCheck.isSelected());
                config.setCalendarJournalEnabled(calJournalCheck.isSelected()); config.setCalendarJournalOnly(calJournalOnlyCheck.isSelected());
                config.setAutoArchive(autoArchiveCheck.isSelected()); config.setShowPriority(showPriorityCheck.isSelected());
                config.setTrackTime(trackTimeCheck.isSelected()); config.setShowTaskType(showTaskTypeCheck.isSelected());
                config.setAllowFavorite(favoriteCheck.isSelected()); config.setShowAnalytics(showAnalyticsCheck.isSelected());
                config.setEnableIcons(enableIconsCheck.isSelected()); config.setEnableZenMode(enableZenModeCheck.isSelected());
                config.setEnableStatsSystem(enableStatsSystemCheck.isSelected()); config.setEnableLinkCards(enableLinkCardsCheck.isSelected());
                config.setEnableDescriptionCards(enableDescriptionCardsCheck.isSelected());
                config.setNotesPage(notesPageCheck.isSelected()); config.setEnableOptionalTasks(enableOptionalTasksCheck.isSelected());
                config.setEnableTaskStyling(enableTaskStylingCheck.isSelected()); config.setEnableTimedTasks(enableTimedTasksCheck.isSelected());
                config.setAllowRepeatingTasks(allowRepeatingTasksCheck.isSelected());

                // --- NEW: Save Lock State ---
                config.setLockCompletedTasks(lockCompletedCheck.isSelected());

                // --- Per-section prevent-editing window ---
                config.setPreventEditingHours(preventEditingSpinner.getValue());

                // --- Categories toggle ---
                config.setEnableCategories(enableCategoriesCheck.isSelected());

                if (isNew) appStats.getSections().add(config);
                onSave.run();
            }
        });
    }

    private static VBox createToggle(CheckBox cb, String desc) {
        cb.setStyle("-fx-text-fill: white;");
        Label descLabel = new Label(desc); descLabel.setStyle("-fx-text-fill: #858585; -fx-font-size: 11px;");
        return new VBox(2, cb, descLabel);
    }
}