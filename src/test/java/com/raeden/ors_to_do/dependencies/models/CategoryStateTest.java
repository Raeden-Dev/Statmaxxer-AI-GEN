package com.raeden.ors_to_do.dependencies.models;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for the per-section category state — the collapse-state persistence and the per-category
 * style lookup. The actual JavaFX rendering isn't covered here; this file guards the model
 * helpers that the renderer depends on.
 */
public class CategoryStateTest {

    // ---- AppStats collapse state ---------------------------------------------

    @Test
    public void collapseState_defaultsToExpanded() {
        AppStats stats = new AppStats();
        assertFalse(stats.isCategoryCollapsed("sec-1", "Work"));
    }

    @Test
    public void collapseState_roundTrips() {
        AppStats stats = new AppStats();
        stats.setCategoryCollapsed("sec-1", "Work", true);
        assertTrue(stats.isCategoryCollapsed("sec-1", "Work"));

        stats.setCategoryCollapsed("sec-1", "Work", false);
        assertFalse(stats.isCategoryCollapsed("sec-1", "Work"));
    }

    @Test
    public void collapseState_isPerSectionAndPerCategory() {
        AppStats stats = new AppStats();
        stats.setCategoryCollapsed("sec-1", "Work", true);

        // Different section, same category name: independent.
        assertFalse(stats.isCategoryCollapsed("sec-2", "Work"));
        // Same section, different category: independent.
        assertFalse(stats.isCategoryCollapsed("sec-1", "Personal"));
    }

    // ---- AppStats separator collapse state ----------------------------------

    @Test
    public void separatorState_defaultsToExpanded() {
        AppStats stats = new AppStats();
        assertFalse(stats.isSeparatorCollapsed("sep-rpg"));
    }

    @Test
    public void separatorState_roundTrips() {
        AppStats stats = new AppStats();
        stats.setSeparatorCollapsed("sep-rpg", true);
        assertTrue(stats.isSeparatorCollapsed("sep-rpg"));

        stats.setSeparatorCollapsed("sep-rpg", false);
        assertFalse(stats.isSeparatorCollapsed("sep-rpg"));
        // Expanded entries are stripped from the map (the default), so the JSON stays compact.
        assertFalse(stats.getCollapsedSeparators().containsKey("sep-rpg"));
    }

    @Test
    public void separatorState_nullIdIsSafe() {
        AppStats stats = new AppStats();
        assertFalse(stats.isSeparatorCollapsed(null));
        stats.setSeparatorCollapsed(null, true); // no-op
        assertTrue(stats.getCollapsedSeparators().isEmpty());
    }

    @Test
    public void collapseState_nullKeysAreSafe() {
        AppStats stats = new AppStats();
        // Should not throw and should report not-collapsed.
        assertFalse(stats.isCategoryCollapsed(null, "Work"));
        assertFalse(stats.isCategoryCollapsed("sec", null));
        stats.setCategoryCollapsed(null, "Work", true);   // no-op
        stats.setCategoryCollapsed("sec", null, true);    // no-op
        assertTrue(stats.getCollapsedCategories().isEmpty());
    }

    // ---- SectionConfig.findCategoryStyle / upsert / remove ------------------

    @Test
    public void findCategoryStyle_returnsNullForUnknown() {
        SectionConfig sc = new SectionConfig("id", "name");
        assertNull(sc.findCategoryStyle("Work"));
    }

    @Test
    public void upsertCategoryStyle_createsThenReuses() {
        SectionConfig sc = new SectionConfig("id", "name");
        CategoryStyle a = sc.upsertCategoryStyle("Work");
        CategoryStyle b = sc.upsertCategoryStyle("Work");
        assertSame("upsert must return the existing instance on the second call", a, b);
        assertEquals(1, sc.getCategoryStyles().size());
    }

    @Test
    public void removeCategoryStyle_dropsTheEntry() {
        SectionConfig sc = new SectionConfig("id", "name");
        sc.upsertCategoryStyle("Work").setBackgroundColor("#112233");
        assertNotNull(sc.findCategoryStyle("Work"));

        sc.removeCategoryStyle("Work");
        assertNull(sc.findCategoryStyle("Work"));
    }

    // ---- SectionConfig.renameCategory ---------------------------------------

