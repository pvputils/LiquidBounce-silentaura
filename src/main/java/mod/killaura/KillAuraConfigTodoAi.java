/* Derived from LiquidBounce KillAura. Copyright (c) 2015-2026 CCBlueX.
 * Licensed under GPL-3.0-or-later; see LICENSE. */
package mod.killaura;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record KillAuraConfigTodoAi(double range, double wallRange, double scanExtra,
        double minCps, double maxCps, float turnSpeed, boolean cooldown, boolean players,
        boolean hostileMobs, boolean animals, boolean invisible, boolean ignoreShield,
        boolean autoBlock, boolean pauseWhileUsing, boolean requireWeapon, boolean criticalOnly) {
    public static KillAuraConfigTodoAi defaults() {
        return new KillAuraConfigTodoAi(3, 0, 2, 8, 12, 180, true, true,
                true, false, false, true, false, true, false, false);
    }
    public static KillAuraConfigTodoAi load(Path path) throws IOException {
        Properties p = new Properties();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) { p.load(reader); }
        } else {
            Files.createDirectories(path.getParent());
            p.setProperty("range", "3.0"); p.setProperty("wallRange", "0.0");
            p.setProperty("scanExtra", "2.0"); p.setProperty("minCps", "8.0");
            p.setProperty("maxCps", "12.0"); p.setProperty("turnSpeed", "180.0");
            p.setProperty("cooldown", "true"); p.setProperty("players", "true");
            p.setProperty("hostileMobs", "true"); p.setProperty("animals", "false");
            p.setProperty("invisible", "false"); p.setProperty("ignoreShield", "true");
            p.setProperty("autoBlock", "false"); p.setProperty("pauseWhileUsing", "true");
            p.setProperty("requireWeapon", "false"); p.setProperty("criticalOnly", "false");
            try (Writer writer = Files.newBufferedWriter(path)) {
                p.store(writer, "KillAura settings. Use /killaura reload after editing.");
            }
        }
        double range = number(p, "range", 3, 0.1, 6);
        double minCps = number(p, "minCps", 8, 1, 20);
        return new KillAuraConfigTodoAi(range, number(p, "wallRange", 0, 0, range),
                number(p, "scanExtra", 2, 0, 7), minCps,
                Math.max(minCps, number(p, "maxCps", 12, 1, 20)),
                (float) number(p, "turnSpeed", 180, 1, 180), flag(p, "cooldown", true),
                flag(p, "players", true), flag(p, "hostileMobs", true), flag(p, "animals", false),
                flag(p, "invisible", false), flag(p, "ignoreShield", true), flag(p, "autoBlock", false),
                flag(p, "pauseWhileUsing", true), flag(p, "requireWeapon", false), flag(p, "criticalOnly", false));
    }
    private static double number(Properties p, String key, double fallback, double min, double max) {
        try {
            double value = Double.parseDouble(p.getProperty(key, Double.toString(fallback)));
            return Double.isFinite(value) ? CombatMathTodoAi.clamp(value, min, max) : fallback;
        } catch (NumberFormatException ignored) { return fallback; }
    }
    private static boolean flag(Properties p, String key, boolean fallback) {
        String value = p.getProperty(key);
        return value == null ? fallback : switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> fallback;
        };
    }
}
