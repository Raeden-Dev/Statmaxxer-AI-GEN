package com.raeden.ors_to_do.dependencies.models;

import java.io.Serializable;
import java.util.UUID;

/**
 * A customizable card attached to a specific calendar day — a journal entry, an event, or a custom
 * "scenario". Journal entries and events render a fixed "log"/"event" tag; a scenario carries a
 * user-defined tag label and tag colour. Used by Calendar Page sections in Journal mode (in
 * Journal-Only mode a day can hold multiple of these).
 */
public class CalendarEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private boolean event;          // false = journal entry, true = event
    private boolean scenario;       // true = custom scenario (overrides event/log tag with tagLabel/tagColor)
    private String tagLabel;        // custom scenario tag text
    private String tagColor;        // custom scenario tag/accent colour (null -> default)
    private String text = "";

    private String bgColor;         // null/"transparent" -> default card background
    private String outlineColor;    // null/"transparent" -> default border
    private String iconSymbol = "None";
    private String iconColor = "#FFFFFF";

    public CalendarEntry() { this.id = UUID.randomUUID().toString(); }

    public CalendarEntry(boolean event, String text) {
        this();
        this.event = event;
        this.text = text == null ? "" : text;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public boolean isEvent() { return event; }
    public void setEvent(boolean event) { this.event = event; }

    public boolean isScenario() { return scenario; }
    public void setScenario(boolean scenario) { this.scenario = scenario; }

    public String getTagLabel() { return tagLabel == null ? "" : tagLabel; }
    public void setTagLabel(String tagLabel) { this.tagLabel = tagLabel; }

    public String getTagColor() { return tagColor == null ? "#569CD6" : tagColor; }
    public void setTagColor(String tagColor) { this.tagColor = tagColor; }

    public String getText() { return text == null ? "" : text; }
    public void setText(String text) { this.text = text; }

    public String getBgColor() { return bgColor; }
    public void setBgColor(String bgColor) { this.bgColor = bgColor; }

    public String getOutlineColor() { return outlineColor; }
    public void setOutlineColor(String outlineColor) { this.outlineColor = outlineColor; }

    public String getIconSymbol() { return iconSymbol == null ? "None" : iconSymbol; }
    public void setIconSymbol(String iconSymbol) { this.iconSymbol = iconSymbol; }
    public boolean hasIcon() { return iconSymbol != null && !iconSymbol.equals("None") && !iconSymbol.isBlank(); }

    public String getIconColor() { return iconColor == null ? "#FFFFFF" : iconColor; }
    public void setIconColor(String iconColor) { this.iconColor = iconColor; }
}
