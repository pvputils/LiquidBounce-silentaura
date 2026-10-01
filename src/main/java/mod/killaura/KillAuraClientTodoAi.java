/* Standalone adaptation of LiquidBounce KillAura. Copyright (c) 2015-2026 CCBlueX.
 * Licensed under GPL-3.0-or-later; see LICENSE. */
package mod.killaura;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class KillAuraClientTodoAi implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("killaura_standalone");
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("killauraTodoAi.properties");
    private KillAuraConfigTodoAi config = KillAuraConfigTodoAi.defaults();
    private KeyMapping toggleKey;
    private boolean enabled;
    private boolean ownsBlocking;
    private long nextAttack;
    private LivingEntity target;
    private ClientLevel lastLevel;

    @Override
    public void onInitializeClient() {
        reload(Minecraft.getInstance(), false);
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("killaura", "controls"));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.killaura.toggle",
                InputConstants.Type.KEYBOARD, InputConstants.KEY_R, category));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
                literal("killaura")
                    .executes(context -> { status(Minecraft.getInstance()); return 1; })
                    .then(literal("toggle").executes(context -> { toggle(Minecraft.getInstance()); return 1; }))
                    .then(literal("reload").executes(context -> {
                        return reload(Minecraft.getInstance(), true) ? 1 : 0;
                    }))));
        LOGGER.info("KillAura Standalone initialized. Press R in game or use /killaura toggle. Disabled by default.");
    }
    private boolean reload(Minecraft mc, boolean notify) {
        try {
            var loaded = KillAuraConfigTodoAi.load(configPath);
            stopBlocking(mc);
            config = loaded;
            target = null;
            nextAttack = 0;
            if (notify) message(mc, "KillAura config reloaded");
            return true;
        } catch (IOException | IllegalArgumentException error) {
            LOGGER.warn("Could not read KillAura configuration; keeping current settings", error);
            if (notify) message(mc, "Could not load KillAura config; see log");
            return false;
        }
    }
    private void toggle(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        enabled = !enabled;
        reset(mc);
        status(mc);
    }
    private void status(Minecraft mc) {
        message(mc, "KillAura " + (enabled ? "enabled" : "disabled") + " | range " + config.range());
    }
    private static void message(Minecraft mc, String text) {
        if (mc.player != null) mc.player.sendOverlayMessage(Component.literal(text));
    }
    private void reset(Minecraft mc) {
        target = null;
        nextAttack = 0;
        stopBlocking(mc);
    }
    private void tick(Minecraft mc) {
        if (mc.level != lastLevel) {
            enabled = false;
            reset(mc);
            lastLevel = mc.level;
        }
        while (toggleKey.consumeClick()) {
            if (mc.gui.screen() == null) toggle(mc);
        }
        if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null
                || mc.player.isSpectator() || !mc.player.isAlive() || mc.isPaused() || mc.gui.screen() != null) {
            reset(mc);
            return;
        }
        if (config.requireWeapon() && !mc.player.getMainHandItem().is(ItemTags.SWORDS)
                && !mc.player.getMainHandItem().is(ItemTags.AXES)) {
            reset(mc);
            return;
        }
        if (config.pauseWhileUsing() && mc.player.isUsingItem() && !ownsBlocking) {
            target = null;
            return;
        }
        double scan = config.range() + config.scanExtra();
        if (target == null || !valid(mc, target, scan)) {
            target = mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(scan),
                    entity -> valid(mc, entity, scan)).stream()
                    .min(Comparator.comparingDouble(entity -> distanceSquared(mc, entity))).orElse(null);
        }
        if (target == null) { stopBlocking(mc); return; }
        Vec3 eyes = mc.player.getEyePosition();
        AABB box = target.getBoundingBox();
        Vec3 point = new Vec3(CombatMathTodoAi.clamp(eyes.x, box.minX + 0.05, box.maxX - 0.05),
                CombatMathTodoAi.clamp(target.getEyeY(), box.minY + 0.05, box.maxY - 0.05),
                CombatMathTodoAi.clamp(eyes.z, box.minZ + 0.05, box.maxZ - 0.05));
        Vec3 offset = point.subtract(eyes);
        float yaw = CombatMathTodoAi.turn(mc.player.getYRot(), CombatMathTodoAi.yaw(offset.x, offset.z), config.turnSpeed());
        float pitch = CombatMathTodoAi.turn(mc.player.getXRot(), CombatMathTodoAi.pitch(offset.x, offset.y, offset.z), config.turnSpeed());
        mc.player.setYRot(yaw);
        mc.player.setXRot((float) CombatMathTodoAi.clamp(pitch, -90, 90));
        double range = attackRange(mc, target);
        if (range <= 0 || distanceSquared(mc, target) > range * range) {
            stopBlocking(mc);
            return;
        }
        long now = System.nanoTime();
        boolean ready = now >= nextAttack && (!config.cooldown() || mc.player.getAttackStrengthScale(0) >= 0.9f);
        boolean criticalReady = !config.criticalOnly() || !mc.player.onGround() && mc.player.getDeltaMovement().y < 0
                && !mc.player.isInWater() && !mc.player.onClimbable();
        if (ready && criticalReady && lookingAt(mc, target, range)) {
            stopBlocking(mc);
            // END_CLIENT_TICK runs after movement: send the new aim before the attack packet.
            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), mc.player.getXRot(),
                    mc.player.onGround(), mc.player.horizontalCollision));
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
            mc.player.connection.send(net.minecraft.network.protocol.game.ServerboundPunchPacket.INSTANCE);
            double cps = config.minCps() == config.maxCps() ? config.minCps()
                    : ThreadLocalRandom.current().nextDouble(config.minCps(), config.maxCps());
            nextAttack = now + CombatMathTodoAi.intervalNanos(cps);
        }
        if (config.autoBlock() && mc.player.getOffhandItem().is(Items.SHIELD) && !mc.player.isUsingItem()) {
            mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
            ownsBlocking = mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.OFF_HAND;
        }
    }
    private boolean valid(Minecraft mc, LivingEntity entity, double scan) {
        if (entity == mc.player || !entity.isAlive() || entity.isSpectator() || !entity.isPickable()
                || mc.player.isAlliedTo(entity) || !mc.player.canAttack(entity)
                || !config.invisible() && entity.isInvisibleTo(mc.player)) return false;
        boolean allowed = entity instanceof Player ? config.players()
                : entity instanceof Enemy ? config.hostileMobs() : entity instanceof Animal && config.animals();
        if (!allowed || distanceSquared(mc, entity) > scan * scan) return false;
        if (!config.ignoreShield() && entity.isBlocking()) return false;
        return mc.player.hasLineOfSight(entity) || config.wallRange() > 0
                && distanceSquared(mc, entity) <= config.wallRange() * config.wallRange();
    }
    private double attackRange(Minecraft mc, LivingEntity entity) {
        return mc.player.hasLineOfSight(entity) ? config.range() : config.wallRange();
    }
    private static double distanceSquared(Minecraft mc, LivingEntity entity) {
        Vec3 eye = mc.player.getEyePosition(); AABB box = entity.getBoundingBox();
        return CombatMathTodoAi.distanceSquaredToBox(eye.x, eye.y, eye.z,
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
    private boolean lookingAt(Minecraft mc, LivingEntity entity, double range) {
        Vec3 start = mc.player.getEyePosition();
        Vec3 end = start.add(mc.player.getViewVector(1).scale(range));
        var hit = entity.getBoundingBox().clip(start, end);
        if (hit.isEmpty()) return false;
        double hitDistance = start.distanceToSqr(hit.get());
        var blockHit = mc.level.clip(new ClipContext(start, hit.get(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mc.player));
        if (blockHit.getType() != HitResult.Type.MISS && hitDistance > config.wallRange() * config.wallRange()) return false;
        // Do not attack a selected entity through another pickable entity.
        for (Entity other : mc.level.getEntities(mc.player,
                mc.player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1), Entity::isPickable)) {
            if (other == entity) continue;
            var intercept = other.getBoundingBox().clip(start, end);
            if (intercept.isPresent() && start.distanceToSqr(intercept.get()) < hitDistance) return false;
        }
        return true;
    }
    private void stopBlocking(Minecraft mc) {
        if (ownsBlocking && mc.player != null && mc.gameMode != null && mc.player.isUsingItem()
                && mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) mc.gameMode.releaseUsingItem(mc.player);
        ownsBlocking = false;
    }
}
