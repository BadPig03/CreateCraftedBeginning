package net.ty.createcraftedbeginning.api.cannonhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.cannonhandlers.creative.CreativeAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.ethereal.EnergizedEtherealAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.ethereal.EtherealAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.ethereal.PressurizedEnergizedEtherealAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.ethereal.PressurizedEtherealAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.moist.MoistAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.natural.EnergizedNaturalAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.natural.NaturalAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.natural.PressurizedEnergizedNaturalAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.natural.PressurizedNaturalAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.sculk.SculkAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.spore.SporeAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.ultrawarm.EnergizedUltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.ultrawarm.PressurizedEnergizedUltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.ultrawarm.PressurizedUltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.ultrawarm.UltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCannonHandlerEvent;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightCannonHandlers {
    public static void register() {
        AirtightCannonHandlerEvent.add(CCBGases.NATURAL_AIR.get(), new NaturalAirCannonHandler());
        AirtightCannonHandlerEvent.add(CCBGases.ENERGIZED_NATURAL_AIR.get(), new EnergizedNaturalAirCannonHandler());
        AirtightCannonHandlerEvent.add(CCBGases.PRESSURIZED_NATURAL_AIR.get(), new PressurizedNaturalAirCannonHandler());
        AirtightCannonHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), new PressurizedEnergizedNaturalAirCannonHandler());

        AirtightCannonHandlerEvent.add(CCBGases.ULTRAWARM_AIR.get(), new UltrawarmAirCannonHandler());
        AirtightCannonHandlerEvent.add(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), new EnergizedUltrawarmAirCannonHandler());
        AirtightCannonHandlerEvent.add(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), new PressurizedUltrawarmAirCannonHandler());
        AirtightCannonHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), new PressurizedEnergizedUltrawarmAirCannonHandler());

        AirtightCannonHandlerEvent.add(CCBGases.ETHEREAL_AIR.get(), new EtherealAirCannonHandler());
        AirtightCannonHandlerEvent.add(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), new EnergizedEtherealAirCannonHandler());
        AirtightCannonHandlerEvent.add(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), new PressurizedEtherealAirCannonHandler());
        AirtightCannonHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), new PressurizedEnergizedEtherealAirCannonHandler());

        AirtightCannonHandlerEvent.add(CCBGases.MOIST_AIR.get(), new MoistAirCannonHandler());
        AirtightCannonHandlerEvent.add(CCBGases.SPORE_AIR.get(), new SporeAirCannonHandler());
        AirtightCannonHandlerEvent.add(CCBGases.SCULK_AIR.get(), new SculkAirCannonHandler());

        AirtightCannonHandlerEvent.add(CCBGases.CREATIVE_AIR.get(), new CreativeAirCannonHandler());
    }
}
