package net.ty.createcraftedbeginning.gas.network.solver.scheduling;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner.Port;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointSlot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasZonePorts;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasRouteSearch.Path;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasRouteTree;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout;
import net.ty.createcraftedbeginning.gas.network.solver.scheduling.GasTransferScheduler.Candidate;
import net.ty.createcraftedbeginning.gas.network.solver.scheduling.GasTransferScheduler.Receipt;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasSupplyRotation {
    private static final int MAX_SUPPLIES = 8;
    private static final int SCAN_PER_TICK = 8;
    private static final int ATTEMPTS_PER_SUPPLY = 2;
    private static final int OUTLET_RETRY_WORK = 4;
    private final Snapshot topology;
    private final GasZoneLayout zones;
    private final GasZonePorts ports;
    private final GasServiceOrder serviceOrder;
    private final BlockFace anchor;
    private final LinkedHashSet<GasEndpointSlot> supplies = new LinkedHashSet<>();
    private @Nullable GasEndpointSlot nextSupply;
    private @Nullable GasEndpointSlot retrySupply;
    private int supplyTurn;
    private boolean scanned;
    private boolean hasStorageSupply;
    private boolean primeOutlet;
    private boolean priorityTurn;
    private boolean retryingOutlet;
    private GasEndpointSlot steadySource = GasEndpointSlot.FIRST;
    private @Nullable GasEndpointSlot steadyTarget;
    private @Nullable GasRouteTree steadyTree;
    private GasStack steadyGas = GasStack.EMPTY;
    private GasEndpointSlot scan = GasEndpointSlot.FIRST;
    private GasEndpointSlot nextOutlet = GasEndpointSlot.FIRST;
    private int unvisitedOutletFaces;

    GasSupplyRotation(Snapshot topology, GasZoneLayout zones, GasZonePorts ports, GasServiceOrder serviceOrder) {
        this.topology = topology;
        this.zones = zones;
        this.ports = ports;
        this.serviceOrder = serviceOrder;
        anchor = topology.endpointFaces().getFirst();
        unvisitedOutletFaces = topology.endpointFaces().size();
    }

    private static boolean needsTurn(GasNetworkPressureEndpoint source, GasServiceRoutes routes, GasEndpointSlot slot) {
        return source.pressureState().fixedPressure() || source.transferLimits().fillLimit() == 0 && !routes.hasBranchCoverage(slot);
    }

    GasEndpointSlot restore(GasEndpointSlot fallback) {
        BlockFace steady = serviceOrder.supply(anchor);
        if (steady == null) {
            return fallback;
        }

        steadySource = ports.source(steady);
        nextOutlet = ports.source(serviceOrder.nextOutlet(anchor));
        primeOutlet = true;
        supplies.add(steadySource);
        return steadySource;
    }

    boolean visitedOutlets() {
        return unvisitedOutletFaces == 0;
    }

    void servedOutlet(Receipt receipt, long tick) {
        serviceOrder.servedOutlet(anchor, receipt.target(), tick);
    }

    void supplied(GasEndpointSlot slot, Receipt receipt) {
        serviceOrder.supply(anchor, receipt.source(), receipt.target());
        if (supplies.size() >= MAX_SUPPLIES) {
            return;
        }

        supplies.add(slot);
    }

    int serve(Level level, GasRouteBatch batch, int budget, GasServiceRoutes routes, Consumer<Candidate> deliver) {
        int work = 0;
        boolean startWithDiscovery = supplies.isEmpty() && routes.size() == 0;
        int scanLimit = Math.min(scanned && hasStorageSupply ? 1 : SCAN_PER_TICK, budget / 4);
        while (work < scanLimit && nextSupply == null) {
            work++;
            GasEndpointSlot inspected = scan;
            Port port = ports.prepareTransfer(level, inspected);
            scan = inspected.after(port.tanks(), topology.endpointFaces().size());
            scanned |= scan.equals(GasEndpointSlot.FIRST);
            if (supplies.contains(inspected)) {
                continue;
            }

            GasStack gas = port.offeredGas();
            GasNetworkPressureEndpoint drain = gas.isEmpty() ? null : port.endpoint(level, gas, true);
            if (drain == null) {
                continue;
            }

            if (!needsTurn(drain, routes, inspected)) {
                rememberRetrySupply(inspected, drain);
                continue;
            }

            if (drain.pressureState().fixedPressure()) {
                startWithDiscovery = false;
            }
            else {
                hasStorageSupply = true;
            }
            if (supplies.size() >= MAX_SUPPLIES) {
                nextSupply = inspected;
                continue;
            }

            supplies.add(inspected);
        }
        if (retryingOutlet) {
            budget = Math.min(budget, work + OUTLET_RETRY_WORK);
        }
        while (work < budget && batch.available() > 0 && batch.collision() == null && topology.state().isCurrent()) {
            work++;
            GasEndpointSlot target = steadyTarget;
            GasRouteTree tree = steadyTree;
            if (target == null) {
                GasEndpointSlot retry = null;
                if (supplyTurn == 0) {
                    if (nextSupply != null && (supplies.size() < MAX_SUPPLIES || supplies.remove(steadySource))) {
                        supplies.addLast(nextSupply);
                        nextSupply = null;
                    }
                    if (startWithDiscovery) {
                        break;
                    }

                    if (supplies.isEmpty()) {
                        GasEndpointSlot standby = retrySupply;
                        if (standby == null) {
                            break;
                        }

                        retry = retryOutlet(routes, level.getGameTime());
                        if (retry == null) {
                            break;
                        }

                        steadySource = standby;
                        budget = Math.min(budget, work + OUTLET_RETRY_WORK - 1);
                    }
                    else {
                        steadySource = supplies.removeFirst();
                        supplies.addLast(steadySource);
                    }
                }
                if (retry == null) {
                    supplyTurn = (supplyTurn + 1) % ATTEMPTS_PER_SUPPLY;
                }
                Port offer = ports.prepareTransfer(level, steadySource);
                GasStack gas = offer.offeredGas();
                GasNetworkPressureEndpoint drain = gas.isEmpty() ? null : offer.endpoint(level, gas, true);
                if (drain == null || retry != null && drain.transferLimits().fillLimit() != 0) {
                    if (steadySource.equals(retrySupply)) {
                        retrySupply = null;
                    }
                    supplies.remove(steadySource);
                    supplyTurn = 0;
                    continue;
                }

                if (retry == null && !needsTurn(drain, routes, steadySource)) {
                    rememberRetrySupply(steadySource, drain);
                    supplies.remove(steadySource);
                    supplyTurn = 0;
                    continue;
                }

                GasEndpointSlot outlet = nextOutlet;
                boolean resumeQueue = true;
                if (retry != null) {
                    outlet = retry;
                    resumeQueue = false;
                }
                else if (primeOutlet) {
                    outlet = ports.source(serviceOrder.nearby(ports.face(steadySource)));
                    primeOutlet = false;
                    resumeQueue = false;
                }
                else {
                    priorityTurn = !priorityTurn;
                    BlockFace due = priorityTurn ? serviceOrder.dueOutlet(anchor, level.getGameTime()) : null;
                    if (due != null) {
                        GasEndpointSlot priority = ports.source(due);
                        if (ports.face(priority).equals(due)) {
                            outlet = priority;
                            resumeQueue = false;
                        }
                    }
                }
                Port port = ports.prepareTransfer(level, outlet);
                if (resumeQueue) {
                    nextOutlet = outlet.after(port.tanks(), ports.size());
                    if (unvisitedOutletFaces > 0 && outlet.face() < topology.endpointFaces().size() && nextOutlet.tank() == 0) {
                        unvisitedOutletFaces--;
                    }
                    serviceOrder.nextOutlet(anchor, ports.face(nextOutlet));
                }
                GasNetworkPressureEndpoint fill = port.endpoint(level, gas, false);
                if (fill == null || drain.transferIdentity() == fill.transferIdentity()) {
                    continue;
                }

                BlockFace root = ports.face(steadySource);
                if (tree == null) {
                    tree = new GasRouteTree(level, zones, root);
                    steadyTree = tree;
                }
                tree.seek(root, ports.face(outlet), gas, drain, fill);
                steadyTarget = outlet;
                steadyGas = gas.copyWithAmount(1);
                retryingOutlet = retry != null;
                continue;
            }

            if (tree == null) {
                throw new IllegalStateException("An active supply outlet search requires a route tree; target " + target + '.');
            }

            Path path = tree.expand();
            if (path != null) {
                Candidate candidate = routes.candidate(steadySource, target, steadyGas, path, topology.state().hasCycle());
                deliver.accept(candidate);
            }
            if (path == null && tree.pending()) {
                continue;
            }

            steadyTarget = null;
            retryingOutlet = false;
        }
        return work;
    }

    private void rememberRetrySupply(GasEndpointSlot slot, GasNetworkPressureEndpoint drain) {
        if (drain.transferLimits().fillLimit() != 0) {
            return;
        }

        retrySupply = slot;
    }

    @Nullable
    private GasEndpointSlot retryOutlet(GasServiceRoutes routes, long tick) {
        BlockFace due = serviceOrder.dueOutlet(anchor, tick);
        if (due == null) {
            return null;
        }

        GasEndpointSlot outlet = ports.source(due);
        if (!ports.face(outlet).equals(due) || routes.hasServed(outlet)) {
            return null;
        }

        return outlet;
    }
}
