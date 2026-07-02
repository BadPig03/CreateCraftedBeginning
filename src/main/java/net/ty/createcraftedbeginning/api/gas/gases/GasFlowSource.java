package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.BlockFace;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class GasFlowSource {
    protected BlockFace location;

    public GasFlowSource(BlockFace location) {
        this.location = location;
    }

    public GasStack provideGas(Predicate<GasStack> extractionPredicate) {
        ICapabilityProvider<IGasHandler> provider = getGasHandlerProvider();
        if (provider == null) {
            return GasStack.EMPTY;
        }

        IGasHandler handler = provider.getCapability();
        if (handler == null) {
            return GasStack.EMPTY;
        }

        GasStack immediate = handler.drain(1, GasAction.SIMULATE);
        if (extractionPredicate.test(immediate)) {
            return immediate;
        }

        for (int i = 0; i < handler.getTanks(); i++) {
            GasStack contained = handler.getGasInTank(i);
            if (contained.isEmpty() || !extractionPredicate.test(contained)) {
                continue;
            }

            return handler.drain(contained.copyWithAmount(1), GasAction.SIMULATE);
        }

        return GasStack.EMPTY;
    }

    public @Nullable ICapabilityProvider<IGasHandler> getGasHandlerProvider() {
        return null;
    }

    public abstract boolean isEndpoint();

    public void manageSource(Level level, BlockEntity networkBE) {
    }

    public static class ExternalHandlerSource extends GasFlowSource {
        @Nullable ICapabilityProvider<IGasHandler> gasHandlerCache;

        public ExternalHandlerSource(BlockFace location) {
            super(location);
            gasHandlerCache = null;
        }

        @Override
        @Nullable
        public ICapabilityProvider<IGasHandler> getGasHandlerProvider() {
            return gasHandlerCache;
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

            BlockEntity blockEntity = level.getBlockEntity(location.getConnectedPos());
            if (blockEntity == null) {
                return;
            }

            if (level instanceof ServerLevel serverLevel) {
                gasHandlerCache = ICapabilityProvider.of(invalidate -> BlockCapabilityCache.create(GasHandler.BLOCK, serverLevel, blockEntity.getBlockPos(), location.getOppositeFace(), () -> !networkBE.isRemoved(), () -> {
                    gasHandlerCache = null;
                    invalidate.run();
                }));
            }
            else if (level instanceof PonderLevel) {
                gasHandlerCache = ICapabilityProvider.of(() -> level.getCapability(GasHandler.BLOCK, blockEntity.getBlockPos(), location.getOppositeFace()));
            }
        }
    }

    public static class AdjacentPipeSource extends GasFlowSource {
        WeakReference<GasTransportBehaviour> cached;

        public AdjacentPipeSource(BlockFace location) {
            super(location);
        }

        @Override
        public GasStack provideGas(Predicate<GasStack> extractionPredicate) {
            if (cached == null || cached.get() == null) {
                return GasStack.EMPTY;
            }

            GasTransportBehaviour behaviour = cached.get();
            if (behaviour == null) {
                return GasStack.EMPTY;
            }

            GasStack providedOutward = behaviour.getProvidedOutwardGas(location.getOppositeFace());
            return extractionPredicate.test(providedOutward) ? providedOutward : GasStack.EMPTY;
        }

        @Override
        public boolean isEndpoint() {
            return false;
        }

        @Override
        public void manageSource(Level level, BlockEntity networkBE) {
            GasTransportBehaviour currentBehaviour;
            if (cached != null) {
                currentBehaviour = cached.get();
                if (currentBehaviour != null && !currentBehaviour.blockEntity.isRemoved()) {
                    return;
                }
            }

            cached = null;
            BlockEntity targetBlockEntity = level.getBlockEntity(location.getConnectedPos());
            currentBehaviour = targetBlockEntity != null ? BlockEntityBehaviour.get(targetBlockEntity, GasTransportBehaviour.TYPE) : BlockEntityBehaviour.get(level, location.getConnectedPos(), GasTransportBehaviour.TYPE);
            if (currentBehaviour == null) {
                return;
            }

            cached = new WeakReference<>(currentBehaviour);
        }
    }

    public static class Blocked extends GasFlowSource {
        public Blocked(BlockFace location) {
            super(location);
        }

        @Override
        public boolean isEndpoint() {
            return false;
        }
    }
}
