package com.raeden.ors_to_do.modules.dependencies.ui.utils;

import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads the bundled decorative fonts (Retro / Pixel Art / Matrix, plus system generics) and applies
 * a chosen font family across the whole JavaFX scene by injecting a tiny {@code .root} stylesheet.
 *
 * <p>Cards set only {@code -fx-font-size} inline, so the family set on {@code .root} cascades to
 * every control. "Default" removes the override and returns to the platform font.</p>
 */
public final class FontManager {

    private FontManager() { }

    /** Display name (shown in the dropdown) -> CSS font-family value (null = no override). */
    private static final Map<String, String> OPTION_TO_FAMILY = new LinkedHashMap<>();
    static {
        OPTION_TO_FAMILY.put("Default", null);
        OPTION_TO_FAMILY.put("Retro (VT323)", "VT323");
        OPTION_TO_FAMILY.put("Pixel Art (Press Start 2P)", "Press Start 2P");
        OPTION_TO_FAMILY.put("Matrix (Share Tech Mono)", "Share Tech Mono");
        OPTION_TO_FAMILY.put("Monospace", "monospace");
        OPTION_TO_FAMILY.put("Serif", "serif");
    }

    private static final String[] BUNDLED = {
            "/fonts/VT323-Regular.ttf",
            "/fonts/PressStart2P-Regular.ttf",
            "/fonts/ShareTechMono-Regular.ttf"
    };

    private static boolean registered = false;
    /** The currently-applied font stylesheet URI, tracked so it can be swapped out cleanly. */
    private static String activeStylesheet = null;

    /** Loads the bundled TTFs into the JavaFX font registry. Safe to call more than once. */
    public static synchronized void registerFonts() {
        if (registered) return;
        for (String path : BUNDLED) {
            try (java.io.InputStream in = FontManager.class.getResourceAsStream(path)) {
                if (in != null) Font.loadFont(in, 12);
            } catch (Exception ignore) { }
        }
        registered = true;
    }

    public static String[] options() {
        return OPTION_TO_FAMILY.keySet().toArray(new String[0]);
    }

    /** Normalises a possibly-stale stored value back to a known option. */
    public static String normalize(String displayName) {
        return OPTION_TO_FAMILY.containsKey(displayName) ? displayName : "Default";
    }

    /** Applies {@code displayName}'s font family to the scene root (or clears it for "Default"). */
    public static void apply(Scene scene, String displayName) {
        if (scene == null) return;
        registerFonts();

        if (activeStylesheet != null) {
            scene.getStylesheets().remove(activeStylesheet);
            activeStylesheet = null;
        }

        String family = OPTION_TO_FAMILY.get(normalize(displayName));
        if (family == null) return; // Default — nothing to inject

        String css = ".root { -fx-font-family: \"" + family + "\"; }";
        activeStylesheet = "data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes());
        scene.getStylesheets().add(activeStylesheet);
    }
}
