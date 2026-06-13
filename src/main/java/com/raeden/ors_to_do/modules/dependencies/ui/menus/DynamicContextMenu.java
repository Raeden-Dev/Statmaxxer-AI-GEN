package com.raeden.ors_to_do.modules.dependencies.ui.menus;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CustomPriority;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;

import java.util.List;

public class DynamicContextMenu {

    public static ContextMenu build(SectionConfig config, AppStats appStats, List<TaskItem> db, Runnable refreshListAction, Runnable syncCallback) {
        ContextMenu bgMenu = new ContextMenu();
        bgMenu.setStyle("-fx-background-color: #2D2D30; -fx-border-color: #555555;");

        MenuItem createItem = new MenuItem(config.isNotesPage() ? "Create New Note" : "Create New Task");
        createItem.setStyle("-fx-text-fill: white;");
        createItem.setOnAction(e -> createAndEditTask(false, false, config, appStats, db, refreshListAction, syncCallback));
        bgMenu.getItems().add(createItem);

        if (config.isEnableLinkCards()) {
            MenuItem createLinkItem = new MenuItem("Create Link Card");
            createLinkItem.setStyle("-fx-text-fill: white;");
            createLinkItem.setOnAction(e -> createAndEditTask(true, false, config, appStats, db, refreshListAction, syncCallback));
            bgMenu.getItems().add(createLinkItem);
        }

        if (config.isEnableOptionalTasks()) {
            MenuItem createOptItem = new MenuItem("Create Optional Card");
            createOptItem.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold;");
            createOptItem.setOnAction(e -> createAndEditTask(false, true, config, appStats, db, refreshListAction, syncCallback));
            bgMenu.getItems().add(createOptItem);
        }

        if (config.isEnableCategories()) {
            bgMenu.getItems().add(new SeparatorMenuItem());
            bgMenu.getItems().add(buildCreateCategoryItem(config, appStats, refreshListAction));
        }

        if (appStats.isEnableTextToTask() && !config.isNotesPage()) {
            bgMenu.getItems().add(new SeparatorMenuItem());
            MenuItem batchItem = new MenuItem("Batch to Task");
            batchItem.setStyle("-fx-text-fill: white;");
            batchItem.setOnAction(e -> {
                TaskItem dummy = new TaskItem("", null, config.getId());
                TaskDialogs.showTextToTaskDialog(dummy, db, () -> {
                    refreshListAction.run();
                    if(syncCallback != null) syncCallback.run();
                });
            });
            bgMenu.getItems().add(batchItem);
        }
        return bgMenu;
    }

    /**
     * Builds the "Create New Category" background-menu item. Prompts for a name and registers it as
     * a (default-styled) category on the section via {@link SectionConfig#upsertCategoryStyle},
     * which makes the empty category render immediately so cards can be moved into it. Shared by the
     * normal background menu and the perk/challenge background menu.
     */
    public static MenuItem buildCreateCategoryItem(SectionConfig config, AppStats appStats, Runnable refresh) {
        MenuItem item = new MenuItem("Create New Category");
        item.setStyle("-fx-text-fill: #DCDCAA; -fx-font-weight: bold;");
        item.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Create New Category");
            dlg.setHeaderText("Enter a name for the new category:");
            dlg.setContentText("Category name:");
            TaskDialogs.styleDialog(dlg);
            dlg.showAndWait().ifPresent(name -> {
                String trimmed = name == null ? "" : name.trim();
                if (trimmed.isEmpty()) return;
                config.upsertCategoryStyle(trimmed); // registers the category (no-op if it exists)
                StorageManager.saveStats(appStats);
                if (refresh != null) refresh.run();
            });
        });
        return item;
    }

    private static void createAndEditTask(boolean isLink, boolean isOptional, SectionConfig config, AppStats appStats, List<TaskItem> db, Runnable refresh, Runnable sync) {
        CustomPriority defaultPrio = null;
        if (config.isShowPriority() && !config.isNotesPage() && !appStats.getCustomPriorities().isEmpty() && !isOptional) {
            defaultPrio = appStats.getCustomPriorities().get(0);
        }

        TaskItem newTask = new TaskItem("", defaultPrio, config.getId());
        newTask.setLinkCard(isLink);
        newTask.setOptional(isOptional);
        if (config.isShowTaskType() && !config.isNotesPage()) newTask.setTaskType("General");

        TaskDialogs.showEditDialog(newTask, config, appStats, db, () -> {
            if (newTask.getTextContent() != null && !newTask.getTextContent().trim().isEmpty()) {
                if (!db.contains(newTask)) db.add(newTask);
                StorageManager.saveTasks(db);
            }
            refresh.run();
            if (sync != null) sync.run();
        });
    }
}