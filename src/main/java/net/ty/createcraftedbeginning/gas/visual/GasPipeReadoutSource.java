package net.ty.createcraftedbeginning.gas.visual;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasPipeReadoutSource {
    static @Nullable GasPipeReadout readout(@Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof GasPipeReadoutSource source)) {
            return null;
        }

        return source.getGasPipeReadout();
    }

    GasPipeReadout getGasPipeReadout();
}