    private static TaskItem taskIn(String sectionId, String category, String text) {
        TaskItem t = new TaskItem(text, null, sectionId);
        t.setCategoryName(category);
        return t;
    }

    @Test
    public void renameCategory_renamesEveryMatchingTaskInTheSection() {
        SectionConfig sc = new SectionConfig("sec", "Section");
        List<TaskItem> tasks = new ArrayList<>();
        tasks.add(taskIn("sec", "Work", "a"));
        tasks.add(taskIn("sec", "Work", "b"));
        tasks.add(taskIn("sec", "Personal", "c"));   // different category — untouched
        tasks.add(taskIn("other", "Work", "d"));      // different section — untouched

        boolean changed = sc.renameCategory("Work", "Office", tasks, new AppStats());

        assertTrue(changed);
        assertEquals("Office", tasks.get(0).getCategoryName());
        assertEquals("Office", tasks.get(1).getCategoryName());
        assertEquals("Personal", tasks.get(2).getCategoryName());
        assertEquals("Work", tasks.get(3).getCategoryName());
    }

    @Test
    public void renameCategory_migratesCollapseState() {
        SectionConfig sc = new SectionConfig("sec", "Section");
        AppStats stats = new AppStats();
        stats.setCategoryCollapsed("sec", "Work", true);

        sc.renameCategory("Work", "Office", new ArrayList<>(), stats);

        assertFalse(stats.isCategoryCollapsed("sec", "Work"));
        assertTrue(stats.isCategoryCollapsed("sec", "Office"));
    }

    @Test
    public void renameCategory_migratesStyleKey() {
        SectionConfig sc = new SectionConfig("sec", "Section");
        CategoryStyle s = sc.upsertCategoryStyle("Work");
        s.setBackgroundColor("#112233");

        sc.renameCategory("Work", "Office", new ArrayList<>(), new AppStats());

        assertNull(sc.findCategoryStyle("Work"));
        CategoryStyle migrated = sc.findCategoryStyle("Office");
        assertNotNull(migrated);
        assertEquals("#112233", migrated.getBackgroundColor());
    }

    @Test
    public void renameCategory_mergeWhenDestinationStyleAlreadyExists() {
        SectionConfig sc = new SectionConfig("sec", "Section");
        sc.upsertCategoryStyle("Work").setBackgroundColor("#OLD000");
        sc.upsertCategoryStyle("Office").setBackgroundColor("#NEW000");

        sc.renameCategory("Work", "Office", new ArrayList<>(), new AppStats());

        // Source dropped, destination's style preserved.
        assertNull(sc.findCategoryStyle("Work"));
        assertEquals("#NEW000", sc.findCategoryStyle("Office").getBackgroundColor());
    }

    @Test
    public void renameCategory_rejectsEmptyOrUnchangedName() {
        SectionConfig sc = new SectionConfig("sec", "Section");
        List<TaskItem> tasks = new ArrayList<>();
        tasks.add(taskIn("sec", "Work", "a"));

        assertFalse(sc.renameCategory("Work", "", tasks, new AppStats()));
        assertFalse(sc.renameCategory("Work", "   ", tasks, new AppStats()));
        assertFalse(sc.renameCategory("Work", "Work", tasks, new AppStats()));
        assertEquals("Work", tasks.get(0).getCategoryName());
    }

    @Test
    public void renameCategory_trimsWhitespace() {
        SectionConfig sc = new SectionConfig("sec", "Section");
        List<TaskItem> tasks = new ArrayList<>();
        tasks.add(taskIn("sec", "Work", "a"));

        assertTrue(sc.renameCategory("Work", "  Office  ", tasks, new AppStats()));
        assertEquals("Office", tasks.get(0).getCategoryName());
    }

    @Test
    public void categoryStyle_isDefault_detectsClearedState() {
        CategoryStyle s = new CategoryStyle("Work");
        assertTrue(s.isDefault());

        s.setBackgroundColor("#FF0000");
        assertFalse(s.isDefault());

        s.setBackgroundColor(null);
        assertTrue(s.isDefault());

        s.setIconSymbol("None");
        assertTrue("'None' icon counts as default", s.isDefault());

        s.setIconSymbol("🔥");
        assertFalse(s.isDefault());
    }
}
