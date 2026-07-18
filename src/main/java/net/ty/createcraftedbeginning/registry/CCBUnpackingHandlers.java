package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.content.crates.CrateUnpackingHandler;
import net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBUnpackingHandlers {
    @SuppressWarnings("UnstableApiUsage")
    public static void register() {
        SimpleRegistry<Block, UnpackingHandler> registry = UnpackingHandler.REGISTRY;

        registry.register(CCBBlocks.ANDESITE_CRATE_BLOCK.get(), CrateUnpackingHandler.standard(AndesiteCrateBlockEntity.class));
        registry.register(CCBBlocks.BRASS_CRATE_BLOCK.get(), CrateUnpackingHandler.standard(BrassCrateBlockEntity.class));
        registry.register(CCBBlocks.STURDY_CRATE_BLOCK.get(), CrateUnpackingHandler.standard(SturdyCrateBlockEntity.class));
        registry.register(CCBBlocks.CARDBOARD_CRATE_BLOCK.get(), CrateUnpackingHandler.discarding(CardboardCrateBlockEntity.class));
    }
}
