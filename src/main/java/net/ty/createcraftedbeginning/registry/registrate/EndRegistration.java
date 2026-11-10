package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.ty.createcraftedbeginning.client.render.CCBSpriteShifts;
import net.ty.createcraftedbeginning.foundation.block.CCBSharedProperties;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class EndRegistration {
    private EndRegistration() {
    }

    @Contract(pure = true)
    public static <B extends CasingBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> endCasing() {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.get())).onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(CCBSpriteShifts.END_CASING))).onRegister(CreateRegistrate.casingConnectivity((block, connectivity) -> connectivity.makeCasing(block, CCBSpriteShifts.END_CASING))).item().properties(properties -> properties.rarity(Rarity.UNCOMMON)).tag(AllItemTags.CASING.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> endAlloyBlock() {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.get(), provider.models().cubeAll(context.getName(), provider.modLoc("block/end_alloy_block")))).item().properties(properties -> properties.rarity(Rarity.UNCOMMON)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> endCasingProperties() {
        return builder -> applyEndComponent(builder).tag(AllBlockTags.CASING.tag);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> endAlloyBlockProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::obsidian).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(properties -> properties.mapColor(MapColor.COLOR_GREEN));
    }

    private static <B extends Block> BlockBuilder<B, CreateRegistrate> applyEndComponent(BlockBuilder<B, CreateRegistrate> builder) {
        return builder.initialProperties(CCBSharedProperties::obsidian).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(properties -> properties.mapColor(MapColor.COLOR_GREEN).noOcclusion()).tag(CCBBlockTags.END_COMPONENTS.tag);
    }
}
