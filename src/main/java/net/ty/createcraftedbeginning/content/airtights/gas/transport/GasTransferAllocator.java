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

        List<Integer> uncappedIndices = new ArrayList<>(capacities.length);
        for (int targetIndex = 0; targetIndex < capacities.length; targetIndex++) {
            if (capacities[targetIndex] <= 0) {
                continue;
            }

            uncappedIndices.add(targetIndex);
        }
        if (uncappedIndices.isEmpty()) {
            return new Result(allocations, cursor);
        }

        long remainingAmount = availableAmount;
        while (!uncappedIndices.isEmpty() && remainingAmount > 0) {
            long equalShare = remainingAmount / uncappedIndices.size();
            boolean removedCappedTarget = false;
            for (int uncappedIndex = uncappedIndices.size() - 1; uncappedIndex >= 0; uncappedIndex--) {
                int targetIndex = uncappedIndices.get(uncappedIndex);
                long targetCapacity = capacities[targetIndex];
                if (targetCapacity > equalShare) {
                    continue;
                }

                allocations[targetIndex] = targetCapacity;
                remainingAmount -= targetCapacity;
                uncappedIndices.remove(uncappedIndex);
                removedCappedTarget = true;
            }
            if (!removedCappedTarget) {
                break;
            }
        }

        int nextCursor = cursor;
        if (!uncappedIndices.isEmpty() && remainingAmount > 0) {
            long equalShare = remainingAmount / uncappedIndices.size();
            long remainderAmount = remainingAmount % uncappedIndices.size();
            if (equalShare > 0) {
                for (int targetIndex : uncappedIndices) {
                    allocations[targetIndex] = equalShare;
                }
            }

            int remainderStartIndex = Math.floorMod(cursor, uncappedIndices.size());
            for (int offset = 0; offset < remainderAmount; offset++) {
                int targetIndex = uncappedIndices.get((remainderStartIndex + offset) % uncappedIndices.size());
                allocations[targetIndex]++;
            }
            if (remainderAmount <= 0) {
                return new Result(allocations, nextCursor);
            }

            nextCursor = (remainderStartIndex + (int) remainderAmount) % uncappedIndices.size();
        }

        return new Result(allocations, nextCursor);
    }

    public record Result(long[] allocations, int nextCursor) {}
}
