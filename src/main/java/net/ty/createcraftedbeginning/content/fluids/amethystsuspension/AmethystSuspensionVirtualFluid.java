package net.ty.createcraftedbeginning.content.fluids.amethystsuspension;

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
    private final boolean isSource;

    private AmethystSuspensionVirtualFluid(Properties properties, boolean isSource) {
        super(properties);
        this.isSource = isSource;
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
        if (!isSource) {
            return this;
        }
        return super.getFlowing();
    }

    @Override
    public Fluid getSource() {
        if (!isSource) {
            return super.getSource();
        }
        return this;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return CCBBlocks.POWDERED_AMETHYST_BLOCK.getDefaultState();
    }

    @Override
    public boolean isSource(FluidState fluidState) {
        return isSource;
    }

    @Override
    public int getAmount(FluidState fluidState) {
        return 0;
    }
}
