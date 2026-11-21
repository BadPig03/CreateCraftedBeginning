package net.ty.createcraftedbeginning.gas.network.solver.scheduling;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute;
import net.ty.createcraftedbeginning.gas.network.solver.scheduling.GasTransferScheduler.Candidate;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasAssistanceQueue {
    private final int limit;
    private final LinkedHashMap<Outlet, ArrayDeque<Candidate>> outlets = new LinkedHashMap<>();
    private int size;

    GasAssistanceQueue(int limit) {
        this.limit = limit;
    }

    private static boolean sameRoute(Candidate first, Candidate second) {
        if (!first.source().equals(second.source()) || !first.target().equals(second.target()) || !GasStack.isSameGasSameComponents(first.gas(), second.gas())) {
            return false;
        }

        GasZoneRoute route = first.path().zoneRoute();
        GasZoneRoute other = second.path().zoneRoute();
        return route == other || route != null && other != null && route.samePath(other);
    }

    int size() {
        return size;
    }

    boolean offer(Object outlet, Candidate candidate) {
        Outlet key = new Outlet(outlet);
        ArrayDeque<Candidate> routes = outlets.get(key);
        if (routes != null) {
            for (Candidate queued : routes) {
                if (sameRoute(queued, candidate)) {
                    return true;
                }
            }
        }
        if (size >= limit) {
            return false;
        }

        if (routes == null) {
            routes = new ArrayDeque<>();
            outlets.put(key, routes);
        }
        routes.addLast(candidate);
        size++;
        return true;
    }

    Candidate removeFirst() {
        Entry<Outlet, ArrayDeque<Candidate>> head = outlets.pollFirstEntry();
        if (head == null) {
            throw new IllegalStateException("No queued gas assistance route.");
        }

        ArrayDeque<Candidate> routes = head.getValue();
        Candidate candidate = routes.removeFirst();
        size--;
        if (!routes.isEmpty()) {
            outlets.putLast(head.getKey(), routes);
        }
        return candidate;
    }

    private record Outlet(Object identity) {
        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof Outlet outlet && outlet.identity == identity;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(identity);
        }
    }
}
