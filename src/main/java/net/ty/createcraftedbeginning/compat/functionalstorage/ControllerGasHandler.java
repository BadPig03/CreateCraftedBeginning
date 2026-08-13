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
        event.registerBlockEntity(GasHandler.BLOCK, (BlockEntityType) type, (blockEntity, side) -> ((GasControllerAccess) blockEntity).ccb$getGasHandler());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerExtensionCapability(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        event.registerBlockEntity(GasHandler.BLOCK, (BlockEntityType) type, (blockEntity, side) -> ccb$getExtensionGasHandler((StorageControllerExtensionTile<?>) blockEntity, side));
    }

    private static @Nullable IGasHandler ccb$getExtensionGasHandler(StorageControllerExtensionTile<?> extension, @Nullable Direction side) {
        BlockPos controllerPos = extension.getControllerPos();
        Level level = extension.getLevel();
        if (controllerPos == null || level == null || !level.isLoaded(controllerPos)) {
            return null;
        }
        return level.getCapability(GasHandler.BLOCK, controllerPos, side);
    }

    private static long fillTank(IGasHandler handler, int tank, GasStack resource, GasAction action) {
        long accepted = fillTankDirect(handler, tank, resource, GasAction.SIMULATE);
        if (accepted <= 0 || !action.execute()) {
            return accepted;
        }
        return fillTankDirect(handler, tank, resource, GasAction.EXECUTE);
    }

    private static long fillTankDirect(IGasHandler handler, int tank, GasStack resource, GasAction action) {
        if (handler instanceof GasDrawerHandler drawer) {
            return drawer.getInternalTank(tank).fill(resource, action);
        }
        return handler.fill(resource, action);
    }

    private static List<GasStack[]> snapshotContents(List<GasDrawerHandler> drawers) {
        List<GasStack[]> snapshots = new ArrayList<>(drawers.size());
        for (GasDrawerHandler drawer : drawers) {
            snapshots.add(drawer.snapshotContents());
        }
        return snapshots;
    }

    private static void restoreContents(List<GasDrawerHandler> drawers, List<GasStack[]> snapshots) {
        for (int drawer = 0; drawer < drawers.size(); drawer++) {
            drawers.get(drawer).restoreContents(snapshots.get(drawer));
        }
    }

    private static void endTransactions(List<GasDrawerHandler> drawers, int begunTransactions, boolean commit) {
        for (int drawer = 0; drawer < begunTransactions; drawer++) {
            drawers.get(drawer).endTransaction(commit);
        }
    }

    public void refresh(List<IGasHandler> handlers) {
        this.handlers = List.copyOf(handlers);
        delegate = new CombinedGasTankWrapper(this.handlers.toArray(IGasHandler[]::new));
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

        long accepted = fillExisting(resource, action);
        if (accepted > 0) {
            return accepted;
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
        boolean commit = false;
        int begunTransactions = 0;
        try {
            for (GasDrawerHandler drawer : drawers) {
                drawer.beginTransaction();
                begunTransactions++;
            }
            boolean success = fillAll(resources);
            commit = success && action.execute();
            return success ? AtomicFillResult.SUCCESS : AtomicFillResult.REJECTED;
        } finally {
            if (!commit) {
                restoreContents(drawers, snapshots);
            }
            endTransactions(drawers, begunTransactions, commit);
        }
    }

    @Override
    public long getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }

    private long fillExisting(GasStack resource, GasAction action) {
        for (IGasHandler handler : handlers) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                GasStack stored = handler.getGasInTank(tank);
                if (stored.isEmpty() || !GasStack.isSameGasSameComponents(stored, resource)) {
                    continue;
                }

                long accepted = fillTank(handler, tank, resource, action);
                if (accepted <= 0) {
                    continue;
                }

                return accepted;
            }
        }
        return 0;
    }

    private long fillEmpty(GasStack resource, GasAction action) {
        for (IGasHandler handler : handlers) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                if (!handler.getGasInTank(tank).isEmpty() || !handler.isGasValid(tank, resource)) {
                    continue;
                }

                long accepted = fillTank(handler, tank, resource, action);
                if (accepted <= 0) {
                    continue;
                }

                return accepted;
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
