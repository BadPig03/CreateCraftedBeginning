package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.breezechamber.CoolingTimeVirtualFluid;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.data.CompressedAirOpenPipeEffect;
import net.ty.createcraftedbeginning.content.compressedair.CompressedAirFakeFluid;
import net.ty.createcraftedbeginning.content.breezechamber.SlushVirtualFluid;

public class CCBFluids {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final FluidEntry<CompressedAirFakeFluid> LOW_PRESSURE_COMPRESSED_AIR = CCB_REGISTRATE.compressed_air_fluid("low_pressure_compressed_air").lang("Low-Pressure Compressed Air").tag(CCBTags.commonFluidTag("compressed_air")).tag(CCBTags.CCBFluidTags.LOW_PRESSURE_COMPRESSED_AIR.tag).register();

    public static final FluidEntry<CompressedAirFakeFluid> MEDIUM_PRESSURE_COMPRESSED_AIR = CCB_REGISTRATE.compressed_air_fluid("medium_pressure_compressed_air").lang("Medium-Pressure Compressed Air").tag(CCBTags.commonFluidTag("compressed_air")).tag(CCBTags.CCBFluidTags.MEDIUM_PRESSURE_COMPRESSED_AIR.tag).register();

    public static final FluidEntry<CompressedAirFakeFluid> HIGH_PRESSURE_COMPRESSED_AIR = CCB_REGISTRATE.compressed_air_fluid("high_pressure_compressed_air").lang("High-Pressure Compressed Air").tag(CCBTags.commonFluidTag("compressed_air")).tag(CCBTags.CCBFluidTags.HIGH_PRESSURE_COMPRESSED_AIR.tag).register();

    public static final FluidEntry<SlushVirtualFluid> SLUSH = CCB_REGISTRATE.slush_fluid("slush").lang("Slush").tag(CCBTags.commonFluidTag("snow")).register();

    public static final FluidEntry<CoolingTimeVirtualFluid> COOLING_TIME = CCB_REGISTRATE.cooling_time("cooling_time").lang("Cooling Time").register();

    static {
        CCB_REGISTRATE.setCreativeTab(CCBCreativeTabs.CREATIVE_TAB);
    }

    public static void register(IEventBus modBus) {
        modBus.register(CCBFluids.class);
    }

    @SubscribeEvent
    public static void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(Reactions::registerOpenPipeEffects);
    }

    public static class Reactions {
        static void registerOpenPipeEffects() {
            OpenPipeEffectHandler.REGISTRY.register(LOW_PRESSURE_COMPRESSED_AIR.getSource(), new CompressedAirOpenPipeEffect());
            OpenPipeEffectHandler.REGISTRY.register(MEDIUM_PRESSURE_COMPRESSED_AIR.getSource(), new CompressedAirOpenPipeEffect());
            OpenPipeEffectHandler.REGISTRY.register(HIGH_PRESSURE_COMPRESSED_AIR.getSource(), new CompressedAirOpenPipeEffect());
        }
    }
}
