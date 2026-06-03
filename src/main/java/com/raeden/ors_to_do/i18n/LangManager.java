package com.raeden.ors_to_do.i18n;

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
 */
public final class LangManager {

    private LangManager() { }

    /**
     * Fetches the message for {@code key} and substitutes any positional placeholders.
     *
     * @param key  the message to fetch
     * @param args values for the {@code {0}}, {@code {1}}, ... placeholders, in order
     * @return the fully formatted, ready-to-display string
     */
    public static String get(Lang key, Object... args) {
        if (key == null) return "";
        String result = key.template();
        if (args == null || args.length == 0) return result;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", args[i] == null ? "" : String.valueOf(args[i]));
        }
        return result;
    }
}
