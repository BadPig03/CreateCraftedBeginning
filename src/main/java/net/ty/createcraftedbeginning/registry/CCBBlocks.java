package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.config.CCBStress;
import net.ty.createcraftedbeginning.content.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.airtightintakeport.AirtightIntakePortBlock;
import net.ty.createcraftedbeginning.content.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.andesitecrate.AndesiteCrateBlock;
import net.ty.createcraftedbeginning.content.brasscrate.BrassCrateBlock;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezechamber.EmptyBreezeChamberBlock;
import net.ty.createcraftedbeginning.content.cardboardcrate.CardboardCrateBlock;
import net.ty.createcraftedbeginning.content.cinderincinerationblower.CinderIncinerationBlowerBlock;
import net.ty.createcraftedbeginning.content.gasinjectionchamber.GasInjectionChamberBlock;
import net.ty.createcraftedbeginning.content.phohostressbearing.PhotoStressBearingBlock;
import net.ty.createcraftedbeginning.content.pneumaticengine.PneumaticEngineBlock;
import net.ty.createcraftedbeginning.content.sturdycrate.SturdyCrateBlock;
import net.ty.createcraftedbeginning.data.CCBBuilderTransformer;
import net.ty.createcraftedbeginning.data.CCBRegistrate;

import static com.simibubi.create.api.contraption.storage.item.MountedItemStorageType.mountedItemStorage;
import static net.ty.createcraftedbeginning.data.CCBTagGen.axeOnly;
import static net.ty.createcraftedbeginning.data.CCBTagGen.axeOrPickaxe;
import static net.ty.createcraftedbeginning.data.CCBTagGen.pickaxeOnly;

public class CCBBlocks {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final BlockEntry<AndesiteCrateBlock> ANDESITE_CRATE_BLOCK = CCB_REGISTRATE.block("andesite_crate", AndesiteCrateBlock::new).initialProperties(SharedProperties::stone).transform(CCBBuilderTransformer.crate("andesite")).transform(axeOrPickaxe()).properties(p -> p.mapColor(MapColor.PODZOL).sound(SoundType.WOOD)).transform(mountedItemStorage(CCBMountedStorage.ANDESITE_CRATE)).register();

