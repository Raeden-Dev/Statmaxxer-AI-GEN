package com.raeden.ors_to_do.modules.dependencies.ui.components;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.i18n.Lang;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the "Hook Tasks / Challenges" multi-select {@link MenuButton} shared by the Perk and
 * Challenge editor dialogs. Both dialogs previously contained ~60 identical lines for this.
 *
 * <p>The supplied {@code selectedDeps} list is the live selection model: it is mutated as the user
 * ticks/unticks tasks, so the caller simply reads it back when the dialog is confirmed.</p>
 */
public final class DependencyMenuBuilder {

    private DependencyMenuBuilder() { }

    /**
     * Returns a copy of {@code selectedDeps} containing only ids that still exist in
     * {@code globalDatabase}. While an editor dialog was open, the user (or a background process)
     * may have deleted one of the hooked tasks; without this filter, the editor would persist a
     * dead id that nothing can ever satisfy. Callers should run this just before saving the
     * dependency list to the model.
     */
    public static List<String> stripStale(List<String> selectedDeps, List<TaskItem> globalDatabase) {
        if (selectedDeps == null) return new ArrayList<>();
        Set<String> alive = new HashSet<>();
        if (globalDatabase != null) {
            for (TaskItem t : globalDatabase) if (t != null) alive.add(t.getId());
        }
        List<String> out = new ArrayList<>(selectedDeps.size());
        for (String id : selectedDeps) if (alive.contains(id)) out.add(id);
        return out;
    }

    /**
     * @param owner         the task being edited (excluded from its own dependency list)
     * @param appStats      provides section grouping
     * @param globalDatabase all tasks to choose from
     * @param selectedDeps  mutable list of currently-selected dependency ids (modified in place)
     * @return a configured, ready-to-add {@link MenuButton}
     */
    public static MenuButton build(TaskItem owner, AppStats appStats, List<TaskItem> globalDatabase, List<String> selectedDeps) {
        MenuButton dependenciesMenu = new MenuButton(Lang.BTN_SELECT_PARENTS.get());
        dependenciesMenu.getStyleClass().add("custom-menu-btn");
        dependenciesMenu.setMaxWidth(Double.MAX_VALUE);

        int[] depCount = {0};

        Map<String, Menu> sectionMenus = new HashMap<>();
        if (appStats != null && appStats.getSections() != null) {
            for (SectionConfig sc : appStats.getSections()) {
                Menu m = new Menu(sc.getName());
                sectionMenus.put(sc.getId(), m);
                dependenciesMenu.getItems().add(m);
            }
        }
        Menu othersMenu = new Menu(Lang.MENU_OTHER_TASKS.get());

        for (TaskItem other : globalDatabase) {
            if (other.getId().equals(owner.getId()) || other.isArchived()) continue;

            CheckBox cb = new CheckBox(other.getTextContent());
            cb.setStyle("-fx-text-fill: white;");
            cb.setSelected(selectedDeps.contains(other.getId()));
            if (cb.isSelected()) depCount[0]++;

            cb.setOnAction(e -> {
                if (cb.isSelected() && !selectedDeps.contains(other.getId())) selectedDeps.add(other.getId());
                else if (!cb.isSelected()) selectedDeps.remove(other.getId());
                dependenciesMenu.setText(Lang.HOOKED_REQUIREMENTS_COUNT.get(selectedDeps.size()));
            });

            CustomMenuItem item = new CustomMenuItem(cb);
            item.setHideOnClick(false);

            Menu targetMenu = sectionMenus.get(other.getSectionId());
            if (targetMenu != null) targetMenu.getItems().add(item);
            else othersMenu.getItems().add(item);
        }

        dependenciesMenu.getItems().removeIf(menuItem -> menuItem instanceof Menu && ((Menu) menuItem).getItems().isEmpty());
        if (!othersMenu.getItems().isEmpty()) dependenciesMenu.getItems().add(othersMenu);

        dependenciesMenu.setText(Lang.HOOKED_REQUIREMENTS_COUNT.get(depCount[0]));
        if (dependenciesMenu.getItems().isEmpty()) {
            CustomMenuItem emptyItem = new CustomMenuItem(new Label(Lang.NO_OTHER_TASKS.get()));
            emptyItem.setDisable(true);
            dependenciesMenu.getItems().add(emptyItem);
        }

        return dependenciesMenu;
    }
}
