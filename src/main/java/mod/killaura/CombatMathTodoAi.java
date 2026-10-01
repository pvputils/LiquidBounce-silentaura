/* Derived from LiquidBounce KillAura. Copyright (c) 2015-2026 CCBlueX.
 * Licensed under GPL-3.0-or-later; see LICENSE. */
package mod.killaura;

public final class CombatMathTodoAi {
    private CombatMathTodoAi() {}
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    public static double distanceSquaredToBox(double x, double y, double z,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double dx = x - clamp(x, minX, maxX);
        double dy = y - clamp(y, minY, maxY);
        double dz = z - clamp(z, minZ, maxZ);
        return dx * dx + dy * dy + dz * dz;
    }
    public static float yaw(double dx, double dz) {
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }
    public static float pitch(double dx, double dy, double dz) {
        return (float) clamp(-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))), -90, 90);
    }
    public static float wrap(float angle) {
        float result = angle % 360;
        if (result >= 180) result -= 360;
        if (result < -180) result += 360;
        return result;
    }
    public static float turn(float current, float desired, float maximumStep) {
        return current + (float) clamp(wrap(desired - current), -maximumStep, maximumStep);
    }
    public static long intervalNanos(double clicksPerSecond) {
        if (!Double.isFinite(clicksPerSecond) || clicksPerSecond <= 0) {
            throw new IllegalArgumentException("CPS must be finite and positive");
        }
        return (long) (1_000_000_000.0 / clicksPerSecond);
    }
}
