package net.ty.createcraftedbeginning.registry;

import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralCogBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.HorizontalAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet.BoilerSteamOutletBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlockEntity;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerBlockEntity;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.EndSculkSilencerBlockEntity;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.EndSculkSilencerStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.laserreceiver.LaserReceiverBlockEntity;
import net.ty.createcraftedbeginning.content.obsolete.pneumaticengine.PneumaticEngineBlockEntity;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrate;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBBlockEntities {
    private static final CCBRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();

    public static final BlockEntityEntry<AirtightPipeBlockEntity> AIRTIGHT_PIPE = CCB_REGISTRATE.blockEntity("airtight_pipe", AirtightPipeBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_PIPE_BLOCK).register();
    public static final BlockEntityEntry<AirtightEncasedPipeBlockEntity> AIRTIGHT_ENCASED_PIPE = CCB_REGISTRATE.blockEntity("airtight_encased_pipe", AirtightEncasedPipeBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK).register();
    public static final BlockEntityEntry<AirtightCheckValveBlockEntity> AIRTIGHT_CHECK_VALVE = CCB_REGISTRATE.blockEntity("airtight_check_valve", AirtightCheckValveBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK).register();
    public static final BlockEntityEntry<SmartAirtightPipeBlockEntity> SMART_AIRTIGHT_PIPE = CCB_REGISTRATE.blockEntity("smart_airtight_pipe", SmartAirtightPipeBlockEntity::new).validBlock(CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK).register();
    public static final BlockEntityEntry<AirtightPumpBlockEntity> AIRTIGHT_PUMP = CCB_REGISTRATE.blockEntity("airtight_pump", AirtightPumpBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_PUMP_BLOCK).register();

    public static final BlockEntityEntry<AirtightTankBlockEntity> AIRTIGHT_TANK = CCB_REGISTRATE.blockEntity("airtight_tank", AirtightTankBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_TANK_BLOCK).register();
    public static final BlockEntityEntry<HorizontalAirtightTankBlockEntity> HORIZONTAL_AIRTIGHT_TANK = CCB_REGISTRATE.blockEntity("horizontal_airtight_tank", HorizontalAirtightTankBlockEntity::new).validBlock(CCBBlocks.HORIZONTAL_AIRTIGHT_TANK_BLOCK).register();
    public static final BlockEntityEntry<CreativeAirtightTankBlockEntity> CREATIVE_AIRTIGHT_TANK = CCB_REGISTRATE.blockEntity("creative_airtight_tank", CreativeAirtightTankBlockEntity::new).validBlock(CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK).register();

    public static final BlockEntityEntry<AirtightHatchBlockEntity> AIRTIGHT_HATCH = CCB_REGISTRATE.blockEntity("airtight_hatch", AirtightHatchBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_HATCH_BLOCK).register();
    public static final BlockEntityEntry<GasInjectionChamberBlockEntity> GAS_INJECTION_CHAMBER = CCB_REGISTRATE.blockEntity("gas_injection_chamber", GasInjectionChamberBlockEntity::new).validBlock(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK).register();
    public static final BlockEntityEntry<PortableGasInterfaceBlockEntity> PORTABLE_GAS_INTERFACE = CCB_REGISTRATE.blockEntity("portable_gas_interface", PortableGasInterfaceBlockEntity::new).validBlock(CCBBlocks.PORTABLE_GAS_INTERFACE_BLOCK).register();

    public static final BlockEntityEntry<BreezeCoolerBlockEntity> BREEZE_COOLER = CCB_REGISTRATE.blockEntity("breeze_cooler", BreezeCoolerBlockEntity::new).validBlock(CCBBlocks.BREEZE_COOLER_BLOCK).register();
    public static final BlockEntityEntry<BreezeChamberBlockEntity> BREEZE_CHAMBER = CCB_REGISTRATE.blockEntity("breeze_chamber", BreezeChamberBlockEntity::new).validBlock(CCBBlocks.BREEZE_CHAMBER_BLOCK).register();

    public static final BlockEntityEntry<AirCompressorBlockEntity> AIR_COMPRESSOR = CCB_REGISTRATE.blockEntity("air_compressor", AirCompressorBlockEntity::new).validBlock(CCBBlocks.AIR_COMPRESSOR_BLOCK).register();

    public static final BlockEntityEntry<AirtightEngineBlockEntity> AIRTIGHT_ENGINE = CCB_REGISTRATE.blockEntity("airtight_engine", AirtightEngineBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).register();
    public static final BlockEntityEntry<ResidueOutletBlockEntity> RESIDUE_OUTLET = CCB_REGISTRATE.blockEntity("residue_outlet", ResidueOutletBlockEntity::new).validBlock(CCBBlocks.RESIDUE_OUTLET_BLOCK).register();
    public static final BlockEntityEntry<BoilerSteamOutletBlockEntity> BOILER_STEAM_OUTLET = CCB_REGISTRATE.blockEntity("boiler_steam_outlet", BoilerSteamOutletBlockEntity::new).validBlock(CCBBlocks.BOILER_STEAM_OUTLET_BLOCK).register();

    public static final BlockEntityEntry<TeslaTurbineBlockEntity> TESLA_TURBINE = CCB_REGISTRATE.blockEntity("tesla_turbine", TeslaTurbineBlockEntity::new).validBlock(CCBBlocks.TESLA_TURBINE_BLOCK).register();
    public static final BlockEntityEntry<TeslaTurbineNozzleBlockEntity> TESLA_TURBINE_NOZZLE = CCB_REGISTRATE.blockEntity("tesla_turbine_nozzle", TeslaTurbineNozzleBlockEntity::new).validBlock(CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK).register();

    public static final BlockEntityEntry<AirtightReactorKettleBlockEntity> AIRTIGHT_REACTOR_KETTLE = CCB_REGISTRATE.blockEntity("airtight_reactor_kettle", AirtightReactorKettleBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK).register();
    public static final BlockEntityEntry<AirtightReactorKettleStructuralBlockEntity> AIRTIGHT_REACTOR_KETTLE_STRUCTURAL = CCB_REGISTRATE.blockEntity("airtight_reactor_kettle_structural", AirtightReactorKettleStructuralBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK).register();
    public static final BlockEntityEntry<AirtightReactorKettleStructuralCogBlockEntity> AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG = CCB_REGISTRATE.blockEntity("airtight_reactor_kettle_structural_cog", AirtightReactorKettleStructuralCogBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG_BLOCK).register();

    public static final BlockEntityEntry<AirtightForgingPressBlockEntity> AIRTIGHT_FORGING_PRESS = CCB_REGISTRATE.blockEntity("airtight_forging_press", AirtightForgingPressBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK).register();
    public static final BlockEntityEntry<AirtightForgingPressStructuralBlockEntity> AIRTIGHT_FORGING_PRESS_STRUCTURAL = CCB_REGISTRATE.blockEntity("airtight_forging_press_structural", AirtightForgingPressStructuralBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_BLOCK).register();
    public static final BlockEntityEntry<AirtightForgingPressStructuralShaftBlockEntity> AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT = CCB_REGISTRATE.blockEntity("airtight_forging_press_structural_shaft", AirtightForgingPressStructuralShaftBlockEntity::new).validBlock(CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT_BLOCK).register();

    public static final BlockEntityEntry<GasPackagerBlockEntity> GAS_PACKAGER = CCB_REGISTRATE.blockEntity("gas_packager", GasPackagerBlockEntity::new).validBlock(CCBBlocks.GAS_PACKAGER_BLOCK).register();
    public static final BlockEntityEntry<GasRepackagerBlockEntity> GAS_REPACKAGER = CCB_REGISTRATE.blockEntity("gas_repackager", GasRepackagerBlockEntity::new).validBlock(CCBBlocks.GAS_REPACKAGER_BLOCK).register();
    public static final BlockEntityEntry<GasFactoryGaugeBlockEntity> GAS_FACTORY_GAUGE = CCB_REGISTRATE.blockEntity("gas_factory_gauge", GasFactoryGaugeBlockEntity::new).validBlock(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK).register();

    public static final BlockEntityEntry<GasCanisterBlockEntity> GAS_CANISTER = CCB_REGISTRATE.blockEntity("gas_canister", GasCanisterBlockEntity::new).validBlock(CCBBlocks.GAS_CANISTER_BLOCK).register();
    public static final BlockEntityEntry<CreativeGasCanisterBlockEntity> CREATIVE_GAS_CANISTER = CCB_REGISTRATE.blockEntity("creative_gas_canister", CreativeGasCanisterBlockEntity::new).validBlock(CCBBlocks.CREATIVE_GAS_CANISTER_BLOCK).register();

    public static final BlockEntityEntry<LaserEmitterBlockEntity> LASER_EMITTER = CCB_REGISTRATE.blockEntity("laser_emitter", LaserEmitterBlockEntity::new).validBlock(CCBBlocks.LASER_EMITTER_BLOCK).register();
    public static final BlockEntityEntry<LaserReceiverBlockEntity> LASER_RECEIVER = CCB_REGISTRATE.blockEntity("laser_receiver", LaserReceiverBlockEntity::new).validBlock(CCBBlocks.LASER_RECEIVER_BLOCK).register();

    public static final BlockEntityEntry<EndIncinerationBlowerBlockEntity> END_INCINERATION_BLOWER = CCB_REGISTRATE.blockEntity("end_incineration_blower", EndIncinerationBlowerBlockEntity::new).validBlock(CCBBlocks.END_INCINERATION_BLOWER_BLOCK).register();
    public static final BlockEntityEntry<EndIncinerationBlowerStructuralBlockEntity> END_INCINERATION_BLOWER_STRUCTURAL = CCB_REGISTRATE.blockEntity("end_incineration_blower_structural", EndIncinerationBlowerStructuralBlockEntity::new).validBlock(CCBBlocks.END_INCINERATION_BLOWER_STRUCTURAL_BLOCK).register();
    public static final BlockEntityEntry<EndSculkSilencerBlockEntity> END_SCULK_SILENCER = CCB_REGISTRATE.blockEntity("end_sculk_silencer", EndSculkSilencerBlockEntity::new).validBlock(CCBBlocks.END_SCULK_SILENCER_BLOCK).register();
    public static final BlockEntityEntry<EndSculkSilencerStructuralBlockEntity> END_SCULK_SILENCER_STRUCTURAL = CCB_REGISTRATE.blockEntity("end_sculk_silencer_structural", EndSculkSilencerStructuralBlockEntity::new).validBlock(CCBBlocks.END_SCULK_SILENCER_STRUCTURAL_BLOCK).register();

    public static final BlockEntityEntry<AndesiteCrateBlockEntity> ANDESITE_CRATE = CCB_REGISTRATE.blockEntity("andesite_crate", AndesiteCrateBlockEntity::new).validBlock(CCBBlocks.ANDESITE_CRATE_BLOCK).register();
    public static final BlockEntityEntry<BrassCrateBlockEntity> BRASS_CRATE = CCB_REGISTRATE.blockEntity("brass_crate", BrassCrateBlockEntity::new).validBlock(CCBBlocks.BRASS_CRATE_BLOCK).register();
    public static final BlockEntityEntry<SturdyCrateBlockEntity> STURDY_CRATE = CCB_REGISTRATE.blockEntity("sturdy_crate", SturdyCrateBlockEntity::new).validBlock(CCBBlocks.STURDY_CRATE_BLOCK).register();
    public static final BlockEntityEntry<CardboardCrateBlockEntity> CARDBOARD_CRATE = CCB_REGISTRATE.blockEntity("cardboard_crate", CardboardCrateBlockEntity::new).validBlock(CCBBlocks.CARDBOARD_CRATE_BLOCK).register();
    public static final BlockEntityEntry<AirVentBlockEntity> AIR_VENT = CCB_REGISTRATE.blockEntity("air_vent", AirVentBlockEntity::new).validBlock(CCBBlocks.AIR_VENT_BLOCK).register();

    public static final BlockEntityEntry<PneumaticEngineBlockEntity> PNEUMATIC_ENGINE = CCB_REGISTRATE.blockEntity("pneumatic_engine", PneumaticEngineBlockEntity::new).validBlock(CCBBlocks.PNEUMATIC_ENGINE_BLOCK).register();

    public static void register() {
    }
}
