package net.ty.createcraftedbeginning.gas.mounted;

import com.google.common.collect.ImmutableMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.gas.storage.handler.CombinedGasStorageHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MountedGasStorageWrapper extends CombinedGasStorageHandler {
    public final ImmutableMap<BlockPos, MountedGasStorage> storages;

    public MountedGasStorageWrapper(ImmutableMap<BlockPos, MountedGasStorage> storages) {
        super(storages.values().toArray(GasStorageHandler[]::new));
        this.storages = storages;
    }
}
