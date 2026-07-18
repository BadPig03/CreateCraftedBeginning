package net.ty.createcraftedbeginning.content.fluids.amethystsuspension;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AmethystSuspensionBucketItem extends BucketItem {
    public AmethystSuspensionBucketItem(BaseFlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult result, @Nullable ItemStack container) {
        boolean emptied = super.emptyContents(player, level, pos, result, container);
        if (!emptied) {
            return false;
        }

        boolean isUltrawarm = level.dimensionType().ultraWarm();
        for (Direction direction : Iterate.horizontalDirections) {
            BlockPos adjacentPos = pos.relative(direction);
            if (!level.getBlockState(adjacentPos).isAir()) {
                continue;
            }

            if (!isUltrawarm) {
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(adjacentPos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
                }
                continue;
            }

            if (level.isClientSide) {
                Fluids.WATER.getFluidType().onVaporize(player, level, adjacentPos, new FluidStack(Fluids.WATER, 1000));
            }
            else {
                CCBAdvancements.DRYING_OUT.awardTo(player);
            }
        }
        return true;
    }
}
