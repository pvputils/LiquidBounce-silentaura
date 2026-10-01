package mod.killaura;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CombatMathTestTodoAi {
    private static int checks;
    private static void close(double actual, double expected) {
        checks++;
        if (Math.abs(actual - expected) > 0.0001) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) throws Exception {
        close(CombatMathTodoAi.yaw(0, 1), 0);
        close(CombatMathTodoAi.yaw(1, 0), -90);
        close(CombatMathTodoAi.pitch(0, 1, 0), -90);
        close(CombatMathTodoAi.turn(179, -179, 1), 180);
        close(CombatMathTodoAi.turn(-179, 179, 1), -180);
        close(CombatMathTodoAi.distanceSquaredToBox(3, 1, 0, -1, 0, -1, 1, 2, 1), 4);
        close(CombatMathTodoAi.distanceSquaredToBox(0, 1, 0, -1, 0, -1, 1, 2, 1), 0);
        close(CombatMathTodoAi.intervalNanos(10), 100_000_000);
        Path dir = Files.createTempDirectory("killaura-testsTodoAi");
        Path config = dir.resolve("settingsTodoAi.properties");
        try {
            KillAuraConfigTodoAi defaults = KillAuraConfigTodoAi.load(config);
            close(defaults.range(), 3);
            Files.writeString(config, "range=NaN\nwallRange=100\nminCps=15\nmaxCps=2\nturnSpeed=-50\nplayers=invalid\n");
            KillAuraConfigTodoAi normalized = KillAuraConfigTodoAi.load(config);
            close(normalized.range(), 3);
            close(normalized.wallRange(), 3);
            close(normalized.minCps(), 15);
            close(normalized.maxCps(), 15);
            close(normalized.turnSpeed(), 1);
            checks++;
            if (!normalized.players()) throw new AssertionError("Invalid flag must retain default");
        } finally { Files.deleteIfExists(config); Files.delete(dir); }
        System.out.println("Passed " + checks + " combat/config checks.");
    }
}
