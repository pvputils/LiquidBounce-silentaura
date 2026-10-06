/* Derived from CCBlueX LiquidBounce; GPL-3.0-or-later. */
package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks

object BlockPreferencesTodoAi {
    val inventoryComparator = ComparatorChain(
        PreferFavourableBlocks, PreferSolidBlocks, PreferFullCubeBlocks, PreferWalkableBlocks,
        PreferAverageHardBlocks(neutralRange = true), PreferStackSize.PREFER_FEWER,
        PreferAverageHardBlocks(neutralRange = false),
    )
    private val unfavorable = setOf(Blocks.CRAFTING_TABLE, Blocks.JIGSAW, Blocks.SMITHING_TABLE,
        Blocks.FLETCHING_TABLE, Blocks.ENCHANTING_TABLE, Blocks.CAULDRON, Blocks.MAGMA_BLOCK)

    fun isUnfavourable(stack: ItemStack): Boolean {
        val block = stack.getBlock() ?: return true
        return block.friction > 0.6F || block.speedFactor < 1.0F || block.jumpFactor < 1.0F ||
            block is BaseEntityBlock ||
            !block.defaultBlockState().isCollisionShapeFullBlock(mc.level!!, BlockPos.ZERO) || block in unfavorable
    }
}
