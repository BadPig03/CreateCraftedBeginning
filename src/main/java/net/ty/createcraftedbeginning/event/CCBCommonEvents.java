package net.ty.createcraftedbeginning.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.ty.createcraftedbeginning.content.aircompressor.AirCompressorBlockEntity;
import net.ty.createcraftedbeginning.content.airtightintakeport.AirtightIntakePortBlockEntity;
import net.ty.createcraftedbeginning.content.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.andesitecrate.AndesiteCrateBlockEntity;
import net.ty.createcraftedbeginning.content.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.cardboardcrate.CardboardCrateBlockEntity;
import net.ty.createcraftedbeginning.content.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.content.sturdycrate.SturdyCrateBlockEntity;

@EventBusSubscriber()
public class CCBCommonEvents {
    @SubscribeEvent
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        AndesiteCrateBlockEntity.registerCapabilities(event);
        BrassCrateBlockEntity.registerCapabilities(event);
        CardboardCrateBlockEntity.registerCapabilities(event);
        SturdyCrateBlockEntity.registerCapabilities(event);
        AirtightTankBlockEntity.registerCapabilities(event);
        AirtightIntakePortBlockEntity.registerCapabilities(event);
        AirCompressorBlockEntity.registerCapabilities(event);
        BreezeChamberBlockEntity.registerCapabilities(event);
        GasInjectionChamberBlockEntity.registerCapabilities(event);
    }
}
