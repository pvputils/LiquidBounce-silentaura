/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.EventState;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.interfaces.LocalPlayerAddition;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRealAttackTodoAi;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends MixinPlayer implements LocalPlayerAddition {

    @Shadow
    public ClientInput input;

    @Shadow
    @Final
    public ClientPacketListener connection;

    @Shadow
    public abstract boolean isUnderWater();



    @Unique
    private PlayerNetworkMovementTickEvent eventMotion;

    @Unique
    private int onGroundTicks = 0;
    @Unique
    private int airTicks = 0;



    /**
     * Hook entity tick event
     */
    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
            shift = At.Shift.BEFORE,
            ordinal = 0),
            cancellable = true)
    private void hookTickEvent(CallbackInfo ci) {
        var tickEvent = new PlayerTickEvent();
        EventManager.INSTANCE.callEvent(tickEvent);

        if (tickEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
            shift = At.Shift.AFTER,
            ordinal = 0))
    private void hookPostTickEvent(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(PlayerPostTickEvent.INSTANCE);


    }

    /**
     * Hook entity movement tick event
     */
    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void hookMovementTickEvent(CallbackInfo callbackInfo) {
        var movementTickEvent = new PlayerMovementTickEvent();
        EventManager.INSTANCE.callEvent(movementTickEvent);

        if (movementTickEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    /**
     * Hook entity movement tick event at HEAD and call out PRE tick movement event
     */
    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void hookMovementPre(CallbackInfo callbackInfo) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        eventMotion = new PlayerNetworkMovementTickEvent(EventState.PRE, player.getX(), player.getY(), player.getZ(), player.onGround());
        EventManager.INSTANCE.callEvent(eventMotion);

        if (eventMotion.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double modifyXPosition(double original) {
        return eventMotion.getX();
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double modifyYPosition(double original) {
        return eventMotion.getY();
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double modifyZPosition(double original) {
        return eventMotion.getZ();
    }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean modifyOnGround(boolean original) {
        return eventMotion.getGround();
    }

    /**
     * Hook entity movement tick event at RETURN and call out POST tick movement event
     */
    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void hookMovementPost(CallbackInfo callbackInfo) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        EventManager.INSTANCE.callEvent(new PlayerNetworkMovementTickEvent(EventState.POST, player.getX(), player.getY(), player.getZ(), player.onGround()));
    }



    /**
     * Hook move function to modify movement
     */
    @ModifyVariable(method = "move", at = @At("HEAD"), name = "delta", argsOnly = true)
    private Vec3 hookMove(Vec3 movement, MoverType type) {
        return EventManager.INSTANCE.callEvent(new PlayerMoveEvent(type, movement)).getMovement();
    }

    /**
     * Hook counter for on ground and air ticks
     */
    @Inject(method = "move", at = @At("RETURN"))
    private void hookGroundAirTimeCounters(CallbackInfo ci) {
        if (this.onGround()) {
            onGroundTicks++;
            airTicks = 0;
        } else {
            airTicks++;
            onGroundTicks = 0;
        }
    }

    @Override
    public int liquid_bounce$getOnGroundTicks() {
        return onGroundTicks;
    }

    @Override
    public int liquid_bounce$getAirTicks() {
        return airTicks;
    }





    // codex start
    @WrapMethod(method = "raycastHitResult")
    private HitResult codexVanillaPickRotation(float partialTick, Entity camera, Operation<HitResult> original) {
        var rotation = KillAuraRealAttackTodoAi.INSTANCE.getAttackRotation();
        if (rotation == null) {
            rotation = RotationManager.INSTANCE.getCurrentRotation();
        }
        if (camera != Minecraft.getInstance().player || rotation == null) {
            return original.call(partialTick, camera);
        }

        // All vanilla raycast branches (weapon, blocks, entities) see the same rotation.
        float yaw = camera.getYRot();
        float pitch = camera.getXRot();
        float oldYaw = camera.yRotO;
        float oldPitch = camera.xRotO;
        try {
            camera.setYRot(rotation.yRot());
            camera.setXRot(rotation.xRot());
            camera.yRotO = rotation.yRot();
            camera.xRotO = rotation.xRot();
            return original.call(partialTick, camera);
        } finally {
            camera.setYRot(yaw);
            camera.setXRot(pitch);
            camera.yRotO = oldYaw;
            camera.xRotO = oldPitch;
        }
    }
    //codex end

    /**
     * Hook custom sneaking multiplier
     */
    @ModifyExpressionValue(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double hookCustomSneakingMultiplier(double original) {
        var playerSneakMultiplier = new PlayerSneakMultiplier(original);
        EventManager.INSTANCE.callEvent(playerSneakMultiplier);
        return playerSneakMultiplier.getMultiplier();
    }

    /**
     * Hook custom multiplier
     *
     * <pre>
     * if (this.isUsingItem() && !this.hasVehicle()) {
     *     vec2f = vec2f.multiply(this.getActiveItemSpeedMultiplier());
     * }
     * </pre>
     */
    @WrapOperation(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec2;scale(F)Lnet/minecraft/world/phys/Vec2;", ordinal = 1))
    private Vec2 hookCustomMultiplier(Vec2 instance, float value, Operation<Vec2> original) {
        var playerUseMultiplier = new PlayerUseMultiplier(value, value);
        EventManager.INSTANCE.callEvent(playerUseMultiplier);
        return new Vec2(
            instance.x * playerUseMultiplier.getSideways(),
            instance.y * playerUseMultiplier.getForward()
        );
    }



    // Silent rotations (Rotation Manager)

    @ModifyExpressionValue(method = {"sendPosition",
        "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float hookSilentRotationYaw(float original) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            return original;
        }

        return rotation.yRot();
    }

    @ModifyExpressionValue(method = {"sendPosition",
        "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float hookSilentRotationPitch(float original) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            return original;
        }

        return rotation.xRot();
    }

    @ModifyReturnValue(method = "isAutoJumpEnabled", at = @At("RETURN"))
    private boolean injectAutoJumpAllowed(boolean original) {
        return EventManager.INSTANCE.callEvent(new AllowAutoJumpEvent(original)).isAllowed();
    }







    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"))
    private boolean hookSprint0(boolean original) {
        var event = new SprintEvent(new DirectionalInput(input), original, SprintEvent.Source.MOVEMENT_TICK);
        EventManager.INSTANCE.callEvent(event);
        return event.getSprint();
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
    private boolean hookSprint1(boolean original) {
        var event = new SprintEvent(new DirectionalInput(input), original, SprintEvent.Source.MOVEMENT_TICK);
        EventManager.INSTANCE.callEvent(event);
        return event.getSprint();
    }



    @ModifyReturnValue(method = "shouldStopRunSprinting", at = @At("RETURN"))
    private boolean hookForceStopSprinting(boolean shouldStop) {
        return shouldStop || liquid_bounce$shouldForceStopSprinting();
    }

    /**
     * ViaFabricPlus injects at HEAD of shouldStopRunSprinting with cancellable=true,
     * bypassing the RETURN instruction so @ModifyReturnValue never fires.
     * Intercepting the call site within aiStep works around this.
     * @see <a href="https://github.com/ViaVersion/ViaFabricPlus/blob/618332d/src/main/java/com/viaversion/viafabricplus/injection/mixin/features/movement/sprinting_and_sneaking/MixinLocalPlayer.java#L262-L270">ViaFabricPlus changeStopSprintingConditions</a>
     */
    @ModifyExpressionValue(
        method = "aiStep",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;shouldStopRunSprinting()Z")
    )
    private boolean hookVfpSprintStop(boolean shouldStop) {
        return shouldStop || liquid_bounce$shouldForceStopSprinting();
    }

    @Unique
    private boolean liquid_bounce$shouldForceStopSprinting() {
        var event = new SprintEvent(
            new DirectionalInput(input),
            true,
            SprintEvent.Source.MOVEMENT_TICK
        );

        EventManager.INSTANCE.callEvent(event);
        return !event.getSprint();
    }



    @ModifyExpressionValue(method = "sendIsSprintingIfNeeded", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z")
    )
    private boolean hookNetworkSprint(boolean original) {
        var event = new SprintEvent(new DirectionalInput(input), original, SprintEvent.Source.NETWORK);
        EventManager.INSTANCE.callEvent(event);
        return event.getSprint();
    }



}
