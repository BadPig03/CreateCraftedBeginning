package net.ty.createcraftedbeginning.mixin.accessor;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import net.ty.createcraftedbeginning.api.gas.gases.MountedGasStorageWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MountedStorageManager.class)
public interface MountedStorageManagerAccessor {
    @Accessor
    MountedGasStorageWrapper getGases();

    @Accessor
    void setGases(MountedGasStorageWrapper gases);
}