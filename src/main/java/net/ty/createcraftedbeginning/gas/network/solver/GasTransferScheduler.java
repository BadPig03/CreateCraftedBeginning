package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.events.GasCollisionEvent;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.GasOrdinaryPathSearch.Path;
import net.ty.createcraftedbeginning.gas.network.solver.GasRouteBatch.Collision;
import net.ty.createcraftedbeginning.gas.network.solver.GasShadowComparison.Observation;
import net.ty.createcraftedbeginning.gas.network.solver.GasZoneRoute.Quote;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner.Port;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasFlowIntegration;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasTransferScheduler {
    static final int WORK_PER_TICK = 128;
    static final long GU_PER_TICK = 65536;
    private static final int PRESSURE_DIVISOR = 4;
    private static final int REPLAY_DIVISOR = 3;
    private static final int SHORT_DETOUR_STEPS = 8;

    private final Snapshot topology;
    private final Map<BlockPos, Long> limits;
    private final int workLimit;
    private final long quantumLimit;
    private int faceCount;
    private @Nullable GasRouteTree tree;
    private final GasZonePorts ports;
    private final @Nullable GasFixedManifold manifold;
    private final @Nullable GasSupplyRotation rotation;
    private final @Nullable GasZoneLayout zones;
    private @Nullable GasZonePressure pressureModel;
    private @Nullable Candidate deferred;
    private final Set<GasEndpointSlot> seeded = new HashSet<>();
    private final @Nullable GasServiceOrder serviceOrder;
    private GasEndpointSlot sweepStart = GasEndpointSlot.FIRST;
    private boolean enteringSource = true;
    private boolean skippedDetour;
    private boolean exhaustiveSweep;
    private boolean completedFirstSweep;
    private GasEndpointSlot firstSource = GasEndpointSlot.FIRST;
    private GasEndpointSlot source = GasEndpointSlot.FIRST;
    private GasEndpointSlot target = GasEndpointSlot.FIRST;
    private int sourceTanks;
    private int targetTanks;
    private boolean sawSource;
    private boolean sawSink;
    private boolean sawGradient;
    private boolean sawPath;
    private @Nullable GasRouteSearch search;
    private GasStack searchingGas = GasStack.EMPTY;
    private final GasServiceRoutes knownRoutes = new GasServiceRoutes();
    private final GasBranchOrder branchOrder = new GasBranchOrder();
    private @Nullable GasBranchRound collecting;
    private @Nullable Candidate hot;
    private @Nullable Tick last;
    private List<Receipt> flows = List.of();

    GasTransferScheduler(Snapshot topology, Map<BlockPos, Long> limits, int workLimit, long quantumLimit) {
        this(topology, limits, workLimit, quantumLimit, null, null, false);
    }

    GasTransferScheduler(Snapshot topology, Map<BlockPos, Long> limits, int workLimit, long quantumLimit, @Nullable GasZoneLayout zones, @Nullable GasServiceOrder serviceOrder, boolean observeOnly) {
        if (workLimit < 1 || quantumLimit < 1) {
            throw new IllegalArgumentException("Gas scheduling requires positive work and transfer budgets.");
        }
        if (zones == null && !topology.state().devicePositions().isEmpty()) {
            throw new IllegalArgumentException("The capacity-only scheduler requires an ordinary network.");
        }

        this.topology = topology;
        this.limits = Map.copyOf(limits);
        this.workLimit = workLimit;
        this.quantumLimit = quantumLimit;
        this.zones = zones;
        this.serviceOrder = serviceOrder;
        ports = new GasZonePorts(topology, zones == null ? List.of() : zones.internal(), observeOnly);
        faceCount = ports.size();
        rotation = !observeOnly && serviceOrder != null && zones != null && !topology.endpointFaces().isEmpty() ? new GasSupplyRotation(topology, zones, ports, serviceOrder) : null;
        manifold = !observeOnly && serviceOrder != null && faceCount >= 3 && faceCount <= GasFixedManifold.MAX_FACES ? new GasFixedManifold() : null;
        pressureModel = zones == null || serviceOrder != null ? null : new GasZonePressure(zones, ports, topology.pressureGauges());
    }

    @Nullable
    Tick last() {
        return last;
    }

    void tick(Level level) {
        List<Receipt> previous = flows;
        boolean cold = last == null;
        if (serviceOrder != null) {
            if (cold) {
                topology.clearPressureTelemetry(level);
            }
            GasRouteBatch batch = new GasRouteBatch(quantumLimit);
            try {
                scheduleBatch(level, batch);
            }
            finally {
                collecting = null;
                flows = batch.grants();
                if (current()) {
                    publishFlows(level, cold, previous);
                }
            }
        }
        else {
            last = advance(level, workLimit, null);
            if (pressureModel != null && current()) {
                flows = last.receipt == null ? List.of() : List.of(last.receipt);
                publishFlows(level, cold, previous);
                pressureModel.tick(level, workLimit);
            }
        }
        GasSolverProfiler.recordScheduledTick(last, last.status == Status.RETIRED ? null : pressureModel, serviceOrder == null ? 0 : zones.lastBuildWork(), flows.size());
    }

    boolean hasZoneFlows() {
        return zones != null;
    }

    @Nullable
    GasZonePressure pressureModel() {
        return pressureModel;
    }

    @Nullable
    FlowState zoneFlow(BlockPos pos, Direction side) {
        if (zones == null || flows.isEmpty()) {
            return null;
        }

        long net = 0;
        GasStack gas = GasStack.EMPTY;
        BlockFace face = new BlockFace(pos, side);
        for (Receipt receipt : flows) {
            int direction = receipt.path.zoneRoute().direction(zones, face);
            if (direction == 0) {
                continue;
            }

            gas = receipt.gas;
            net += direction * gas.getAmount();
        }
        if (net == 0) {
            return null;
        }

        if (net < 0) {
            return new FlowState(gas, FlowDirection.INBOUND, -net);
        }

        return new FlowState(gas, FlowDirection.OUTBOUND, net);
    }

    GasRouteBatch previewBatch(Level level) {
        if (!ports.observeOnly || zones == null) {
            throw new IllegalStateException("Preview requires an isolated shadow scheduler.");
        }

        GasRouteBatch batch = new GasRouteBatch(quantumLimit);
        scheduleBatch(level, batch);
        return batch;
    }

    Tick preview(Level level) {
        previewBatch(level);
        return last;
    }

    int buildWork() {
        return zones.lastBuildWork();
    }

    private void publishFlows(Level level, boolean cold, List<Receipt> previous) {
        Set<BlockPos> changed = new HashSet<>();
        if (cold) {
            changed.addAll(topology.state().devicePositions());
        }
        if (cold || !previous.isEmpty() || !flows.isEmpty()) {
            changed.addAll(topology.meteredPipes());
        }
        for (Receipt receipt : previous) {
            receipt.path.zoneRoute().devices().forEach(pass -> changed.add(pass.vertex().pos));
        }
        for (Receipt receipt : flows) {
            receipt.path.zoneRoute().devices().forEach(pass -> changed.add(pass.vertex().pos));
        }
        for (BlockPos pos : changed) {
            GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pos);
            if (behaviour == null) {
                continue;
            }
            boolean changedFlow = false;
            for (Direction side : Iterate.directions) {
                GasPipeConnection connection = behaviour.getConnection(side);
                if (connection == null) {
                    continue;
                }
                FlowState flow = zoneFlow(pos, side);
                if (flow == null) {
                    changedFlow |= connection.clearFlowState();
                }
                else {
                    changedFlow |= connection.setFlowState(flow.gas(), flow.direction() == FlowDirection.INBOUND, flow.flowRate());
                }
            }
            if ((!changedFlow && !cold) || !behaviour.publishFlowTelemetry()) {
                continue;
            }

            GasSolverProfiler.recordFlowTelemetryUpdate();
        }
    }

    private boolean current() {
        if (!topology.state().isCurrent()) {
            return false;
        }
        if (ports.observeOnly) {
            return topology.ownsShadow(this);
        }
        return topology.scheduler() == this;
    }

    private void scheduleBatch(Level level, GasRouteBatch batch) {
        if (!current()) {
            last = new Tick(Status.RETIRED, 0, 0, false, null);
            return;
        }

        if (last == null && !topology.endpointFaces().isEmpty()) {
            firstSource = ports.source(serviceOrder.lastSource(topology.endpointFaces().getFirst()));
            source = firstSource;
            if (rotation != null) {
                firstSource = rotation.restore(firstSource);
                source = firstSource;
            }
        }
        int pressureBudget = workLimit / PRESSURE_DIVISOR;
        if (workLimit < PRESSURE_DIVISOR && last != null && level.getGameTime() % PRESSURE_DIVISOR == 0 && (workLimit > 1 || knownRoutes.size() == 0)) {
            pressureBudget = 1;
        }
        int build = zones.build(level, last == null ? Math.min(1, pressureBudget) : pressureBudget);
        faceCount = ports.size();
        int pressureWork = 0;
        if (zones.complete()) {
            if (pressureModel == null) {
                pressureModel = new GasZonePressure(zones, ports, topology.pressureGauges());
            }
            pressureModel.tick(level, pressureBudget - build);
            pressureWork = pressureModel.lastWork();
        }
        int availableWork = workLimit - build - pressureWork;
        GasBranchRound round = null;
        int budget = availableWork;
        if (rotation != null && availableWork >= 8 && branchOrder.hasPeers()) {
            budget = Math.max(2, availableWork / 3);
            int routes = (availableWork - budget - 1) / 2;
            round = new GasBranchRound(routes, completedFirstSweep || rotation.visitedOutlets());
            branchOrder.seed(round, Math.max(1, routes - Math.max(1, routes / 3)));
        }
        collecting = round;
        int replay = Math.min(knownRoutes.size(), Math.max(1, budget / REPLAY_DIVISOR));
        int discovery = budget - replay;
        int work = 0;
        int expanded = 0;
        boolean continuation = true;
        Status status = Status.CONTINUING;
        boolean coldSlice = last == null && topology.pipePositions().size() > pressureBudget && pressureBudget > 0;
        if (budget > 0 && (deferred == null || knownRoutes.hasServed(deferred.target))) {
            Candidate due = knownRoutes.overdue(level.getGameTime(), !ports.observeOnly);
            if (due != null) {
                work++;
                if (settle(level, due, batch).status == Status.OFFER_CHANGED) {
                    knownRoutes.forgetSink(due);
                }
            }
        }
        if (rotation != null && faceCount > GasFixedManifold.MAX_FACES && work < discovery) {
            int outletBudget = last == null ? discovery - 1 : discovery - discovery / PRESSURE_DIVISOR;
            work += rotation.serve(level, batch, Math.max(0, outletBudget - work), knownRoutes, candidate -> {
                if (settle(level, candidate, batch).receipt != null) {
                    remember(candidate);
                }
            });
        }
        if (deferred != null && work < budget && batch.available() > 0 && batch.collision() == null) {
            Candidate next = deferred;
            deferred = null;
            work++;
            Settlement service = settle(level, next, batch);
            if (service.receipt != null) {
                remember(next);
            }
        }
        while (work < discovery && (work < Math.max(1, pressureBudget / 2) || !coldSlice || !batch.hasGrants()) && deferred == null && batch.available() > 0 && batch.collision() == null && continuation) {
            Tick step = advance(level, discovery - work, batch);
            work += step.work();
            expanded += step.expanded();
            continuation = step.continuation();
            status = step.status();
        }
        int alternates = Math.min(knownRoutes.alternateCount(), Math.min(replay, budget - work));
        int beforeReplay = replayAlternates(level, batch, (alternates + 1) / 2);
        work += beforeReplay;
        int count = Math.min(knownRoutes.size(), budget - work);
        for (int index = 0; index < count && batch.available() > 0 && batch.collision() == null; index++) {
            work++;
            Candidate candidate = knownRoutes.next();
            if (batch.tried(candidate)) {
                knownRoutes.keep(candidate);
                continue;
            }
            Settlement settlement = settle(level, candidate, batch);
            if (settlement.status != Status.OFFER_CHANGED) {
                knownRoutes.keep(candidate);
            }
        }
        work += replayAlternates(level, batch, Math.min(alternates - beforeReplay, budget - work));
        collecting = null;
        if (round != null && current()) {
            GasBranchRound offers = round;
            work += offers.dispatch(batch, availableWork - work, candidate -> settle(level, candidate, batch, quantumLimit, offers, null), (candidate, limit, sharing) -> {
                Settlement result = settle(level, candidate, batch, limit, null, sharing);
                if (result.receipt == null) {
                    return 0;
                }

                remember(candidate);
                return result.receipt.gas.getAmount();
            });
            while (!batch.hasGrants() && batch.collision() == null && work < availableWork && current()) {
                Tick step = advance(level, availableWork - work, batch);
                work += step.work();
                expanded += step.expanded();
                status = step.status();
                continuation = step.continuation();
                if (!continuation) {
                    break;
                }
            }
        }
        List<Receipt> grants = batch.grants();
        Collision collision = batch.collision();
        if (collision != null) {
            status = Status.GAS_COLLISION;
            if (!ports.observeOnly) {
                GasSolverProfiler.recordTransferStop(GasTransferStopReason.GAS_COLLISION);
                GasCollisionEvent.handleCollision(level, collision.pos(), collision.existing(), collision.incoming());
            }
        }
        else if (!grants.isEmpty()) {
            status = ports.observeOnly ? Status.PROPOSED : Status.COMMITTED;
        }
        else if (status == Status.QUEUED) {
            status = Status.CONTINUING;
        }
        last = new Tick(status, work, expanded, continuation, grants.isEmpty() ? null : grants.getFirst());
    }

    private int replayAlternates(Level level, GasRouteBatch batch, int limit) {
        int used = 0;
        while (used < limit && batch.available() > 0 && batch.collision() == null) {
            used++;
            Candidate candidate = knownRoutes.nextAlternate();
            if (!batch.tried(candidate)) {
                settle(level, candidate, batch);
            }
        }
        return used;
    }

    private Tick advance(Level level, int budget, @Nullable GasRouteBatch batch) {
        if (!current()) {
            return new Tick(Status.RETIRED, 0, 0, false, null);
        }
        if (faceCount == 0) {
            return new Tick(Status.NO_SOURCE, 0, 0, false, null);
        }

        int work = 0;
        int expanded = 0;
        Status status = Status.CONTINUING;
        Candidate ready = null;
        Settlement prepared = null;
        while (work < budget) {
            work++;
            if (search == null) {
                Port offer = read(level, source);
                sourceTanks = offer.tanks();
                GasStack gas = offer.offeredGas();
                GasNetworkPressureEndpoint drain = gas.isEmpty() ? null : offer.endpoint(level, gas, true);
                if (drain == null) {
                    source = source.after(sourceTanks, faceCount);
                    target = GasEndpointSlot.FIRST;
                    enteringSource = true;
                    if (source.equals(firstSource)) {
                        status = finishSweep();
                        break;
                    }
                    continue;
                }

                sawSource = true;
                if (enteringSource && serviceOrder != null) {
                    sweepStart = seeded.add(source) ? ports.afterTarget(serviceOrder.lastTarget(face(source))) : GasEndpointSlot.FIRST;
                    target = sweepStart;
                    enteringSource = false;
                }
                Port demand = read(level, target);
                targetTanks = demand.tanks();
                GasNetworkPressureEndpoint fill = demand.endpoint(level, gas, false);
                if (fill != null && drain.transferIdentity() != fill.transferIdentity()) {
                    sawSink = true;
                    boolean hasDevices = zones != null && (zones.complete() ? !zones.devices().isEmpty() : !topology.state().devicePositions().isEmpty());
                    if (hasDevices || GasPassiveQuantum.limit(drain.pressureState(), fill.pressureState(), quantumLimit) > 0) {
                        sawGradient = true;
                        if (zones == null && hot != null && hot.source.equals(source) && hot.target.equals(target) && GasStack.isSameGasSameComponents(hot.gas, gas)) {
                            ready = hot;
                        }
                        else {
                            searchingGas = gas.copyWithAmount(1);
                            if (batch != null && !ports.observeOnly && topology.state().hasCycle() && knownRoutes.knows(source, target)) {
                                search = new GasZoneRouteSearch(level, zones, face(source), face(target), searchingGas, drain, fill);
                            }
                            else if (batch != null) {
                                BlockFace root = face(source);
                                if (tree == null || !tree.rootedAt(root)) {
                                    tree = new GasRouteTree(level, zones, root);
                                }
                                tree.seek(face(target), searchingGas, drain, fill);
                                search = tree;
                            }
                            else if (zones != null) {
                                search = new GasZoneRouteSearch(level, zones, face(source), face(target), searchingGas, drain, fill);
                            }
                            else {
                                search = new GasOrdinaryPathSearch(topology.state(), limits, face(source).getPos(), face(target).getPos());
                            }
                            if (search.pending()) {
                                continue;
                            }
                        }
                    }
                }
            }
            else {
                expanded++;
                Path path = search.expand();
                if (path == null && search.pending()) {
                    if (exhaustiveSweep || !(search instanceof GasRouteTree routeTree) || !routeTree.hasLongDetour(SHORT_DETOUR_STEPS)) {
                        continue;
                    }
                    skippedDetour = true;
                }
                if (path != null) {
                    ready = new Candidate(source, target, searchingGas, path);
                    if (zones != null && search.pending()) {
                        prepared = settle(level, ready, batch);
                        if (prepared.status == Status.ENDPOINT_RESERVED) {
                            deferred = ready;
                        }
                        if (prepared.receipt == null && prepared.status != Status.ENDPOINT_RESERVED && (batch == null || batch.collision() == null)) {
                            break;
                        }
                    }
                }
            }

            sawPath |= ready != null;
            search = null;
            searchingGas = GasStack.EMPTY;
            target = target.after(targetTanks, faceCount);
            if (target.equals(sweepStart)) {
                enteringSource = true;
                source = source.after(sourceTanks, faceCount);
                if (source.equals(firstSource)) {
                    status = finishSweep();
                    break;
                }
            }
            if (ready != null) {
                break;
            }
        }

        Candidate previous = batch == null ? hot : null;
        Candidate candidate = ready == null ? previous : ready;
        if (candidate == null) {
            return new Tick(status, work, expanded, status == Status.CONTINUING, null);
        }

        Settlement settlement = prepared == null ? settle(level, candidate, batch) : prepared;
        if (settlement.receipt == null && previous != null && previous != candidate && topology.state().isCurrent()) {
            candidate = previous;
            settlement = settle(level, candidate, batch);
        }
        if (settlement.receipt != null) {
            if (batch == null) {
                hot = candidate;
            }
            else {
                remember(candidate);
            }
        }
        else if (candidate == hot) {
            hot = null;
        }
        return new Tick(settlement.status, work, expanded, status == Status.CONTINUING, settlement.receipt);
    }

    private void remember(Candidate candidate) {
        BlockFace from = face(candidate.source);
        serviceOrder.served(from, face(candidate.target));
        if (!topology.endpointFaces().isEmpty()) {
            serviceOrder.servedFrom(topology.endpointFaces().getFirst(), from);
        }
        knownRoutes.keep(candidate);
    }

    private Settlement settle(Level level, Candidate candidate, @Nullable GasRouteBatch batch) {
        if (collecting != null) {
            collecting.add(candidate);
            return new Settlement(Status.QUEUED, null);
        }
        if (batch != null && !batch.attempt(candidate)) {
            return new Settlement(Status.NO_REMAINDER, null);
        }

        return settle(level, candidate, batch, quantumLimit, null, null);
    }

    private Settlement settle(Level level, Candidate candidate, @Nullable GasRouteBatch batch, long limit, @Nullable GasBranchRound offers, @Nullable GasBranchRound sharing) {
        if (!current()) {
            return new Settlement(Status.RETIRED, null);
        }
        if (batch != null && batch.collision() != null) {
            return new Settlement(Status.GAS_COLLISION, null);
        }
        Port supply = read(level, candidate.source);
        GasStack gas = candidate.gas;
        GasNetworkPressureEndpoint drain = GasStack.isSameGasSameComponents(gas, supply.offeredGas()) ? supply.endpoint(level, gas, true) : null;
        GasNetworkPressureEndpoint fill = read(level, candidate.target).endpoint(level, gas, false);
        boolean attempted = false;
        if (drain != null && fill != null && drain.transferIdentity() != fill.transferIdentity()) {
            GasNetworkPressureEndpoint sourceEnd = drain;
            GasNetworkPressureEndpoint targetEnd = fill;
            long usedDrain = 0;
            long usedFill = 0;
            long maximum = quantumLimit;
            GasZoneRoute route = candidate.path.zoneRoute();
            long fair = quantumLimit;
            if (manifold != null && route != null && drain.pressureState().fixedPressure() && fill.pressureState().fixedPressure()) {
                long solo = route.quote(level, gas, drain, fill, quantumLimit).amount();
                if (solo > 0) {
                    fair = manifold.share(level.getGameTime(), drain, fill, route, solo, quantumLimit);
                }
            }
            if (batch != null && ports.observeOnly) {
                usedDrain = batch.drained(drain);
                usedFill = batch.filled(fill);
                maximum = Math.min(batch.available(), Math.min(drain.transferLimits().drainLimit() - usedDrain, fill.transferLimits().fillLimit() - usedFill));
                sourceEnd = batch.project(drain, gas);
                targetEnd = batch.project(fill, gas);
                if (sourceEnd == null || targetEnd == null || maximum <= 0) {
                    return new Settlement(Status.ENDPOINT_RESERVED, null);
                }
            }
            else if (batch != null) {
                maximum = batch.remainingQuota(drain, fill);
                if (maximum <= 0) {
                    return new Settlement(Status.ENDPOINT_RESERVED, null);
                }
            }
            double pressure = sourceEnd.pressureState().pressurePa();
            maximum = Math.min(limit, Math.min(maximum, fair));
            long cap;
            if (route != null) {
                Quote quote = route.quote(level, gas, sourceEnd, targetEnd, maximum, batch, sharing);
                cap = quote.amount();
                pressure = quote.pressure();
            }
            else {
                cap = GasPassiveQuantum.limit(drain.pressureState(), fill.pressureState(), Math.min(quantumLimit, candidate.path.capacity()));
            }
            long offered = Math.max(0, drain.maxQuantizedDrainAmount(gas, BoundedMath.saturatedAdd(usedDrain, cap)) - usedDrain);
            long accepted = Math.max(0, fill.maxQuantizedFillAmount(gas, BoundedMath.saturatedAdd(usedFill, offered), pressure) - usedFill);
            if (route != null && accepted == 0 && offered > 1) {
                Quote minimum = route.quote(level, gas, sourceEnd, targetEnd, 1, batch);
                pressure = minimum.pressure();
                long single = Math.max(0, drain.maxQuantizedDrainAmount(gas, BoundedMath.saturatedAdd(usedDrain, minimum.amount())) - usedDrain);
                accepted = Math.max(0, fill.maxQuantizedFillAmount(gas, BoundedMath.saturatedAdd(usedFill, single), pressure) - usedFill);
            }
            if (accepted > 0 && current()) {
                if (offers != null) {
                    long sourceLimit = Math.min(quantumLimit, drain.transferLimits().drainLimit());
                    long available = cap >= sourceLimit ? offered : drain.maxQuantizedDrainAmount(gas, sourceLimit);
                    offers.offer(candidate, drain.transferIdentity(), available, branchOrder.available(candidate, fill.transferIdentity()), accepted);
                    return new Settlement(Status.QUEUED, null);
                }
                Receipt quote = new Receipt(face(candidate.source), face(candidate.target), gas.copyWithAmount(accepted), candidate.path);
                if (batch != null && !batch.compatible(quote)) {
                    return new Settlement(Status.GAS_COLLISION, null);
                }
                attempted = true;
                if (ports.observeOnly) {
                    recordDelivery(level, batch, candidate, drain, fill, quote);
                    return new Settlement(Status.PROPOSED, quote);
                }
                long amount;
                if (batch == null) {
                    amount = new GasFlowIntegration().executeQuantum(level, gas, drain, fill, GasPressure.round(pressure), accepted);
                }
                else {
                    amount = batch.integration().executeQuantum(level, gas, drain, fill, GasPressure.round(pressure), accepted, received -> recordDelivery(level, batch, candidate, drain, fill, quote.withAmount(received)));
                }
                if (amount > 0) {
                    if (batch == null && route != null) {
                        route.served();
                    }
                    return new Settlement(Status.COMMITTED, quote.withAmount(amount));
                }
            }
        }
        if (!current()) {
            return new Settlement(Status.RETIRED, null);
        }
        if (rotation != null) {
            boolean unavailable = attempted || fill == null || fill.transferLimits().fillLimit() <= 0 || !fill.pressureState().fixedPressure() && fill.pressureState().currentAmount() >= fill.pressureState().maxAmount();
            if (offers != null || unavailable) {
                branchOrder.unavailable(candidate, unavailable);
            }
        }
        if (batch != null && (drain != null && batch.drained(drain) > 0 || fill != null && batch.filled(fill) > 0)) {
            return new Settlement(Status.NO_REMAINDER, null);
        }
        if (offers != null) {
            knownRoutes.discard(candidate);
        }
        if (attempted && GasSolverProfiler.isProfilingCurrentNetwork()) {
            GasSolverProfiler.recordTransferStop(GasTransferStopReason.HANDLER_REJECTED);
        }
        return new Settlement(attempted ? Status.HANDLER_REJECTED : Status.OFFER_CHANGED, null);
    }

    private void recordDelivery(Level level, GasRouteBatch batch, Candidate candidate, GasNetworkPressureEndpoint drain, GasNetworkPressureEndpoint fill, Receipt receipt) {
        batch.record(drain, fill, receipt, ports.observeOnly);
        if (rotation != null && faceCount >= 3) {
            branchOrder.received(candidate, fill.transferIdentity(), receipt.gas.getAmount());
        }
        if (manifold != null && drain.pressureState().fixedPressure() && fill.pressureState().fixedPressure()) {
            manifold.record(drain, fill, receipt.gas.getAmount());
        }
        if (rotation != null && faceCount > GasFixedManifold.MAX_FACES && candidate.target.face() < topology.endpointFaces().size()) {
            rotation.servedOutlet(receipt, level.getGameTime());
        }
        if (rotation != null && drain.pressureState().fixedPressure() && candidate.source.face() < topology.endpointFaces().size()) {
            rotation.supplied(candidate.source, receipt);
        }
        if (!ports.observeOnly && GasSolverProfiler.isProfilingCurrentNetwork()) {
            Observation observation = GasSolverProfiler.observation();
            if (observation != null) {
                observation.receipt(level, receipt);
            }
        }
        knownRoutes.served(candidate, level.getGameTime());
        receipt.path.zoneRoute().served();
    }

    private Status finishSweep() {
        completedFirstSweep = true;
        Status status = !sawSource ? Status.NO_SOURCE : !sawSink ? Status.NO_SINK : !sawGradient ? Status.AT_EQUILIBRIUM : sawPath ? Status.SWEEP_COMPLETE : Status.NO_PATH;
        sawSource = false;
        sawSink = false;
        sawGradient = false;
        sawPath = false;
        exhaustiveSweep = skippedDetour;
        skippedDetour = false;
        if (exhaustiveSweep) {
            return Status.CONTINUING;
        }
        return status;
    }

    private BlockFace face(GasEndpointSlot slot) {
        return ports.face(slot);
    }

    private Port read(Level level, GasEndpointSlot slot) {
        return ports.read(level, slot);
    }

    enum Status {
        CONTINUING, COMMITTED, PROPOSED, QUEUED, NO_SOURCE, NO_SINK, AT_EQUILIBRIUM, NO_PATH, SWEEP_COMPLETE, OFFER_CHANGED, HANDLER_REJECTED, RETIRED, NO_REMAINDER, ENDPOINT_RESERVED, GAS_COLLISION
    }

    record Tick(Status status, int work, int expanded, boolean continuation, @Nullable Receipt receipt) {}

    record Receipt(BlockFace source, BlockFace target, GasStack gas, Path path) {
        Receipt withAmount(long amount) {
            if (gas.getAmount() == amount) {
                return this;
            }

            return new Receipt(source, target, gas.copyWithAmount(amount), path);
        }
    }

    private record Settlement(Status status, @Nullable Receipt receipt) {}

    record Candidate(GasEndpointSlot source, GasEndpointSlot target, GasStack gas, Path path) {}
}
