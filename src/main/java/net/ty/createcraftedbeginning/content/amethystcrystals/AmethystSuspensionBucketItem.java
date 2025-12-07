package net.ty.createcraftedbeginning.content.amethystcrystals;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class AmethystSuspensionBucketItem extends BucketItem {
    public AmethystSuspensionBucketItem(BaseFlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public boolean emptyContents(@Nullable Player player, @NotNull Level level, @NotNull BlockPos pos, @Nullable BlockHitResult result, @Nullable ItemStack container) {
        boolean emptied = super.emptyContents(player, level, pos, result, container);
        if (!emptied) {
            return false;
        }

        boolean isUltrawarm = level.dimensionType().ultraWarm();
        for (Direction direction : Iterate.horizontalDirections) {
            BlockPos relative = pos.relative(direction);
            if (!level.getBlockState(relative).isAir()) {
                continue;
            }

            if (isUltrawarm) {
                if (level.isClientSide) {
                    Fluids.WATER.getFluidType().onVaporize(player, level, relative, new FluidStack(Fluids.WATER, 1));
                }
                else {
                    CCBAdvancements.NATURAL_EVAPORATION.awardTo(player);
                }
                continue;
            }

            if (level.isClientSide) {
                continue;
            }

            level.setBlockAndUpdate(relative, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        }
        return true;
    }
}
