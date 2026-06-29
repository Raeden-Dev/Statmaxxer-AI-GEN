package com.raeden.ors_to_do.dependencies.models;

import java.io.Serializable;
import java.util.UUID;

/**
 * A reusable, named bundle of visual styling — icon, icon colour, background and outline colours.
 * Used to save and re-apply "Customize Day" looks and journal entry / event looks on Calendar pages,
 * so a user doesn't have to recreate the same colour/icon combination by hand each time.
 *
 * <p>Any field may be null (meaning "leave unchanged / default"); background and outline are unused
 * by day-icon-only customisation.</p>
 */
public class StylePreset implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String iconSymbol;
    private String iconColor;
    private String bgColor;
    private String outlineColor;

    public StylePreset() { this.id = UUID.randomUUID().toString(); }

    public StylePreset(String name) {
        this();
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIconSymbol() { return iconSymbol; }
    public void setIconSymbol(String iconSymbol) { this.iconSymbol = iconSymbol; }

    public String getIconColor() { return iconColor; }
    public void setIconColor(String iconColor) { this.iconColor = iconColor; }

    public String getBgColor() { return bgColor; }
    public void setBgColor(String bgColor) { this.bgColor = bgColor; }

    public String getOutlineColor() { return outlineColor; }
    public void setOutlineColor(String outlineColor) { this.outlineColor = outlineColor; }

    @Override
    public String toString() { return name == null ? "Unnamed" : name; }
}
