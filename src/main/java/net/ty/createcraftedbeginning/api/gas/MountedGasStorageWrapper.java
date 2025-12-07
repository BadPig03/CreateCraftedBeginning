package net.ty.createcraftedbeginning.api.gas;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import org.jetbrains.annotations.NotNull;

public class MountedGasStorageWrapper extends GasCombinedTankWrapper {
    public final ImmutableMap<BlockPos, MountedGasStorage> storages;

    public MountedGasStorageWrapper(@NotNull ImmutableMap<BlockPos, MountedGasStorage> storages) {
        super(storages.values().toArray(IGasHandler[]::new));
        this.storages = storages;
    }
}
