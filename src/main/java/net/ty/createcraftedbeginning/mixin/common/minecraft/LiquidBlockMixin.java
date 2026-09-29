package net.ty.createcraftedbeginning.mixin.common.minecraft;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.LiquidReplacementUpgrade;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin extends Block {
    protected LiquidBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float progress = super.getDestroyProgress(state, player, level, pos);
        if (!LiquidReplacementUpgrade.INSTANCE.canApply(player.getMainHandItem())) {
            return progress;
        }

        return progress * LiquidReplacementUpgrade.HARDNESS_DIVISOR;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!LiquidReplacementUpgrade.INSTANCE.canApply(player.getMainHandItem())) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }

        int updateFlags = UPDATE_ALL;
        if (level.isClientSide) {
            updateFlags |= UPDATE_IMMEDIATE;
        }

        return level.setBlock(pos, Blocks.AIR.defaultBlockState(), updateFlags);
    }
}
