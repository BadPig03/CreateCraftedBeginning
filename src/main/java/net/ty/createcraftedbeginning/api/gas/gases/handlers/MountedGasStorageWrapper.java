package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import com.google.common.collect.ImmutableMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MountedGasStorageWrapper extends CombinedGasTankWrapper {
    public final ImmutableMap<BlockPos, MountedGasStorage> storages;

    /**
     * Creates a new {@code MountedGasStorageWrapper} instance.
     *
     * @param storages the storages to use
     */
    public MountedGasStorageWrapper(ImmutableMap<BlockPos, MountedGasStorage> storages) {
        super(storages.values().toArray(IGasHandler[]::new));
        this.storages = storages;
    }
}
