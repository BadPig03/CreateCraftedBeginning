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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasNetwork {
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

    public GasNetwork(Level level, BlockFace location, Supplier<@Nullable ICapabilityProvider<IGasHandler>> sourceSupplier, GasStack initialPendingTransfer, Consumer<GasStack> pendingTransferSink) {
        this.level = level;
        this.sourceSupplier = sourceSupplier;
        this.pendingTransferSink = pendingTransferSink;
        start = location;
        pendingTransfer = initialPendingTransfer.isEmpty() ? GasStack.EMPTY : initialPendingTransfer.copy();
        traversal = new GasNetworkTraversal(level, location);
        planner = new GasTransferPlanner(traversal, location);
        reset();
    }

    public void reset() {
        recoverPendingTransferInternal();
        traversal.reset();
        pauseBeforePropagation = PAUSE_INTERVAL;
        active = true;
    }

    public void stop() {
        traversal.clear();
        transferCreditUnits = 0;
        pauseBeforePropagation = 0;
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public boolean recoverPendingTransfer() {
        return recoverPendingTransferInternal();
    }

    public void tick() {
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

        GasStack gas = traversal.getGas();
        long transferRateUnits = traversal.getTransferRateUnits();
        if (gas.isEmpty() || transferRateUnits <= 0) {
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

        IGasHandler sourceCap = sourceProvider.getCapability();
        if (sourceCap == null || !traversal.hasTransferTargets()) {
            return;
        }

        GasStack available = GasTransferExecutor.simulateSourceDrain(sourceCap, gas, transferBudget);
        if (available.isEmpty()) {
            return;
        }

        List<PlannedTransfer> transferPlan = planner.createTransferPlan(available, sourceCap);
        if (transferPlan.isEmpty()) {
            return;
        }

        GasStack remainder = GasTransferExecutor.executeTransferPlan(sourceCap, available, transferPlan);
        if (remainder.isEmpty()) {
            return;
        }

        remainder = redistributeRemainder(sourceCap, remainder);
        if (remainder.isEmpty()) {
            return;
        }

        setPendingTransfer(sourceProvider, remainder);
        recoverPendingTransferInternal();
    }

    private long consumeTransferBudget(long transferRateUnits) {
        Step step = GasTransferBudget.consume(transferRateUnits, transferCreditUnits);
        transferCreditUnits = step.creditUnits();
        return step.budget();
    }

    private void recoverPendingTransferToTargets() {
        GasStack gas = traversal.getGas();
        if (pendingTransfer.isEmpty() || gas.isEmpty() || !GasStack.isSameGasSameComponents(pendingTransfer, gas) || !traversal.hasTransferTargets()) {
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

        IGasHandler sourceCap = sourceProvider.getCapability();
        if (sourceCap == null) {
            return;
        }

        GasStack remainder = distributeToTargets(sourceCap, pendingTransfer.copy(), 2);
        setPendingTransfer(sourceProvider, remainder);
        pendingTransfer.isEmpty();
    }

    private GasStack redistributeRemainder(IGasHandler sourceCap, GasStack remainder) {
        GasStack gas = traversal.getGas();
        if (remainder.isEmpty() || gas.isEmpty() || !GasStack.isSameGasSameComponents(remainder, gas)) {
            return remainder;
        }
        return distributeToTargets(sourceCap, remainder, 1);
    }

    private GasStack distributeToTargets(IGasHandler sourceCap, GasStack available, int maxPasses) {
        if (available.isEmpty() || maxPasses <= 0) {
            return available;
        }

        GasStack remainder = available.copy();
        for (int pass = 0; pass < maxPasses && !remainder.isEmpty() && planner.hasProbeBudget(); pass++) {
            List<PlannedTransfer> transferPlan = planner.createTransferPlan(remainder, sourceCap);
            if (transferPlan.isEmpty()) {
                continue;
            }

            remainder = GasTransferExecutor.executeTargetPlan(remainder, transferPlan);
        }
        return remainder;
    }

    private boolean recoverPendingTransferInternal() {
        if (pendingTransfer.isEmpty()) {
            return true;
        }

        ICapabilityProvider<IGasHandler> sourceProvider = pendingSourceProvider;
        IGasHandler sourceCap = sourceProvider == null ? null : sourceProvider.getCapability();
        if (sourceCap == null) {
            sourceProvider = sourceSupplier.get();
            if (sourceProvider == null) {
                return false;
            }

            sourceCap = sourceProvider.getCapability();
            if (sourceCap == null) {
                return false;
            }
        }

        if (sourceCap instanceof IVentingGasSource) {
            setPendingTransfer(null, GasStack.EMPTY);
            return true;
        }

        long returned = sourceCap.fill(pendingTransfer.copy(), GasAction.EXECUTE);
        returned = Math.clamp(returned, 0, pendingTransfer.getAmount());
        if (returned <= 0) {
            pendingSourceProvider = sourceProvider;
            return false;
        }

        GasStack remainder = pendingTransfer.copy();
        remainder.shrink(returned);
        setPendingTransfer(remainder.isEmpty() ? null : sourceProvider, remainder);
        return pendingTransfer.isEmpty();
    }

    private void setPendingTransfer(@Nullable ICapabilityProvider<IGasHandler> sourceProvider, GasStack remainder) {
        GasStack normalized = remainder.isEmpty() ? GasStack.EMPTY : remainder.copy();
        boolean changed = !GasStack.matches(pendingTransfer, normalized);
        pendingTransfer = normalized;
        pendingSourceProvider = normalized.isEmpty() ? null : sourceProvider;
        if (!changed) {
            return;
        }

        pendingTransferSink.accept(normalized.isEmpty() ? GasStack.EMPTY : normalized.copy());
        BlockEntity blockEntity = level.getBlockEntity(start.getPos());
        if (blockEntity == null) {
            return;
        }

        blockEntity.setChanged();
    }
}
