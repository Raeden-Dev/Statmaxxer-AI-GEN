package com.raeden.ors_to_do.modules;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.Debuff;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.CustomStat;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.i18n.Lang;
import com.raeden.ors_to_do.modules.dependencies.ui.cards.ChallengeCard;
import com.raeden.ors_to_do.modules.dependencies.ui.cards.DebuffCard;
import com.raeden.ors_to_do.modules.dependencies.ui.cards.PerkCard;
import com.raeden.ors_to_do.modules.dependencies.ui.cards.RepeatableTaskCard;
import com.raeden.ors_to_do.modules.dependencies.ui.cards.StatCard;
import com.raeden.ors_to_do.modules.dependencies.ui.cards.TaskCard;
import com.raeden.ors_to_do.modules.dependencies.ui.components.CategoryGroupRenderer;
import com.raeden.ors_to_do.modules.dependencies.ui.components.DynamicInputPanel;
import com.raeden.ors_to_do.modules.dependencies.ui.components.FilterSortHeader;
import com.raeden.ors_to_do.modules.dependencies.ui.layout.ZenModeOverlay;
import com.raeden.ors_to_do.modules.dependencies.ui.menus.DynamicContextMenu;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.DynamicSortHelper;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

import java.util.*;

public class DynamicModule extends StackPane {

    private BorderPane mainContent;
    private ZenModeOverlay zenOverlay;
    private FilterSortHeader filterSortHeader;
    private VBox listContainer;

    private boolean isZenMode = false;
    private SectionConfig config;
    private List<TaskItem> globalDatabase;
    private AppStats appStats;
    private Runnable syncCallback;
    private List<Timeline> activeTimelines = new ArrayList<>();

    public DynamicModule(SectionConfig config, List<TaskItem> globalDatabase, AppStats appStats, Runnable syncCallback) {
        this.config = config;
        this.globalDatabase = globalDatabase;
        this.appStats = appStats;
        this.syncCallback = syncCallback;

        mainContent = new BorderPane();
        mainContent.setPadding(new Insets(15));

        boolean isSpecialOverlay = config.isNotesPage() || config.isStatPage() || config.isPerkPage() || config.isChallengePage();
        Runnable zenToggleAction = isSpecialOverlay ? () -> {} : this::toggleZenMode;

        zenOverlay = new ZenModeOverlay(config, appStats, globalDatabase, zenToggleAction, syncCallback, activeTimelines, this::reorderTasks);
        filterSortHeader = new FilterSortHeader(config, appStats, globalDatabase, zenToggleAction, this::refreshList);

        if (isSpecialOverlay) {
            filterSortHeader.getChildren().forEach(node -> {
                if (node instanceof HBox) ((HBox) node).getChildren().removeIf(n -> n instanceof Button && ((Button) n).getText().contains("Zen Mode"));
            });
        }

        getChildren().addAll(mainContent, zenOverlay);
        mainContent.setTop(filterSortHeader);

        listContainer = new VBox(8);
        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1E1E1E;");
        scrollPane.setBorder(Border.EMPTY);
        mainContent.setCenter(scrollPane);

        ContextMenu bgMenu = DynamicContextMenu.build(config, appStats, globalDatabase, this::refreshList, syncCallback);

        if (!config.isStatPage() && !config.isPerkPage() && !config.isChallengePage()) {
            scrollPane.setOnContextMenuRequested(e -> {
                Node target = (Node) e.getTarget();
                boolean isTaskCard = false;
                while (target != null) {
                    if (target instanceof TaskCard || target instanceof RepeatableTaskCard) { isTaskCard = true; break; }
                    target = target.getParent();
                }
                if (!isTaskCard) bgMenu.show(scrollPane, e.getScreenX(), e.getScreenY());
            });
        }

        scrollPane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> { if (bgMenu.isShowing()) bgMenu.hide(); });

        if (!config.isStatPage()) {
            // Challenge-page flagging happens inside DynamicInputPanel.addTask itself, so a plain
            // refreshList() is all that's needed here.
            DynamicInputPanel inputPanel = new DynamicInputPanel(config, appStats, globalDatabase, filterSortHeader, this::refreshList);
            mainContent.setBottom(inputPanel);
        }

