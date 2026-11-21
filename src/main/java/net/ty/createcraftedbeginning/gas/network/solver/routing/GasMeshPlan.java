package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasRouteSearch.Path;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Pass;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Sharing;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Usage;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasMeshPlan {
    private final List<Branch> branches = new ArrayList<>();
    private final Map<Object, Bottleneck> limits = new HashMap<>();
    private double totalWeight;
    private double smallestWeight = Double.POSITIVE_INFINITY;
    private boolean complete;

    void finish() {
        complete = true;
    }

    boolean complete() {
        return complete;
    }

    public int size() {
        return branches.size();
    }

    public Path path(int index) {
        return branches.get(index).path;
    }

    double fraction(int index) {
        return branches.get(index).cumulative / totalWeight;
    }

    public GasZoneRoute route(int index) {
        return branches.get(index).route;
    }

    public Allocation allocate(long maximum, Usage usage, @Nullable Sharing sharing, long tick) {
        long balanced = maximum;
        long[] room = new long[limits.size()];
        for (Bottleneck limit : limits.values()) {
            long remaining = Math.max(0, limit.capacity - usage.used(limit.key));
            if (sharing != null) {
                remaining = Math.ceilDiv(remaining, sharing.count(limit.key));
            }
            room[limit.index] = remaining;
            balanced = Math.min(balanced, (long) Math.ceil(remaining * (totalWeight / limit.weight)));
        }
        int count = branches.size();
        long[] amounts = new long[count];
        double phase = balanced * smallestWeight < totalWeight ? (Integer.toUnsignedLong(Integer.reverse((int) tick)) + 0.5) * 0x1.0p-32 : 0.5;
        long assigned = 0;
        long prior = 0;
        for (int i = 0; i < count; i++) {
            Branch branch = branches.get(i);
            long cumulative = i + 1 == count ? balanced : (long) Math.floor(balanced * (branch.cumulative / totalWeight) + phase);
            amounts[i] = branch.reserve(cumulative - prior, room);
            assigned += amounts[i];
            prior = cumulative;
        }
        for (int i = 0; i < count && assigned < maximum; i++) {
            long extra = branches.get(i).reserve(maximum - assigned, room);
            amounts[i] += extra;
            assigned += extra;
        }
        return new Allocation(this, amounts, assigned);
    }

    private void limit(List<Bottleneck> restricted, Object key, long capacity, double weight) {
        if (capacity == Long.MAX_VALUE) {
            return;
        }

        Bottleneck limit = limits.computeIfAbsent(key, ignored -> new Bottleneck(key, capacity, limits.size()));
        restricted.add(limit);
        limit.weight += weight;
    }

    final class BranchBuilder {
        private final GasZoneRoute.Builder route;
        private final List<Bottleneck> restricted = new ArrayList<>();
        private final double weight;
        private long capacity = Long.MAX_VALUE;

        BranchBuilder(Level level, double weight) {
            route = new GasZoneRoute.Builder(level);
            this.weight = weight;
        }

        void add(Pass pass) {
            route.add(pass);
            capacity = Math.min(capacity, Math.min(pass.vertexCapacity(), pass.corridorCapacity()));
            limit(restricted, pass.vertexKey(), pass.vertexCapacity(), weight);
            Object corridor = pass.corridorKey();
            if (corridor != null) {
                limit(restricted, corridor, pass.corridorCapacity(), weight);
            }
        }

        void finish(Pass last) {
            totalWeight += weight;
            smallestWeight = Math.min(smallestWeight, weight);
            GasZoneRoute frozen = new GasZoneRoute(route, capacity);
            Path path = new Path(last.position(), capacity, null, frozen);
            branches.add(new Branch(path, frozen, totalWeight, restricted));
        }
    }

    public static final class Allocation {
        private final GasMeshPlan plan;
        private final long[] amounts;
        private final long amount;

        private Allocation(GasMeshPlan plan, long[] amounts, long amount) {
            this.plan = plan;
            this.amounts = amounts;
            this.amount = amount;
        }

        public GasMeshPlan plan() {
            return plan;
        }

        public long amount() {
            return amount;
        }

        public Allocation limitTo(long received) {
            if (received == amount) {
                return this;
            }

            long[] parts = new long[amounts.length];
            long cumulative = 0;
            long assigned = 0;
            for (int index = 0; index < parts.length; index++) {
                cumulative += amounts[index];
                long next = Math.round(received * ((double) cumulative / amount));
                parts[index] = next - assigned;
                assigned = next;
            }
            return new Allocation(plan, parts, received);
        }

        public long part(int index) {
            return amounts[index];
        }
    }

    private record Branch(Path path, GasZoneRoute route, double cumulative, List<Bottleneck> limits) {
        long reserve(long amount, long[] room) {
            for (Bottleneck limit : limits) {
                amount = Math.min(amount, room[limit.index]);
            }
            for (Bottleneck limit : limits) {
                room[limit.index] -= amount;
            }
            return amount;
        }
    }

    private static final class Bottleneck {
        private final Object key;
        private final long capacity;
        private double weight;
        private final int index;

        private Bottleneck(Object key, long capacity, int index) {
            this.index = index;
            this.key = key;
            this.capacity = capacity;
        }
    }
}
