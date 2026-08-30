package net.ty.createcraftedbeginning.compat.functionalstorage;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.StorageControllerExtensionTile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.CombinedGasTankWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.compat.functionalstorage.access.GasControllerAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ControllerGasHandler implements IGasHandler {
    private List<IGasHandler> handlers = List.of();
    private IGasHandler delegate = new CombinedGasTankWrapper();

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerControllerCapability(event, FunctionalStorage.DRAWER_CONTROLLER.type().get());
        registerControllerCapability(event, FunctionalStorage.FRAMED_DRAWER_CONTROLLER.type().get());
        registerExtensionCapability(event, FunctionalStorage.CONTROLLER_EXTENSION.type().get());
        registerExtensionCapability(event, FunctionalStorage.FRAMED_CONTROLLER_EXTENSION.type().get());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerControllerCapability(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        event.registerBlockEntity(GasHandler.BLOCK, (BlockEntityType) type, (blockEntity, ignoredDirection) -> ((GasControllerAccess) blockEntity).ccb$getGasHandler());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerExtensionCapability(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        event.registerBlockEntity(GasHandler.BLOCK, (BlockEntityType) type, (blockEntity, side) -> ccb$getExtensionGasHandler((StorageControllerExtensionTile<?>) blockEntity, side));
    }

    private static @Nullable IGasHandler ccb$getExtensionGasHandler(StorageControllerExtensionTile<?> extension, @Nullable Direction capabilitySide) {
        BlockPos controllerPos = extension.getControllerPos();
        Level level = extension.getLevel();
        if (controllerPos == null || level == null || !level.isLoaded(controllerPos)) {
            return null;
        }
        return level.getCapability(GasHandler.BLOCK, controllerPos, capabilitySide);
    }

    private static long fillTank(IGasHandler handler, int tankIndex, GasStack gasStack, GasAction action) {
        long acceptedAmount = fillTankDirect(handler, tankIndex, gasStack, GasAction.SIMULATE);
        if (acceptedAmount <= 0 || !action.execute()) {
            return acceptedAmount;
        }
        return fillTankDirect(handler, tankIndex, gasStack, GasAction.EXECUTE);
    }

    private static long fillTankDirect(IGasHandler handler, int tankIndex, GasStack gasStack, GasAction action) {
        if (handler instanceof GasDrawerHandler drawer) {
            return drawer.getInternalTank(tankIndex).fill(gasStack, action);
        }
        return handler.fill(gasStack, action);
    }

    private static List<GasStack[]> snapshotContents(List<GasDrawerHandler> drawers) {
        List<GasStack[]> snapshots = new ArrayList<>(drawers.size());
        for (GasDrawerHandler drawer : drawers) {
            snapshots.add(drawer.snapshotContents());
        }
        return snapshots;
    }

    private static void restoreContents(List<GasDrawerHandler> drawers, List<GasStack[]> snapshots) {
        for (int drawerIndex = 0; drawerIndex < drawers.size(); drawerIndex++) {
            drawers.get(drawerIndex).restoreContents(snapshots.get(drawerIndex));
        }
    }

    private static void endTransactions(List<GasDrawerHandler> drawers, int begunTransactions, boolean commit) {
        for (int drawerIndex = 0; drawerIndex < begunTransactions; drawerIndex++) {
            drawers.get(drawerIndex).endTransaction(commit);
        }
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return delegate.isGasValid(tank, stack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        return delegate.drain(resource, action);
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        return delegate.drain(maxDrain, action);
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return delegate.getGasInTank(tank);
    }

    @Override
    public int getTanks() {
        return delegate.getTanks();
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        if (resource.isEmpty()) {
            return 0;
        }

        long acceptedAmount = fillExisting(resource, action);
        if (acceptedAmount > 0) {
            return acceptedAmount;
        }
        return fillEmpty(resource, action);
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        if (!GasDrawerHandler.hasResources(resources)) {
            return AtomicFillResult.SUCCESS;
        }

        List<GasDrawerHandler> drawers = new ArrayList<>(handlers.size());
        if (!collectDrawers(drawers)) {
            return AtomicFillResult.UNSUPPORTED;
        }
        if (drawers.isEmpty()) {
            return AtomicFillResult.REJECTED;
        }

        List<GasStack[]> snapshots = snapshotContents(drawers);
        boolean shouldCommit = false;
        int transactionCount = 0;
        try {
            for (GasDrawerHandler drawer : drawers) {
                drawer.beginTransaction();
                transactionCount++;
            }
            boolean filledAllResources = fillAll(resources);
            shouldCommit = filledAllResources && action.execute();
            if (!filledAllResources) {
                return AtomicFillResult.REJECTED;
            }
            return AtomicFillResult.SUCCESS;
        } finally {
            if (!shouldCommit) {
                restoreContents(drawers, snapshots);
            }
            endTransactions(drawers, transactionCount, shouldCommit);
        }
    }

    @Override
    public long getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }

    public void refresh(List<IGasHandler> handlers) {
        this.handlers = List.copyOf(handlers);
        delegate = new CombinedGasTankWrapper(this.handlers.toArray(IGasHandler[]::new));
    }

    private long fillExisting(GasStack resource, GasAction action) {
        for (IGasHandler handler : handlers) {
            for (int tankIndex = 0; tankIndex < handler.getTanks(); tankIndex++) {
                GasStack storedGas = handler.getGasInTank(tankIndex);
                if (storedGas.isEmpty() || !GasStack.isSameGasSameComponents(storedGas, resource)) {
                    continue;
                }

                long acceptedAmount = fillTank(handler, tankIndex, resource, action);
                if (acceptedAmount <= 0) {
                    continue;
                }

                return acceptedAmount;
            }
        }
        return 0;
    }

    private long fillEmpty(GasStack resource, GasAction action) {
        for (IGasHandler handler : handlers) {
            for (int tankIndex = 0; tankIndex < handler.getTanks(); tankIndex++) {
                if (!handler.getGasInTank(tankIndex).isEmpty() || !handler.isGasValid(tankIndex, resource)) {
                    continue;
                }

                long acceptedAmount = fillTank(handler, tankIndex, resource, action);
                if (acceptedAmount <= 0) {
                    continue;
                }

                return acceptedAmount;
            }
        }
        return 0;
    }

    private boolean collectDrawers(List<GasDrawerHandler> drawers) {
        for (IGasHandler handler : handlers) {
            if (!(handler instanceof GasDrawerHandler drawer)) {
                return false;
            }
            
            drawers.add(drawer);
        }
        return true;
    }

    private boolean fillAll(List<GasStack> resources) {
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty() || fill(resource, GasAction.EXECUTE) == resource.getAmount()) {
                continue;
            }

            return false;
        }
        return true;
    }
}
