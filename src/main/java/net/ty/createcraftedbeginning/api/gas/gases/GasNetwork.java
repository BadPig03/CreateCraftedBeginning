package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection.AirFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class GasNetwork {
    private static final int CYCLES_PER_TICK = 16;
    private static final int PAUSE_INTERVAL = 2;
    private static final int TWO_SECONDS_IN_TICKS = 40;

    private final Level level;
    private final BlockFace start;
    private final List<BlockFace> queued;
    private final Set<Pair<BlockFace, GasPipeConnection>> frontier;
    private final Set<BlockPos> visited;
    private final List<Pair<BlockFace, GasFlowSource>> targets;
    private final Map<BlockPos, WeakReference<GasTransportBehaviour>> cache;
    private final Supplier<@Nullable ICapabilityProvider<IGasHandler>> sourceSupplier;
    private @Nullable ICapabilityProvider<IGasHandler> source;
    private long transferSpeed;
    private int pauseBeforePropagation;
    private GasStack gas;

    public GasNetwork(Level level, BlockFace location, Supplier<@Nullable ICapabilityProvider<IGasHandler>> sourceSupplier) {
        this.level = level;
        start = location;
        this.sourceSupplier = sourceSupplier;
        gas = GasStack.EMPTY;
        frontier = new HashSet<>();
        visited = new HashSet<>();
        targets = new ArrayList<>();
        cache = new HashMap<>();
        queued = new ArrayList<>();
        reset();
    }

    public void reset() {
        frontier.clear();
        visited.clear();
        targets.clear();
        queued.clear();
        gas = GasStack.EMPTY;
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
                        targets.add(Pair.of(adjacentLocation, adjacent.source.get()));
                        continue;
                    }

                    if (visited.add(adjacentLocation.getConnectedPos())) {
                        queued.add(adjacentLocation.getOpposite());
                        shouldContinue = true;
                    }
                }
                if (canRemove) {
                    iterator.remove();
                }
            }
            if (!shouldContinue) {
                break;
            }
        }

        if (source == null) {
            source = sourceSupplier.get();
        }
        if (source == null) {
            return;
        }

        if (targets.isEmpty()) {
            return;
        }
        for (Pair<BlockFace, GasFlowSource> pair : targets) {
            if (level.getGameTime() % TWO_SECONDS_IN_TICKS != 0) {
                continue;
            }

            GasPipeConnection connection = get(pair.getFirst());
            if (connection == null) {
                continue;
            }

            connection.source.ifPresent(fs -> {
                if (fs.isEndpoint()) {
                    pair.setSecond(fs);
                }
            });
        }

        long flowSpeed = transferSpeed;
        Map<IGasHandler, Long> accumulatedFill = new IdentityHashMap<>();

        for (boolean simulate : Iterate.trueAndFalse) {
            GasAction action = simulate ? GasAction.SIMULATE : GasAction.EXECUTE;

            if (source == null) {
                return;
            }
            IGasHandler sourceCap = source.getCapability();
            if (sourceCap == null) {
                return;
            }

            GasStack transfer = GasStack.EMPTY;
            for (int i = 0; i < sourceCap.getTanks(); i++) {
                GasStack contained = sourceCap.getGasInTank(i);
                if (contained.isEmpty() || !GasStack.isSameGasSameComponents(contained, gas)) {
                    continue;
                }
                GasStack toExtract = contained.copyWithAmount(flowSpeed);
                transfer = sourceCap.drain(toExtract, action);
                break;
            }

            if (transfer.isEmpty()) {
                GasStack genericExtract = sourceCap.drain(flowSpeed, action);
                if (!genericExtract.isEmpty() && GasStack.isSameGasSameComponents(genericExtract, gas)) {
                    transfer = genericExtract;
                }
            }

            if (transfer.isEmpty()) {
                return;
            }
            if (simulate) {
                flowSpeed = transfer.getAmount();
            }

            List<Pair<BlockFace, GasFlowSource>> availableOutputs = new ArrayList<>(targets);
            while (!availableOutputs.isEmpty() && transfer.getAmount() > 0) {
                long dividedTransfer = transfer.getAmount() / availableOutputs.size();
                long remainder = transfer.getAmount() % availableOutputs.size();

                for (Iterator<Pair<BlockFace, GasFlowSource>> iterator = availableOutputs.iterator(); iterator.hasNext(); ) {
                    Pair<BlockFace, GasFlowSource> pair = iterator.next();
                    long toTransfer = dividedTransfer;
                    if (remainder > 0) {
                        toTransfer++;
                        remainder--;
                    }

                    if (transfer.isEmpty()) {
                        break;
                    }
                    @Nullable ICapabilityProvider<IGasHandler> targetHandlerProvider = pair.getSecond().provideHandler();
                    if (targetHandlerProvider == null) {
                        iterator.remove();
                        continue;
                    }
                    IGasHandler targetHandler = targetHandlerProvider.getCapability();
                    if (targetHandler == null) {
                        iterator.remove();
                        continue;
                    }

                    long simulatedTransfer = toTransfer;
                    if (simulate) {
                        simulatedTransfer += accumulatedFill.getOrDefault(targetHandler, 0L);
                    }

                    GasStack divided = transfer.copy();
                    divided.setAmount(simulatedTransfer);
                    long filled = targetHandler.fill(divided, action);

                    if (simulate) {
                        accumulatedFill.put(targetHandler, filled);
                        filled -= simulatedTransfer - toTransfer;
                    }

                    transfer.setAmount(transfer.getAmount() - filled);
                    if (filled < simulatedTransfer) {
                        iterator.remove();
                    }
                }
            }

            flowSpeed -= transfer.getAmount();
        }
    }

    @Nullable
    private GasPipeConnection get(@NotNull BlockFace location) {
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

    private boolean isPresent(@NotNull BlockFace location) {
        return level.isLoaded(location.getPos());
    }
}