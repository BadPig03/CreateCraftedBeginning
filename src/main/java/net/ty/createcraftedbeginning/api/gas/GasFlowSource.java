package net.ty.createcraftedbeginning.api.gas;

import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.BlockFace;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public abstract class GasFlowSource {
    private static final ICapabilityProvider<IGasHandler> EMPTY = null;

    protected BlockFace location;

    public GasFlowSource(BlockFace location) {
        this.location = location;
    }

    public GasStack provideGas(Predicate<GasStack> extractionPredicate) {
        ICapabilityProvider<IGasHandler> tankCache = provideHandler();
        if (tankCache == null) {
            return GasStack.EMPTY;
        }

        IGasHandler tank = tankCache.getCapability();
        if (tank == null) {
            return GasStack.EMPTY;
        }

        GasStack immediateFluid = tank.drain(1, GasAction.SIMULATE);
        if (extractionPredicate.test(immediateFluid)) {
            return immediateFluid;
        }

        for (int i = 0; i < tank.getTanks(); i++) {
            GasStack contained = tank.getGasInTank(i);
            if (contained.isEmpty() || !extractionPredicate.test(contained)) {
                continue;
            }

            return tank.drain(contained.copyWithAmount(1), GasAction.SIMULATE);
        }

        return GasStack.EMPTY;
    }

    public @Nullable ICapabilityProvider<IGasHandler> provideHandler() {
        return EMPTY;
    }

    public void keepAlive() {
    }

    public abstract boolean isEndpoint();

    public void manageSource(Level level, BlockEntity networkBE) {
    }

    public void whileFlowPresent(Level level, boolean pulling) {
    }

    public static class GasHandler extends GasFlowSource {
        @Nullable
        ICapabilityProvider<IGasHandler> gasHandlerCache;

        public GasHandler(BlockFace location) {
            super(location);
            gasHandlerCache = EMPTY;
        }

        @Override
        @Nullable
        public ICapabilityProvider<IGasHandler> provideHandler() {
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
                gasHandlerCache = ICapabilityProvider.of(BlockCapabilityCache.create(GasCapabilities.GasHandler.BLOCK, serverLevel, blockEntity.getBlockPos(), location.getOppositeFace(), () -> !networkBE.isRemoved(), () -> gasHandlerCache = EMPTY));
            }
            else if (level instanceof PonderLevel) {
                gasHandlerCache = ICapabilityProvider.of(() -> level.getCapability(GasCapabilities.GasHandler.BLOCK, blockEntity.getBlockPos(), location.getOppositeFace()));
            }
        }
    }

    public static class OtherPipe extends GasFlowSource {
        WeakReference<GasTransportBehaviour> cached;

        public OtherPipe(BlockFace location) {
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

            GasStack providedOutwardFluid = behaviour.getProvidedOutwardGas(location.getOppositeFace());
            return extractionPredicate.test(providedOutwardFluid) ? providedOutwardFluid : GasStack.EMPTY;
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
