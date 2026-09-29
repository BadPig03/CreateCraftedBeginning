package net.ty.createcraftedbeginning.gas.multiblock;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface OverpressureMultiblockPart {
    double getMultiblockOverpressureStress();

    void setMultiblockOverpressureStress(double stress);
}
