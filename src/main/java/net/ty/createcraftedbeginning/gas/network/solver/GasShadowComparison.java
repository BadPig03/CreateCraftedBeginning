package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.GasRouteBatch.Collision;
import net.ty.createcraftedbeginning.gas.network.solver.GasTransferScheduler.Receipt;
import net.ty.createcraftedbeginning.gas.network.solver.GasTransferScheduler.Tick;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasShadowComparison implements AutoCloseable {
    private final Map<Level, GasServiceOrder> orders = new WeakHashMap<>();
    final int work;
    final long quantum;
    final Map<BlockPos, Long> capacities;
    private final Set<Snapshot> networks = Collections.newSetFromMap(new WeakHashMap<>());
    private boolean closed;

    public GasShadowComparison(int work, long quantum) {
        this(work, quantum, Map.of());
    }

    GasShadowComparison(int work, long quantum, Map<BlockPos, Long> capacities) {
        if (work < 1 || quantum < 1) {
            throw new IllegalArgumentException("Shadow budgets must be positive.");
        }

        this.work = work;
        this.quantum = quantum;
        this.capacities = Map.copyOf(capacities);
    }

    private static String gasKey(Level level, GasStack stack) {
        return stack.copyWithAmount(1).save(level.registryAccess()).toString();
    }

    private static String face(BlockPos pos, Direction side) {
        return pos.toShortString() + "/" + side.getName();
    }

    @Override
    public void close() {
        closed = true;
        for (Snapshot network : networks) {
            network.releaseShadow(this);
        }
        networks.clear();
        orders.clear();
    }

    GasServiceOrder order(Level level) {
        return orders.computeIfAbsent(level, ignored -> new GasServiceOrder());
    }

    void attach(Snapshot network) {
        if (closed) {
            throw new IllegalStateException("Closed gas shadow session.");
        }

        networks.add(network);
    }

    public record Reading(long minimumPa, long maximumPa, long measuredTick) {}
    public record Transfer(String source, String target, String gas, long amount) {}
    public record Grant(Transfer transfer, long pathCapacity) {}
    public record Conflict(String position, String existingGas, long existingGU, String incomingGas, long incomingGU) {}
    public record Sample(long tick, boolean cold, String status, int searchWork, boolean continuation, int pressureNodes, int pressureWork, long nanos, long allocatedBytes, @Nullable Transfer proposal, long pathCapacity, List<Transfer> receipts, Map<String, Reading> candidatePressure, Map<String, Reading> actualPressure, List<Grant> grants, @Nullable Conflict conflict, int buildWork, boolean layoutReady, long setupNanos) {
        public Sample(long tick, boolean cold, String status, int searchWork, boolean continuation, int pressureNodes, int pressureWork, long nanos, long allocatedBytes, @Nullable Transfer proposal, long pathCapacity, List<Transfer> receipts, Map<String, Reading> candidatePressure, Map<String, Reading> actualPressure, List<Grant> grants, @Nullable Conflict conflict) {
            this(tick, cold, status, searchWork, continuation, pressureNodes, pressureWork, nanos, allocatedBytes, proposal, pathCapacity, receipts, candidatePressure, actualPressure, grants, conflict, 0, true, 0);
        }

        public Sample(long tick, boolean cold, String status, int searchWork, boolean continuation, int pressureNodes, int pressureWork, long nanos, long allocatedBytes, @Nullable Transfer proposal, long pathCapacity, List<Transfer> receipts, Map<String, Reading> candidatePressure, Map<String, Reading> actualPressure) {
            this(tick, cold, status, searchWork, continuation, pressureNodes, pressureWork, nanos, allocatedBytes, proposal, pathCapacity, receipts, candidatePressure, actualPressure, proposal == null ? List.of() : List.of(new Grant(proposal, pathCapacity)), null);
        }
    }

    static final class Observation {
        private final List<Transfer> actual = new ArrayList<>();
        private final Map<String, Reading> actualPressure = new HashMap<>();
        private final long tick;
        private final boolean cold;
        private final Tick candidate;
        private final int buildWork;
        private final boolean layoutReady;
        private final int pressureNodes;
        private final int pressureWork;
        private final long nanos;
        private final long setupNanos;
        private final long allocatedBytes;
        private final List<Grant> grants;
        private final @Nullable Conflict conflict;
        private final @Nullable Transfer proposal;
        private final long capacity;
        private final Map<String, Reading> pressure;

        Observation(Level level, Snapshot topology, GasShadowComparison comparison) {
            tick = level.getGameTime();
            cold = !topology.hasContinuousShadow(comparison, tick);
            long start = System.nanoTime();
            long before = GasSolverProfiler.allocatedBytes();
            GasTransferScheduler scheduler = topology.shadow(level, comparison, tick, cold);
            setupNanos = cold ? System.nanoTime() - start : 0;
            GasRouteBatch batch = scheduler.previewBatch(level);
            candidate = scheduler.last();
            GasZonePressure model = scheduler.pressureModel();
            buildWork = scheduler.buildWork();
            layoutReady = model != null;
            pressureNodes = model == null ? 0 : model.nodes();
            pressureWork = model == null ? 0 : model.lastWork();
            long after = GasSolverProfiler.allocatedBytes();
            allocatedBytes = before < 0 || after < 0 ? -1 : after - before;
            nanos = System.nanoTime() - start;
            pressure = model == null ? Map.of() : Map.copyOf(model.shadowReadings());
            List<Grant> proposed = new ArrayList<>();
            for (Receipt receipt : batch.grants()) {
                BlockFace from = receipt.source();
                BlockFace to = receipt.target();
                GasStack gas = receipt.gas();
                Transfer transfer = new Transfer(face(from.getPos(), from.getFace()), face(to.getPos(), to.getFace()), gasKey(level, gas), gas.getAmount());
                proposed.add(new Grant(transfer, receipt.path().capacity()));
            }
            grants = List.copyOf(proposed);
            proposal = grants.isEmpty() ? null : grants.getFirst().transfer;
            capacity = grants.isEmpty() ? 0 : grants.getFirst().pathCapacity;
            Collision collision = batch.collision();
            conflict = collision == null ? null : new Conflict(collision.pos().toShortString(), gasKey(level, collision.existing()), collision.existing().getAmount(), gasKey(level, collision.incoming()), collision.incoming().getAmount());
        }

        void receipt(Level level, Receipt receipt) {
            BlockFace from = receipt.source();
            BlockFace to = receipt.target();
            GasStack gas = receipt.gas();
            actual.add(new Transfer(face(from.getPos(), from.getFace()), face(to.getPos(), to.getFace()), gasKey(level, gas), gas.getAmount()));
        }

        void pressure(BlockPos pos, long min, long max) {
            actualPressure.put(pos.toShortString(), new Reading(min, max, tick));
        }

        Sample finish() {
            return new Sample(tick, cold, candidate.status().name(), candidate.work(), candidate.continuation(), pressureNodes, pressureWork, nanos, allocatedBytes, proposal, capacity, List.copyOf(actual), pressure, Map.copyOf(actualPressure), grants, conflict, buildWork, layoutReady, setupNanos);
        }
    }
}
