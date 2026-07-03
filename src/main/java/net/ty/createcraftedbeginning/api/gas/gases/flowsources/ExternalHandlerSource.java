package net.ty.createcraftedbeginning.api.gas.gases.flowsources;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ExternalHandlerSource extends GasFlowSource {
    @Nullable ICapabilityProvider<IGasHandler> gasHandlerCache;

    public ExternalHandlerSource(BlockFace location) {
        super(location);
        gasHandlerCache = null;
    }

    @Override
    public boolean isEndpoint() {
        return true;
    }

    @Override
    public void manageSource(Level level, BlockEntity networkBE) {
        if (gasHandlerCache != null) {
            return;
        }

        BlockEntity targetBE = level.getBlockEntity(location.getConnectedPos());
        if (targetBE == null) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            gasHandlerCache = ICapabilityProvider.of(invalidate -> BlockCapabilityCache.create(GasHandler.BLOCK, serverLevel, targetBE.getBlockPos(), location.getOppositeFace(), () -> !networkBE.isRemoved(), () -> {
                gasHandlerCache = null;
                invalidate.run();
            }));
        }
        else if (level instanceof PonderLevel) {
            gasHandlerCache = ICapabilityProvider.of(() -> level.getCapability(GasHandler.BLOCK, targetBE.getBlockPos(), location.getOppositeFace()));
        }
    }

    @Override
    @Nullable
    public ICapabilityProvider<IGasHandler> getGasHandlerProvider() {
        return gasHandlerCache;
    }
}
