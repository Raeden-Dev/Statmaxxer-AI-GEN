package com.raeden.ors_to_do.modules.dependencies.ui.forms;

import com.raeden.ors_to_do.dependencies.models.SectionConfig;
import com.raeden.ors_to_do.dependencies.models.TaskItem;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.raeden.ors_to_do.modules.dependencies.ui.dialogs.TaskDialogs.ICON_LIST;

public class TaskStyleForm {
    private ColorPicker bgColorPicker, outlinePicker, sideboxPicker, iconColorPicker, preC;
    private ComboBox<String> iconBox;
    private TextField prefixFieldEdit;
    private boolean allowStyling, allowIcons, allowPrefix;

    public void buildUI(GridPane grid, AtomicInteger rowIdx, TaskItem task, SectionConfig config) {
        // Link cards are always styleable (background + outline + sidebox) regardless of the
        // section's "Enable Task Styling" flag, so they can be themed the same as ordinary cards.
        allowStyling = task.isLinkCard() || (config != null && (config.isNotesPage() || config.isEnableTaskStyling()));
        allowIcons = config == null || config.isEnableIcons();
        allowPrefix = config == null || config.isShowPrefix();

        if (!allowStyling && !allowIcons && !allowPrefix) return;

        grid.add(new Separator(), 0, rowIdx.get(), 2, 1); rowIdx.getAndIncrement();
        Label styleHeader = new Label("Task Appearance & Styling:");
        styleHeader.setStyle("-fx-text-fill: #569CD6; -fx-font-weight: bold;");
        grid.add(styleHeader, 0, rowIdx.get(), 2, 1); rowIdx.getAndIncrement();

        if (allowStyling) {
            bgColorPicker = createColorPicker(task.getColorHex());
            outlinePicker = createColorPicker(task.getCustomOutlineColor());
            sideboxPicker = createColorPicker(task.getCustomSideboxColor());

            grid.add(new Label("Background Color:"), 0, rowIdx.get()); grid.add(bgColorPicker, 1, rowIdx.getAndIncrement());
            grid.add(new Label("Outline Color:"), 0, rowIdx.get()); grid.add(outlinePicker, 1, rowIdx.getAndIncrement());
            grid.add(new Label("Sidebox Color:"), 0, rowIdx.get()); grid.add(sideboxPicker, 1, rowIdx.getAndIncrement());
        }

        if (allowIcons) {
            iconBox = new ComboBox<>();
            iconBox.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(iconBox, Priority.ALWAYS);
            iconBox.getItems().addAll(ICON_LIST);
            iconBox.setValue(task.getIconSymbol() != null ? task.getIconSymbol() : "None");
            iconColorPicker = new ColorPicker(Color.web(task.getIconColor() != null ? task.getIconColor() : "#FFFFFF"));

            grid.add(new Label("Icon & Color:"), 0, rowIdx.get());
            grid.add(new HBox(10, iconBox, iconColorPicker), 1, rowIdx.getAndIncrement());
        }

        if (allowPrefix) {
            prefixFieldEdit = new TextField(task.getPrefix() != null ? task.getPrefix() : "");
            prefixFieldEdit.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(prefixFieldEdit, Priority.ALWAYS);
            preC = new ColorPicker(Color.web(task.getPrefixColor() != null ? task.getPrefixColor() : "#4EC9B0"));

            grid.add(new Label("Prefix & Color:"), 0, rowIdx.get());
            grid.add(new HBox(10, prefixFieldEdit, preC), 1, rowIdx.getAndIncrement());
        }

        Button randomBtn = new Button("🎲 Randomize Style");
        randomBtn.setMaxWidth(Double.MAX_VALUE);
        randomBtn.setOnAction(e -> applyRandomStyle());
        grid.add(randomBtn, 1, rowIdx.getAndIncrement());
        grid.add(new Separator(), 0, rowIdx.get(), 2, 1); rowIdx.getAndIncrement();
    }

    public void applyTo(TaskItem task) {
        if (allowStyling) {
            task.setColorHex(toHexString(bgColorPicker.getValue()));
            task.setCustomOutlineColor(toHexString(outlinePicker.getValue()));
            task.setCustomSideboxColor(toHexString(sideboxPicker.getValue()));
        }
        if (allowIcons) {
            task.setIconSymbol(iconBox.getValue());
            task.setIconColor(toHexString(iconColorPicker.getValue()));
        }
        if (allowPrefix) {
            task.setPrefix(prefixFieldEdit.getText().trim());
            task.setPrefixColor(toHexString(preC.getValue()));
        }
    }

    private void applyRandomStyle() {
        Random rand = new Random();
        double hue = rand.nextDouble() * 360.0;
        if (iconBox != null) iconBox.setValue(ICON_LIST[rand.nextInt(ICON_LIST.length - 1) + 1]);
        if (iconColorPicker != null) iconColorPicker.setValue(Color.hsb(hue, 0.5, 0.95));
        if (preC != null) preC.setValue(Color.hsb(hue, 0.7, 0.55));
        if (bgColorPicker != null) bgColorPicker.setValue(Color.hsb(hue, 0.8, 0.2));
        if (outlinePicker != null) outlinePicker.setValue(Color.hsb(hue, 0.8, 0.8));
        if (sideboxPicker != null) sideboxPicker.setValue(Color.hsb(hue, 0.6, 0.9));
    }

    private ColorPicker createColorPicker(String hex) {
        ColorPicker cp = new ColorPicker();
        cp.setMaxWidth(Double.MAX_VALUE);
        cp.setValue(hex != null && !hex.equals("transparent") ? Color.web(hex) : Color.TRANSPARENT);
        return cp;
    }

    private String toHexString(Color color) {
        // --- FIXED: Explicitly check for 0.0 opacity so transparent colors don't default to Black ---
        if (color == null || color.getOpacity() == 0.0) return "transparent";
        return String.format("#%02X%02X%02X", (int)(color.getRed()*255), (int)(color.getGreen()*255), (int)(color.getBlue()*255));
    }
}