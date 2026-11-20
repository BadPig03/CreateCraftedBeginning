package net.ty.createcraftedbeginning.gas.network.solver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.solver.GasTransferScheduler.Candidate;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasServiceRoutes {
    private static final int BRANCHES_PER_SUPPLY = 8;
    private static final int ROTATION_HANDOFF_BRANCHES = 3;
    private final LinkedHashMap<GasEndpointSlot, LinkedHashMap<GasEndpointSlot, Candidate>> sources = new LinkedHashMap<>();
    private final LinkedHashMap<GasEndpointSlot, Service> sinks = new LinkedHashMap<>();
    private final LinkedHashSet<GasEndpointSlot> alternates = new LinkedHashSet<>();
    private int size;

    int size() {
        return size;
    }

    boolean hasServed(GasEndpointSlot target) {
        return sinks.containsKey(target);
    }

    boolean hasBranchCoverage(GasEndpointSlot source) {
        LinkedHashMap<GasEndpointSlot, Candidate> routes = sources.get(source);
        return routes != null && routes.size() >= ROTATION_HANDOFF_BRANCHES;
    }

    boolean knows(GasEndpointSlot source, GasEndpointSlot target) {
        LinkedHashMap<GasEndpointSlot, Candidate> routes = sources.get(source);
        return routes != null && routes.containsKey(target);
    }

    void served(Candidate candidate, long tick) {
        Service service = sinks.get(candidate.target());
        if (service == null) {
            service = new Service();
            alternates.add(candidate.target());
        }
        service.route = candidate;
        service.turn = tick;
        sinks.putLast(candidate.target(), service);
    }

    @Nullable
    Candidate overdue(long tick, boolean live) {
        Entry<GasEndpointSlot, Service> oldest = sinks.firstEntry();
        if (oldest == null || tick - oldest.getValue().turn < Math.max(2, live ? sinks.size() / 2 : sinks.size() - 1)) {
            return null;
        }

        Service service = oldest.getValue();
        service.turn = tick;
        sinks.putLast(oldest.getKey(), service);
        return service.route;
    }

    int alternateCount() {
        return alternates.size();
    }

    Candidate nextAlternate() {
        GasEndpointSlot target = alternates.removeFirst();
        alternates.addLast(target);
        return sinks.get(target).route;
    }

    void forgetSink(Candidate candidate) {
        sinks.remove(candidate.target());
        alternates.remove(candidate.target());
    }

    void discard(Candidate candidate) {
        LinkedHashMap<GasEndpointSlot, Candidate> routes = sources.get(candidate.source());
        if (routes != null && routes.get(candidate.target()) == candidate) {
            routes.remove(candidate.target());
            size--;
            if (routes.isEmpty()) {
                sources.remove(candidate.source());
            }
        }
        Service sink = sinks.get(candidate.target());
        if (sink == null || sink.route != candidate) {
            return;
        }

        forgetSink(candidate);
    }

    void keep(Candidate candidate) {
        LinkedHashMap<GasEndpointSlot, Candidate> routes = sources.get(candidate.source());
        if (routes == null) {
            routes = new LinkedHashMap<>();
            sources.put(candidate.source(), routes);
        }
        if (routes.putLast(candidate.target(), candidate) == null) {
            size++;
        }
        if (routes.size() > BRANCHES_PER_SUPPLY) {
            routes.pollFirstEntry();
            size--;
        }
    }

    Candidate next() {
        Entry<GasEndpointSlot, LinkedHashMap<GasEndpointSlot, Candidate>> entry = sources.pollFirstEntry();
        LinkedHashMap<GasEndpointSlot, Candidate> routes = entry.getValue();
        Candidate next = routes.pollFirstEntry().getValue();
        if (!routes.isEmpty()) {
            sources.putLast(entry.getKey(), routes);
        }
        size--;
        return next;
    }

    private static final class Service {
        private Candidate route;
        private long turn;
    }
}
