package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.ty.createcraftedbeginning.content.andesitecrate.AndesiteCrateBlockEntity;
import net.ty.createcraftedbeginning.content.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.content.cardboardcrate.CardboardCrateBlockEntity;
import net.ty.createcraftedbeginning.content.pneumaticengine.PneumaticEngineBlockEntity;
import net.ty.createcraftedbeginning.content.pneumaticengine.PneumaticEngineRenderer;
import net.ty.createcraftedbeginning.content.sturdycrate.SturdyCrateBlockEntity;

public class CCBBlockEntities {
    private static final CreateRegistrate CREATE_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final BlockEntityEntry<AndesiteCrateBlockEntity> ANDESITE_CRATE = CREATE_REGISTRATE.blockEntity("andesite_crate", AndesiteCrateBlockEntity::new)
            .validBlocks(CCBBlocks.ANDESITE_CRATE_BLOCK)
            .renderer(() -> SmartBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<BrassCrateBlockEntity> BRASS_CRATE = CREATE_REGISTRATE.blockEntity("brass_crate", BrassCrateBlockEntity::new)
            .validBlocks(CCBBlocks.BRASS_CRATE_BLOCK)
            .renderer(() -> SmartBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<CardboardCrateBlockEntity> CARDBOARD_CRATE = CREATE_REGISTRATE.blockEntity("cardboard_crate", CardboardCrateBlockEntity::new)
            .validBlocks(CCBBlocks.CARDBOARD_CRATE_BLOCK)
            .renderer(() -> SmartBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<SturdyCrateBlockEntity> STURDY_CRATE = CREATE_REGISTRATE.blockEntity("sturdy_crate", SturdyCrateBlockEntity::new)
            .validBlocks(CCBBlocks.STURDY_CRATE_BLOCK)
            .renderer(() -> SmartBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<PneumaticEngineBlockEntity> PNEUMATIC_ENGINE = CREATE_REGISTRATE.blockEntity("pneumatic_engine", PneumaticEngineBlockEntity::new)
            .validBlocks(CCBBlocks.PNEUMATIC_ENGINE_BLOCK)
            .renderer(() -> PneumaticEngineRenderer::new)
            .register();

    public static void register() {
    }
}