    public static final BlockEntry<BrassCrateBlock> BRASS_CRATE_BLOCK = CCB_REGISTRATE.block("brass_crate", BrassCrateBlock::new).initialProperties(SharedProperties::stone).transform(CCBBuilderTransformer.crate("brass")).transform(axeOrPickaxe()).properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN).sound(SoundType.WOOD)).transform(mountedItemStorage(CCBMountedStorage.BRASS_CRATE)).register();

    public static final BlockEntry<CardboardCrateBlock> CARDBOARD_CRATE_BLOCK = CCB_REGISTRATE.block("cardboard_crate", CardboardCrateBlock::new).initialProperties(() -> Blocks.MUSHROOM_STEM).transform(CCBBuilderTransformer.crate("cardboard")).transform(axeOnly()).properties(p -> p.mapColor(MapColor.COLOR_BROWN).sound(SoundType.CHISELED_BOOKSHELF).ignitedByLava()).transform(mountedItemStorage(CCBMountedStorage.CARDBOARD_CRATE)).register();

    public static final BlockEntry<SturdyCrateBlock> STURDY_CRATE_BLOCK = CCB_REGISTRATE.block("sturdy_crate", SturdyCrateBlock::new).initialProperties(SharedProperties::stone).transform(CCBBuilderTransformer.uncontainable_crate()).transform(pickaxeOnly()).properties(p -> p.mapColor(MapColor.TERRACOTTA_CYAN).sound(SoundType.NETHERITE_BLOCK)).transform(mountedItemStorage(CCBMountedStorage.STURDY_CRATE)).register();

    public static final BlockEntry<PneumaticEngineBlock> PNEUMATIC_ENGINE_BLOCK = CCB_REGISTRATE.block("pneumatic_engine", PneumaticEngineBlock::new).initialProperties(SharedProperties::copperMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.pneumatic_engine()).transform(CCBStress.setCapacity(6.0F)).properties(p -> p.mapColor(MapColor.COLOR_ORANGE).noOcclusion()).register();

    public static final BlockEntry<PhotoStressBearingBlock> PHOTO_STRESS_BEARING_BLOCK = CCB_REGISTRATE.block("photo-stress_bearing", PhotoStressBearingBlock::new).initialProperties(SharedProperties::stone).transform(CCBStress.setCapacity(8.0F)).transform(pickaxeOnly()).transform(CCBBuilderTransformer.photo_stress_bearing()).properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion()).register();

    public static final BlockEntry<Block> CINDER_ALLOY_BLOCK = CCB_REGISTRATE.block("cinder_alloy_block", Block::new).initialProperties(SharedProperties::softMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.simple_block("cinder_alloy_block")).properties(p -> p.mapColor(MapColor.COLOR_BROWN)).register();

    public static final BlockEntry<CasingBlock> CINDER_CASING_BLOCK = CCB_REGISTRATE.block("cinder_casing", CasingBlock::new).initialProperties(SharedProperties::softMetal).transform(axeOrPickaxe()).transform(CCBBuilderTransformer.casing(() -> CCBSpriteShifts.CINDER_CASING)).properties(p -> p.mapColor(MapColor.COLOR_BROWN)).register();

    public static final BlockEntry<CinderIncinerationBlowerBlock> CINDER_INCINERATION_BLOWER_BLOCK = CCB_REGISTRATE.block("cinder_incineration_blower", CinderIncinerationBlowerBlock::new).initialProperties(SharedProperties::softMetal).transform(CCBStress.setImpact(4.0F)).transform(pickaxeOnly()).transform(CCBBuilderTransformer.cinder_incineration_blower()).properties(p -> p.mapColor(MapColor.COLOR_YELLOW).noOcclusion()).register();

    public static final BlockEntry<AirtightTankBlock> AIRTIGHT_TANK_BLOCK = CCB_REGISTRATE.block("airtight_tank", AirtightTankBlock::new).initialProperties(SharedProperties::netheriteMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.airtight_tank()).properties(p -> p.mapColor(MapColor.METAL).isRedstoneConductor((p1, p2, p3) -> true).noOcclusion()).register();

    public static final BlockEntry<AirtightPipeBlock> AIRTIGHT_PIPE_BLOCK = CCB_REGISTRATE.block("airtight_pipe", AirtightPipeBlock::new).initialProperties(SharedProperties::netheriteMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.airtight_pipe()).properties(p -> p.mapColor(MapColor.METAL)).register();

    public static final BlockEntry<AirtightPumpBlock> AIRTIGHT_PUMP_BLOCK = CCB_REGISTRATE.block("airtight_pump", AirtightPumpBlock::new).initialProperties(SharedProperties::netheriteMetal).transform(pickaxeOnly()).transform(CCBStress.setImpact(8.0F)).transform(CCBBuilderTransformer.airtight_pump()).properties(p -> p.mapColor(MapColor.METAL)).register();

    public static final BlockEntry<AirtightIntakePortBlock> AIRTIGHT_INTAKE_PORT_BLOCK = CCB_REGISTRATE.block("airtight_intake_port", AirtightIntakePortBlock::new).initialProperties(SharedProperties::netheriteMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.airtight_intake_port()).properties(p -> p.mapColor(MapColor.METAL).noOcclusion()).register();

    public static final BlockEntry<AirCompressorBlock> AIR_COMPRESSOR_BLOCK = CCB_REGISTRATE.block("air_compressor", AirCompressorBlock::new).initialProperties(SharedProperties::netheriteMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.air_compressor()).transform(CCBStress.setImpact(8.0F)).properties(p -> p.mapColor(MapColor.METAL).noOcclusion()).register();

    public static final BlockEntry<EmptyBreezeChamberBlock> EMPTY_BREEZE_CHAMBER_BLOCK = CCB_REGISTRATE.block("empty_breeze_chamber", EmptyBreezeChamberBlock::new).initialProperties(SharedProperties::softMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.empty_breeze_chamber()).properties(p -> p.mapColor(MapColor.COLOR_BLUE)).register();

    public static final BlockEntry<BreezeChamberBlock> BREEZE_CHAMBER_BLOCK = CCB_REGISTRATE.block("breeze_chamber", BreezeChamberBlock::new).initialProperties(SharedProperties::softMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.breeze_chamber()).properties(p -> p.mapColor(MapColor.COLOR_BLUE)).register();

    public static final BlockEntry<GasInjectionChamberBlock> GAS_INJECTION_CHAMBER_BLOCK = CCB_REGISTRATE.block("gas_injection_chamber", GasInjectionChamberBlock::new).initialProperties(SharedProperties::softMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.gas_injection_chamber()).properties(p -> p.mapColor(MapColor.COLOR_BLUE)).register();

    public static void register() {
    }
}
