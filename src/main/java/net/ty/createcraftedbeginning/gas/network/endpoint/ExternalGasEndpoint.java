package net.ty.createcraftedbeginning.gas.network.endpoint;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ExternalGasEndpoint extends GasConnectionEndpoint {
    @Nullable
    private ICapabilityProvider<GasHandler> gasHandlerCache;

    public ExternalGasEndpoint(BlockFace location) {
        super(location);
        gasHandlerCache = null;
    }

    @Override
    public void bind(Level level, BlockEntity networkBlockEntity) {
        if (gasHandlerCache != null) {
            return;
        }

        BlockPos targetPos = location.getConnectedPos();
        if (level instanceof PonderLevel) {
            gasHandlerCache = ICapabilityProvider.of(() -> level.getCapability(GasCapabilities.BLOCK, targetPos, location.getOppositeFace()));
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        gasHandlerCache = ICapabilityProvider.of(invalidate -> BlockCapabilityCache.create(GasCapabilities.BLOCK, serverLevel, targetPos, location.getOppositeFace(), () -> !networkBlockEntity.isRemoved(), () -> {
            gasHandlerCache = null;
            invalidate.run();
        }));
    }

    @Override
    @Nullable
    public ICapabilityProvider<GasHandler> getGasHandlerProvider() {
        return gasHandlerCache;
    }
}
