package net.ty.createcraftedbeginning.content.fluids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AmethystSuspensionVirtualFluid extends BaseFlowingFluid {
    private final boolean source;

    public AmethystSuspensionVirtualFluid(Properties properties, boolean source) {
        super(properties);
        this.source = source;
    }

    @Contract("_ -> new")
    public static AmethystSuspensionVirtualFluid createSource(Properties properties) {
        return new AmethystSuspensionVirtualFluid(properties, true);
    }

    @Contract("_ -> new")
    public static AmethystSuspensionVirtualFluid createFlowing(Properties properties) {
        return new AmethystSuspensionVirtualFluid(properties, false);
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
    protected BlockState createLegacyBlock(FluidState state) {
        return CCBBlocks.POWDERED_AMETHYST_BLOCK.getDefaultState();
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
