package com.raeden.ors_to_do.i18n;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Central access point for all user-facing text in the application.
 *
 * <p>Every message lives in the {@link Lang} enum. Code asks for a message by its
 * enum key instead of hard-coding the literal, which keeps all copy in one place,
 * makes it trivial to audit/translate, and prevents the same sentence from drifting
 * across files.</p>
 *
 * <p>Messages support positional placeholders ({@code {0}}, {@code {1}}, ...) that are
 * substituted by the {@code args} passed in. Example:</p>
 * <pre>
 *     LangManager.get(Lang.LOOT_GLOBAL_PTS, points);   // "+25 Global Pts"
 *     Lang.LOOT_GLOBAL_PTS.get(points);                // convenience, same result
 * </pre>
 *
 * <h3>Localisation</h3>
 * The {@link Lang} enum's built-in templates are the implicit {@code "en"} locale. Additional
 * languages can be plugged in at runtime without touching any call site:
 * <pre>
 *     Map&lt;Lang, String&gt; fr = new EnumMap&lt;&gt;(Lang.class);
 *     fr.put(Lang.BTN_CHALLENGE_DONE, "Défi terminé");
 *     LangManager.registerLocale("fr", fr);   // partial tables are fine
 *     LangManager.setLocale("fr");            // missing keys fall back to the enum default
 * </pre>
 */
public final class LangManager {

    /** Locale id -> (message key -> translated template). The enum defaults are the "en" baseline. */
    private static final Map<String, Map<Lang, String>> LOCALES = new HashMap<>();

    private static volatile String activeLocale = "en";

    private LangManager() { }

    /**
     * Registers (or replaces) a locale's translation table. Tables may be partial — any key not
     * present falls back to the {@link Lang} enum's default template.
     */
    public static void registerLocale(String locale, Map<Lang, String> table) {
        if (locale == null || table == null) return;
        LOCALES.put(locale, new EnumMap<>(table));
    }

    /** Switches the active locale used by all subsequent {@link #get} calls. */
    public static void setLocale(String locale) {
        if (locale != null) activeLocale = locale;
    }

    public static String getLocale() {
        return activeLocale;
    }

    /**
     * Fetches the message for {@code key} (in the active locale) and substitutes any positional
     * placeholders.
     *
     * @param key  the message to fetch
     * @param args values for the {@code {0}}, {@code {1}}, ... placeholders, in order
     * @return the fully formatted, ready-to-display string
     */
    public static String get(Lang key, Object... args) {
        if (key == null) return "";

        String result = resolveTemplate(key);
        if (args == null || args.length == 0) return result;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", args[i] == null ? "" : String.valueOf(args[i]));
        }
        return result;
    }

    private static String resolveTemplate(Lang key) {
        Map<Lang, String> table = LOCALES.get(activeLocale);
        if (table != null) {
            String override = table.get(key);
            if (override != null) return override;
        }
        return key.template();
    }
}
