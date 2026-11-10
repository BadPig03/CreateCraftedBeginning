package net.ty.createcraftedbeginning.gas.visual;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasStorageReadoutSource {
    static GasStorageReadout readout(@Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof GasStorageReadoutSource source)) {
            return GasStorageReadout.AMOUNT_ONLY;
        }

        return source.getGasStorageReadout();
    }

    GasStorageReadout getGasStorageReadout();
}
