package com.raeden.ors_to_do.modules.dependencies.ui.dialogs;

import com.raeden.ors_to_do.dependencies.models.AppStats;
import com.raeden.ors_to_do.dependencies.models.CategoryStyle;
import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import com.raeden.ors_to_do.dependencies.storage.StorageManager;
import com.raeden.ors_to_do.i18n.Lang;
import com.raeden.ors_to_do.modules.dependencies.ui.utils.ColorUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Small editor for the colour/icon of a single category bar. Opened from the chevron-header gear
 * button or the right-click context menu in {@link com.raeden.ors_to_do.modules.dependencies.ui.components.CategoryGroupRenderer}.
 *
 * <p>Styles live on {@link SectionConfig#getCategoryStyles()} keyed by category name, so a category
 * keeps its colour even after the user navigates away and comes back. Choosing "Reset to Default"
 * deletes the style entry, dropping the bar back to the built-in dark theme.</p>
 */
public final class CategoryStyleDialog {

    private CategoryStyleDialog() { }

    /** Default colour values shown when no custom style exists yet. */
    private static final String DEFAULT_BG = "#252526";
    private static final String DEFAULT_BORDER = "#3E3E42";
    private static final String DEFAULT_TEXT = "#DCDCAA";
    private static final String DEFAULT_ICON_COLOR = "#FFFFFF";

    /**
     * Opens the styling dialog. On OK, mutates / inserts the {@link CategoryStyle} on
     * {@code section}; on Reset, removes any existing style entry. If the user typed a new
     * category name, every matching task is renamed and the persisted collapse state + style key
     * are migrated. Returns true if anything was persisted (so the caller knows to save +
     * refresh).
     */
    public static boolean show(SectionConfig section, String categoryName, List<TaskItem> globalDatabase, AppStats appStats) {
        if (section == null || categoryName == null) return false;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Lang.CATEGORY_STYLE_DIALOG_TITLE.get(categoryName));
        TaskDialogs.styleDialog(dialog);

        // The Uncategorized bucket is a virtual fallback for null/empty categoryName — renaming it
        // wouldn't reassign the null-category tasks, so block edits on its name.
        boolean isUncategorized = Lang.CATEGORY_UNCATEGORIZED.get().equals(categoryName);

        TextField nameField = new TextField(categoryName);
        if (isUncategorized) {
            nameField.setEditable(false);
            nameField.setDisable(true);
            nameField.setTooltip(new Tooltip(Lang.CATEGORY_NAME_UNCATEGORIZED_TOOLTIP.get()));
        }

        CategoryStyle existing = section.findCategoryStyle(categoryName);

        ColorPicker bgPicker = new ColorPicker(Color.web(existing != null && existing.getBackgroundColor() != null ? existing.getBackgroundColor() : DEFAULT_BG));
        ColorPicker borderPicker = new ColorPicker(Color.web(existing != null && existing.getBorderColor() != null ? existing.getBorderColor() : DEFAULT_BORDER));
        ColorPicker textPicker = new ColorPicker(Color.web(existing != null && existing.getTextColor() != null ? existing.getTextColor() : DEFAULT_TEXT));

        ComboBox<String> iconBox = new ComboBox<>();
        iconBox.getItems().addAll(TaskDialogs.ICON_LIST);
        iconBox.setValue(existing != null && existing.getIconSymbol() != null ? existing.getIconSymbol() : "None");

        ColorPicker iconColorPicker = new ColorPicker(Color.web(existing != null && existing.getIconColor() != null ? existing.getIconColor() : DEFAULT_ICON_COLOR));

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(10));
        ColumnConstraints c1 = new ColumnConstraints(); c1.setMinWidth(140);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        grid.add(new Label(Lang.CATEGORY_NAME_LABEL.get()), 0, 0);       grid.add(nameField, 1, 0);
        grid.add(new Label(Lang.CATEGORY_STYLE_BACKGROUND.get()), 0, 1); grid.add(bgPicker, 1, 1);
        grid.add(new Label(Lang.CATEGORY_STYLE_BORDER.get()), 0, 2);     grid.add(borderPicker, 1, 2);
        grid.add(new Label(Lang.CATEGORY_STYLE_TEXT.get()), 0, 3);       grid.add(textPicker, 1, 3);
        grid.add(new Label(Lang.CATEGORY_STYLE_ICON.get()), 0, 4);       grid.add(new HBox(10, iconBox, iconColorPicker), 1, 4);

        // Randomize button — picks a random hue and derives a cohesive palette around it. Matches
        // the same pattern used in TaskStyleForm / ChallengeConfigDialog so the look is consistent.
        Button randomBtn = new Button(Lang.BTN_RANDOMIZE_STYLE.get());
        randomBtn.setMaxWidth(Double.MAX_VALUE);
        randomBtn.setOnAction(e -> {
            java.util.Random rand = new java.util.Random();
            double hue = rand.nextDouble() * 360.0;
            // Skip "None" (index 0) so the random icon is always visible.
            iconBox.setValue(TaskDialogs.ICON_LIST[rand.nextInt(TaskDialogs.ICON_LIST.length - 1) + 1]);
            bgPicker.setValue(Color.hsb(hue, 0.8, 0.2));       // dark backdrop in the hue
            borderPicker.setValue(Color.hsb(hue, 0.8, 0.8));   // bright accent border
            textPicker.setValue(Color.hsb(hue, 0.5, 0.95));    // light, readable text
            iconColorPicker.setValue(Color.hsb(hue, 0.5, 0.95));
        });
        grid.add(randomBtn, 1, 5);

        // Reset button — clears the style entry entirely.
        ButtonType resetBtnType = new ButtonType(Lang.CATEGORY_STYLE_RESET.get(), ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(resetBtnType, ButtonType.OK, ButtonType.CANCEL);

        ButtonType result = dialog.showAndWait().orElse(ButtonType.CANCEL);

        if (result == ButtonType.OK) {
            // Rename first (if the user changed the name), then mutate the style under whatever the
            // effective key is now. The Uncategorized check above keeps nameField disabled for the
            // virtual bucket, so this branch only runs for real categories.
            String effectiveName = categoryName;
            if (!isUncategorized) {
                String typed = nameField.getText() != null ? nameField.getText().trim() : "";
                if (!typed.isEmpty() && !typed.equals(categoryName)) {
                    if (section.renameCategory(categoryName, typed, globalDatabase, appStats) && globalDatabase != null) {
                        StorageManager.saveTasks(globalDatabase);
                    }
                    effectiveName = typed;
                }
            }

            CategoryStyle style = section.upsertCategoryStyle(effectiveName);
            style.setBackgroundColor(ColorUtil.toHex(bgPicker.getValue()));
            style.setBorderColor(ColorUtil.toHex(borderPicker.getValue()));
            style.setTextColor(ColorUtil.toHex(textPicker.getValue()));
            String chosenIcon = iconBox.getValue();
            style.setIconSymbol(chosenIcon != null && !"None".equals(chosenIcon) ? chosenIcon : null);
            style.setIconColor(style.getIconSymbol() != null ? ColorUtil.toHex(iconColorPicker.getValue()) : null);
            return true;
        }
        if (result == resetBtnType) {
            section.removeCategoryStyle(categoryName);
            return true;
        }
        return false;
    }
}
