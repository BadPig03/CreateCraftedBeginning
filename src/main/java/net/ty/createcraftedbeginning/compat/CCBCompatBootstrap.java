package net.ty.createcraftedbeginning.compat;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.compat.functionalstorage.FunctionalStorageCompat;
import net.ty.createcraftedbeginning.compat.sable.SableSubLevelCompat;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBCompatBootstrap {
    private CCBCompatBootstrap() {
    }

    public static void initialize() {
        CCBCompatMods.SABLE.executeIfInstalled(() -> SableSubLevelCompat::install);
        if (!CCBCompatMods.FUNCTIONAL_STORAGE.isLoaded()) {
            return;
        }

        FunctionalStorageHook.register();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (!CCBCompatMods.FUNCTIONAL_STORAGE.isLoaded()) {
            return;
        }

        FunctionalStorageHook.registerCapabilities(event);
    }

    private static final class FunctionalStorageHook {
        private static void register() {
            FunctionalStorageCompat.register();
        }

        private static void registerCapabilities(RegisterCapabilitiesEvent event) {
            FunctionalStorageCompat.registerCapabilities(event);
        }
    }
}
