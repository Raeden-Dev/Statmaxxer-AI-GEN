package com.raeden.ors_to_do.dependencies.models;

import java.io.Serializable;

/**
 * Per-category visual customization for a section. Stored on {@link SectionConfig} as a small list
 * so the user's choices (background, border, text colour, icon) survive restarts and travel with
 * the section preset. A category bar without a matching {@code CategoryStyle} falls back to the
 * built-in dark theme.
 */
public class CategoryStyle implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Category name this style applies to. Used as the lookup key. */
    private String name;

    private String backgroundColor;
    private String borderColor;
    private String textColor;

    private String iconSymbol;
    private String iconColor;

    public CategoryStyle() { }

    public CategoryStyle(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    public String getBorderColor() { return borderColor; }
    public void setBorderColor(String borderColor) { this.borderColor = borderColor; }

    public String getTextColor() { return textColor; }
    public void setTextColor(String textColor) { this.textColor = textColor; }

    public String getIconSymbol() { return iconSymbol; }
    public void setIconSymbol(String iconSymbol) { this.iconSymbol = iconSymbol; }

    public String getIconColor() { return iconColor; }
    public void setIconColor(String iconColor) { this.iconColor = iconColor; }

    /** True when every styling field is still at its default — handy for the "reset" path. */
    public boolean isDefault() {
        return backgroundColor == null && borderColor == null && textColor == null
                && (iconSymbol == null || "None".equals(iconSymbol)) && iconColor == null;
    }
}
