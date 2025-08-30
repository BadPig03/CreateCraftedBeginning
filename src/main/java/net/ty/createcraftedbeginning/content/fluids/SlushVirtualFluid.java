package net.ty.createcraftedbeginning.content.fluids;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import org.jetbrains.annotations.NotNull;

public class SlushVirtualFluid extends BaseFlowingFluid {
    private final boolean source;

    public SlushVirtualFluid(Properties properties, boolean source) {
        super(properties);
        this.source = source;
    }

    public static SlushVirtualFluid createSource(Properties properties) {
        return new SlushVirtualFluid(properties, true);
    }

    public static SlushVirtualFluid createFlowing(Properties properties) {
        return new SlushVirtualFluid(properties, false);
    }

    @Override
    public @NotNull Fluid getSource() {
        if (source) {
            return this;
        }
        return super.getSource();
    }

    @Override
    public @NotNull Fluid getFlowing() {
        if (source) {
            return super.getFlowing();
        }
        return this;
    }

    @Override
    public @NotNull Item getBucket() {
        return Items.POWDER_SNOW_BUCKET;
    }

    @Override
    protected @NotNull BlockState createLegacyBlock(@NotNull FluidState state) {
        return Blocks.POWDER_SNOW.defaultBlockState();
    }

    @Override
    public boolean isSource(@NotNull FluidState fluidState) {
        return source;
    }

    @Override
    public int getAmount(@NotNull FluidState fluidState) {
        return 0;
    }

}