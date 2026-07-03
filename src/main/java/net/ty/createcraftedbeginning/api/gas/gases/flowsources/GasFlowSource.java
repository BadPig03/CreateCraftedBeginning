package net.ty.createcraftedbeginning.api.gas.gases.flowsources;

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

    public GasFlowSource(BlockFace location) {
        this.location = location;
    }

    public abstract boolean isEndpoint();

    public abstract void manageSource(Level level, BlockEntity networkBE);

    public GasStack provideGas(Predicate<GasStack> predicate) {
        ICapabilityProvider<IGasHandler> provider = getGasHandlerProvider();
        if (provider == null) {
            return GasStack.EMPTY;
        }

        IGasHandler handler = provider.getCapability();
        if (handler == null) {
            return GasStack.EMPTY;
        }

        GasStack simulateGas = handler.drain(1, GasAction.SIMULATE);
        if (predicate.test(simulateGas)) {
            return simulateGas;
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

    @Nullable
    public ICapabilityProvider<IGasHandler> getGasHandlerProvider() {
        return null;
    }
}
