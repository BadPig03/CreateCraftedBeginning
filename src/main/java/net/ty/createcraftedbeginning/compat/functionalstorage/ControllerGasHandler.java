package net.ty.createcraftedbeginning.compat.functionalstorage;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.StorageControllerExtensionTile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.compat.functionalstorage.access.GasControllerAccess;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import net.ty.createcraftedbeginning.gas.storage.handler.CombinedGasStorageHandler;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureFillService;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ControllerGasHandler implements GasStorageHandler {
    private List<GasStorageHandler> handlers = List.of();
    private GasStorageHandler delegate = new CombinedGasStorageHandler();

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerControllerCapability(event, FunctionalStorage.DRAWER_CONTROLLER.type().get());
        registerControllerCapability(event, FunctionalStorage.FRAMED_DRAWER_CONTROLLER.type().get());
        registerExtensionCapability(event, FunctionalStorage.CONTROLLER_EXTENSION.type().get());
        registerExtensionCapability(event, FunctionalStorage.FRAMED_CONTROLLER_EXTENSION.type().get());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerControllerCapability(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        event.registerBlockEntity(GasCapabilities.BLOCK, (BlockEntityType) type, (blockEntity, ignoredDirection) -> ((GasControllerAccess) blockEntity).ccb$getGasHandler());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerExtensionCapability(RegisterCapabilitiesEvent event, BlockEntityType<?> type) {
        event.registerBlockEntity(GasCapabilities.BLOCK, (BlockEntityType) type, (blockEntity, side) -> ccb$getExtensionGasHandler((StorageControllerExtensionTile<?>) blockEntity, side));
    }

    private static @Nullable GasHandler ccb$getExtensionGasHandler(StorageControllerExtensionTile<?> extension, @Nullable Direction capabilitySide) {
        BlockPos controllerPos = extension.getControllerPos();
        Level level = extension.getLevel();
        if (controllerPos == null || level == null || !level.isLoaded(controllerPos)) {
            return null;
        }

        return level.getCapability(GasCapabilities.BLOCK, controllerPos, capabilitySide);
    }

    private static long fillTank(GasStorageHandler handler, int tankIndex, GasStack gasStack, GasAction action) {
        GasPressureCompartment compartment = handler.getPressureCompartment(tankIndex);
        long acceptedAmount = compartment.fill(gasStack, GasAction.SIMULATE);
        if (acceptedAmount <= 0 || !action.execute()) {
            return acceptedAmount;
        }

        return compartment.fill(gasStack, GasAction.EXECUTE);
    }

    private static List<GasTankState[]> snapshotStates(List<GasDrawerHandler> drawers) {
        List<GasTankState[]> snapshots = new ArrayList<>(drawers.size());
        for (GasDrawerHandler drawer : drawers) {
            snapshots.add(drawer.snapshotStates());
        }
        return snapshots;
    }

    private static void restoreStates(List<GasDrawerHandler> drawers, List<GasTankState[]> snapshots) {
        if (drawers.size() != snapshots.size()) {
            throw new IllegalArgumentException("Gas drawer snapshot count mismatch: expected " + drawers.size() + ", got " + snapshots.size() + '.');
        }

        for (int drawerIndex = 0; drawerIndex < drawers.size(); drawerIndex++) {
            drawers.get(drawerIndex).validateStates(snapshots.get(drawerIndex));
        }
        for (int drawerIndex = 0; drawerIndex < drawers.size(); drawerIndex++) {
            drawers.get(drawerIndex).restoreStates(snapshots.get(drawerIndex));
        }
    }

    private static void endTransactions(List<GasDrawerHandler> drawers, int begunTransactions, boolean commit) {
        for (int drawerIndex = 0; drawerIndex < begunTransactions; drawerIndex++) {
            drawers.get(drawerIndex).endTransaction(commit);
        }
    }

    @Override
    public GasPressureCompartment getPressureCompartment(int tank) {
        return delegate.getPressureCompartment(tank);
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

        List<GasTankState[]> snapshots = snapshotStates(drawers);
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
        }
        finally {
            if (!shouldCommit) {
                restoreStates(drawers, snapshots);
            }
            endTransactions(drawers, transactionCount, shouldCommit);
        }
    }

    @Override
    public AtomicFillResult tryFillAtomicallyFromPressure(List<GasStack> resources, long sourcePressurePa, GasAction action) {
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

        List<GasTankState[]> snapshots = snapshotStates(drawers);
        boolean shouldCommit = false;
        int transactionCount = 0;
        try {
            for (GasDrawerHandler drawer : drawers) {
                drawer.beginTransaction();
                transactionCount++;
            }
            boolean filledAllResources = GasPressureFillService.fillAllFromFixedPressure(this, resources, sourcePressurePa);
            shouldCommit = filledAllResources && action.execute();
            if (filledAllResources) {
                return AtomicFillResult.SUCCESS;
            }

            return AtomicFillResult.REJECTED;
        }
        finally {
            if (!shouldCommit) {
                restoreStates(drawers, snapshots);
            }
            endTransactions(drawers, transactionCount, shouldCommit);
        }
    }

    @Override
    public long getTankVolume(int tank) {
        return delegate.getTankVolume(tank);
    }

    @Override
    public long getTankPressurePa(int tank) {
        return delegate.getTankPressurePa(tank);
    }

    @Override
    public long getTankMaxPressurePa(int tank) {
        return delegate.getTankMaxPressurePa(tank);
    }

    @Override
    public long getTankMaxAmount(int tank) {
        return delegate.getTankMaxAmount(tank);
    }

    @Override
    public PressureModel getTankPressureModel(int tank) {
        return delegate.getTankPressureModel(tank);
    }

    public void refresh(List<GasHandler> handlers) {
        this.handlers = handlers.stream().filter(GasStorageHandler.class::isInstance).map(GasStorageHandler.class::cast).toList();
        delegate = new CombinedGasStorageHandler(this.handlers.toArray(GasStorageHandler[]::new));
    }

    private long fillExisting(GasStack resource, GasAction action) {
        for (GasStorageHandler handler : handlers) {
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
        for (GasStorageHandler handler : handlers) {
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
        for (GasStorageHandler handler : handlers) {
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
