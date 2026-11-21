package net.ty.createcraftedbeginning.gas.network.solver.scheduling;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Pass;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasFixedManifold {
    static final int MAX_FACES = 8;
    private final Map<Pair, Set<Object>> learning = new HashMap<>();
    private final Map<Pair, Long> seen = new HashMap<>();
    private Map<Pair, Set<Object>> active = Map.of();
    private final Map<Pair, Long> paid = new HashMap<>();
    private long tick = Long.MIN_VALUE;

    long share(long now, GasNetworkPressureEndpoint from, GasNetworkPressureEndpoint to, GasZoneRoute route, long solo, long quantum, boolean assisted) {
        if (tick != now) {
            learning.keySet().removeIf(key -> now - seen.get(key) > MAX_FACES * 2);
            seen.keySet().retainAll(learning.keySet());
            active = new HashMap<>(learning);
            paid.clear();
            tick = now;
        }
        Pair pair = new Pair(from.transferIdentity(), to.transferIdentity());
        if (!seen.containsKey(pair) && seen.size() >= MAX_FACES * MAX_FACES) {
            return solo;
        }

        seen.put(pair, now);
        Set<Object> devices = learning.computeIfAbsent(pair, ignored -> new HashSet<>());
        for (int index = 0; index < route.deviceCount(); index++) {
            Pass pass = route.device(index);
            if (route.hasPumpAt(pass.position()) && !assisted) {
                continue;
            }

            devices.add(pass.vertexKey());
        }
        if (!active.containsKey(pair) || active.size() < 2) {
            return solo;
        }

        int outlets = 0;
        int inlets = 0;
        int sharingDevice = 0;
        for (Entry<Pair, Set<Object>> entry : active.entrySet()) {
            Pair other = entry.getKey();
            if (other.source == pair.source) {
                outlets++;
            }
            if (other.target == pair.target) {
                inlets++;
            }
            if (devices.isEmpty() || entry.getValue().stream().noneMatch(devices::contains)) {
                continue;
            }

            sharingDevice++;
        }
        long limit = Math.max(1, quantum / active.size());
        if (sharingDevice > 1) {
            limit = Math.min(limit, Math.max(1, solo / sharingDevice));
        }
        if (devices.isEmpty()) {
            double resistance = outlets * GasFlowResistance.endpointResistance(from.flowResistance().drainResistanceUnits()) + inlets * GasFlowResistance.endpointResistance(to.flowResistance().fillResistanceUnits());
            double head = from.pressureState().pressurePa() - to.pressureState().pressurePa();
            limit = Math.min(limit, Math.max(1, (long) (head / resistance)));
        }
        return Math.max(0, Math.min(solo, limit - paid.getOrDefault(pair, 0L)));
    }

    void record(GasNetworkPressureEndpoint from, GasNetworkPressureEndpoint to, long amount) {
        paid.merge(new Pair(from.transferIdentity(), to.transferIdentity()), amount, Long::sum);
    }

    private record Pair(Object source, Object target) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Pair(Object source1, Object target1) && source == source1 && target == target1;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(source) + System.identityHashCode(target);
        }
    }
}
