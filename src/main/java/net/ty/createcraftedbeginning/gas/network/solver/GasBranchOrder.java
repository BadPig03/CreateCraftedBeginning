package net.ty.createcraftedbeginning.gas.network.solver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.GasTransferScheduler.Candidate;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasBranchOrder {
    private static final int ROUTES_PER_OUTLET = 8;
    private static final int GAS_LANES = 16;
    private final LinkedHashMap<GasStack, Lane> gases = new LinkedHashMap<>();
    private int contested;
    private long turn;

    boolean hasPeers() {
        return contested > 0;
    }

    Outlet available(Candidate candidate, Object identity) {
        GasStack gas = candidate.gas();
        Lane lane = gases.get(gas);
        Outlet previous = lane == null ? null : lane.slots.get(candidate.target());
        if (previous != null && previous.identity != identity) {
            forget(candidate.target(), previous);
            lane = gases.get(gas);
        }
        if (lane == null) {
            if (gases.size() >= GAS_LANES && gases.pollFirstEntry().getValue().order.size() > 1) {
                contested--;
            }
            lane = new Lane(gas);
            gases.put(gas, lane);
        }
        Outlet outlet = lane.identities.get(identity);
        if (outlet == null) {
            outlet = new Outlet(lane, identity, ++turn);
            lane.identities.put(identity, outlet);
            lane.order.add(outlet);
            if (lane.order.size() == 2) {
                contested++;
            }
        }
        lane.slots.put(candidate.target(), outlet);
        outlet.faces.add(candidate.target());
        outlet.routes.put(candidate.source(), candidate);
        if (outlet.routes.size() > ROUTES_PER_OUTLET) {
            outlet.routes.pollFirstEntry();
        }
        return outlet;
    }

    void received(Candidate candidate, Object identity, long amount) {
        Outlet outlet = available(candidate, identity);
        Lane lane = outlet.lane;
        lane.order.remove(outlet);
        outlet.received += amount;
        outlet.turn = ++turn;
        lane.order.add(outlet);
        lane.admission = lane.order.getLast().received;
    }

    void unavailable(Candidate candidate, boolean cannotFill) {
        Lane lane = gases.get(candidate.gas());
        Outlet outlet = lane == null ? null : lane.slots.get(candidate.target());
        if (outlet == null) {
            return;
        }
        Candidate route = outlet.routes.get(candidate.source());
        if (!cannotFill) {
            if (route != candidate) {
                return;
            }
            outlet.routes.remove(candidate.source());
            if (!outlet.routes.isEmpty()) {
                return;
            }
        }
        forget(candidate.target(), outlet);
    }

    void seed(GasBranchRound round, int limit) {
        List<Outlet> selected = new ArrayList<>();
        int lanes = gases.size();
        int idle = 0;
        while (selected.size() < limit && idle < lanes) {
            Entry<GasStack, Lane> entry = gases.pollFirstEntry();
            Lane lane = entry.getValue();
            gases.putLast(entry.getKey(), lane);
            Outlet outlet = lane.order.pollFirst();
            if (outlet == null) {
                idle++;
                continue;
            }

            idle = 0;
            selected.add(outlet);
            round.add(outlet.nextRoute());
        }
        int remaining = limit - selected.size();
        for (Outlet outlet : selected) {
            outlet.lane.order.add(outlet);
            if (remaining == 0 || outlet.routes.size() < 2) {
                continue;
            }

            round.add(outlet.nextRoute());
            remaining--;
        }
    }

    private void forget(GasEndpointSlot slot, Outlet outlet) {
        Lane lane = outlet.lane;
        lane.slots.remove(slot);
        outlet.faces.remove(slot);
        outlet.routes.values().removeIf(route -> route.target().equals(slot));
        if (!outlet.routes.isEmpty()) {
            return;
        }

        for (GasEndpointSlot face : outlet.faces) {
            lane.slots.remove(face, outlet);
        }
        lane.order.remove(outlet);
        lane.identities.remove(outlet.identity);
        if (lane.order.size() == 1) {
            contested--;
        }
        if (lane.order.isEmpty()) {
            gases.remove(lane.gas);
            return;
        }

        lane.admission = lane.order.getLast().received;
    }

    static final class Outlet {
        private final Lane lane;
        private final Object identity;
        private final LinkedHashMap<GasEndpointSlot, Candidate> routes = new LinkedHashMap<>();
        private final Set<GasEndpointSlot> faces = new HashSet<>();
        private long received;
        private long turn;

        private Outlet(Lane lane, Object identity, long turn) {
            this.lane = lane;
            this.identity = identity;
            this.turn = turn;
            received = lane.admission;
        }

        long received() {
            return received;
        }

        long turn() {
            return turn;
        }

        private Candidate nextRoute() {
            Entry<GasEndpointSlot, Candidate> entry = routes.pollFirstEntry();
            routes.putLast(entry.getKey(), entry.getValue());
            return entry.getValue();
        }
    }

    private static final class Lane {
        private final GasStack gas;
        private final Map<GasEndpointSlot, Outlet> slots = new HashMap<>();
        private final Map<Object, Outlet> identities = new IdentityHashMap<>();
        private final TreeSet<Outlet> order = new TreeSet<>(Comparator.comparingLong(Outlet::received).thenComparingLong(Outlet::turn));
        private long admission;

        private Lane(GasStack gas) {
            this.gas = gas;
        }
    }
}
