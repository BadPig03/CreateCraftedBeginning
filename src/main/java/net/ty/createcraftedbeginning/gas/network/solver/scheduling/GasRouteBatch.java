package net.ty.createcraftedbeginning.gas.network.solver.scheduling;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Pass;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Usage;
import net.ty.createcraftedbeginning.gas.network.solver.scheduling.GasTransferScheduler.Receipt;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasFlowIntegration;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasRouteBatch implements Usage {
    private final GasZoneLayout layout;
    private final GasServiceOrder serviceOrder;
    private final Map<Object, Long> traffic = new IdentityHashMap<>();
    private final Map<Object, GasStack> occupants = new IdentityHashMap<>();
    private final Map<Object, Long> drained = new IdentityHashMap<>();
    private final Map<Object, Long> drainLimits = new IdentityHashMap<>();
    private final Map<Object, Long> fillLimits = new IdentityHashMap<>();
    private final Map<Object, Long> filled = new IdentityHashMap<>();
    private final Set<Object> naturalSinks = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Receipt> grants = new ArrayList<>();
    private final Set<Object> attempts = Collections.newSetFromMap(new IdentityHashMap<>());
    private @Nullable GasFlowIntegration integration;
    private long remaining;
    private @Nullable Collision collision;

    GasRouteBatch(long quantum, GasZoneLayout layout, GasServiceOrder serviceOrder) {
        remaining = quantum;
        this.layout = layout;
        this.serviceOrder = serviceOrder;
    }

    @Override
    public long used(Object segment) {
        return traffic.getOrDefault(segment, 0L);
    }

    @Override
    public long drained(GasNetworkPressureEndpoint end) {
        return drained.getOrDefault(end.transferIdentity(), 0L);
    }

    @Override
    public long filled(GasNetworkPressureEndpoint end) {
        return filled.getOrDefault(end.transferIdentity(), 0L);
    }

    GasZoneLayout layout() {
        return layout;
    }

    GasServiceOrder serviceOrder() {
        return serviceOrder;
    }

    GasFlowIntegration integration() {
        if (integration == null) {
            integration = new GasFlowIntegration();
        }
        return integration;
    }

    boolean tried(Object route) {
        return attempts.contains(route);
    }

    boolean attempt(Object route) {
        return attempts.add(route);
    }

    long available() {
        return remaining;
    }

    long remainingQuota(GasNetworkPressureEndpoint from, GasNetworkPressureEndpoint to) {
        long source = drainLimits.merge(from.transferIdentity(), from.transferLimits().drainLimit(), Math::max);
        long target = fillLimits.merge(to.transferIdentity(), to.transferLimits().fillLimit(), Math::max);
        return Math.min(remaining, Math.min(source - drained(from), target - filled(to)));
    }

    boolean compatible(Receipt receipt) {
        GasStack gas = receipt.gas();
        GasZoneRoute route = receipt.requireZoneRoute();
        for (Pass pass : route.passes()) {
            Object vertex = pass.vertexKey();
            GasStack prior = occupants.get(vertex);
            if (prior != null && !GasStack.isSameGasSameComponents(prior, gas)) {
                collision = new Collision(pass.position(), prior.copyWithAmount(used(vertex)), gas);
                return false;
            }
        }

        return true;
    }

    void record(GasNetworkPressureEndpoint from, GasNetworkPressureEndpoint to, Receipt receipt) {
        GasStack gas = receipt.gas();
        GasZoneRoute route = receipt.requireZoneRoute();
        long amount = gas.getAmount();
        for (Pass pass : route.passes()) {
            Object vertex = pass.vertexKey();
            occupants.putIfAbsent(vertex, gas);
            traffic.put(vertex, used(vertex) + amount);
            Object corridor = pass.corridorKey();
            if (corridor == null) {
                continue;
            }

            traffic.put(corridor, used(corridor) + amount);
        }
        drained.put(from.transferIdentity(), drained(from) + amount);
        filled.put(to.transferIdentity(), filled(to) + amount);
        if (!receipt.assisted() && from.pressureState().pressurePa() > to.pressureState().pressurePa()) {
            naturalSinks.add(to.transferIdentity());
        }
        remaining -= amount;
        grants.add(receipt);
    }

    boolean receivedNaturally(GasNetworkPressureEndpoint endpoint) {
        return naturalSinks.contains(endpoint.transferIdentity());
    }

    boolean hasGrants() {
        return !grants.isEmpty();
    }

    List<Receipt> grants() {
        return List.copyOf(grants);
    }

    @Nullable
    Collision collision() {
        return collision;
    }

    record Collision(BlockPos pos, GasStack existing, GasStack incoming) {}
}
