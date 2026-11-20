package net.ty.createcraftedbeginning.gas.network.solver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressurePrediction;
import net.ty.createcraftedbeginning.gas.network.solver.GasTransferScheduler.Receipt;
import net.ty.createcraftedbeginning.gas.network.solver.GasZoneRoute.Pass;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PredictionAccess;
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
final class GasRouteBatch {
    private final Map<Object, Long> traffic = new IdentityHashMap<>();
    private final Map<Object, GasStack> occupants = new IdentityHashMap<>();
    private final Map<Object, Long> drained = new IdentityHashMap<>();
    private final Map<Object, Long> drainLimits = new IdentityHashMap<>();
    private final Map<Object, Long> fillLimits = new IdentityHashMap<>();
    private final Map<Object, Long> filled = new IdentityHashMap<>();
    private final Map<Object, GasStack> contents = new IdentityHashMap<>();
    private final Set<Object> affected = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Receipt> grants = new ArrayList<>();
    private final Set<Object> attempts = Collections.newSetFromMap(new IdentityHashMap<>());
    private @Nullable GasFlowIntegration integration;
    private long remaining;
    private @Nullable Collision collision;

    GasRouteBatch(long quantum) {
        remaining = quantum;
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

    long used(Object segment) {
        return traffic.getOrDefault(segment, 0L);
    }

    long drained(GasNetworkPressureEndpoint end) {
        return drained.getOrDefault(end.transferIdentity(), 0L);
    }

    long filled(GasNetworkPressureEndpoint end) {
        return filled.getOrDefault(end.transferIdentity(), 0L);
    }

    long remainingQuota(GasNetworkPressureEndpoint from, GasNetworkPressureEndpoint to) {
        long source = drainLimits.merge(from.transferIdentity(), from.transferLimits().drainLimit(), Math::max);
        long target = fillLimits.merge(to.transferIdentity(), to.transferLimits().fillLimit(), Math::max);
        return Math.min(remaining, Math.min(source - drained(from), target - filled(to)));
    }

    @Nullable
    GasNetworkPressureEndpoint project(GasNetworkPressureEndpoint end, GasStack gas) {
        if (end.pressureState().fixedPressure()) {
            return end;
        }

        Object id = end.transferIdentity();
        GasStack stored = contents.get(id);
        if (stored != null && !stored.isEmpty() && !GasStack.isSameGasSameComponents(stored, gas)) {
            return null;
        }
        for (PredictionAccess access : end.predictionAccesses()) {
            if (affected.contains(access.compartment().getPredictionGroup())) {
                return end.withAmountDelta(gas, filled(end) - drained(end), new GasPressurePrediction(contents));
            }
        }
        return end;
    }

    boolean compatible(Receipt receipt) {
        GasStack gas = receipt.gas();
        GasZoneRoute route = receipt.path().zoneRoute();
        for (Pass pass : route.passes()) {
            GasStack prior = occupants.get(pass.vertex());
            if (prior != null && !GasStack.isSameGasSameComponents(prior, gas)) {
                collision = new Collision(pass.vertex().pos, prior.copyWithAmount(used(pass.vertex())), gas);
                return false;
            }
        }

        return true;
    }

    void record(GasNetworkPressureEndpoint from, GasNetworkPressureEndpoint to, Receipt receipt, boolean projected) {
        GasStack gas = receipt.gas();
        GasZoneRoute route = receipt.path().zoneRoute();
        long amount = gas.getAmount();
        for (Pass pass : route.passes()) {
            occupants.putIfAbsent(pass.vertex(), gas);
            traffic.put(pass.vertex(), used(pass.vertex()) + amount);
            if (pass.next() != null) {
                traffic.put(pass.next().shared, used(pass.next().shared) + amount);
            }
        }
        drained.put(from.transferIdentity(), drained(from) + amount);
        filled.put(to.transferIdentity(), filled(to) + amount);
        if (projected) {
            remember(from, gas);
            remember(to, gas);
        }
        remaining -= amount;
        grants.add(receipt);
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

    private void remember(GasNetworkPressureEndpoint end, GasStack gas) {
        if (end.pressureState().fixedPressure()) {
            return;
        }

        long amount = end.pressureState().currentAmount() + (filled(end) - drained(end));
        contents.put(end.transferIdentity(), gas.copyWithAmount(amount));
        for (PredictionAccess access : end.predictionAccesses()) {
            affected.add(access.compartment().getPredictionGroup());
        }
    }

    record Collision(BlockPos pos, GasStack existing, GasStack incoming) {}
}
