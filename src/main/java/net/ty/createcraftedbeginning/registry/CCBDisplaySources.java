package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightFlowmeterDisplaySource;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightManometerDisplaySource;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightManometerDisplaySource.Reading;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBDisplaySources {
    private static final CreateRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();

    public static final RegistryEntry<DisplaySource, AirtightFlowmeterDisplaySource> AIRTIGHT_FLOWMETER_FLOW_RATE = CCB_REGISTRATE.displaySource("airtight_flowmeter_flow_rate", AirtightFlowmeterDisplaySource::new).register();
    public static final RegistryEntry<DisplaySource, AirtightManometerDisplaySource> AIRTIGHT_MANOMETER_MAX_PRESSURE = CCB_REGISTRATE.displaySource("airtight_manometer_max_pressure", () -> new AirtightManometerDisplaySource(Reading.MAX_PRESSURE)).register();
    public static final RegistryEntry<DisplaySource, AirtightManometerDisplaySource> AIRTIGHT_MANOMETER_PRESSURE_DIFFERENCE = CCB_REGISTRATE.displaySource("airtight_manometer_pressure_difference", () -> new AirtightManometerDisplaySource(Reading.PRESSURE_DIFFERENCE)).register();

    public static void register() {
    }
}
