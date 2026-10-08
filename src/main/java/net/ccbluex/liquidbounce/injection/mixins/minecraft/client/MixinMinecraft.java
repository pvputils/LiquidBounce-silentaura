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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Window;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRealAttackTodoAi;
import net.ccbluex.liquidbounce.event.CoroutineTicker;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.TickLoopTaskExecutor;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.misc.SelfDestruct;
import net.ccbluex.liquidbounce.render.ClientTesselator;
import net.ccbluex.liquidbounce.render.buffers.StaticGpuBufferPool;
import net.ccbluex.liquidbounce.render.mesh.MeshDraw;
import net.ccbluex.liquidbounce.render.utils.RenderingDebug;
import net.ccbluex.liquidbounce.utils.client.vfp.VfpCompatibility;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.User;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static net.ccbluex.liquidbounce.utils.client.ProtocolUtilKt.getUsesViaFabricPlus;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    // codex start
    @Shadow
    protected abstract void pick(float partialTick);

    @WrapMethod(method = "startAttack")
    private boolean codexVanillaKillAuraAttack(Operation<Boolean> original) {
        var rotation = ModuleKillAura.INSTANCE.rotationForAttack();
        if (rotation == null) {
            return original.call();
        }
        try {
            return KillAuraRealAttackTodoAi.INSTANCE.withVanillaAttack(rotation, () -> {
                // Refresh through Minecraft's own picker; never substitute the aura target.
                pick(1.0F);
                return original.call();
            });
        } finally {
            // Do not leave a Snap click's hit result behind for item use or block breaking.
            pick(1.0F);
        }
    }
    //codex end


    @Shadow
    @Nullable
    public LocalPlayer player;
    @Shadow
    @Nullable
    public HitResult hitResult;
    @Shadow
    @Final
    public Options options;
    @Shadow
    @Nullable
    private IntegratedServer singleplayerServer;
    @Shadow
    private int rightClickDelay;
    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Shadow
    @Nullable
    public abstract ClientPacketListener getConnection();

    @Shadow
    public abstract @Nullable ServerData getCurrentServer();

    @Shadow
    public abstract Window getWindow();

    @Shadow
    public abstract int getFps();

    @Shadow
    public abstract User getUser();

    @Shadow
    protected abstract void continueAttack(boolean breaking);

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    @Final
    public Gui gui;

    /**
     * Entry point
     */
    @Inject(method = "<init>(Lnet/minecraft/client/main/GameConfig;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;resizeGui()V"))
    private void startClient(CallbackInfo callback) {
        EventManager.INSTANCE.callEvent(ClientStartEvent.INSTANCE);
    }

    /**
     * Exit point
     */
    @Inject(method = "close", at = @At("HEAD"))
    private void stopClient(CallbackInfo callback) {
        MeshDraw.DefaultUploader.close();
        EventManager.INSTANCE.callEvent(ClientShutdownEvent.INSTANCE);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/main/GameConfig;)V", at = @At(value = "FIELD",
        target = "Lnet/minecraft/client/Minecraft;profileKeyPairManager:Lnet/minecraft/client/multiplayer/ProfileKeyPairManager;",
        ordinal = 0, shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD))
    private void onSessionInit(CallbackInfo callback) {
        EventManager.INSTANCE.callEvent(new SessionEvent(getUser()));
    }

    /**
     * Modify window title to our client title.
     * Example: LiquidBounce v1.0.0 | 1.16.3
     *
     * @param callback our window title
     *                 <p>
     *                 todo: modify constant Minecraft instead
     */
    @Inject(method = "createTitle", at = @At(
            value = "INVOKE",
            target = "Ljava/lang/StringBuilder;append(Ljava/lang/String;)Ljava/lang/StringBuilder;",
            ordinal = 1),
            cancellable = true)
    private void getClientTitle(CallbackInfoReturnable<String> callback) {
        if (SelfDestruct.INSTANCE.isDestructed()) {
            return;
        }

        LiquidBounce.INSTANCE.getLogger().debug("Modifying window title");

        StringBuilder titleBuilder = new StringBuilder(LiquidBounce.CLIENT_NAME);
        titleBuilder.append(" v");
        titleBuilder.append(LiquidBounce.INSTANCE.getClientVersion());
        titleBuilder.append(" ");

        if (LiquidBounce.IN_DEVELOPMENT) {
            titleBuilder.append("(dev) ");
        }

        titleBuilder.append(LiquidBounce.INSTANCE.getClientCommit());

        titleBuilder.append(" | ");

        // ViaFabricPlus compatibility
        if (getUsesViaFabricPlus()) {
            var protocolVersion = VfpCompatibility.INSTANCE.unsafeGetProtocolVersion();

            if (protocolVersion != null) {
                titleBuilder.append(protocolVersion.name());
            } else {
                titleBuilder.append(SharedConstants.getCurrentVersion().name());
            }
        } else {
            titleBuilder.append(SharedConstants.getCurrentVersion().name());
        }

        EventManager.INSTANCE.callEvent(new WindowTitleEvent(titleBuilder));

        ClientPacketListener clientPlayNetworkHandler = this.getConnection();
        if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isConnected()) {
            titleBuilder.append(" - ");
            ServerData serverInfo = this.getCurrentServer();
            if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
                titleBuilder.append(I18n.get("title.singleplayer"));
            } else if (serverInfo != null && serverInfo.isRealm()) {
                titleBuilder.append(I18n.get("title.multiplayer.realms"));
            } else if (this.singleplayerServer == null && (serverInfo == null || !serverInfo.isLan())) {
                titleBuilder.append(I18n.get("title.multiplayer.other"));
            } else {
                titleBuilder.append(I18n.get("title.multiplayer.lan"));
            }
        }

        callback.setReturnValue(titleBuilder.toString());
    }

    /**
     * Hook game tick event at HEAD
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void hookTickEvent(CallbackInfo callbackInfo) {
        CoroutineTicker.INSTANCE.beginMinecraftTick();
        TickLoopTaskExecutor.INSTANCE.onTickLoopStart();
        CoroutineTicker.INSTANCE.tick();
        EventManager.INSTANCE.callEvent(GameTickEvent.INSTANCE);
    }

    @Inject(method = "tick", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V",
        shift = At.Shift.AFTER
    ))
    private void hookTickLoopCompletedAfterTickEndPacket(CallbackInfo callbackInfo) {
        TickLoopTaskExecutor.INSTANCE.onTickLoopCompleted();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void fallbackCompleteTickLoop(CallbackInfo callbackInfo) {
        if (TickLoopTaskExecutor.INSTANCE.isInTickLoop()) {
            TickLoopTaskExecutor.INSTANCE.onTickLoopCompleted();
        }

        CoroutineTicker.INSTANCE.endMinecraftTick();
    }

    /**
     * Hook game render task queue event
     */
    @Inject(method = "runTick", at = @At("HEAD"))
    private void hookRenderTaskQueue(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(GameRenderTaskQueueEvent.INSTANCE);
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runAllTasks()V", shift = At.Shift.BEFORE))
    private void hookPacketProcess(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(TickPacketProcessEvent.INSTANCE);
    }

    /**
     * Hook input handling
     */
    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void hookHandleInputEvent(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(InputHandleEvent.INSTANCE);
    }

    /**
     * Hook item use cooldown
     */
    @Inject(method = "startUseItem", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD))
    private void hookItemUseCooldown(CallbackInfo callbackInfo) {
        UseCooldownEvent useCooldownEvent = new UseCooldownEvent(rightClickDelay);
        EventManager.INSTANCE.callEvent(useCooldownEvent);
        rightClickDelay = useCooldownEvent.getCooldown();
    }











    @Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
    private void hookWorldChangeEvent(ClientLevel world, boolean bl, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new WorldChangeEvent(world));
    }

    @Inject(method = "renderFrame", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;fps:I",
        ordinal = 0, shift = At.Shift.AFTER, opcode = Opcodes.PUTSTATIC))
    private void hookFpsChange(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new FpsChangeEvent(this.getFps()));
    }

    @Inject(method = "onResourceLoadFinished", at = @At("HEAD"))
    private void onFinishedLoading(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(ResourceReloadEvent.INSTANCE);
    }

    @Inject(method = "clearDownloadedResourcePacks", at = @At("HEAD"))
    private void handleDisconnection(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(DisconnectEvent.INSTANCE);
    }



    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;submit()V", shift = At.Shift.BEFORE))
    private void endDynamicGpuBufferFrame(boolean advanceGameTime, CallbackInfo ci) {
        MeshDraw.DefaultUploader.endFrame();
    }

    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;endFrame()V", shift = At.Shift.AFTER))
    private void onFlipFrame(boolean advanceGameTime, CallbackInfo ci) {
        RenderingDebug.flipFrame();
        ClientTesselator.Shared.clear();
        StaticGpuBufferPool.cleanup();
    }


}
