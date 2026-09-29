package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRequestAmounts {
    private GasRequestAmounts() {
    }

    public static int scroll(int current, int step, double direction, boolean control, int maximum) {
        if (direction == 0 || step <= 0) {
            return current;
        }

        if (!control && direction > 0 && current == 1 && step > 1) {
            step--;
        }
        long next = (long) current + (direction > 0 ? step : -step);
        return (int) Math.clamp(next, 1L, maximum);
    }
}
