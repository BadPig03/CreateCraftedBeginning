package net.ty.createcraftedbeginning.api.gas.gases;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class MountedGasStorageWrapper extends CombinedGasTankWrapper {
    public final ImmutableMap<BlockPos, MountedGasStorage> storages;

    public MountedGasStorageWrapper(@NotNull ImmutableMap<BlockPos, MountedGasStorage> storages) {
        super(storages.values().toArray(IGasHandler[]::new));
        this.storages = storages;
    }
}
