package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection.AirFlow;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasNetwork {
    private static final int CYCLES_PER_TICK = 16;
    private static final int PAUSE_INTERVAL = 2;

    private final Level level;
    private final BlockFace start;
    private final List<BlockFace> queued;
    private final Set<Pair<BlockFace, GasPipeConnection>> frontier;
    private final Set<BlockPos> visited;
    private final Map<BlockFace, GasFlowSource> targets;
    private final Map<BlockPos, WeakReference<GasTransportBehaviour>> cache;
    private final Supplier<@Nullable ICapabilityProvider<IGasHandler>> sourceSupplier;
    private @Nullable ICapabilityProvider<IGasHandler> source;
    private long transferSpeed;
    private int pauseBeforePropagation;
    private GasStack gas;

    public GasNetwork(Level level, BlockFace location, Supplier<@Nullable ICapabilityProvider<IGasHandler>> sourceSupplier) {
        this.level = level;
        this.sourceSupplier = sourceSupplier;
        start = location;
        gas = GasStack.EMPTY;
        frontier = new HashSet<>();
        visited = new HashSet<>();
        targets = new LinkedHashMap<>();
        cache = new HashMap<>();
        queued = new ArrayList<>();
        reset();
    }

    private static List<PlannedTransfer> createTransferPlan(GasStack available, List<TransferTarget> availableTargets) {
        List<PlannedTransfer> transferPlan = new ArrayList<>();
        GasStack remaining = available.copy();
        Map<IGasHandler, Long> accumulatedFill = new IdentityHashMap<>();
        List<TransferTarget> outputs = new ArrayList<>(availableTargets);
        while (!outputs.isEmpty() && remaining.getAmount() > 0) {
            long dividedTransfer = remaining.getAmount() / outputs.size();
            long remainder = remaining.getAmount() % outputs.size();
            boolean acceptedAny = false;
            for (Iterator<TransferTarget> iterator = outputs.iterator(); iterator.hasNext(); ) {
                TransferTarget target = iterator.next();
                long toTransfer = dividedTransfer;
                if (remainder > 0) {
                    toTransfer++;
                    remainder--;
                }
                if (toTransfer <= 0) {
                    continue;
                }

                long alreadyAccepted = accumulatedFill.getOrDefault(target.handler, 0L);
                long simulatedTransfer = alreadyAccepted + toTransfer;
                GasStack simulatedStack = available.copyWithAmount(simulatedTransfer);
                long totalAccepted = target.handler.fill(simulatedStack, GasAction.SIMULATE);
                totalAccepted = Math.clamp(totalAccepted, 0, simulatedTransfer);
                long accepted = Math.max(0, totalAccepted - alreadyAccepted);
                accepted = Math.min(accepted, toTransfer);
                if (accepted > 0) {
                    accumulatedFill.put(target.handler, alreadyAccepted + accepted);
                    transferPlan.add(new PlannedTransfer(target.handler, accepted));
                    remaining.shrink(accepted);
                    acceptedAny = true;
                }
                if (accepted >= toTransfer) {
                    continue;
                }

                iterator.remove();
            }

            if (!acceptedAny) {
                break;
            }
        }

        return transferPlan;
    }

    private static void executeTransferPlan(IGasHandler sourceCap, GasStack gasType, List<PlannedTransfer> transferPlan) {
        GasStack buffered = GasStack.EMPTY;
        for (PlannedTransfer plannedTransfer : transferPlan) {
            long needed = plannedTransfer.amount;
            if (needed <= 0) {
                continue;
            }

            if (buffered.getAmount() < needed) {
                GasStack request = gasType.copyWithAmount(needed - buffered.getAmount());
                GasStack drained = executeSourceDrain(sourceCap, request);
                if (!drained.isEmpty()) {
                    if (buffered.isEmpty()) {
                        buffered = drained;
                    }
                    else {
                        buffered.grow(drained.getAmount());
                    }
                }
            }
            if (buffered.isEmpty()) {
                continue;
            }

            long offeredAmount = Math.min(needed, buffered.getAmount());
            GasStack offered = buffered.copyWithAmount(offeredAmount);
            long filled = plannedTransfer.handler.fill(offered, GasAction.EXECUTE);
            filled = Math.clamp(filled, 0, offeredAmount);
            buffered.shrink(filled);
        }

        if (!buffered.isEmpty()) {
            sourceCap.fill(buffered, GasAction.EXECUTE);
        }
    }

    private static GasStack executeSourceDrain(IGasHandler sourceCap, GasStack request) {
        if (request.isEmpty()) {
            return GasStack.EMPTY;
        }

        GasStack drained = sourceCap.drain(request, GasAction.EXECUTE);
        if (!drained.isEmpty()) {
            if (GasStack.isSameGasSameComponents(drained, request)) {
                return drained;
            }

            sourceCap.fill(drained, GasAction.EXECUTE);
            return GasStack.EMPTY;
        }

        GasStack genericPreview = sourceCap.drain(request.getAmount(), GasAction.SIMULATE);
        if (genericPreview.isEmpty() || !GasStack.isSameGasSameComponents(genericPreview, request)) {
            return GasStack.EMPTY;
        }

        drained = sourceCap.drain(request.getAmount(), GasAction.EXECUTE);
        if (drained.isEmpty()) {
            return GasStack.EMPTY;
        }
        if (GasStack.isSameGasSameComponents(drained, request)) {
            return drained;
        }

        sourceCap.fill(drained, GasAction.EXECUTE);
        return GasStack.EMPTY;
    }

    public void reset() {
        frontier.clear();
        visited.clear();
        targets.clear();
        queued.clear();
        gas = GasStack.EMPTY;
        source = null;
        transferSpeed = 0;
        queued.add(start);
        pauseBeforePropagation = PAUSE_INTERVAL;
    }

    public void tick() {
        if (pauseBeforePropagation > 0) {
            pauseBeforePropagation--;
            return;
        }

        for (int cycle = 0; cycle < CYCLES_PER_TICK; cycle++) {
            boolean shouldContinue = false;
            for (Iterator<BlockFace> iterator = queued.iterator(); iterator.hasNext(); ) {
                BlockFace blockFace = iterator.next();
                if (!isPresent(blockFace)) {
                    continue;
                }

                GasPipeConnection connection = get(blockFace);
                if (connection != null) {
                    if (blockFace.equals(start)) {
                        transferSpeed = (int) Math.max(1, connection.pressure.get(true) / 2.0f);
                    }
                    frontier.add(Pair.of(blockFace, connection));
                }
                iterator.remove();
            }

            for (Iterator<Pair<BlockFace, GasPipeConnection>> iterator = frontier.iterator(); iterator.hasNext(); ) {
                Pair<BlockFace, GasPipeConnection> pair = iterator.next();
                BlockFace blockFace = pair.getFirst();
                GasPipeConnection connection = pair.getSecond();
                if (connection.flow.isEmpty()) {
                    continue;
                }

                AirFlow flow = connection.flow.get();
                if (!gas.isEmpty() && !GasStack.isSameGasSameComponents(flow.gas, gas)) {
                    iterator.remove();
                    continue;
                }

                if (!flow.inbound) {
                    if (connection.comparePressure() >= 0) {
                        iterator.remove();
                    }
                    continue;
                }

                if (gas.isEmpty()) {
                    gas = flow.gas;
                }
                boolean canRemove = true;
                for (Direction side : Iterate.directions) {
                    if (side == blockFace.getFace()) {
                        continue;
                    }

                    BlockFace adjacentLocation = new BlockFace(blockFace.getPos(), side);
                    GasPipeConnection adjacent = get(adjacentLocation);
                    if (adjacent == null) {
                        continue;
                    }

                    if (adjacent.flow.isEmpty()) {
                        if (adjacent.hasPressure() && adjacent.pressure.getSecond() > 0) {
                            canRemove = false;
                        }
                        continue;
                    }

                    AirFlow outFlow = adjacent.flow.get();
                    if (outFlow.inbound) {
                        if (adjacent.comparePressure() > 0) {
                            canRemove = false;
                        }
                        continue;
                    }

                    if (adjacent.source.isEmpty() && !adjacent.determineSource(level, blockFace.getPos())) {
                        canRemove = false;
                        continue;
                    }

                    if (adjacent.source.isPresent() && adjacent.source.get().isEndpoint()) {
                        targets.put(adjacentLocation, adjacent.source.get());
                        continue;
                    }

                    if (!visited.add(adjacentLocation.getConnectedPos())) {
                        continue;
                    }

                    queued.add(adjacentLocation.getOpposite());
                    shouldContinue = true;
                }
                if (canRemove) {
                    iterator.remove();
                }
            }
            if (!shouldContinue) {
                break;
            }
        }

        transferGas();
    }

    private void transferGas() {
        if (gas.isEmpty() || transferSpeed <= 0) {
            return;
        }

        if (source == null) {
            source = sourceSupplier.get();
        }
        if (source == null) {
            return;
        }

        IGasHandler sourceCap = source.getCapability();
        if (sourceCap == null) {
            source = null;
            return;
        }

        if (targets.isEmpty()) {
            return;
        }

        GasStack available = simulateSourceDrain(sourceCap, transferSpeed);
        if (available.isEmpty()) {
            return;
        }

        List<TransferTarget> availableTargets = collectAvailableTargets();
        if (availableTargets.isEmpty()) {
            return;
        }

        List<PlannedTransfer> transferPlan = createTransferPlan(available, availableTargets);
        if (transferPlan.isEmpty()) {
            return;
        }

        executeTransferPlan(sourceCap, available, transferPlan);
    }

    private GasStack simulateSourceDrain(IGasHandler sourceCap, long maxAmount) {
        if (maxAmount <= 0 || gas.isEmpty()) {
            return GasStack.EMPTY;
        }

        for (int i = 0; i < sourceCap.getTanks(); i++) {
            GasStack contained = sourceCap.getGasInTank(i);
            if (contained.isEmpty() || !GasStack.isSameGasSameComponents(contained, gas)) {
                continue;
            }

            GasStack drained = sourceCap.drain(contained.copyWithAmount(maxAmount), GasAction.SIMULATE);
            if (!drained.isEmpty()) {
                return GasStack.isSameGasSameComponents(drained, gas) ? drained : GasStack.EMPTY;
            }
            break;
        }

        GasStack drained = sourceCap.drain(maxAmount, GasAction.SIMULATE);
        return !drained.isEmpty() && GasStack.isSameGasSameComponents(drained, gas) ? drained : GasStack.EMPTY;
    }

    private List<TransferTarget> collectAvailableTargets() {
        refreshTargets();
        List<TransferTarget> availableTargets = new ArrayList<>();
        for (Entry<BlockFace, GasFlowSource> entry : targets.entrySet()) {
            ICapabilityProvider<IGasHandler> provider = entry.getValue().getGasHandlerProvider();
            if (provider == null) {
                continue;
            }

            IGasHandler targetHandler = provider.getCapability();
            if (targetHandler == null) {
                continue;
            }

            availableTargets.add(new TransferTarget(targetHandler));
        }
        return availableTargets;
    }

    private void refreshTargets() {
        for (Iterator<Entry<BlockFace, GasFlowSource>> iterator = targets.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<BlockFace, GasFlowSource> entry = iterator.next();
            GasFlowSource refreshed = refreshTarget(entry.getKey());
            if (refreshed == null) {
                iterator.remove();
                continue;
            }

            entry.setValue(refreshed);
        }
    }

    @Nullable
    private GasFlowSource refreshTarget(BlockFace location) {
        if (!isPresent(location)) {
            return null;
        }

        GasPipeConnection connection = get(location);
        if (connection == null || connection.flow.isEmpty()) {
            return null;
        }

        AirFlow flow = connection.flow.get();
        if (flow.inbound || !GasStack.isSameGasSameComponents(flow.gas, gas)) {
            return null;
        }

        if (connection.source.isEmpty() && !connection.determineSource(level, location.getPos())) {
            return null;
        }
        if (connection.source.isEmpty() || !connection.source.get().isEndpoint()) {
            return null;
        }

        return connection.source.get();
    }

    @Nullable
    private GasPipeConnection get(BlockFace location) {
        BlockPos pos = location.getPos();
        GasTransportBehaviour transfer = getGasTransfer(pos);
        if (transfer == null) {
            return null;
        }

        return transfer.getConnection(location.getFace());
    }

    @Nullable
    private GasTransportBehaviour getGasTransfer(BlockPos pos) {
        WeakReference<GasTransportBehaviour> weakReference = cache.get(pos);
        GasTransportBehaviour behaviour = weakReference != null ? weakReference.get() : null;
        if (behaviour != null && behaviour.blockEntity.isRemoved()) {
            behaviour = null;
        }
        if (behaviour == null) {
            behaviour = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
            if (behaviour != null) {
                cache.put(pos, new WeakReference<>(behaviour));
            }
        }
        return behaviour;
    }

    private boolean isPresent(BlockFace location) {
        return level.isLoaded(location.getPos());
    }

    private record TransferTarget(IGasHandler handler) {}

    private record PlannedTransfer(IGasHandler handler, long amount) {}
}