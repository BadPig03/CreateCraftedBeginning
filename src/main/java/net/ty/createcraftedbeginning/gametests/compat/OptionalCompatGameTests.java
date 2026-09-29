package net.ty.createcraftedbeginning.gametests.compat;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.gametests.compat.curios.CuriosCanisterPackGameTests;
import net.ty.createcraftedbeginning.gametests.compat.functionalstorage.GasDrawerConnectionsGameTests;
import net.ty.createcraftedbeginning.gametests.compat.functionalstorage.GasDrawerPressureTransferGameTests;
import net.ty.createcraftedbeginning.gametests.compat.jade.gas.GasStorageEncodingGameTests;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public final class OptionalCompatGameTests {
    private OptionalCompatGameTests() {
    }

    @SubscribeEvent
    private static void register(RegisterGameTestsEvent event) {
        if (CCBCompatMods.CURIOS.isLoaded()) {
            event.register(CuriosCanisterPackGameTests.class);
        }
        if (CCBCompatMods.JADE.isLoaded()) {
            event.register(GasStorageEncodingGameTests.class);
        }
        if (!CCBCompatMods.FUNCTIONAL_STORAGE.isLoaded()) {
            return;
        }

        event.register(GasDrawerConnectionsGameTests.class);
        event.register(GasDrawerPressureTransferGameTests.class);
    }
}
