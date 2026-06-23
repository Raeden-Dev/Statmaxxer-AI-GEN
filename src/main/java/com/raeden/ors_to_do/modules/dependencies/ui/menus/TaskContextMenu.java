package com.raeden.ors_to_do.modules.dependencies.ui.menus;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CategoryStyle;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.models.SubTask;
import com.raeden.ors_to_do.dependencies.models.TaskLink;
import com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import static com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs.*;

public class TaskContextMenu {

    public static ContextMenu build(TaskItem task, SectionConfig config, AppStats appStats, List<TaskItem> globalDatabase, Runnable onUpdate, Consumer<TaskItem> onGoToPage) {
        ContextMenu contextMenu = new ContextMenu();
        boolean isNoteMode = config != null && config.isNotesPage();
        boolean isRewardMode = config != null && config.isRewardsPage();

        // --- NEW: Evaluate the Section's Completion Lock ---
        boolean isCompletionLocked = config != null && config.isLockCompletedTasks() && task.isFinished();

        boolean allowStyling = config != null && (config.isNotesPage() || config.isEnableTaskStyling());

        MenuItem editItem = new MenuItem(isNoteMode ? "Edit Note" : "Edit Task");
        String baseEditText = editItem.getText();
        editItem.setOnAction(e -> showEditDialog(task, config, appStats, globalDatabase, onUpdate));

        MenuItem pinItem = null;
        if (isNoteMode) {
            pinItem = new MenuItem(task.isPinned() ? "Unpin Note" : "Pin Note to Top");
            pinItem.setOnAction(e -> {
                task.setPinned(!task.isPinned());
                StorageManager.saveTasks(globalDatabase);
                onUpdate.run();
            });
        }

        MenuItem copyItem = new MenuItem("Copy Text");
        copyItem.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(task.getTextContent());
            Clipboard.getSystemClipboard().setContent(content);
        });

        MenuItem addSubTaskItem = null;
        if ((config == null || config.isEnableSubTasks()) && !isNoteMode) {
            addSubTaskItem = new MenuItem("Add Sub-tasks");
            addSubTaskItem.setOnAction(e -> showAddSubTaskDialog(task, globalDatabase, onUpdate));

            // Link cards may now carry sub-tasks; only the completion lock blocks adding them.
            if (isCompletionLocked) {
                addSubTaskItem.setDisable(true);
            }
        }

        MenuItem addLinkItem = null;
        if ((config == null || config.isEnableLinks()) && !isNoteMode) {
            addLinkItem = new MenuItem("Add External Link");
            addLinkItem.setOnAction(e -> showLinkDialog(task, null, globalDatabase, onUpdate));
            if (isCompletionLocked) addLinkItem.setDisable(true);
        }

        Menu linksMenu = null;
        if ((config == null || config.isEnableLinks()) && task.getTaskLinks() != null && !task.getTaskLinks().isEmpty() && !isNoteMode) {
            linksMenu = new Menu("External Links");
            for (TaskLink link : task.getTaskLinks()) {
                Menu singleLinkMenu = new Menu(link.getName());

                MenuItem openLinkItem = new MenuItem("Open Link");
                openLinkItem.setOnAction(e -> {
                    new Thread(() -> {
                        try {
                            if (link.getUrl().startsWith("http")) {
                                java.awt.Desktop.getDesktop().browse(new java.net.URI(link.getUrl()));
                            } else {
                                java.io.File file = new java.io.File(link.getUrl());
                                if (file.exists()) {
                                    java.awt.Desktop.getDesktop().open(file);
                                } else {
                                    Runtime.getRuntime().exec(link.getUrl());
                                }
                            }
                        } catch (Exception ex) {
                            javafx.application.Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to open link: \n" + link.getUrl());
                                TaskDialogs.styleDialog(alert);
                                alert.show();
                            });
                        }
                    }).start();
                });

                MenuItem editSpecificLinkItem = new MenuItem("Edit");
                editSpecificLinkItem.setOnAction(e -> showLinkDialog(task, link, globalDatabase, onUpdate));
                if (isCompletionLocked) editSpecificLinkItem.setDisable(true);

                MenuItem copyLinkItem = new MenuItem("Copy Link");
                copyLinkItem.setOnAction(e -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(link.getUrl());
                    Clipboard.getSystemClipboard().setContent(content);
                });

                MenuItem deleteLinkItem = new MenuItem("Delete Link");
                deleteLinkItem.setOnAction(e -> {
                    task.getTaskLinks().remove(link);
                    StorageManager.saveTasks(globalDatabase);
                    onUpdate.run();
                });
                if (isCompletionLocked) deleteLinkItem.setDisable(true);

                singleLinkMenu.getItems().addAll(openLinkItem, editSpecificLinkItem, copyLinkItem, deleteLinkItem);
                linksMenu.getItems().add(singleLinkMenu);
            }
        }

        MenuItem duplicateItem = new MenuItem("Duplicate");
        duplicateItem.setOnAction(e -> {
            TaskItem copy = new TaskItem(task.getTextContent() + " (Copy)", task.getPriority(), task.getSectionId());
            copy.setPrefix(task.getPrefix());
            copy.setPrefixColor(task.getPrefixColor());
            copy.setIconSymbol(task.getIconSymbol());
            copy.setIconColor(task.getIconColor());
            copy.setDeadline(task.getDeadline());
            copy.setColorHex(task.getColorHex());
            copy.setTaskType(task.getTaskType());
            copy.setRewardPoints(task.getRewardPoints());
            copy.setCostPoints(task.getCostPoints());
            copy.setPenaltyPoints(task.getPenaltyPoints());
            copy.setCounterMode(task.isCounterMode());
            copy.setMaxCount(task.getMaxCount());
            copy.setCustomOutlineColor(task.getCustomOutlineColor());
            copy.setCustomSideboxColor(task.getCustomSideboxColor());
            copy.setOptional(task.isOptional());
            copy.setFavorite(task.isFavorite());

            copy.setLinkCard(task.isLinkCard());
            copy.setLinkActionPath(task.getLinkActionPath());

            if (task.getStatRewards() != null) copy.setStatRewards(new java.util.HashMap<>(task.getStatRewards()));
            if (task.getStatCapRewards() != null) copy.setStatCapRewards(new java.util.HashMap<>(task.getStatCapRewards()));
            if (task.getStatCosts() != null) copy.setStatCosts(new java.util.HashMap<>(task.getStatCosts()));
            if (task.getStatPenalties() != null) copy.setStatPenalties(new java.util.HashMap<>(task.getStatPenalties()));
            if (task.getStatRequirements() != null) copy.setStatRequirements(new java.util.HashMap<>(task.getStatRequirements()));
            if (task.getInflictedDebuffIds() != null) copy.setInflictedDebuffIds(new java.util.ArrayList<>(task.getInflictedDebuffIds()));

            copy.setRepeatingMode(task.isRepeatingMode());
            copy.setRepetitionCount(task.getRepetitionCount());

            for (SubTask sub : task.getSubTasks()) {
                copy.getSubTasks().add(new SubTask(sub.getTextContent()));
            }
            if (task.getTaskLinks() != null) {
                for (TaskLink link : task.getTaskLinks()) {
                    copy.getTaskLinks().add(new TaskLink(link.getName(), link.getUrl()));
                }
            }

            globalDatabase.add(globalDatabase.indexOf(task) + 1, copy);
            StorageManager.saveTasks(globalDatabase);
            onUpdate.run();
        });

        MenuItem colorItem = new MenuItem(allowStyling ? "Reset Task Style" : "Clear Background Color");
        colorItem.setOnAction(e -> {
            task.setColorHex("transparent");
            task.setCustomOutlineColor("transparent");
            task.setCustomSideboxColor("transparent");
            task.setIconSymbol("None");
            task.setIconColor("#FFFFFF");
            task.setPrefix("");
            task.setPrefixColor("#4EC9B0");
            StorageManager.saveTasks(globalDatabase);
            onUpdate.run();
        });

        Menu moveMenu = new Menu("Move to...");
        if (appStats != null) {
            for (SectionConfig sc : appStats.getSections()) {
                if (sc.getId().equals(task.getSectionId())) continue;

                Rectangle colorRect = new Rectangle(10, 10, Color.web(sc.getSidebarColor()));
                MenuItem sectionItem = new MenuItem(sc.getName(), colorRect);
                sectionItem.setOnAction(e -> {
                    task.setSectionId(sc.getId());
                    StorageManager.saveTasks(globalDatabase);
                    onUpdate.run();
                });
                moveMenu.getItems().add(sectionItem);
            }
        }
        if (moveMenu.getItems().isEmpty()) {
            MenuItem emptyInfo = new MenuItem("No other dynamic sections available");
            emptyInfo.setDisable(true);
            moveMenu.getItems().add(emptyInfo);
        }

        // --- Move to Category (only when this page has categories enabled) ---
        Menu categoryMenu = buildMoveToCategoryMenu(task, config, globalDatabase, onUpdate);

        MenuItem toggleFavoriteItem = new MenuItem(task.isFavorite() ? "Remove Favorite" : "Favorite Task");
        if (config != null && !config.isAllowFavorite()) toggleFavoriteItem.setDisable(true);
        toggleFavoriteItem.setOnAction(e -> {
            task.setFavorite(!task.isFavorite());
            StorageManager.saveTasks(globalDatabase);
            onUpdate.run();
        });

        MenuItem archiveItem = new MenuItem(task.isArchived() ? "Unarchive" : "Archive");
        archiveItem.setOnAction(e -> {
            task.setArchived(!task.isArchived());
            StorageManager.saveTasks(globalDatabase);
            onUpdate.run();
        });

        MenuItem focusLinkItem = null;
        if (onGoToPage != null && !task.isArchived()) {
            focusLinkItem = new MenuItem("Go to Original Page ↗");
            focusLinkItem.setStyle("-fx-text-fill: #569CD6; -fx-font-weight: bold;");
            focusLinkItem.setOnAction(e -> onGoToPage.accept(task));
        }

        MenuItem deleteItem = new MenuItem("Delete Task");
        deleteItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this task?", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText("Confirm Deletion");
            TaskDialogs.styleDialog(alert);

            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
                    appStats.getDeletedTaskHistory().add("[" + timestamp + "] " + task.getTextContent());
                    appStats.setLifetimeDeletedTasks(appStats.getLifetimeDeletedTasks() + 1);

                    globalDatabase.remove(task);

                    for (TaskItem other : globalDatabase) {
                        if (other.getDependsOnTaskIds() != null) {
                            other.getDependsOnTaskIds().remove(task.getId());
                        }
                    }

                    StorageManager.saveTasks(globalDatabase);
                    StorageManager.saveStats(appStats);
                    onUpdate.run();
                }
            });
        });

        contextMenu.getItems().addAll(editItem);
        if (pinItem != null) contextMenu.getItems().add(pinItem);

        contextMenu.getItems().addAll(
                copyItem,
                new SeparatorMenuItem()
        );

        if (focusLinkItem != null) contextMenu.getItems().addAll(focusLinkItem, new SeparatorMenuItem());

        if (addSubTaskItem != null) contextMenu.getItems().add(addSubTaskItem);
        if (addLinkItem != null) contextMenu.getItems().add(addLinkItem);
        if (linksMenu != null) contextMenu.getItems().add(linksMenu);

        contextMenu.getItems().addAll(
                new SeparatorMenuItem(),
                duplicateItem,
                colorItem,
                moveMenu
        );
        if (categoryMenu != null) contextMenu.getItems().add(categoryMenu);
        contextMenu.getItems().addAll(
                toggleFavoriteItem,
                new SeparatorMenuItem(),
                archiveItem,
                deleteItem
        );

        contextMenu.setOnShowing(e -> {
            // Per-section prevent-editing window (formerly a global setting).
            int lockHours = config != null ? config.getPreventEditingHours() : 0;
            boolean isTimeLocked = !(isNoteMode || isRewardMode) && lockHours > 0 && java.time.LocalDateTime.now().isAfter(task.getDateCreated().plusHours(lockHours));

            // Apply lock if time expired OR if the task is completed and the section enforces completion locks
            if (isTimeLocked || isCompletionLocked) {
                editItem.setDisable(true);
                editItem.setText(baseEditText + " (Locked)");

                // Block unarchiving if it's completed and locked
                if (isCompletionLocked && task.isArchived()) {
                    archiveItem.setDisable(true);
                    archiveItem.setText("Unarchive (Locked)");
                }
            } else {
                editItem.setDisable(false);
                editItem.setText(baseEditText);
                archiveItem.setDisable(false);
            }
        });

        return contextMenu;
    }

    /**
     * Builds the "Move to Category" submenu for {@code task}, listing every category available on
     * its page (categories used by tasks in the section plus any defined styles), with the card's
     * current category pre-selected and a "Set as Uncategorized" option. Returns {@code null} when
     * the section is missing or doesn't have categories enabled, so callers can simply skip adding
     * it. Shared by normal task cards and the specialized perk/challenge cards.
     */
    public static Menu buildMoveToCategoryMenu(TaskItem task, SectionConfig config, List<TaskItem> globalDatabase, Runnable onUpdate) {
        if (config == null || !config.isEnableCategories()) return null;

        Menu categoryMenu = new Menu("Move to Category");

        // Gather every category available on this page: styled categories plus any categoryName
        // already in use by tasks belonging to this section.
        java.util.LinkedHashSet<String> categories = new java.util.LinkedHashSet<>();
        for (CategoryStyle cs : config.getCategoryStyles()) {
            if (cs.getName() != null && !cs.getName().trim().isEmpty()) categories.add(cs.getName().trim());
        }
        if (globalDatabase != null) {
            for (TaskItem other : globalDatabase) {
                if (!config.getId().equals(other.getSectionId())) continue;
                String c = other.getCategoryName();
                if (c != null && !c.trim().isEmpty()) categories.add(c.trim());
            }
        }
        java.util.List<String> sortedCategories = new java.util.ArrayList<>(categories);
        sortedCategories.sort(String.CASE_INSENSITIVE_ORDER);

        String currentCategory = task.getCategoryName();
        ToggleGroup categoryGroup = new ToggleGroup();

        for (String cat : sortedCategories) {
            RadioMenuItem catItem = new RadioMenuItem(cat);
            catItem.setToggleGroup(categoryGroup);
            catItem.setSelected(cat.equals(currentCategory));
            catItem.setOnAction(e -> {
                task.setCategoryName(cat);
                applyCategoryAutoStyle(task, config);
                StorageManager.saveTasks(globalDatabase);
                onUpdate.run();
            });
            categoryMenu.getItems().add(catItem);
        }
        if (!sortedCategories.isEmpty()) categoryMenu.getItems().add(new SeparatorMenuItem());

        RadioMenuItem uncategorizedItem = new RadioMenuItem("Set as Uncategorized");
        uncategorizedItem.setToggleGroup(categoryGroup);
        uncategorizedItem.setSelected(currentCategory == null || currentCategory.trim().isEmpty());
        uncategorizedItem.setOnAction(e -> {
            task.setCategoryName(null);
            StorageManager.saveTasks(globalDatabase);
            onUpdate.run();
        });
        categoryMenu.getItems().add(uncategorizedItem);

        return categoryMenu;
    }

    /**
     * When the section has "Auto-Style on Category" enabled and the card is completely unstyled,
     * copy the target category's icon and colours onto the card so it visually adopts the category
     * it was just moved into. One-way and non-destructive: a card with any styling of its own is
     * left untouched.
     */
    public static void applyCategoryAutoStyle(TaskItem task, SectionConfig config) {
        if (config == null || !config.isAutoStyleOnCategoryDrop()) return;
        String cat = task.getCategoryName();
        if (cat == null || cat.trim().isEmpty()) return;
        if (!isCompletelyUnstyled(task)) return;

        CategoryStyle style = config.findCategoryStyle(cat);
        if (style == null) return;

        if (style.getBackgroundColor() != null) task.setColorHex(style.getBackgroundColor());
        if (style.getBorderColor() != null) task.setCustomOutlineColor(style.getBorderColor());
        if (style.getIconSymbol() != null && !"None".equals(style.getIconSymbol())) {
            task.setIconSymbol(style.getIconSymbol());
            task.setIconColor(style.getIconColor() != null ? style.getIconColor() : style.getTextColor());
        }
    }

    /** True when the card carries no custom styling of its own (background, outline, sidebox, icon). */
    private static boolean isCompletelyUnstyled(TaskItem task) {
        boolean noBg = isBlankOrTransparent(task.getColorHex());
        boolean noOutline = isBlankOrTransparent(task.getCustomOutlineColor());
        boolean noSidebox = isBlankOrTransparent(task.getCustomSideboxColor());
        boolean noIcon = task.getIconSymbol() == null || task.getIconSymbol().isBlank() || "None".equals(task.getIconSymbol());
        boolean noIconColor = task.getIconColor() == null || task.getIconColor().isBlank();
        return noBg && noOutline && noSidebox && noIcon && noIconColor;
    }

    private static boolean isBlankOrTransparent(String color) {
        return color == null || color.isBlank() || "transparent".equalsIgnoreCase(color);
    }
}