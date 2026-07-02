package net.ty.createcraftedbeginning.content.fluids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SlushVirtualFluid extends BaseFlowingFluid {
    private final boolean source;

    public SlushVirtualFluid(Properties properties, boolean source) {
        super(properties);
        this.source = source;
    }

    @Contract("_ -> new")
    public static SlushVirtualFluid createSource(Properties properties) {
        return new SlushVirtualFluid(properties, true);
    }

    @Contract("_ -> new")
    public static SlushVirtualFluid createFlowing(Properties properties) {
        return new SlushVirtualFluid(properties, false);
    }

    @Override
    public Fluid getFlowing() {
        return source ? super.getFlowing() : this;
    }

    @Override
    public Fluid getSource() {
        return source ? this : super.getSource();
    }

    @Override
    public Item getBucket() {
        return Items.POWDER_SNOW_BUCKET;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.POWDER_SNOW.defaultBlockState();
    }

    @Override
    public boolean isSource(FluidState fluidState) {
        return source;
    }

    @Override
    public int getAmount(FluidState fluidState) {
        return 0;
    }

}