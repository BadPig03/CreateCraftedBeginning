package net.ty.createcraftedbeginning.content.fluids;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class AmethystSuspensionVirtualFluid extends BaseFlowingFluid {
    private final boolean source;

    public AmethystSuspensionVirtualFluid(Properties properties, boolean source) {
        super(properties);
        this.source = source;
    }

    @Contract("_ -> new")
    public static @NotNull AmethystSuspensionVirtualFluid createSource(Properties properties) {
        return new AmethystSuspensionVirtualFluid(properties, true);
    }

    @Contract("_ -> new")
    public static @NotNull AmethystSuspensionVirtualFluid createFlowing(Properties properties) {
        return new AmethystSuspensionVirtualFluid(properties, false);
    }

    @Override
    public @NotNull Fluid getFlowing() {
        return source ? super.getFlowing() : this;
    }

    @Override
    public @NotNull Fluid getSource() {
        return source ? this : super.getSource();
    }

    @Override
    protected @NotNull BlockState createLegacyBlock(@NotNull FluidState state) {
        return CCBBlocks.POWDERED_AMETHYST_BLOCK.getDefaultState();
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
