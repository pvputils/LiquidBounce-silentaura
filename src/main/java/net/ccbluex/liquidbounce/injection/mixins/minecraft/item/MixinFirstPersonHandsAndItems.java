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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.ccbluex.liquidbounce.utils.render.FirstPersonShieldTint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ShieldItem;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MixinFirstPersonHandsAndItems {

    @Shadow
    @Final
    private static float ITEM_POS_Y;

    @WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void hookFirstPersonShieldTint(
        ItemStackRenderState instance, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, int outlineColor, Operation<Void> original,
        @Local(argsOnly = true, name = "itemStack") ItemStack itemStack
    ) {
        if (itemStack.getItem() instanceof ShieldItem) {
            FirstPersonShieldTint.render(
                () -> original.call(instance, poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor));
            return;
        }

        original.call(instance, poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
    }

    @Unique
    private static void liquid_bounce$applyTransformations(PoseStack matrices, float translateX, float translateY, float translateZ, float rotateX, float rotateY, float rotateZ) {
        matrices.translate(translateX, translateY, translateZ);
        matrices.rotate(Axis.XP.rotationDegrees(rotateX));
        matrices.rotate(Axis.YP.rotationDegrees(rotateY));
        matrices.rotate(Axis.ZP.rotationDegrees(rotateZ));
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void hideShield(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (hand == InteractionHand.OFF_HAND && ModuleSwordBlock.INSTANCE.shouldHideOffhand(itemStack)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "submitArmWithItem", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;",
        ordinal = 0
    ))
    private ItemUseAnimation hookUseAction(
        ItemUseAnimation original,
        @Local(argsOnly = true, name = "itemStack") ItemStack itemStack,
        @Local(argsOnly = true, name = "playerState") PlayerRenderState playerState
    ) {
        var entity = liquid_bounce$getEntity(playerState);
        if (entity instanceof LivingEntity livingEntity && ModuleSwordBlock.shouldAnimateSwordBlock(livingEntity, itemStack)) {
            return ItemUseAnimation.BLOCK;
        }
        return original;
    }

    @ModifyExpressionValue(method = "submitArmWithItem", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;isUsingItem:Z",
        ordinal = 1,
        opcode = Opcodes.GETFIELD)
    )
    private boolean hookIsUseItem(boolean original, @Local(argsOnly = true, name = "playerState") PlayerRenderState playerState) {
        var entity = liquid_bounce$getEntity(playerState);
        if (entity instanceof LivingEntity livingEntity && ModuleSwordBlock.shouldAnimateSwordBlock(livingEntity)) {
            return true;
        }

        return original;
    }

    @ModifyExpressionValue(method = "submitArmWithItem", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;useItemHand:Lnet/minecraft/world/InteractionHand;",
        ordinal = 1,
        opcode = Opcodes.GETFIELD)
    )
    private InteractionHand hookActiveHand(InteractionHand original, @Local(argsOnly = true, name = "playerState") PlayerRenderState playerState) {
        var entity = liquid_bounce$getEntity(playerState);
        if (entity instanceof LivingEntity livingEntity && ModuleSwordBlock.shouldAnimateSwordBlock(livingEntity)) {
            return InteractionHand.MAIN_HAND;
        }

        return original;
    }

    @ModifyExpressionValue(method = "submitArmWithItem", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;useItemRemainingTicks:I",
        ordinal = 2,
        opcode = Opcodes.GETFIELD)
    )
    private int hookItemUseItem(int original, @Local(argsOnly = true, name = "playerState") PlayerRenderState playerState) {
        var entity = liquid_bounce$getEntity(playerState);
        if (entity instanceof LivingEntity livingEntity && ModuleSwordBlock.shouldAnimateSwordBlock(livingEntity)) {
            return 7200;
        }
        return original;
    }

    @Unique
    private static @Nullable Entity liquid_bounce$getEntity(PlayerRenderState playerState) {
        return playerState.avatarRenderState instanceof EntityRenderStateAddition entityRenderState ? entityRenderState.liquid_bounce$getEntity() : null;
    }

}
