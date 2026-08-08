package net.ty.createcraftedbeginning.content.airtights.gas.flowsources;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
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

    /**
     * Creates a new {@code ExternalHandlerSource} instance.
     *
     * @param location the resource location identifying the target value
     */
    public ExternalHandlerSource(BlockFace location) {
        super(location);
        gasHandlerCache = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEndpoint() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void manageSource(Level level, BlockEntity networkBE) {
        if (gasHandlerCache != null) {
            return;
        }

        BlockPos targetPos = location.getConnectedPos();
        if (level instanceof ServerLevel serverLevel) {
            gasHandlerCache = ICapabilityProvider.of(invalidate -> BlockCapabilityCache.create(GasHandler.BLOCK, serverLevel, targetPos, location.getOppositeFace(), () -> !networkBE.isRemoved(), () -> {
                gasHandlerCache = null;
                invalidate.run();
            }));
        }
        else if (level instanceof PonderLevel) {
            gasHandlerCache = ICapabilityProvider.of(() -> level.getCapability(GasHandler.BLOCK, targetPos, location.getOppositeFace()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public ICapabilityProvider<IGasHandler> getGasHandlerProvider() {
        return gasHandlerCache;
    }
}
