package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateUnpackingHandler;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateUnpackingHandler;
import net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateUnpackingHandler;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateUnpackingHandler;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class CCBUnpackingHandlers {
    @SuppressWarnings("UnstableApiUsage")
    public static void register() {
        SimpleRegistry<Block, UnpackingHandler> registry = UnpackingHandler.REGISTRY;

        registry.register(CCBBlocks.ANDESITE_CRATE_BLOCK.get(), AndesiteCrateUnpackingHandler.INSTANCE);
        registry.register(CCBBlocks.BRASS_CRATE_BLOCK.get(), BrassCrateUnpackingHandler.INSTANCE);
        registry.register(CCBBlocks.STURDY_CRATE_BLOCK.get(), SturdyCrateUnpackingHandler.INSTANCE);
        registry.register(CCBBlocks.CARDBOARD_CRATE_BLOCK.get(), CardboardCrateUnpackingHandler.INSTANCE);
    }
}
