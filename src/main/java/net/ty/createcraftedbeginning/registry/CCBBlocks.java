package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.foundation.data.SharedProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.config.CCBStress;
import net.ty.createcraftedbeginning.content.andesitecrate.AndesiteCrateBlock;
import net.ty.createcraftedbeginning.content.brasscrate.BrassCrateBlock;
import net.ty.createcraftedbeginning.content.cardboardcrate.CardboardCrateBlock;
import net.ty.createcraftedbeginning.content.phohostressbearing.PhotoStressBearingBlock;
import net.ty.createcraftedbeginning.content.pneumaticengine.PneumaticEngineBlock;
import net.ty.createcraftedbeginning.content.sturdycrate.SturdyCrateBlock;
import net.ty.createcraftedbeginning.util.BuilderTransformer;

import static com.simibubi.create.api.contraption.storage.item.MountedItemStorageType.mountedItemStorage;
import static com.simibubi.create.foundation.data.TagGen.*;

public class CCBBlocks {
    private static final CreateRegistrate CREATE_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final BlockEntry<AndesiteCrateBlock> ANDESITE_CRATE_BLOCK = CREATE_REGISTRATE.block("andesite_crate", AndesiteCrateBlock::new)
            .initialProperties(SharedProperties::stone)
            .transform(BuilderTransformer.crate("andesite"))
            .transform(axeOrPickaxe())
            .properties(p -> p.mapColor(MapColor.PODZOL).sound(SoundType.WOOD))
            .transform(mountedItemStorage(CCBMountedStorage.ANDESITE_CRATE))
            .register();

    public static final BlockEntry<BrassCrateBlock> BRASS_CRATE_BLOCK = CREATE_REGISTRATE.block("brass_crate", BrassCrateBlock::new)
            .initialProperties(SharedProperties::stone)
            .transform(BuilderTransformer.crate("brass"))
            .transform(axeOrPickaxe())
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN).sound(SoundType.WOOD))
            .transform(mountedItemStorage(CCBMountedStorage.BRASS_CRATE))
            .register();

    public static final BlockEntry<CardboardCrateBlock> CARDBOARD_CRATE_BLOCK = CREATE_REGISTRATE.block("cardboard_crate", CardboardCrateBlock::new)
            .initialProperties(() -> Blocks.MUSHROOM_STEM)
            .transform(BuilderTransformer.crate("cardboard"))
            .transform(axeOnly())
            .properties(p -> p.mapColor(MapColor.COLOR_BROWN).sound(SoundType.CHISELED_BOOKSHELF).ignitedByLava())
            .transform(mountedItemStorage(CCBMountedStorage.CARDBOARD_CRATE))
            .register();

    public static final BlockEntry<SturdyCrateBlock> STURDY_CRATE_BLOCK = CREATE_REGISTRATE.block("sturdy_crate", SturdyCrateBlock::new)
            .initialProperties(SharedProperties::stone)
            .transform(BuilderTransformer.uncontainable_crate("sturdy"))
            .transform(pickaxeOnly())
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_CYAN).sound(SoundType.NETHERITE_BLOCK))
            .transform(mountedItemStorage(CCBMountedStorage.STURDY_CRATE))
            .register();

    public static final BlockEntry<PneumaticEngineBlock> PNEUMATIC_ENGINE_BLOCK = CREATE_REGISTRATE.block("pneumatic_engine", PneumaticEngineBlock::new)
            .initialProperties(SharedProperties::copperMetal)
            .transform(pickaxeOnly())
            .transform(BuilderTransformer.pneumatic_engine())
            .transform(CCBStress.setCapacity(16.0F))
            .properties(p -> p.mapColor(MapColor.COLOR_ORANGE).noOcclusion())
            .register();

    public static final BlockEntry<PhotoStressBearingBlock> PHOTO_STRESS_BEARING_BLOCK = CREATE_REGISTRATE.block("photo-stress_bearing", PhotoStressBearingBlock::new)
            .initialProperties(SharedProperties::stone)
            .transform(CCBStress.setCapacity(12.0F))
            .transform(pickaxeOnly())
            .transform(BuilderTransformer.photo_stress_bearing())
            .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion())
            .register();

    public static void register() {
    }
}
