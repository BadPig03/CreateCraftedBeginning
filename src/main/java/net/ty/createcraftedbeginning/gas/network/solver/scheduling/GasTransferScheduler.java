package net.ty.createcraftedbeginning.gas.network.solver.scheduling;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.events.GasCollisionEvent;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner.Port;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointSlot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasZonePorts;
import net.ty.createcraftedbeginning.gas.network.solver.pressure.GasZonePressure;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasPassiveQuantum;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasRouteSearch;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasRouteSearch.Path;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasRouteTree;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.PumpUse;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Quote;
import net.ty.createcraftedbeginning.gas.network.solver.scheduling.GasRouteBatch.Collision;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasFlowIntegration;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasTransferScheduler {
    public static final int WORK_PER_TICK = 128;
    public static final long GU_PER_TICK = 65536;
    private static final int PRESSURE_DIVISOR = 4;
    private static final int REPLAY_DIVISOR = 3;
    private static final int ASSISTANCE_DIVISOR = 3;
    private static final int ASSISTANCE_WORK_PER_ROUTE = 2;
    private static final int SHORT_DETOUR_STEPS = 8;

    private final Snapshot topology;
    private final Predicate<GasTransferScheduler> owner;
    private final Map<BlockPos, Long> limits;
    private final int workLimit;
    private final long quantumLimit;
    private int faceCount;
    private @Nullable GasRouteTree tree;
    private final GasZonePorts ports;
    private final @Nullable GasFixedManifold manifold;
    private @Nullable GasFixedManifold pumpManifold;
    private final GasAssistanceQueue pendingAssistance;
    private boolean servingAssistance;
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
    private final GasBranchOrder pumpOrder = new GasBranchOrder();
    private @Nullable GasBranchRound collecting;
    private @Nullable Candidate hot;
    private @Nullable Tick last;
    private List<Receipt> flows = List.of();

    public GasTransferScheduler(Snapshot topology, Map<BlockPos, Long> limits, int workLimit, long quantumLimit, @Nullable GasZoneLayout zones, @Nullable GasServiceOrder serviceOrder, Predicate<GasTransferScheduler> owner) {
        if (workLimit < 1 || quantumLimit < 1) {
            throw new IllegalArgumentException("Gas scheduling budgets require at least 1 work step per tick and 1 GU/t; got " + workLimit + " work steps per tick and " + quantumLimit + " GU/t.");
        }

        if (zones == null && !topology.state().devicePositions().isEmpty()) {
            throw new IllegalArgumentException("The capacity-only scheduler requires an ordinary network.");
        }

        this.topology = topology;
        this.owner = owner;
        this.limits = Map.copyOf(limits);
        this.workLimit = workLimit;
        pendingAssistance = new GasAssistanceQueue(workLimit);
        this.quantumLimit = quantumLimit;
        this.zones = zones;
        this.serviceOrder = serviceOrder;
        ports = new GasZonePorts(topology.endpointFaces(), topology.atmosphericFaces(), zones == null ? List.of() : zones.internalFaces());
        faceCount = ports.size();
        rotation = serviceOrder != null && zones != null && !topology.endpointFaces().isEmpty() ? new GasSupplyRotation(topology, zones, ports, serviceOrder) : null;
        manifold = serviceOrder != null && faceCount >= 3 && faceCount <= GasFixedManifold.MAX_FACES ? new GasFixedManifold() : null;
        pressureModel = zones == null || serviceOrder != null ? null : new GasZonePressure(zones, ports, topology.pressureGauges(), this::assistedPumpFlow);
    }

    private static void collectFlowDevices(List<Receipt> receipts, Set<BlockPos> changed) {
        for (Receipt receipt : receipts) {
            GasZoneRoute route = receipt.requireZoneRoute();
            for (int index = 0; index < route.deviceCount(); index++) {
                changed.add(route.device(index).position());
            }
        }
    }

    public void tick(Level level) {
        List<Receipt> previous = flows;
        boolean cold = last == null;
        GasServiceOrder order = serviceOrder;
        if (order == null) {
            last = advance(level, workLimit, null);
            if (pressureModel == null || !current()) {
                return;
            }

            Receipt receipt = last.receipt;
            flows = receipt == null ? List.of() : List.of(receipt);
            publishFlows(level, cold, previous);
            pressureModel.tick(level, workLimit);
            return;
        }

        GasZoneLayout layout = zones;
        if (layout == null) {
            throw new IllegalStateException("Batched gas scheduling requires a pressure zone layout.");
        }

        if (cold) {
            GasZonePressure.clearTelemetry(level, topology.pressureGauges());
        }
        GasRouteBatch batch = new GasRouteBatch(quantumLimit, layout, order);
        Throwable schedulingFailure = null;
        try {
            scheduleBatch(level, batch);
        }
        catch (RuntimeException | Error failure) {
            schedulingFailure = failure;
            throw failure;
        }
        finally {
            collecting = null;
            flows = batch.grants();
            try {
                if (current()) {
                    publishFlows(level, cold, previous);
                }
            }
            catch (RuntimeException | Error publicationFailure) {
                if (schedulingFailure == null) {
                    throw publicationFailure;
                }

                if (publicationFailure != schedulingFailure) {
                    schedulingFailure.addSuppressed(publicationFailure);
                }
            }
        }
    }

    public boolean hasZoneFlows() {
        return zones != null;
    }

    @Nullable
    public FlowState zoneFlow(BlockPos pos, Direction side) {
        GasZoneLayout layout = zones;
        if (layout == null || flows.isEmpty()) {
            return null;
        }

        long net = 0;
        GasStack gas = GasStack.EMPTY;
        BlockFace face = new BlockFace(pos, side);
        for (Receipt receipt : flows) {
            int direction = receipt.requireZoneRoute().direction(layout, face);
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

    @Nullable
    Tick last() {
        return last;
    }

    @Nullable
    GasZonePressure pressureModel() {
        return pressureModel;
    }

    private void publishFlows(Level level, boolean cold, List<Receipt> previous) {
        Set<BlockPos> changed = new HashSet<>();
        if (cold) {
            changed.addAll(topology.state().devicePositions());
        }
        if (cold || !previous.isEmpty() || !flows.isEmpty()) {
            changed.addAll(topology.meteredPipes());
        }
        collectFlowDevices(previous, changed);
        collectFlowDevices(flows, changed);
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
            if (!changedFlow && !cold) {
                continue;
            }

            behaviour.publishFlowTelemetry();
        }
    }

    private long assistedPumpFlow(BlockPos pos, GasStack gas) {
        long amount = 0;
        boolean assisted = false;
        for (Receipt receipt : flows) {
            if (!receipt.requireZoneRoute().hasPumpAt(pos) || !GasStack.isSameGasSameComponents(receipt.gas, gas)) {
                continue;
            }

            amount += receipt.gas.getAmount();
            assisted |= receipt.assisted;
        }
        if (!assisted) {
            return -1;
        }

        return amount;
    }

    private boolean current() {
        return topology.state().isCurrent() && owner.test(this);
    }

    private int assistanceWork(int budget) {
        if (faceCount <= 2 || !topology.state().hasAssistancePump()) {
            return 0;
        }

        int pending = pendingAssistance.size();
        if (budget <= 1) {
            return pending > 0 ? budget : 0;
        }

        int newOutlets = faceCount <= GasFixedManifold.MAX_FACES ? faceCount - 1 : 1;
        int cap = Math.min(budget - 1, Math.max(1, budget / ASSISTANCE_DIVISOR));
        return (int) Math.min(cap, (long) ASSISTANCE_WORK_PER_ROUTE * Math.max(newOutlets, pending));
    }

    private void scheduleBatch(Level level, GasRouteBatch batch) {
        if (!current()) {
            last = new Tick(Status.RETIRED, 0, 0, false, null);
            return;
        }

        GasZoneLayout layout = batch.layout();
        GasServiceOrder order = batch.serviceOrder();
        if (last == null && !topology.endpointFaces().isEmpty()) {
            firstSource = ports.source(order.lastSource(topology.endpointFaces().getFirst()));
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
        int build = layout.build(level, last == null ? Math.min(1, pressureBudget) : pressureBudget);
        faceCount = ports.size();
        int pressureWork = 0;
        if (layout.complete()) {
            if (pressureModel == null) {
                pressureModel = new GasZonePressure(layout, ports, topology.pressureGauges(), this::assistedPumpFlow);
            }
            pressureModel.tick(level, pressureBudget - build);
            pressureWork = pressureModel.lastWork();
        }
        int totalWork = workLimit - build - pressureWork;
        int pumpWork = assistanceWork(totalWork);
        int availableWork = totalWork - pumpWork;
        GasBranchRound round = null;
        int budget = availableWork;
        if (rotation != null && availableWork >= 8 && branchOrder.hasPeers()) {
            budget = Math.max(2, availableWork / 3);
            int routes = (availableWork - budget - 1) / 2;
            round = new GasBranchRound(routes, completedFirstSweep || rotation.visitedOutlets());
            branchOrder.seed(round, Math.max(1, routes - Math.max(1, routes / 3)));
        }
        collecting = round;
        int replay = Math.min(budget, Math.min(knownRoutes.size(), Math.max(1, budget / REPLAY_DIVISOR)));
        int discovery = budget - replay;
        int work = 0;
        int expanded = 0;
        boolean continuation = true;
        Status status = Status.CONTINUING;
        boolean coldSlice = last == null && topology.pipePositions().size() > pressureBudget && pressureBudget > 0;
        if (budget > 0 && (deferred == null || knownRoutes.hasServed(deferred.target))) {
            Candidate due = knownRoutes.overdue(level.getGameTime());
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
                    remember(candidate, order);
                }
            });
        }
        if (deferred != null && work < budget && batch.available() > 0 && batch.collision() == null) {
            Candidate next = deferred;
            deferred = null;
            work++;
            Settlement service = settle(level, next, batch);
            if (service.receipt != null) {
                remember(next, order);
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
            if (settlement.status == Status.OFFER_CHANGED) {
                continue;
            }

            knownRoutes.keep(candidate);
        }
        work += replayAlternates(level, batch, Math.min(alternates - beforeReplay, budget - work));
        collecting = null;
        if (round != null && current()) {
            work += dispatchRound(level, batch, round, availableWork - work);
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
        if (pendingAssistance.size() > 0 && work < totalWork && batch.available() > 0 && batch.collision() == null && current()) {
            servingAssistance = true;
            try {
                int remaining = totalWork - work;
                if (pendingAssistance.size() > 1 && remaining >= 2 * ASSISTANCE_WORK_PER_ROUTE) {
                    int pumpRoutes = Math.min(pendingAssistance.size(), remaining / ASSISTANCE_WORK_PER_ROUTE);
                    GasBranchRound pumps = new GasBranchRound(pumpRoutes, true);
                    for (int i = 0; i < pumpRoutes; i++) {
                        pumps.add(pendingAssistance.removeFirst());
                    }
                    work += dispatchRound(level, batch, pumps, remaining);
                }
                else {
                    Candidate candidate = pendingAssistance.removeFirst();
                    work++;
                    if (settle(level, candidate, batch, quantumLimit, null, null).receipt != null) {
                        remember(candidate, order);
                    }
                }
            }
            finally {
                servingAssistance = false;
            }
        }
        List<Receipt> grants = batch.grants();
        Collision collision = batch.collision();
        if (collision != null) {
            status = Status.GAS_COLLISION;
            GasCollisionEvent.handleCollision(level, collision.pos(), collision.existing(), collision.incoming());
        }
        else if (!grants.isEmpty()) {
            status = Status.COMMITTED;
        }
        else if (status == Status.QUEUED) {
            status = Status.CONTINUING;
        }
        last = new Tick(status, work, expanded, continuation, grants.isEmpty() ? null : grants.getFirst());
    }

    private int dispatchRound(Level level, GasRouteBatch batch, GasBranchRound offers, int work) {
        return offers.dispatch(batch, work, candidate -> settle(level, candidate, batch, quantumLimit, offers, null), (candidate, limit, sharing) -> {
            Settlement result = settle(level, candidate, batch, limit, null, sharing);
            if (result.receipt == null) {
                return 0;
            }

            remember(candidate, batch.serviceOrder());
            return result.receipt.gas.getAmount();
        });
    }

    private int replayAlternates(Level level, GasRouteBatch batch, int limit) {
        int used = 0;
        while (used < limit && batch.available() > 0 && batch.collision() == null) {
            used++;
            Candidate candidate = knownRoutes.nextAlternate();
            if (batch.tried(candidate)) {
                continue;
            }

            settle(level, candidate, batch);
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

        boolean cyclic = batch != null && topology.state().hasCycle();
        int work = 0;
        int expanded = 0;
        Status status = Status.CONTINUING;
        Candidate ready = null;
        Settlement prepared = null;
        while (work < budget) {
            work++;
            if (search == null) {
                Port offer = ports.prepareTransfer(level, source);
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
                if (enteringSource && batch != null) {
                    sweepStart = seeded.add(source) ? ports.afterTarget(batch.serviceOrder().lastTarget(face(source))) : GasEndpointSlot.FIRST;
                    target = sweepStart;
                    enteringSource = false;
                }
                Port demand = ports.prepareTransfer(level, target);
                targetTanks = demand.tanks();
                GasNetworkPressureEndpoint fill = demand.endpoint(level, gas, false);
                if (fill != null && drain.transferIdentity() != fill.transferIdentity()) {
                    sawSink = true;
                    boolean hasDevices = zones != null && (zones.complete() ? zones.deviceCount() > 0 : !topology.state().devicePositions().isEmpty());
                    if (hasDevices || GasPassiveQuantum.limit(drain.pressureState(), fill.pressureState(), quantumLimit) > 0) {
                        sawGradient = true;
                        if (zones == null && hot != null && hot.source.equals(source) && hot.target.equals(target) && GasStack.isSameGasSameComponents(hot.gas, gas)) {
                            ready = hot;
                        }
                        else {
                            searchingGas = gas.copyWithAmount(1);
                            if (batch != null && cyclic && knownRoutes.knows(source, target)) {
                                search = GasRouteSearch.zoned(level, batch.layout(), face(source), face(target), searchingGas, drain, fill);
                            }
                            else if (batch != null) {
                                BlockFace root = face(source);
                                GasRouteTree routeTree = tree;
                                if (routeTree == null || !routeTree.rootedAt(root)) {
                                    routeTree = new GasRouteTree(level, batch.layout(), root);
                                    tree = routeTree;
                                }
                                routeTree.seek(face(target), searchingGas, drain, fill);
                                search = routeTree;
                            }
                            else if (zones != null) {
                                search = GasRouteSearch.zoned(level, zones, face(source), face(target), searchingGas, drain, fill);
                            }
                            else {
                                search = GasRouteSearch.ordinary(topology.state(), limits, face(source).getPos(), face(target).getPos());
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
                    ready = knownRoutes.candidate(source, target, searchingGas, path, cyclic);
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
            settlement = settle(level, candidate, null);
        }
        if (settlement.receipt != null) {
            if (batch == null) {
                hot = candidate;
            }
            else {
                remember(candidate, batch.serviceOrder());
            }
        }
        else if (candidate == hot) {
            hot = null;
        }
        return new Tick(settlement.status, work, expanded, status == Status.CONTINUING, settlement.receipt);
    }

    private void remember(Candidate candidate, GasServiceOrder order) {
        BlockFace from = face(candidate.source);
        order.served(from, face(candidate.target));
        if (!topology.endpointFaces().isEmpty()) {
            order.servedFrom(topology.endpointFaces().getFirst(), from);
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

        Port supply = ports.prepareTransfer(level, candidate.source);
        GasStack gas = candidate.gas;
        GasNetworkPressureEndpoint drain = GasStack.isSameGasSameComponents(gas, supply.offeredGas()) ? supply.endpoint(level, gas, true) : null;
        GasNetworkPressureEndpoint fill = ports.prepareTransfer(level, candidate.target).endpoint(level, gas, false);
        boolean attempted = false;
        if (drain != null && fill != null && drain.transferIdentity() != fill.transferIdentity()) {
            long maximum = quantumLimit;
            GasZoneRoute route = candidate.path.zoneRoute();
            if (servingAssistance && batch != null && batch.receivedNaturally(drain)) {
                return new Settlement(Status.NO_REMAINDER, null);
            }

            PumpUse pumpUse = PumpUse.NATURAL;
            if (servingAssistance) {
                pumpUse = PumpUse.ASSIST;
            }
            else if (batch == null || faceCount <= 2) {
                pumpUse = PumpUse.BEST;
            }
            GasFixedManifold allocation = manifold;
            if (servingAssistance && manifold != null) {
                if (pumpManifold == null) {
                    pumpManifold = new GasFixedManifold();
                }
                allocation = pumpManifold;
            }
            long fair = quantumLimit;
            if (allocation != null && route != null && drain.pressureState().fixedPressure() && fill.pressureState().fixedPressure()) {
                long solo = route.quote(level, gas, drain, fill, quantumLimit, pumpUse).amount();
                if (solo > 0) {
                    fair = allocation.share(level.getGameTime(), drain, fill, route, solo, quantumLimit, servingAssistance);
                }
            }
            if (batch != null) {
                maximum = batch.remainingQuota(drain, fill);
                if (maximum <= 0) {
                    return new Settlement(Status.ENDPOINT_RESERVED, null);
                }
            }
            double pressure = drain.pressureState().pressurePa();
            maximum = Math.min(limit, Math.min(maximum, fair));
            long cap;
            long assistRoom = 0;
            boolean assisted = false;
            if (route != null) {
                Quote quote = route.quote(level, gas, drain, fill, maximum, batch, sharing, pumpUse);
                cap = quote.amount();
                pressure = quote.pressure();
                assistRoom = quote.assistRoom();
                assisted = quote.assisted();
            }
            else {
                cap = GasPassiveQuantum.limit(drain.pressureState(), fill.pressureState(), Math.min(quantumLimit, candidate.path.capacity()));
            }
            long offered = Math.max(0, drain.maxQuantizedDrainAmount(gas, cap));
            long accepted = Math.max(0, fill.maxQuantizedFillAmount(gas, offered, pressure));
            if (route != null && accepted == 0 && offered > 1) {
                Quote minimum = route.quote(level, gas, drain, fill, 1, batch, sharing, pumpUse);
                pressure = minimum.pressure();
                assisted = minimum.assisted();
                long single = Math.max(0, drain.maxQuantizedDrainAmount(gas, minimum.amount()));
                accepted = Math.max(0, fill.maxQuantizedFillAmount(gas, single, pressure));
            }
            boolean queued = pumpUse == PumpUse.NATURAL && assistRoom > accepted && current() && pendingAssistance.offer(fill.transferIdentity(), candidate);
            if (accepted > 0 && current()) {
                if (offers != null) {
                    if (route == null) {
                        throw new IllegalStateException("A branch offer requires a pressure zone route.");
                    }

                    long sourceLimit = Math.min(quantumLimit, drain.transferLimits().drainLimit());
                    long available = cap >= sourceLimit ? offered : drain.maxQuantizedDrainAmount(gas, sourceLimit);
                    GasBranchOrder order = assisted ? pumpOrder : branchOrder;
                    offers.offer(candidate, route.passes(), drain.transferIdentity(), available, order.available(candidate, fill.transferIdentity()), accepted);
                    return new Settlement(Status.QUEUED, null);
                }

                Receipt quote = new Receipt(face(candidate.source), face(candidate.target), gas.copyWithAmount(accepted), candidate.path, assisted);
                if (batch != null && !batch.compatible(quote)) {
                    return new Settlement(Status.GAS_COLLISION, null);
                }

                attempted = true;
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
            if (queued) {
                return new Settlement(Status.QUEUED, null);
            }
        }
        if (!current()) {
            return new Settlement(Status.RETIRED, null);
        }

        if (rotation != null) {
            boolean unavailable = attempted || fill == null || fill.transferLimits().fillLimit() <= 0 || !fill.pressureState().fixedPressure() && fill.pressureState().currentAmount() >= fill.pressureState().maxAmount();
            if (offers != null || unavailable) {
                GasBranchOrder order = servingAssistance ? pumpOrder : branchOrder;
                order.unavailable(candidate, unavailable);
            }
        }
        if (batch != null && (drain != null && batch.drained(drain) > 0 || fill != null && batch.filled(fill) > 0)) {
            return new Settlement(Status.NO_REMAINDER, null);
        }

        if (offers != null) {
            knownRoutes.discard(candidate);
        }
        if (attempted) {
            return new Settlement(Status.HANDLER_REJECTED, null);
        }

        return new Settlement(Status.OFFER_CHANGED, null);
    }

    private void recordDelivery(Level level, GasRouteBatch batch, Candidate candidate, GasNetworkPressureEndpoint drain, GasNetworkPressureEndpoint fill, Receipt receipt) {
        GasParallelRoutes parallel = candidate.parallel;
        batch.record(drain, fill, receipt, parallel == null ? List.of() : parallel.paths(), candidate.parallelBranch);
        if (receipt.assisted && servingAssistance && workLimit > 1 && current()) {
            pendingAssistance.offer(fill.transferIdentity(), candidate);
        }
        if (rotation != null && faceCount >= 3) {
            GasBranchOrder order = receipt.assisted ? pumpOrder : branchOrder;
            order.received(candidate, fill.transferIdentity(), receipt.gas.getAmount());
        }
        GasFixedManifold allocation = receipt.assisted ? pumpManifold : manifold;
        if (allocation != null && drain.pressureState().fixedPressure() && fill.pressureState().fixedPressure()) {
            allocation.record(drain, fill, receipt.gas.getAmount());
        }
        if (rotation != null && faceCount > GasFixedManifold.MAX_FACES && candidate.target.face() < topology.endpointFaces().size()) {
            rotation.servedOutlet(receipt, level.getGameTime());
        }
        if (rotation != null && drain.pressureState().fixedPressure() && candidate.source.face() < topology.endpointFaces().size()) {
            rotation.supplied(candidate.source, receipt);
        }
        knownRoutes.served(candidate, level.getGameTime());
    }

    private Status finishSweep() {
        completedFirstSweep = true;
        Status status = Status.SWEEP_COMPLETE;
        if (!sawSource) {
            status = Status.NO_SOURCE;
        }
        else if (!sawSink) {
            status = Status.NO_SINK;
        }
        else if (!sawGradient) {
            status = Status.AT_EQUILIBRIUM;
        }
        else if (!sawPath) {
            status = Status.NO_PATH;
        }
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

    enum Status {
        CONTINUING, COMMITTED, QUEUED, NO_SOURCE, NO_SINK, AT_EQUILIBRIUM, NO_PATH, SWEEP_COMPLETE, OFFER_CHANGED, HANDLER_REJECTED, RETIRED, NO_REMAINDER, ENDPOINT_RESERVED, GAS_COLLISION
    }

    record Tick(Status status, int work, int expanded, boolean continuation, @Nullable Receipt receipt) {}

    record Receipt(BlockFace source, BlockFace target, GasStack gas, Path path, boolean assisted) {
        GasZoneRoute requireZoneRoute() {
            GasZoneRoute route = path.zoneRoute();
            if (route == null) {
                throw new IllegalStateException("Gas transfer from " + source + " to " + target + " has no pressure zone route.");
            }

            return route;
        }

        Receipt withAmount(long amount) {
            if (gas.getAmount() == amount) {
                return this;
            }

            return new Receipt(source, target, gas.copyWithAmount(amount), path, assisted);
        }
    }

    private record Settlement(Status status, @Nullable Receipt receipt) {}

    record Candidate(GasEndpointSlot source, GasEndpointSlot target, GasStack gas, Path path, @Nullable GasParallelRoutes parallel, int parallelBranch) {}
}
