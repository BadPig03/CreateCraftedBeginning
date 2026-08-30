package net.ty.createcraftedbeginning.content.airtights.gas.transport;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IVentingGasSource;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasTransferBudget.Step;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasTransferExecutor.PlannedTransfer;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasNetwork {
    private static final int PAUSE_INTERVAL = 2;

    private final Level level;
    private final BlockFace start;
    private final Supplier<@Nullable ICapabilityProvider<IGasHandler>> sourceSupplier;
    private final Consumer<GasStack> pendingTransferSink;
    private final GasNetworkTraversal traversal;
    private final GasTransferPlanner planner;
    @Nullable
    private ICapabilityProvider<IGasHandler> pendingSourceProvider;
    private GasStack pendingTransfer;
    private long transferCreditUnits;
    private int pauseBeforePropagation;
    private boolean active;

    GasNetwork(Level level, BlockFace location, Supplier<@Nullable ICapabilityProvider<IGasHandler>> sourceSupplier, GasStack initialPendingTransfer, Consumer<GasStack> pendingTransferSink) {
        this.level = level;
        this.sourceSupplier = sourceSupplier;
        this.pendingTransferSink = pendingTransferSink;
        start = location;
        pendingTransfer = initialPendingTransfer.isEmpty() ? GasStack.EMPTY : initialPendingTransfer.copy();
        traversal = new GasNetworkTraversal(level, location);
        planner = new GasTransferPlanner(traversal, location);
        reset();
    }

    void reset() {
        recoverPendingTransferInternal();
        traversal.reset();
        pauseBeforePropagation = PAUSE_INTERVAL;
        active = true;
    }

    void stop() {
        traversal.clear();
        transferCreditUnits = 0;
        pauseBeforePropagation = 0;
        active = false;
    }

    boolean isActive() {
        return active;
    }

    boolean recoverPendingTransfer() {
        return recoverPendingTransferInternal();
    }

    void tick() {
        if (!active) {
            recoverPendingTransferInternal();
            return;
        }

        if (pauseBeforePropagation > 0) {
            pauseBeforePropagation--;
            return;
        }

        planner.beginTick();
        traversal.tick();
        transferGas();
    }

    private void transferGas() {
        if (!pendingTransfer.isEmpty()) {
            recoverPendingTransferToTargets();
            if (!pendingTransfer.isEmpty()) {
                recoverPendingTransferInternal();
            }
            return;
        }

        GasStack networkGas = traversal.getGas();
        long transferRateUnits = traversal.getTransferRateUnits();
        if (networkGas.isEmpty() || transferRateUnits <= 0) {
            return;
        }

        long transferBudget = consumeTransferBudget(transferRateUnits);
        if (transferBudget <= 0) {
            return;
        }

        ICapabilityProvider<IGasHandler> sourceProvider = sourceSupplier.get();
        if (sourceProvider == null) {
            return;
        }

        IGasHandler sourceHandler = sourceProvider.getCapability();
        if (sourceHandler == null || !traversal.hasTransferTargets()) {
            return;
        }

        GasStack availableGas = GasTransferExecutor.simulateSourceDrain(sourceHandler, networkGas, transferBudget);
        if (availableGas.isEmpty()) {
            return;
        }

        List<PlannedTransfer> transferPlan = planner.createTransferPlan(availableGas, sourceHandler);
        if (transferPlan.isEmpty()) {
            return;
        }

        GasStack remainingGas = GasTransferExecutor.executeTransferPlan(sourceHandler, availableGas, transferPlan);
        if (remainingGas.isEmpty()) {
            return;
        }

        remainingGas = redistributeRemainder(sourceHandler, remainingGas);
        if (remainingGas.isEmpty()) {
            return;
        }

        setPendingTransfer(sourceProvider, remainingGas);
        recoverPendingTransferInternal();
    }

    private long consumeTransferBudget(long transferRateUnits) {
        Step budgetStep = GasTransferBudget.consume(transferRateUnits, transferCreditUnits);
        transferCreditUnits = budgetStep.creditUnits();
        return budgetStep.budget();
    }

    private void recoverPendingTransferToTargets() {
        GasStack networkGas = traversal.getGas();
        if (pendingTransfer.isEmpty() || networkGas.isEmpty() || !GasStack.isSameGasSameComponents(pendingTransfer, networkGas) || !traversal.hasTransferTargets()) {
            pendingTransfer.isEmpty();
            return;
        }

        ICapabilityProvider<IGasHandler> sourceProvider = pendingSourceProvider;
        if (sourceProvider == null || sourceProvider.getCapability() == null) {
            sourceProvider = sourceSupplier.get();
        }
        if (sourceProvider == null) {
            return;
        }

        IGasHandler sourceHandler = sourceProvider.getCapability();
        if (sourceHandler == null) {
            return;
        }

        GasStack remainingGas = distributeToTargets(sourceHandler, pendingTransfer.copy(), 2);
        setPendingTransfer(sourceProvider, remainingGas);
        pendingTransfer.isEmpty();
    }

    private GasStack redistributeRemainder(IGasHandler sourceHandler, GasStack remainingGas) {
        GasStack networkGas = traversal.getGas();
        if (remainingGas.isEmpty() || networkGas.isEmpty() || !GasStack.isSameGasSameComponents(remainingGas, networkGas)) {
            return remainingGas;
        }
        return distributeToTargets(sourceHandler, remainingGas, 1);
    }

    private GasStack distributeToTargets(IGasHandler sourceHandler, GasStack availableGas, int maxPasses) {
        if (availableGas.isEmpty() || maxPasses <= 0) {
            return availableGas;
        }

        GasStack remainingGas = availableGas.copy();
        for (int pass = 0; pass < maxPasses && !remainingGas.isEmpty() && planner.hasProbeBudget(); pass++) {
            List<PlannedTransfer> transferPlan = planner.createTransferPlan(remainingGas, sourceHandler);
            if (transferPlan.isEmpty()) {
                continue;
            }

            remainingGas = GasTransferExecutor.executeTargetPlan(remainingGas, transferPlan);
        }
        return remainingGas;
    }

    private boolean recoverPendingTransferInternal() {
        if (pendingTransfer.isEmpty()) {
            return true;
        }

        ICapabilityProvider<IGasHandler> sourceProvider = pendingSourceProvider;
        IGasHandler sourceHandler = sourceProvider == null ? null : sourceProvider.getCapability();
        if (sourceHandler == null) {
            sourceProvider = sourceSupplier.get();
            if (sourceProvider == null) {
                return false;
            }

            sourceHandler = sourceProvider.getCapability();
            if (sourceHandler == null) {
                return false;
            }
        }

        if (sourceHandler instanceof IVentingGasSource) {
            setPendingTransfer(null, GasStack.EMPTY);
            return true;
        }

        long returnedAmount = sourceHandler.fill(pendingTransfer.copy(), GasAction.EXECUTE);
        returnedAmount = CCBMathUtils.clampNonNegative(returnedAmount, pendingTransfer.getAmount());
        if (returnedAmount <= 0) {
            pendingSourceProvider = sourceProvider;
            return false;
        }

        GasStack remainingTransfer = pendingTransfer.copy();
        remainingTransfer.shrink(returnedAmount);
        setPendingTransfer(remainingTransfer.isEmpty() ? null : sourceProvider, remainingTransfer);
        return pendingTransfer.isEmpty();
    }

    private void setPendingTransfer(@Nullable ICapabilityProvider<IGasHandler> sourceProvider, GasStack remainingTransfer) {
        GasStack normalizedTransfer = remainingTransfer.isEmpty() ? GasStack.EMPTY : remainingTransfer.copy();
        boolean pendingTransferChanged = !GasStack.matches(pendingTransfer, normalizedTransfer);
        pendingTransfer = normalizedTransfer;
        pendingSourceProvider = normalizedTransfer.isEmpty() ? null : sourceProvider;
        if (!pendingTransferChanged) {
            return;
        }

        pendingTransferSink.accept(normalizedTransfer.isEmpty() ? GasStack.EMPTY : normalizedTransfer.copy());
        BlockEntity blockEntity = level.getBlockEntity(start.getPos());
        if (blockEntity == null) {
            return;
        }

        blockEntity.setChanged();
    }
}
