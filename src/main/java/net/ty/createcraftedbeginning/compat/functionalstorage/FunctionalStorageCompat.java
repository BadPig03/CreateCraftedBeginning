package net.ty.createcraftedbeginning.compat.functionalstorage;

import com.hrznstudio.titanium.nbthandler.NBTManager;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.compat.functionalstorage.registry.CCBFunctionalStorageBlockEntities;
import net.ty.createcraftedbeginning.compat.functionalstorage.registry.CCBFunctionalStorageBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FunctionalStorageCompat {
    private FunctionalStorageCompat() {
    }

    public static void register() {
        CCBFunctionalStorageBlocks.register();
        CCBFunctionalStorageBlockEntities.register();
        NBTManager.getInstance().scanTileClassForAnnotations(GasDrawerBlockEntity.class);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        GasDrawerBlockEntity.registerCapabilities(event);
        ControllerGasHandler.registerCapabilities(event);
    }
}