        refreshList();
    }

    private void toggleZenMode() {
        if (config.isNotesPage() || config.isStatPage() || config.isPerkPage() || config.isChallengePage()) return;
        isZenMode = !isZenMode;

        if (getScene() != null && getScene().getRoot() instanceof BorderPane) {
            Node sidebar = ((BorderPane) getScene().getRoot()).getLeft();
            if (sidebar != null) {
                sidebar.setVisible(!isZenMode);
                sidebar.setManaged(!isZenMode);
            }
        }

        if (isZenMode) {
            mainContent.setVisible(false);
            zenOverlay.setVisible(true);
            zenOverlay.refreshZenMode(false);
        } else {
            mainContent.setVisible(true);
            zenOverlay.setVisible(false);
            refreshList();
        }
    }

    public void refreshList() {
        for (Timeline t : activeTimelines) t.stop();
        activeTimelines.clear();

        if (isZenMode) { zenOverlay.refreshZenMode(false); return; }

        listContainer.getChildren().clear();

        if (config.isStatPage()) { loadStatPage(); return; }
        if (config.isPerkPage()) { loadPerkPage(); return; }
        if (config.isChallengePage()) { loadChallengePage(); return; }

        int availableCount = 0; int completedCount = 0;
        Set<String> uniqueTags = new HashSet<>();
        List<TaskItem> tasksToDisplay = new ArrayList<>();

        for (TaskItem task : globalDatabase) {
            if (task.getSectionId() != null && task.getSectionId().equals(config.getId()) && !task.isArchived()) {
                String tag = null;
                if (config.isShowTaskType() && task.getTaskType() != null && !task.getTaskType().isEmpty()) tag = task.getTaskType();
                else if (config.isShowPrefix() && task.getPrefix() != null && !task.getPrefix().isEmpty()) tag = task.getPrefix();
                if (tag != null) uniqueTags.add(tag);

                boolean passesFilter = filterSortHeader.getActiveFilter().equals("All") || (tag != null && tag.equals(filterSortHeader.getActiveFilter()));

                if (passesFilter) {
                    tasksToDisplay.add(task);
                    if (!task.isFinished()) availableCount++;
                    else completedCount++;
                }
            }
        }

        DynamicSortHelper.sortTasks(tasksToDisplay, filterSortHeader.getSortMode(), config, appStats);
        filterSortHeader.updateBadges(availableCount, completedCount);

        if (tasksToDisplay.isEmpty()) {
            String emptyText = config.isNotesPage() ? Lang.EMPTY_NOTES.get() : (config.isRewardsPage() ? Lang.EMPTY_REWARDS.get() : Lang.EMPTY_TASKS.get());
            Label emptyLabel = new Label(emptyText);
            emptyLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 16px; -fx-font-style: italic; -fx-padding: 30 0 0 0;");
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            emptyLabel.setAlignment(Pos.CENTER);
            listContainer.getChildren().add(emptyLabel);
        } else {
            Runnable onUpdateTrigger = () -> { refreshList(); if (syncCallback != null) syncCallback.run(); };
            java.util.function.Function<TaskItem, javafx.scene.Node> cardFor = t -> t.isRepeatingMode()
                    ? new RepeatableTaskCard(t, config, appStats, globalDatabase, onUpdateTrigger, activeTimelines, this::reorderTasks)
                    : new TaskCard(t, config, appStats, globalDatabase, onUpdateTrigger, activeTimelines, this::reorderTasks);

            if (config.isEnableCategories()) {
                // Pass refreshList as the post-toggle / post-style callback so the page redraws
                // (and new style colours take effect) immediately after a click in the header.
                CategoryGroupRenderer.render(listContainer, tasksToDisplay, globalDatabase, config, appStats, cardFor, this::refreshList);
            } else {
                for (TaskItem task : tasksToDisplay) listContainer.getChildren().add(cardFor.apply(task));
            }
        }

        if (config.isShowTags()) filterSortHeader.updateFilterPills(uniqueTags, this::refreshList);
    }

    private void loadStatPage() {
        boolean changed = appStats.getActiveDebuffs().removeIf(d -> d.getExpiryDate() != null && java.time.LocalDateTime.now().isAfter(d.getExpiryDate()));
        if (changed) StorageManager.saveStats(appStats);

        // --- FIXED: Removed the Debuff Manager button from here since it's now in the header ---
        Label debuffLabel = new Label(Lang.DEBUFFS_TITLE.get());
        debuffLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FF6666; -fx-padding: 0 0 5 0;");
        listContainer.getChildren().add(debuffLabel);

        HBox debuffBox = new HBox(10);
        for (Debuff d : appStats.getActiveDebuffs()) {
            debuffBox.getChildren().add(new DebuffCard(d, appStats, () -> { refreshList(); if (syncCallback != null) syncCallback.run(); }));
        }

        if (appStats.getActiveDebuffs().isEmpty()) {
            Label noDebuffs = new Label(Lang.NO_DEBUFFS.get());
            noDebuffs.setStyle("-fx-text-fill: #858585; -fx-font-style: italic;");
            debuffBox.getChildren().add(noDebuffs);
        }

        ScrollPane debuffScroll = new ScrollPane(debuffBox);
        debuffScroll.setFitToHeight(true);
        debuffScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        debuffScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        debuffScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        debuffScroll.setBorder(Border.EMPTY);

        String scrollCss = ".scroll-pane { -fx-background-color: transparent; } .scroll-pane > .viewport { -fx-background-color: transparent; } .scroll-bar:horizontal { -fx-background-color: transparent; -fx-pref-height: 5; } .scroll-bar:horizontal .thumb { -fx-background-color: #3E3E42; -fx-background-radius: 5; } .scroll-bar:horizontal .thumb:hover { -fx-background-color: #555555; }";
        debuffScroll.getStylesheets().add("data:text/css;base64," + java.util.Base64.getEncoder().encodeToString(scrollCss.getBytes()));

        listContainer.getChildren().addAll(debuffScroll, new Separator());

        if (!appStats.isGlobalStatsEnabled() || appStats.getCustomStats().isEmpty()) {
            Label emptyMsg = new Label(Lang.NO_CUSTOM_STATS.get());
            emptyMsg.setStyle("-fx-text-fill: #555555; -fx-font-size: 16px; -fx-font-style: italic; -fx-padding: 30 0 0 0;");
            emptyMsg.setMaxWidth(Double.MAX_VALUE); emptyMsg.setAlignment(Pos.CENTER);
            listContainer.getChildren().add(emptyMsg);
        } else {
            for (CustomStat stat : appStats.getCustomStats()) {
                listContainer.getChildren().add(new StatCard(stat, appStats, () -> { refreshList(); if (syncCallback != null) syncCallback.run(); }));
            }
        }
        filterSortHeader.updateBadges(appStats.getCustomStats().size(), appStats.getActiveDebuffs().size());
    }

    private void loadPerkPage() {
        List<TaskItem> perks = new ArrayList<>();
        int totalCount = 0;
        int activeCount = 0;

        for (TaskItem task : globalDatabase) {
            if (task.getSectionId() != null && task.getSectionId().equals(config.getId()) && !task.isArchived()) {
                perks.add(task);
                totalCount++;
                if (task.getPerkLevel() > 0 || task.getPerkUnlockedDate() != null) {
                    activeCount++;
                }
            }
        }

        DynamicSortHelper.sortTasks(perks, filterSortHeader.getSortMode(), config, appStats);

        if (perks.isEmpty()) {
            Label emptyMsg = new Label(Lang.EMPTY_PERKS.get());
            emptyMsg.setStyle("-fx-text-fill: #555555; -fx-font-size: 16px; -fx-font-style: italic; -fx-padding: 30 0 0 0;");
            emptyMsg.setMaxWidth(Double.MAX_VALUE); emptyMsg.setAlignment(Pos.CENTER);
            listContainer.getChildren().add(emptyMsg);
        } else {
            Runnable onUpdate = () -> { refreshList(); if (syncCallback != null) syncCallback.run(); };
            java.util.function.Function<TaskItem, javafx.scene.Node> perkCardFor =
                    perk -> new PerkCard(perk, appStats, globalDatabase, onUpdate);
            if (config.isEnableCategories()) {
                CategoryGroupRenderer.render(listContainer, perks, globalDatabase, config, appStats, perkCardFor, this::refreshList);
            } else {
                for (TaskItem perk : perks) listContainer.getChildren().add(perkCardFor.apply(perk));
            }
        }
        filterSortHeader.updateBadges(totalCount, activeCount);
    }

    private void loadChallengePage() {
        List<TaskItem> challenges = new ArrayList<>();
        int availableCount = 0;
        int completedCount = 0;

        for (TaskItem task : globalDatabase) {
            if (task.getSectionId() != null && task.getSectionId().equals(config.getId()) && !task.isArchived()) {
                challenges.add(task);
                if (task.isFinished()) completedCount++;
                else availableCount++;
            }
        }

        DynamicSortHelper.sortTasks(challenges, filterSortHeader.getSortMode(), config, appStats);

        if (challenges.isEmpty()) {
            Label emptyMsg = new Label(Lang.EMPTY_CHALLENGES.get());
            emptyMsg.setStyle("-fx-text-fill: #555555; -fx-font-size: 16px; -fx-font-style: italic; -fx-padding: 30 0 0 0;");
            emptyMsg.setMaxWidth(Double.MAX_VALUE); emptyMsg.setAlignment(Pos.CENTER);
            listContainer.getChildren().add(emptyMsg);
        } else {
            Runnable onUpdate = () -> { refreshList(); if (syncCallback != null) syncCallback.run(); };
            java.util.function.Function<TaskItem, javafx.scene.Node> chCardFor =
                    challenge -> new ChallengeCard(challenge, appStats, globalDatabase, onUpdate);
            if (config.isEnableCategories()) {
                CategoryGroupRenderer.render(listContainer, challenges, globalDatabase, config, appStats, chCardFor, this::refreshList);
            } else {
                for (TaskItem challenge : challenges) listContainer.getChildren().add(chCardFor.apply(challenge));
            }
        }
        filterSortHeader.updateBadges(availableCount, completedCount);
    }

    private void reorderTasks(String draggedId, String targetId) {
        if (draggedId.equals(targetId)) return;
        TaskItem draggedTask = null, targetTask = null;
        for (TaskItem task : globalDatabase) {
            if (task.getId().equals(draggedId)) draggedTask = task;
            if (task.getId().equals(targetId)) targetTask = task;
        }
        if (draggedTask != null && targetTask != null) {
            int draggedIdx = globalDatabase.indexOf(draggedTask);
            int targetIdx = globalDatabase.indexOf(targetTask);
            globalDatabase.remove(draggedIdx);
            if (draggedIdx < targetIdx) targetIdx--;
            globalDatabase.add(targetIdx, draggedTask);
            StorageManager.saveTasks(globalDatabase);
            filterSortHeader.resetSortMode();
            refreshList();
        }
    }
}