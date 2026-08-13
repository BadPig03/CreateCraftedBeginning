package net.ty.createcraftedbeginning.compat.functionalstorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.bus.api.IEventBus;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.compat.functionalstorage.client.FunctionalStorageClientCompat;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FunctionalStorageClientBootstrap {
    private FunctionalStorageClientBootstrap() {
    }

    public static void addListeners(IEventBus modEventBus) {
        if (!CCBCompatMods.FUNCTIONAL_STORAGE.isLoaded()) {
            return;
        }

        modEventBus.addListener(FunctionalStorageClientCompat::registerClientExtensions);
        modEventBus.addListener(FunctionalStorageClientCompat::registerRenderers);
    }
}
