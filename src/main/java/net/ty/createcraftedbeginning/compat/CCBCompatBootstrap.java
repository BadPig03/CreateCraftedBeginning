package net.ty.createcraftedbeginning.compat;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.compat.createdragonsplus.CreateDragonsPlusCompat;
import net.ty.createcraftedbeginning.compat.dndesires.DnDesiresCompat;
import net.ty.createcraftedbeginning.compat.functionalstorage.FunctionalStorageCompat;
import net.ty.createcraftedbeginning.compat.functionalstorage.client.FunctionalStorageClientCompat;
import net.ty.createcraftedbeginning.compat.jei.CCBJEICompat;
import net.ty.createcraftedbeginning.compat.sable.SableSubLevelCompat;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBCompatBootstrap {
    private CCBCompatBootstrap() {
    }

    public static void initialize() {
        CCBCompatMods.CREATE_DRAGONS_PLUS.executeIfInstalled(() -> CreateDragonsPlusCompat::register);
        CCBCompatMods.DNDESIRES.executeIfInstalled(() -> DnDesiresCompat::register);
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

    public static void commonSetup(FMLCommonSetupEvent event) {
        if (!CCBCompatMods.JEI.isLoaded()) {
            return;
        }

        event.enqueueWork(JEIHook::registerMysteriousItemConversions);
    }

    public static void registerClientListeners(IEventBus modEventBus) {
        if (!CCBCompatMods.FUNCTIONAL_STORAGE.isLoaded()) {
            return;
        }

        FunctionalStorageClientHook.registerListeners(modEventBus);
    }

    private static final class FunctionalStorageHook {
        private static void register() {
            FunctionalStorageCompat.register();
        }

        private static void registerCapabilities(RegisterCapabilitiesEvent event) {
            FunctionalStorageCompat.registerCapabilities(event);
        }
    }

    private static final class FunctionalStorageClientHook {
        private static void registerListeners(IEventBus modEventBus) {
            modEventBus.addListener(FunctionalStorageClientCompat::registerClientExtensions);
            modEventBus.addListener(FunctionalStorageClientCompat::registerRenderers);
        }
    }

    private static final class JEIHook {
        private static void registerMysteriousItemConversions() {
            CCBJEICompat.registerMysteriousItemConversions();
        }
    }
}
