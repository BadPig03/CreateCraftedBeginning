package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlockEntity;
import net.ty.createcraftedbeginning.content.obsolete.airtightintakeport.AirtightIntakePortBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberRenderer;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberVisual;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerRenderer;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerVisual;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberRenderer;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceRenderer;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceVisual;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeRenderer;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineRenderer;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlockEntity;
import net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlockEntity;
import net.ty.createcraftedbeginning.content.obsolete.cinderincinerationblower.CinderIncinerationBlowerBlockEntity;
import net.ty.createcraftedbeginning.content.obsolete.cinderincinerationblower.CinderIncinerationBlowerRenderer;
import net.ty.createcraftedbeginning.content.obsolete.phohostressbearing.PhotoStressBearingBlockEntity;
import net.ty.createcraftedbeginning.content.obsolete.phohostressbearing.PhotoStressBearingRenderer;
import net.ty.createcraftedbeginning.content.obsolete.pneumaticengine.PneumaticEngineBlockEntity;
import net.ty.createcraftedbeginning.content.obsolete.pneumaticengine.PneumaticEngineRenderer;
import net.ty.createcraftedbeginning.data.CCBRegistrate;

public class CCBBlockEntities {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final BlockEntityEntry<AirCompressorBlockEntity> AIR_COMPRESSOR = CCB_REGISTRATE.blockEntity("air_compressor", AirCompressorBlockEntity::new).validBlocks(CCBBlocks.AIR_COMPRESSOR_BLOCK).renderer(() -> AirCompressorRenderer::new).register();
    public static final BlockEntityEntry<AirtightEncasedPipeBlockEntity> AIRTIGHT_ENCASED_PIPE = CCB_REGISTRATE.blockEntity("airtight_encased_pipe", AirtightEncasedPipeBlockEntity::new).renderer(() -> AirtightEncasedPipeRenderer::new).validBlocks(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK).register();
    public static final BlockEntityEntry<AirtightEngineBlockEntity> AIRTIGHT_ENGINE = CCB_REGISTRATE.blockEntity("airtight_engine", AirtightEngineBlockEntity::new).validBlocks(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).renderer(() -> AirtightEngineRenderer::new).register();
    public static final BlockEntityEntry<AirtightHatchBlockEntity> AIRTIGHT_HATCH = CCB_REGISTRATE.blockEntity("airtight_hatch", AirtightHatchBlockEntity::new).validBlocks(CCBBlocks.AIRTIGHT_HATCH_BLOCK).register();
    public static final BlockEntityEntry<AirtightIntakePortBlockEntity> AIRTIGHT_INTAKE_PORT = CCB_REGISTRATE.blockEntity("airtight_intake_port", AirtightIntakePortBlockEntity::new).validBlocks(CCBBlocks.AIRTIGHT_INTAKE_PORT_BLOCK).register();
    public static final BlockEntityEntry<AirtightPipeBlockEntity> AIRTIGHT_PIPE = CCB_REGISTRATE.blockEntity("airtight_pipe", AirtightPipeBlockEntity::new).validBlocks(CCBBlocks.AIRTIGHT_PIPE_BLOCK).register();
    public static final BlockEntityEntry<AirtightPumpBlockEntity> AIRTIGHT_PUMP = CCB_REGISTRATE.blockEntity("airtight_pump", AirtightPumpBlockEntity::new).visual(() -> SingleAxisRotatingVisual.ofZ(CCBPartialModels.AIRTIGHT_PUMP_COGS)).validBlocks(CCBBlocks.AIRTIGHT_PUMP_BLOCK).renderer(() -> AirtightPumpRenderer::new).register();
    public static final BlockEntityEntry<AirtightTankBlockEntity> AIRTIGHT_TANK = CCB_REGISTRATE.blockEntity("airtight_tank", AirtightTankBlockEntity::new).validBlocks(CCBBlocks.AIRTIGHT_TANK_BLOCK).register();
    public static final BlockEntityEntry<AndesiteCrateBlockEntity> ANDESITE_CRATE = CCB_REGISTRATE.blockEntity("andesite_crate", AndesiteCrateBlockEntity::new).validBlocks(CCBBlocks.ANDESITE_CRATE_BLOCK).renderer(() -> SmartBlockEntityRenderer::new).register();
    public static final BlockEntityEntry<BrassCrateBlockEntity> BRASS_CRATE = CCB_REGISTRATE.blockEntity("brass_crate", BrassCrateBlockEntity::new).validBlocks(CCBBlocks.BRASS_CRATE_BLOCK).renderer(() -> SmartBlockEntityRenderer::new).register();
    public static final BlockEntityEntry<BreezeChamberBlockEntity> BREEZE_CHAMBER = CCB_REGISTRATE.blockEntity("breeze_chamber", BreezeChamberBlockEntity::new).visual(() -> BreezeChamberVisual::new, false).validBlocks(CCBBlocks.BREEZE_CHAMBER_BLOCK).renderer(() -> BreezeChamberRenderer::new).register();
    public static final BlockEntityEntry<BreezeCoolerBlockEntity> BREEZE_COOLER = CCB_REGISTRATE.blockEntity("breeze_cooler", BreezeCoolerBlockEntity::new).visual(() -> BreezeCoolerVisual::new, false).validBlocks(CCBBlocks.BREEZE_COOLER_BLOCK).renderer(() -> BreezeCoolerRenderer::new).register();
    public static final BlockEntityEntry<CardboardCrateBlockEntity> CARDBOARD_CRATE = CCB_REGISTRATE.blockEntity("cardboard_crate", CardboardCrateBlockEntity::new).validBlocks(CCBBlocks.CARDBOARD_CRATE_BLOCK).renderer(() -> SmartBlockEntityRenderer::new).register();
    public static final BlockEntityEntry<AirtightCheckValveBlockEntity> AIRTIGHT_CHECK_VALVE = CCB_REGISTRATE.blockEntity("airtight_check_valve", AirtightCheckValveBlockEntity::new).validBlocks(CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK).register();
    public static final BlockEntityEntry<CinderIncinerationBlowerBlockEntity> CINDER_NOZZLE = CCB_REGISTRATE.blockEntity("cinder_nozzle", CinderIncinerationBlowerBlockEntity::new).validBlocks(CCBBlocks.CINDER_INCINERATION_BLOWER_BLOCK).renderer(() -> CinderIncinerationBlowerRenderer::new).register();
    public static final BlockEntityEntry<CreativeAirtightTankBlockEntity> CREATIVE_AIRTIGHT_TANK = CCB_REGISTRATE.blockEntity("creative_airtight_tank", CreativeAirtightTankBlockEntity::new).validBlocks(CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK).register();
    public static final BlockEntityEntry<GasCanisterBlockEntity> GAS_CANISTER = CCB_REGISTRATE.blockEntity("gas_canister", GasCanisterBlockEntity::new).validBlocks(CCBBlocks.GAS_CANISTER_BLOCK).register();
    public static final BlockEntityEntry<GasInjectionChamberBlockEntity> GAS_INJECTION_CHAMBER = CCB_REGISTRATE.blockEntity("gas_injection_chamber", GasInjectionChamberBlockEntity::new).validBlocks(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK).renderer(() -> GasInjectionChamberRenderer::new).register();
    public static final BlockEntityEntry<PhotoStressBearingBlockEntity> PHOTO_STRESS_BEARING = CCB_REGISTRATE.blockEntity("photo-stress_bearing", PhotoStressBearingBlockEntity::new).validBlocks(CCBBlocks.PHOTO_STRESS_BEARING_BLOCK).renderer(() -> PhotoStressBearingRenderer::new).register();
    public static final BlockEntityEntry<PneumaticEngineBlockEntity> PNEUMATIC_ENGINE = CCB_REGISTRATE.blockEntity("pneumatic_engine", PneumaticEngineBlockEntity::new).validBlocks(CCBBlocks.PNEUMATIC_ENGINE_BLOCK).renderer(() -> PneumaticEngineRenderer::new).register();
    public static final BlockEntityEntry<PortableGasInterfaceBlockEntity> PORTABLE_GAS_INTERFACE = CCB_REGISTRATE.blockEntity("portable_gas_interface", PortableGasInterfaceBlockEntity::new).visual(() -> PortableGasInterfaceVisual::new).validBlocks(CCBBlocks.PORTABLE_GAS_INTERFACE_BLOCK).renderer(() -> PortableGasInterfaceRenderer::new).register();
    public static final BlockEntityEntry<ResidueOutletBlockEntity> RESIDUE_OUTLET = CCB_REGISTRATE.blockEntity("residue_outlet", ResidueOutletBlockEntity::new).validBlocks(CCBBlocks.RESIDUE_OUTLET_BLOCK).register();
    public static final BlockEntityEntry<SmartAirtightPipeBlockEntity> SMART_AIRTIGHT_PIPE = CCB_REGISTRATE.blockEntity("smart_airtight_pipe", SmartAirtightPipeBlockEntity::new).validBlocks(CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK).renderer(() -> SmartAirtightPipeRenderer::new).register();
    public static final BlockEntityEntry<SturdyCrateBlockEntity> STURDY_CRATE = CCB_REGISTRATE.blockEntity("sturdy_crate", SturdyCrateBlockEntity::new).validBlocks(CCBBlocks.STURDY_CRATE_BLOCK).renderer(() -> SmartBlockEntityRenderer::new).register();
    public static final BlockEntityEntry<TeslaTurbineBlockEntity> TESLA_TURBINE = CCB_REGISTRATE.blockEntity("tesla_turbine", TeslaTurbineBlockEntity::new).validBlocks(CCBBlocks.TESLA_TURBINE_BLOCK).renderer(() -> TeslaTurbineRenderer::new).register();
    public static final BlockEntityEntry<TeslaTurbineNozzleBlockEntity> TESLA_TURBINE_NOZZLE = CCB_REGISTRATE.blockEntity("tesla_turbine_nozzle", TeslaTurbineNozzleBlockEntity::new).validBlocks(CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK).register();

    public static void register() {
    }
}
