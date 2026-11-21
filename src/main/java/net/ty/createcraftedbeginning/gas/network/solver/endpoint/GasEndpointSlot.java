package net.ty.createcraftedbeginning.gas.network.solver.endpoint;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasEndpointSlot(int face, int tank) {
    public static final GasEndpointSlot FIRST = new GasEndpointSlot(0, 0);

    public GasEndpointSlot after(int tankCount, int faceCount) {
        if (tank + 1 < tankCount) {
            return new GasEndpointSlot(face, tank + 1);
        }
        if (face + 1 < faceCount) {
            return new GasEndpointSlot(face + 1, 0);
        }

        return FIRST;
    }
}
