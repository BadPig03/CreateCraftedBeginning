package net.ty.createcraftedbeginning.content.airtights.gas.flowsources;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class GasFlowSource {
    protected BlockFace location;

    /**
     * Creates a new {@code GasFlowSource} instance.
     *
     * @param location the resource location identifying the target value
     */
    public GasFlowSource(BlockFace location) {
        this.location = location;
    }

    /**
     * Checks whether this value is endpoint.
     *
     * @return {@code true} if this value is endpoint; otherwise {@code false}
     */
    public abstract boolean isEndpoint();

    /**
     * Updates and manages the source.
     *
     * @param level     the level in which the operation is performed
     * @param networkBE the block entity participating in the gas network
     */
    public abstract void manageSource(Level level, BlockEntity networkBE);

    /**
     * Provides the gas.
     *
     * @param predicate the predicate used to select matching values
     * @return the resulting gas stack
     */
    public GasStack provideGas(Predicate<GasStack> predicate) {
        ICapabilityProvider<IGasHandler> provider = getGasHandlerProvider();
        if (provider == null) {
            return GasStack.EMPTY;
        }

        IGasHandler handler = provider.getCapability();
        if (handler == null) {
            return GasStack.EMPTY;
        }

        GasStack simulated = handler.drain(1, GasAction.SIMULATE);
        if (predicate.test(simulated)) {
            return simulated;
        }

        for (int i = 0; i < handler.getTanks(); i++) {
            GasStack contained = handler.getGasInTank(i);
            if (contained.isEmpty() || !predicate.test(contained)) {
                continue;
            }

            return handler.drain(contained.copyWithAmount(1), GasAction.SIMULATE);
        }
        return GasStack.EMPTY;
    }

    /**
     * Returns the gas handler provider.
     *
     * @return the gas handler provider
     */
    @Nullable
    public ICapabilityProvider<IGasHandler> getGasHandlerProvider() {
        return null;
    }
}
