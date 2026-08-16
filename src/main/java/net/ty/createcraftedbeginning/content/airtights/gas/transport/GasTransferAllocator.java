package net.ty.createcraftedbeginning.content.airtights.gas.transport;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasTransferAllocator {
    private GasTransferAllocator() {
    }

    public static Result allocate(long availableAmount, long[] capacities, int cursor) {
        long[] allocations = new long[capacities.length];
        if (availableAmount <= 0 || capacities.length == 0) {
            return new Result(allocations, cursor);
        }

        List<Integer> uncapped = new ArrayList<>(capacities.length);
        for (int i = 0; i < capacities.length; i++) {
            if (capacities[i] <= 0) {
                continue;
            }

            uncapped.add(i);
        }
        if (uncapped.isEmpty()) {
            return new Result(allocations, cursor);
        }

        long remaining = availableAmount;
        while (!uncapped.isEmpty() && remaining > 0) {
            long equalShare = remaining / uncapped.size();
            boolean removedTarget = false;
            for (int i = uncapped.size() - 1; i >= 0; i--) {
                int index = uncapped.get(i);
                long capacity = capacities[index];
                if (capacity > equalShare) {
                    continue;
                }

                allocations[index] = capacity;
                remaining -= capacity;
                uncapped.remove(i);
                removedTarget = true;
            }
            if (!removedTarget) {
                break;
            }
        }

        int nextCursor = cursor;
        if (!uncapped.isEmpty() && remaining > 0) {
            long equalShare = remaining / uncapped.size();
            long remainder = remaining % uncapped.size();
            if (equalShare > 0) {
                for (int index : uncapped) {
                    allocations[index] = equalShare;
                }
            }

            int remainderStart = Math.floorMod(cursor, uncapped.size());
            for (int i = 0; i < remainder; i++) {
                int index = uncapped.get((remainderStart + i) % uncapped.size());
                allocations[index]++;
            }
            if (remainder <= 0) {
                return new Result(allocations, nextCursor);
            }

            nextCursor = (remainderStart + (int) remainder) % uncapped.size();
        }

        return new Result(allocations, nextCursor);
    }

    public record Result(long[] allocations, int nextCursor) {}
}
