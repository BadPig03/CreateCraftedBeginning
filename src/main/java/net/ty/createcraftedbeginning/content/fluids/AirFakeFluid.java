package net.ty.createcraftedbeginning.content.fluids;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class AirFakeFluid extends BaseFlowingFluid {
    private final boolean source;

    public AirFakeFluid(Properties properties, boolean source) {
        super(properties);
        this.source = source;
    }

    @Contract("_ -> new")
    public static @NotNull AirFakeFluid createSource(Properties properties) {
        return new AirFakeFluid(properties, true);
    }

    @Contract("_ -> new")
    public static @NotNull AirFakeFluid createFlowing(Properties properties) {
        return new AirFakeFluid(properties, false);
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
        return Items.AIR;
    }

    @Override
    protected @NotNull BlockState createLegacyBlock(@NotNull FluidState state) {
        return Blocks.AIR.defaultBlockState();
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
