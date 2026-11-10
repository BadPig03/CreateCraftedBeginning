package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BooleanSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface OpticalPowerConsumerBlockEntity {
    void applyNetworkAllocation(double fluxLm, BooleanSupplier allocationValidity);
}
