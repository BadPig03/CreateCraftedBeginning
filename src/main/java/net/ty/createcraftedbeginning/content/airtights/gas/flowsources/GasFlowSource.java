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
    BlockFace location;

    GasFlowSource(BlockFace location) {
        this.location = location;
    }

    public abstract boolean isEndpoint();

    public abstract void manageSource(Level level, BlockEntity networkBE);

    public GasStack provideGas(Predicate<GasStack> gasPredicate) {
        ICapabilityProvider<IGasHandler> sourceProvider = getGasHandlerProvider();
        if (sourceProvider == null) {
            return GasStack.EMPTY;
        }

        IGasHandler sourceHandler = sourceProvider.getCapability();
        if (sourceHandler == null) {
            return GasStack.EMPTY;
        }

        GasStack simulatedGas = sourceHandler.drain(1, GasAction.SIMULATE);
        if (gasPredicate.test(simulatedGas)) {
            return simulatedGas;
        }

        for (int tankIndex = 0; tankIndex < sourceHandler.getTanks(); tankIndex++) {
            GasStack tankGas = sourceHandler.getGasInTank(tankIndex);
            if (tankGas.isEmpty() || !gasPredicate.test(tankGas)) {
                continue;
            }

            return sourceHandler.drain(tankGas.copyWithAmount(1), GasAction.SIMULATE);
        }
        return GasStack.EMPTY;
    }

    @Nullable
    public ICapabilityProvider<IGasHandler> getGasHandlerProvider() {
        return null;
    }
}
