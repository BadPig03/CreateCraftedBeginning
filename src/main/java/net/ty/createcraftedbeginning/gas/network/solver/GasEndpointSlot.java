package net.ty.createcraftedbeginning.gas.network.solver;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record GasEndpointSlot(int face, int tank) {
    static final GasEndpointSlot FIRST = new GasEndpointSlot(0, 0);

    GasEndpointSlot after(int tankCount, int faceCount) {
        if (tank + 1 < tankCount) {
            return new GasEndpointSlot(face, tank + 1);
        }
        if (face + 1 < faceCount) {
            return new GasEndpointSlot(face + 1, 0);
        }

        return FIRST;
    }
}
