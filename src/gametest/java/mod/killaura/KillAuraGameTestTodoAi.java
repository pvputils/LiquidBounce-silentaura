package mod.killaura;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class KillAuraGameTestTodoAi implements FabricClientGameTest {
    private static float health(TestServerContext server, String tag) {
        return server.computeOnServer(instance -> instance.getLevel(Level.OVERWORLD)
                .getEntitiesOfClass(Husk.class, new AABB(-8, 75, -8, 8, 90, 10),
                        mob -> mob.entityTags().contains(tag)).stream().findFirst().orElseThrow().getHealth());
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            server.runCommand("fill -8 79 -8 8 79 8 minecraft:stone");
            server.runCommand("fill -8 80 -8 8 85 8 minecraft:air");
            server.runCommand("tp @p 0 80 0");
            server.runCommand("gamemode survival @p");
            server.runCommand("summon minecraft:husk 0 80 2 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"killaura_near\"]}");
            server.runCommand("summon minecraft:husk 0 80 5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"killaura_far\"]}");
            context.waitFor(mc -> mc.player != null && mc.player.getY() >= 79.9 && mc.level != null);
            context.waitTicks(20);
            check(health(server, "killaura_near") == 20, "Aura attacked while disabled");
            context.getInput().pressKey(InputConstants.KEY_R);
            server.waitFor(instance -> instance.getLevel(Level.OVERWORLD).getEntitiesOfClass(Husk.class, new AABB(-8, 75, -8, 8, 90, 10), mob -> mob.entityTags().contains("killaura_near")).stream().anyMatch(mob -> mob.getHealth() < 20), 100);
            check(health(server, "killaura_far") == 20, "Aura attacked beyond its range");
            context.getInput().pressKey(InputConstants.KEY_R);
            context.waitTicks(5);
            float afterDisable = health(server, "killaura_near");
            context.waitTicks(20);
            check(health(server, "killaura_near") == afterDisable, "Aura kept attacking after toggle off");
            server.runCommand("fill -1 80 1 1 83 1 minecraft:stone");
            context.waitTicks(5);
            context.getInput().pressKey(InputConstants.KEY_R);
            context.waitTicks(30);
            check(health(server, "killaura_near") == afterDisable, "Aura attacked through a wall with wallRange=0");
            context.getInput().pressKey(InputConstants.KEY_R);
            context.takeScreenshot("killaura-standaloneTodoAi");
            System.out.println("KillAura in-world checks passed: disabled default, key toggle, attack, range, disable, walls.");
        }
    }
}
