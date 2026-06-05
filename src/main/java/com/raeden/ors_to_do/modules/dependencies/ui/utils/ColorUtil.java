package com.raeden.ors_to_do.modules.dependencies.ui.utils;

import javafx.scene.paint.Color;

/**
 * Single source of truth for converting a JavaFX {@link Color} to the {@code #RRGGBB} hex strings
 * the app persists. Previously this exact conversion was copy-pasted as a private {@code toHexString}
 * into {@code ChallengeCard}, {@code PerkCard}, {@code SectionEditDialog}, {@code TaskDialogs}, etc.
 */
public final class ColorUtil {

    private ColorUtil() { }

    /**
     * @return {@code #RRGGBB} for the colour, or {@code null} if the colour is {@code null}.
     */
    public static String toHex(Color color) {
        if (color == null) return null;
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * Variant used by the card editors: a fully transparent colour is stored as the literal
     * {@code "transparent"} (so "no colour" round-trips), otherwise {@code #RRGGBB}.
     */
    public static String toHexOrTransparent(Color color) {
        if (color == null || color.getOpacity() == 0.0) return "transparent";
        return toHex(color);
    }
}
