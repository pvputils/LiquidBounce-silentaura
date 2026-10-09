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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.utils.client.SilentHotbar;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {

    /**
     * Hook attacking entity
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void hookAttack(Player player, Entity target, CallbackInfo callbackInfo) {
        var event = EventManager.INSTANCE.callEvent(new AttackEntityEvent(target));
        if (event.isCancelled()) callbackInfo.cancel();
    }

    // codex start
    // /**
    //  * Hook into updateBlockBreakingProgress method at HEAD and call BlockBreakingProgress event.
    //  */
    // @Inject(method = "continueDestroyBlock", at = @At(value = "HEAD"))
    // private void hookBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
    //     EventManager.INSTANCE.callEvent(new BlockBreakingProgressEvent(pos));
    // }
    // codex end

    // codex start
    // /**
    //  * Hook into cancel block breaking at HEAD and call cancel block breaking event, which is able to cancel the execution.
    //  */
    // @Inject(method = "stopDestroyBlock", at = @At("HEAD"), cancellable = true)
    // private void hookCancelBlockBreaking(CallbackInfo ci) {
    //     final var event = EventManager.INSTANCE.callEvent(new CancelBlockBreakingEvent());
    //     if (event.isCancelled()) ci.cancel();
    // }
    // codex end

    // codex start
    // @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    // private void hookAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
    //     final var event =  EventManager.INSTANCE.callEvent(new BlockAttackEvent(pos));
    //     if (event.isCancelled()) cir.setReturnValue(false);
    // }
    // codex end

    /**
     * @author superblaubeere27
     */
    @ModifyExpressionValue(method = "ensureHasSentCarriedItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedSlot()I"))
    private int hookCustomSelectedSlot(int original) {
        return SilentHotbar.INSTANCE.getServersideSlot();
    }

    // codex start
    // @Inject(method = "useItem", at = @At("RETURN"))
    // private void hookItemInteractAtReturn(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
    //     final PlayerInteractedItemEvent cancelEvent = new PlayerInteractedItemEvent(player, hand, cir.getReturnValue());
    //     EventManager.INSTANCE.callEvent(cancelEvent);
    // }
    // codex end

    // codex start
    // @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    // private void hookItemInteractAtHead(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
    //     final var event = EventManager.INSTANCE.callEvent(new PlayerInteractItemEvent(player, hand));
    //     if (event.isCancelled()) cir.setReturnValue(InteractionResult.PASS);
    // }
    // codex end

    // codex start
    // @Inject(method = "releaseUsingItem", at = @At("HEAD"))
    // private void stopUsingItem(Player player, CallbackInfo callbackInfo) {
    //     ModuleAutoBow.onStopUsingItem();
    // }
    //
    // codex end
    // codex start
    // @Inject(method = "setLocalMode(Lnet/minecraft/world/level/GameType;)V", at = @At("RETURN"))
    // private void setGameMode(GameType mode, CallbackInfo callbackInfo) {
    //     EventManager.INSTANCE.callEvent(new GameModeChangeEvent(mode));
    // }
    // codex end

    // codex start
    // @Inject(method = "setLocalMode(Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/GameType;)V", at = @At("RETURN"))
    // private void setGameModes(GameType mode, GameType previousMode, CallbackInfo callbackInfo) {
    //     EventManager.INSTANCE.callEvent(new GameModeChangeEvent(mode));
    // }
    // codex end

    // codex start
    // // codex start
    // // @Inject(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;destroy(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", shift = At.Shift.AFTER))
    // // private void hookBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    // //     ClientBlockBreakTrigger.INSTANCE.clientBreakHandler();
    // // }
    // //
    // // codex end
    // @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    // private void hookContainerInput(int containerId, int slotNum, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
    //     final var event = EventManager.INSTANCE.callEvent(new PlayerContainerInputEvent(containerId, slotNum, buttonNum, containerInput));
    //     if (event.isCancelled()) ci.cancel();
    // }
    // codex end

}
