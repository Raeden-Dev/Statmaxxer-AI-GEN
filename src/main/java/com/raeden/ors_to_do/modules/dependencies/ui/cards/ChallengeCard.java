package com.raeden.ors_to_do.modules.dependencies.ui.cards;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.i18n.Lang;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.ChallengeConfigDialog;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.Design;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.TaskActionHandler;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.TaskLinkUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ChallengeCard extends VBox {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private boolean isExpanded = false;

    public ChallengeCard(TaskItem challengeTask, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate) {
        super(10);

        boolean isCompleted = challengeTask.isFinished();
        // A challenge is "failed" when it was locked in as finished without an unlock/conquer date.
        boolean isFailed = isCompleted && challengeTask.getPerkUnlockedDate() == null && challengeTask.getPerkLostDate() != null;
        boolean meetsRequirements = true;
        VBox requirementsBox = new VBox(5);
        requirementsBox.setPadding(new Insets(5, 0, 0, 0));

        // 1. Check Stat Requirements (Unlock Conditions)
        for (Map.Entry<String, Integer> req : challengeTask.getStatRequirements().entrySet()) {
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

        // 2. Check Hooked Dependencies
        if (challengeTask.getDependsOnTaskIds() != null && !challengeTask.getDependsOnTaskIds().isEmpty()) {
            for (String depId : challengeTask.getDependsOnTaskIds()) {
                TaskItem depTask = globalDatabase.stream().filter(t -> t.getId().equals(depId)).findFirst().orElse(null);
                if (depTask != null) {
                    boolean isDepUnlocked = TaskLinkUtil.isDependencyUnlocked(depTask);
                    if (!isDepUnlocked) {
                        meetsRequirements = false;
                        Label l = depTask.isCounterMode()
                                ? new Label(Lang.REQ_DEP_COUNTER_UNMET.get(depTask.getTextContent(), depTask.getCurrentCount(), depTask.getMaxCount()))
                                : new Label(Lang.REQ_DEP_UNMET.get(depTask.getTextContent()));
                        l.setStyle("-fx-text-fill: #FF6666; -fx-font-size: 12px;");
                        requirementsBox.getChildren().add(l);
                    } else {
                        Label l = new Label(Lang.DEP_HOOKED.get(depTask.getTextContent()));
                        l.setStyle("-fx-text-fill: #4EC9B0; -fx-font-size: 12px;");
                        requirementsBox.getChildren().add(l);
                    }
                }
            }
        }

        // 3. Deadline Check
        // When a deadline passes without completion we lock the challenge in as Failed (same as the
        // explicit Fail button). Without this, an expired challenge stays in limbo: the UI shows
        // "Expired!" but the model never persists the failure, so a subsequent rollover (or any
        // ProgressionService re-check) could still treat it as in-flight.
        boolean isExpired = false;
        if (challengeTask.getDeadline() != null && !isCompleted) {
            isExpired = LocalDateTime.now().isAfter(challengeTask.getDeadline());
            if (isExpired) {
                meetsRequirements = false;
                challengeTask.setFinished(true);
                challengeTask.setPermaLock(true);
                challengeTask.setPerkUnlockedDate(null);
                challengeTask.setPerkLostDate(LocalDateTime.now());
                StorageManager.saveTasks(globalDatabase);
                isCompleted = true;
                isFailed = true;
            }
        }

        // 4. Setup Phase & Locking
        LocalDateTime creationTime = challengeTask.getDateCreated();
        boolean isSetupPhase = LocalDateTime.now().isBefore(creationTime.plusMinutes(15));
        boolean isLocked = (!meetsRequirements || isSetupPhase) && !isCompleted;

        // --- STATUS BOX (Always Visible) ---
        VBox statusBox = new VBox(5);
        if (isSetupPhase && !isCompleted) {
            long minsLeft = Duration.between(LocalDateTime.now(), creationTime.plusMinutes(15)).toMinutes();
            Label setupLbl = new Label(Lang.SETUP_PHASE_INACTIVE.get(minsLeft + 1));
            setupLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12px; -fx-font-weight: bold;");
            statusBox.getChildren().add(setupLbl);
        }

        if (isFailed) {
            Label failLbl = new Label(Lang.FAILED_ON.get(challengeTask.getPerkLostDate().format(STAMP)));
            failLbl.setStyle("-fx-text-fill: #E06666; -fx-font-size: 12px; -fx-font-weight: bold;");
            statusBox.getChildren().add(failLbl);
        } else if (isCompleted && challengeTask.getPerkUnlockedDate() != null) {
            Label compLbl = new Label(Lang.CONQUERED_ON.get(challengeTask.getPerkUnlockedDate().format(STAMP)));
            compLbl.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12px; -fx-font-weight: bold;");
            statusBox.getChildren().add(compLbl);
        }

        // Dynamic Custom Colors
        String bgColor = challengeTask.getColorHex() != null && !challengeTask.getColorHex().equals("transparent") ? challengeTask.getColorHex() : "#2D2D30";
        String outlineColor = challengeTask.getCustomOutlineColor() != null && !challengeTask.getCustomOutlineColor().equals("transparent") ? challengeTask.getCustomOutlineColor() : "#FF8C00";
        String iconColor = challengeTask.getIconColor() != null && !challengeTask.getIconColor().equals("transparent") ? challengeTask.getIconColor() : "#FFFFFF";
        String iconStr = (challengeTask.getIconSymbol() != null && !challengeTask.getIconSymbol().equals("None")) ? challengeTask.getIconSymbol() + " " : "⚔️ ";

        // Visual Styling & Glow
        if (isFailed) {
            setStyle("-fx-background-color: #1E1414; -fx-padding: 15; -fx-background-radius: 5; -fx-border-color: #5A2A2A; -fx-border-width: 2; -fx-border-radius: 5; -fx-opacity: 0.85;");
        } else if (isCompleted) {
            setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 15; -fx-background-radius: 5; -fx-border-color: " + outlineColor + "; -fx-border-width: 2; -fx-border-radius: 5; -fx-effect: dropshadow(three-pass-box, " + outlineColor + ", 10, 0.2, 0, 0);");
        } else if (!isLocked) {
            setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 15; -fx-background-radius: 5; -fx-border-color: " + outlineColor + "; -fx-border-radius: 5;");
        } else {
            setStyle("-fx-background-color: #1E1E1E; -fx-padding: 15; -fx-background-radius: 5; -fx-border-color: #3E3E42; -fx-border-radius: 5; -fx-opacity: 0.7;");
        }

        // --- CHECK EDIT TIME LOCK FOR UI ---
        // Prevent-editing window is now stored per-section; legacy global value migrates on launch.
        com.raeden.ors_to_do.dependencies.models.SectionConfig owningSection = appStats.findSection(challengeTask.getSectionId());
        int lockHours = owningSection != null ? owningSection.getPreventEditingHours() : 0;
        boolean isTimeLocked = lockHours > 0 && LocalDateTime.now().isAfter(challengeTask.getDateCreated().plusHours(lockHours));

        // Final aliases for lambda capture (lambdas can't read locals that we reassigned above).
        final boolean completedFinal = isCompleted;
        final boolean lockedFinal = isLocked;

        // --- DOUBLE CLICK EDIT BLOCK ---
        this.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                if (completedFinal) return;

                if (isTimeLocked) {
                    Design.warn(Lang.EDIT_LOCKED_HEADER, Lang.EDIT_LOCKED_CHALLENGE_BODY, lockHours);
                } else {
                    ChallengeConfigDialog.open(challengeTask, appStats, globalDatabase, onUpdate);
                }
                e.consume();
            }
        });

        // --- HEADER ---
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label((isLocked ? "🔒 " : iconStr) + challengeTask.getTextContent());
        nameLabel.setStyle("-fx-text-fill: " + (isLocked ? "#858585" : iconColor) + "; -fx-font-size: 16px; -fx-font-weight: bold; -fx-strikethrough: " + isCompleted + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (challengeTask.getDeadline() != null && !isCompleted) {
            Label timeLbl = new Label();
            Duration duration = Duration.between(LocalDateTime.now(), challengeTask.getDeadline());

            if (isExpired) {
                timeLbl.setText(Lang.EXPIRED.get());
                timeLbl.setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-color: #331A1A; -fx-padding: 2 6; -fx-background-radius: 3; -fx-border-color: #FF4444; -fx-border-radius: 3;");
            } else {
                long days = duration.toDays();
                long hours = duration.toHours() % 24;
                long minutes = duration.toMinutes() % 60;

                if (days > 0) timeLbl.setText(Lang.EXPIRES_IN_DAYS.get(days, hours));
                else timeLbl.setText(Lang.EXPIRES_IN_HOURS.get(hours, minutes));

                timeLbl.setStyle("-fx-text-fill: #E0E0E0; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-color: #3E3E42; -fx-padding: 2 6; -fx-background-radius: 3;");
            }
            header.getChildren().add(timeLbl);
        }

        Label typeLabel = new Label("CHALLENGE");
        typeLabel.setStyle("-fx-text-fill: " + outlineColor + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: " + outlineColor + "; -fx-border-radius: 3; -fx-padding: 2 5;");

        // --- ⚙ SETTINGS BUTTON LOCK ---
        Button editBtn = new Button("⚙");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #AAAAAA; -fx-cursor: hand;");

        if (isCompleted || isTimeLocked) {
            editBtn.setDisable(true);
            if (isTimeLocked) editBtn.setTooltip(new Tooltip(Lang.EDIT_LOCKED_TOOLTIP.get()));
        } else {
            editBtn.setOnAction(e -> ChallengeConfigDialog.open(challengeTask, appStats, globalDatabase, onUpdate));
        }

        header.getChildren().addAll(nameLabel, spacer, typeLabel, editBtn);

        // --- DESCRIPTION (Always visible) ---
        VBox descBox = new VBox(5);
        Label descLabel = new Label(challengeTask.getPerkDescription() == null || challengeTask.getPerkDescription().isEmpty() ? Lang.NO_DESCRIPTION.get() : challengeTask.getPerkDescription());
        descLabel.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 13px; -fx-font-style: italic;");
        descLabel.setWrapText(true);
        descBox.getChildren().add(descLabel);

        // --- LOOT BOX ---
        FlowPane lootBox = new FlowPane(10, 5);
        lootBox.setAlignment(Pos.CENTER_LEFT);
        boolean hasLoot = false;

        if (challengeTask.getRewardPoints() > 0) {
            Label l = new Label(Lang.LOOT_GLOBAL_PTS.get(challengeTask.getRewardPoints()));
            l.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #332B00; -fx-padding: 2 6; -fx-background-radius: 5;");
            lootBox.getChildren().add(l);
            hasLoot = true;
        }

        for (CustomStat s : appStats.getCustomStats()) {
            int rVal = challengeTask.getStatRewards().getOrDefault(s.getId(), 0);
            int cVal = challengeTask.getStatCapRewards().getOrDefault(s.getId(), 0);

            String txtColor = s.getTextColor() != null ? s.getTextColor() : "#4EC9B0";

            if (rVal > 0) {
                Label l = new Label(Lang.LOOT_STAT_XP.get(rVal, s.getName()));
                l.setStyle("-fx-text-fill: " + txtColor + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #1E1E1E; -fx-padding: 2 6; -fx-background-radius: 5;");
                lootBox.getChildren().add(l);
                hasLoot = true;
            }
            if (cVal > 0) {
                Label l = new Label(Lang.LOOT_STAT_CAP.get(cVal, s.getName()));
                l.setStyle("-fx-text-fill: #C586C0; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: #1E1E1E; -fx-padding: 2 6; -fx-background-radius: 5;");
                lootBox.getChildren().add(l);
                hasLoot = true;
            }
        }

        // --- EXPANDABLE REQS & REWARDS ---
        VBox expandableBox = new VBox(10);
        expandableBox.setVisible(false);
        expandableBox.setManaged(false);
        expandableBox.setPadding(new Insets(5, 0, 0, 0));

        boolean hasReqs = !requirementsBox.getChildren().isEmpty();
        if (hasReqs) {
            Label reqTitle = new Label(Lang.REQUIREMENTS_TITLE.get());
            reqTitle.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 11px;");
            VBox reqWrap = new VBox(5, reqTitle, requirementsBox);
            expandableBox.getChildren().add(reqWrap);
        }

        if (hasLoot) {
            if (hasReqs) expandableBox.getChildren().add(new Separator());
            Label lootTitle = new Label(Lang.LOOT_TITLE.get());
            lootTitle.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 11px;");
            VBox lootWrap = new VBox(5, lootTitle, lootBox);
            expandableBox.getChildren().add(lootWrap);
        }

        // --- MAIN LAYOUT ASSEMBLY ---
        getChildren().addAll(header);
        if (!statusBox.getChildren().isEmpty()) getChildren().add(statusBox);
        getChildren().add(descBox);

        if (hasReqs || hasLoot) {
            Button toggleExpandBtn = new Button(Lang.TOGGLE_SHOW_REQS.get());
            toggleExpandBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + outlineColor + "; -fx-cursor: hand; -fx-padding: 0;");
            toggleExpandBtn.setOnAction(e -> {
                isExpanded = !isExpanded;
                expandableBox.setVisible(isExpanded);
                expandableBox.setManaged(isExpanded);
                toggleExpandBtn.setText(isExpanded ? Lang.TOGGLE_HIDE_REQS.get() : Lang.TOGGLE_SHOW_REQS.get());
            });
            getChildren().addAll(toggleExpandBtn, expandableBox);
        }

        // --- COMPLETE / FAIL BUTTONS ---
        if (!isCompleted && !isLocked) {
            Button completeBtn = new Button(Lang.BTN_CHALLENGE_DONE.get());
            completeBtn.setStyle("-fx-background-color: #3A0A0A; -fx-border-color: #E06666; -fx-border-radius: 3; -fx-background-radius: 3; -fx-text-fill: #E06666; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
            completeBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(completeBtn, Priority.ALWAYS);
            completeBtn.setOnAction(e -> {
                if (Design.confirmedYes(Lang.CONFIRM_CHALLENGE_DONE_HEADER, Lang.CONFIRM_CHALLENGE_DONE_BODY)) {
                    challengeTask.setFinished(true);
                    challengeTask.setPointsClaimed(true);
                    challengeTask.setPermaLock(true);
                    challengeTask.setPerkUnlockedDate(LocalDateTime.now());
                    challengeTask.setPerkLostDate(null);

                    TaskActionHandler.processRPGStats(challengeTask, appStats, true);
                    appStats.setGlobalScore(appStats.getGlobalScore() + challengeTask.getRewardPoints());

                    StorageManager.saveStats(appStats);
                    StorageManager.saveTasks(globalDatabase);
                    onUpdate.run();
                }
            });

            // Fail option: locks the challenge in as failed, granting no rewards.
            Button failBtn = new Button(Lang.BTN_CHALLENGE_FAIL.get());
            failBtn.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #777777; -fx-border-radius: 3; -fx-background-radius: 3; -fx-text-fill: #AAAAAA; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
            failBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(failBtn, Priority.ALWAYS);
            failBtn.setOnAction(e -> {
                if (Design.confirmedYes(Lang.CONFIRM_CHALLENGE_FAIL_HEADER, Lang.CONFIRM_CHALLENGE_FAIL_BODY)) {
                    challengeTask.setFinished(true);
                    challengeTask.setPermaLock(true);
                    challengeTask.setPerkUnlockedDate(null);
                    challengeTask.setPerkLostDate(LocalDateTime.now());

                    StorageManager.saveTasks(globalDatabase);
                    onUpdate.run();
                }
            });

            HBox actionRow = new HBox(10, completeBtn, failBtn);
            getChildren().add(actionRow);
        }

        // --- Context Menu for Deletion & Editing ---
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem(Lang.MENU_EDIT_CHALLENGE.get());
        editItem.setOnAction(e -> ChallengeConfigDialog.open(challengeTask, appStats, globalDatabase, onUpdate));

        MenuItem deleteItem = new MenuItem(Lang.MENU_DELETE_CHALLENGE.get());
        deleteItem.setStyle("-fx-text-fill: #FF6666; -fx-font-weight: bold;");
        deleteItem.setOnAction(e -> {
            if (Design.confirmedYes(Lang.CONFIRM_DELETE_CHALLENGE_HEADER, Lang.CONFIRM_DELETE_CHALLENGE_BODY, challengeTask.getTextContent())) {
                globalDatabase.remove(challengeTask);
                StorageManager.saveTasks(globalDatabase);
                onUpdate.run();
            }
        });
        contextMenu.getItems().add(editItem);

        // "Move to Category" — only when this challenge's page has categories enabled.
        Menu categoryMenu = com.raeden.ors_to_do.modules.dependencies.ui.menus.TaskContextMenu
                .buildMoveToCategoryMenu(challengeTask, owningSection, globalDatabase, onUpdate);
        if (categoryMenu != null) contextMenu.getItems().add(categoryMenu);

        contextMenu.getItems().addAll(new SeparatorMenuItem(), deleteItem);

        this.setOnContextMenuRequested(e -> {
            if (isTimeLocked || completedFinal) {
                editItem.setDisable(true);
                editItem.setText(Lang.MENU_EDIT_CHALLENGE_LOCKED.get());
            } else {
                editItem.setDisable(false);
                editItem.setText(Lang.MENU_EDIT_CHALLENGE.get());
            }
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }
}
