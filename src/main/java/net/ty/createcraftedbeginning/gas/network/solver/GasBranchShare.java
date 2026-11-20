package net.ty.createcraftedbeginning.gas.network.solver;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class GasBranchShare {
    private static final int CATCH_UP_DIVISOR = 4;
    long received;
    long turn;
    long room;
    long grant;

    GasBranchShare(long received, long turn, long room) {
        this.received = received;
        this.turn = turn;
        this.room = room;
    }

    static void divide(List<? extends GasBranchShare> branches, long available) {
        branches.sort(Comparator.comparingLong((GasBranchShare branch) -> branch.received).thenComparingLong(branch -> branch.turn));
        long base = branches.getFirst().received;
        long correction = Math.max(1, available / branches.size() / CATCH_UP_DIVISOR);
        long low = 0;
        long high = Math.min(correction, Long.MAX_VALUE - available) + available;
        while (low < high) {
            long gap = high - low;
            long level = low + gap / 2 + gap % 2;
            long used = 0;
            for (GasBranchShare branch : branches) {
                used += branch.share(base, level, correction);
                if (used <= available) {
                    continue;
                }

                break;
            }
            if (used > available) {
                high = level - 1;
            }
            else {
                low = level;
            }
        }
        for (GasBranchShare branch : branches) {
            branch.grant = branch.share(base, low, correction);
            available -= branch.grant;
        }
        for (GasBranchShare branch : branches) {
            if (available == 0 || branch.grant >= branch.room || branch.lead(base, correction) > low) {
                continue;
            }

            branch.grant++;
            available--;
        }
    }

    private long share(long base, long level, long correction) {
        return Math.clamp(level - lead(base, correction), 0, room);
    }

    private long lead(long base, long correction) {
        return Math.clamp(received - base - 1, 0, correction);
    }
}
