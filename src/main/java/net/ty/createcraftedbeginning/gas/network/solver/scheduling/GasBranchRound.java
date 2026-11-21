package net.ty.createcraftedbeginning.gas.network.solver.scheduling;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Pass;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Sharing;
import net.ty.createcraftedbeginning.gas.network.solver.scheduling.GasBranchOrder.Outlet;
import net.ty.createcraftedbeginning.gas.network.solver.scheduling.GasTransferScheduler.Candidate;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasBranchRound implements Sharing {
    private static final int MAX_REFILL_ATTEMPTS = 4;
    private final int limit;
    private final boolean refillEnabled;
    private final Set<Candidate> candidates = new LinkedHashSet<>();
    private final Map<GasStack, GasPool> gases = new LinkedHashMap<>();
    private final Map<Object, Integer> competitors = new IdentityHashMap<>();

    GasBranchRound(int limit, boolean refillEnabled) {
        this.limit = limit;
        this.refillEnabled = refillEnabled;
    }

    @Override
    public int count(Object segment) {
        return Math.max(1, competitors.getOrDefault(segment, 1));
    }

    void add(Candidate candidate) {
        if (candidates.size() >= limit) {
            return;
        }

        candidates.add(candidate);
    }

    void offer(Candidate candidate, List<Pass> passes, Object source, long available, Outlet outlet, long accepted) {
        GasPool pool = gases.computeIfAbsent(candidate.gas(), ignored -> new GasPool());
        Supply supply = pool.sources.get(source);
        available = Math.max(available, accepted);
        if (supply == null) {
            supply = new Supply(source, available);
            pool.sources.put(source, supply);
            pool.supplies.add(supply);
            pool.available += available;
        }
        else if (available > supply.remaining) {
            pool.available += available - supply.remaining;
            supply.remaining = available;
        }
        Offer prior = supply.outlets.get(outlet);
        if (prior != null) {
            if (prior.alternate == null && !prior.passes.equals(passes)) {
                prior.alternate = candidate;
                prior.capacity += accepted;
                prior.target.room += accepted;
            }
            return;
        }

        GasBranchShare target = pool.targets.get(outlet);
        if (target == null) {
            target = new GasBranchShare(outlet.received(), outlet.turn(), 0);
            pool.targets.put(outlet, target);
            pool.shares.add(target);
        }
        target.room += accepted;
        Offer offer = new Offer(candidate, passes, outlet, accepted, target, supply);
        supply.outlets.put(outlet, offer);
        supply.offers.add(offer);
        countRoute(source, passes, 1);
    }

    int dispatch(GasRouteBatch batch, int budget, Consumer<Candidate> probe, Delivery delivery) {
        int work = 0;
        boolean delivered = batch.hasGrants();
        for (Candidate candidate : candidates) {
            probe.accept(candidate);
            work++;
        }
        for (GasPool pool : gases.values()) {
            GasBranchShare.divide(pool.shares, Math.min(pool.available, batch.available()));
            for (Supply supply : pool.supplies) {
                supply.plan(batch.available(), true);
                for (Offer offer : supply.offers) {
                    if (offer.grant > 0 && batch.available() > 0 && batch.collision() == null) {
                        long amount = delivery.send(offer.route, offer.grant, this);
                        delivered |= amount > 0;
                        offer.shortfall = offer.grant - amount;
                        if (refillEnabled && offer.shortfall > 0 && amount > 0 && offer.alternate == null) {
                            pool.shortOffers.add(offer);
                        }
                        offer.paid += amount;
                        offer.target.grant -= amount;
                        supply.remaining -= amount;
                        work++;
                    }
                    countRoute(supply.identity, offer.passes, -1);
                }
            }
        }
        for (GasPool pool : gases.values()) {
            for (Supply supply : pool.supplies) {
                if (work >= budget || batch.available() <= 0 || batch.collision() != null) {
                    return work;
                }
                if (supply.remaining <= 0) {
                    continue;
                }

                if (!delivered) {
                    for (Offer offer : supply.offers) {
                        if (work >= budget || batch.collision() != null || delivered) {
                            break;
                        }

                        long amount = delivery.send(offer.route, Math.min(offer.capacity, supply.remaining), null);
                        delivered = amount > 0;
                        offer.paid += amount;
                        offer.target.grant -= amount;
                        supply.remaining -= amount;
                        work++;
                    }
                }
                supply.plan(batch.available(), false);
                for (Offer offer : supply.offers) {
                    if (work >= budget || batch.available() <= 0 || batch.collision() != null) {
                        return work;
                    }
                    if (offer.grant <= 0) {
                        continue;
                    }

                    Candidate route = offer.alternate == null ? offer.route : offer.alternate;
                    long amount = delivery.send(route, offer.grant, null);
                    offer.target.grant -= amount;
                    supply.remaining -= amount;
                    work++;
                }
            }
        }
        int refillBudget = Math.min(MAX_REFILL_ATTEMPTS, budget - work);
        for (GasPool pool : gases.values()) {
            if (refillBudget <= 0 || batch.available() <= 0 || batch.collision() != null) {
                break;
            }

            int used = pool.refill(batch, refillBudget, delivery);
            refillBudget -= used;
            work += used;
        }
        return work;
    }

    private void countRoute(Object source, List<Pass> passes, int delta) {
        competitors.merge(source, delta, Integer::sum);
        for (Pass pass : passes) {
            competitors.merge(pass.vertexKey(), delta, Integer::sum);
            Object corridor = pass.corridorKey();
            if (corridor == null) {
                continue;
            }

            competitors.merge(corridor, delta, Integer::sum);
        }
    }

    @FunctionalInterface
    interface Delivery {
        long send(Candidate candidate, long limit, @Nullable GasBranchRound sharing);
    }

    private static final class GasPool {
        private final Map<Object, Supply> sources = new IdentityHashMap<>();
        private final List<Supply> supplies = new ArrayList<>();
        private final Map<Outlet, GasBranchShare> targets = new IdentityHashMap<>();
        private final List<GasBranchShare> shares = new ArrayList<>();
        private final List<Offer> shortOffers = new ArrayList<>();
        private long available;

        private int refill(GasRouteBatch batch, int budget, Delivery delivery) {
            int work = 0;
            while (work < budget && batch.available() > 0 && batch.collision() == null) {
                Offer next = null;
                for (Offer offer : shortOffers) {
                    if (offer.shortfall <= 0 || offer.target.grant <= 0 || offer.supply.remaining <= 0 || next != null && !offer.precedes(next)) {
                        continue;
                    }

                    next = offer;
                }
                if (next == null) {
                    break;
                }

                long limit = Math.min(next.shortfall, Math.min(next.supply.remaining, next.target.grant));
                next.shortfall = 0;
                long amount = delivery.send(next.route, Math.min(limit, batch.available()), null);
                next.supply.refillAttempts++;
                next.supply.remaining -= amount;
                next.target.grant -= amount;
                work++;
            }
            return work;
        }
    }

    private static final class Supply {
        private final Object identity;
        private final Map<Outlet, Offer> outlets = new IdentityHashMap<>();
        private final List<Offer> offers = new ArrayList<>();
        private long remaining;
        private int refillAttempts;

        private Supply(Object identity, long available) {
            this.identity = identity;
            remaining = available;
        }

        private void plan(long available, boolean fair) {
            boolean open = false;
            for (Offer offer : offers) {
                offer.received = offer.outlet.received();
                offer.turn = offer.outlet.turn();
                offer.room = offer.capacity - offer.paid;
                if (fair) {
                    offer.room = Math.min(offer.room, offer.target.grant);
                }
                else if (offer.shortfall > 0 && offer.alternate == null) {
                    offer.room = 0;
                }
                offer.grant = 0;
                open |= offer.room > 0;
            }
            if (!open) {
                return;
            }

            GasBranchShare.divide(offers, Math.min(remaining, available));
        }
    }

    private static final class Offer extends GasBranchShare {
        private final Candidate route;
        private final List<Pass> passes;
        private final Outlet outlet;
        private long capacity;
        private final GasBranchShare target;
        private final Supply supply;
        private @Nullable Candidate alternate;
        private long shortfall;
        private long paid;

        private Offer(Candidate route, List<Pass> passes, Outlet outlet, long capacity, GasBranchShare target, Supply supply) {
            super(outlet.received(), outlet.turn(), capacity);
            this.route = route;
            this.passes = passes;
            this.outlet = outlet;
            this.capacity = capacity;
            this.target = target;
            this.supply = supply;
        }

        private boolean precedes(Offer other) {
            if (supply.refillAttempts != other.supply.refillAttempts) {
                return supply.refillAttempts < other.supply.refillAttempts;
            }
            if (supply.remaining != other.supply.remaining) {
                return supply.remaining > other.supply.remaining;
            }
            if (outlet.received() != other.outlet.received()) {
                return outlet.received() < other.outlet.received();
            }

            return outlet.turn() < other.outlet.turn();
        }
    }
}
