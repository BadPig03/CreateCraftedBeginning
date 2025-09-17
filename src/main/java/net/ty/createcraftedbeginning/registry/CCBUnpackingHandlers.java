package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateUnpackingHandler;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateUnpackingHandler;
import net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateUnpackingHandler;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateUnpackingHandler;

public class CCBUnpackingHandlers {
    @SuppressWarnings("UnstableApiUsage")
    public static void register() {
        UnpackingHandler.REGISTRY.register(CCBBlocks.ANDESITE_CRATE_BLOCK.get(), AndesiteCrateUnpackingHandler.INSTANCE);
        UnpackingHandler.REGISTRY.register(CCBBlocks.BRASS_CRATE_BLOCK.get(), BrassCrateUnpackingHandler.INSTANCE);
		UnpackingHandler.REGISTRY.register(CCBBlocks.STURDY_CRATE_BLOCK.get(), SturdyCrateUnpackingHandler.INSTANCE);
        UnpackingHandler.REGISTRY.register(CCBBlocks.CARDBOARD_CRATE_BLOCK.get(), CardboardCrateUnpackingHandler.INSTANCE);
	}
}
