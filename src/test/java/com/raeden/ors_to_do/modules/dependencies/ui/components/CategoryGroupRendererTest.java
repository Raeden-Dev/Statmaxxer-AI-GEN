package com.raeden.ors_to_do.modules.dependencies.ui.components;

import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.i18n.Lang;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Pure tests for {@link CategoryGroupRenderer}'s grouping helpers. The actual JavaFX rendering
 * isn't covered here (it requires a UI thread); the grouping logic is what matters for
 * correctness.
 */
public class CategoryGroupRendererTest {

    private static TaskItem t(String text, String category) {
        TaskItem ti = new TaskItem(text, null, "sec");
        ti.setCategoryName(category);
        return ti;
    }

    @Test
    public void groupByCategory_bucketsNullAndBlankAsUncategorized() {
        List<TaskItem> tasks = new ArrayList<>();
        tasks.add(t("a", "Work"));
        tasks.add(t("b", null));
        tasks.add(t("c", "  "));
        tasks.add(t("d", "Work"));

        Map<String, List<TaskItem>> grouped = CategoryGroupRenderer.groupByCategory(tasks);

        assertEquals(2, grouped.get("Work").size());
        assertEquals(2, grouped.get(Lang.CATEGORY_UNCATEGORIZED.get()).size());
    }

    @Test
    public void groupByCategory_preservesFirstSeenOrder() {
        List<TaskItem> tasks = new ArrayList<>();
        tasks.add(t("a", "Beta"));
        tasks.add(t("b", "Alpha"));
        tasks.add(t("c", "Beta"));

        Map<String, List<TaskItem>> grouped = CategoryGroupRenderer.groupByCategory(tasks);

        String[] keys = grouped.keySet().toArray(new String[0]);
        assertEquals("Beta", keys[0]);
        assertEquals("Alpha", keys[1]);
    }

    @Test
    public void uniqueCategories_skipsBlanksAndDeduplicates() {
        List<TaskItem> tasks = new ArrayList<>();
        tasks.add(t("a", "X"));
        tasks.add(t("b", null));
        tasks.add(t("c", "Y"));
        tasks.add(t("d", "X"));
        tasks.add(t("e", "   "));

        List<String> uniq = CategoryGroupRenderer.uniqueCategories(tasks);
        assertEquals(2, uniq.size());
        assertEquals("X", uniq.get(0));
        assertEquals("Y", uniq.get(1));
    }
}
