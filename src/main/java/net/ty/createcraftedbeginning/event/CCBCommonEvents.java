package net.ty.createcraftedbeginning.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.ty.createcraftedbeginning.content.andesitecrate.AndesiteCrateBlockEntity;
import net.ty.createcraftedbeginning.content.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.content.cardboardcrate.CardboardCrateBlockEntity;
import net.ty.createcraftedbeginning.content.sturdycrate.SturdyCrateBlockEntity;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CCBCommonEvents {
    @SubscribeEvent
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        AndesiteCrateBlockEntity.registerCapabilities(event);
        BrassCrateBlockEntity.registerCapabilities(event);
        CardboardCrateBlockEntity.registerCapabilities(event);
        SturdyCrateBlockEntity.registerCapabilities(event);
    }
}
