package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.ty.createcraftedbeginning.foundation.block.CCBSharedProperties;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ObsidianRegistration {
    private ObsidianRegistration() {
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> obsidianAlikeBlocks(String name) {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.get(), provider.models().cubeAll(context.getName(), provider.modLoc("block/obsidians/" + name)))).item().tag(CCBItemTags.OBSIDIAN_BRICKS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> cryingObsidianAlikeBlocks(String name) {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.get(), provider.models().cubeAll(context.getName(), provider.modLoc("block/obsidians/" + name)))).item().tag(CCBItemTags.CRYING_OBSIDIAN_BRICKS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends SlabBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> obsidianAlikeSlabs(String name) {
        return obsidianAlikeSlabs(name, name);
    }

    @Contract(pure = true)
    public static <B extends SlabBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> obsidianAlikeSlabs(String sideName, String topName) {
        return builder -> builder.blockstate((context, provider) -> {
            BlockModelProvider models = provider.models();
            ResourceLocation side = provider.modLoc("block/obsidians/" + sideName);
            ResourceLocation top = provider.modLoc("block/obsidians/" + topName);
            String name = context.getName();
            provider.slabBlock(context.get(), models.slab(name, side, top, top), models.slabTop(name + "_top", side, top, top), models.cubeColumn(name + "_double", side, top));
        }).item().tag(ItemTags.SLABS).build();
    }

    @Contract(pure = true)
    public static <B extends StairBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> obsidianAlikeStairs(String name) {
        return builder -> builder.blockstate((context, provider) -> provider.stairsBlock(context.get(), provider.modLoc("block/obsidians/" + name))).item().tag(ItemTags.STAIRS).build();
    }

    @Contract(pure = true)
    public static <B extends WallBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> obsidianAlikeWall(String name) {
        return builder -> builder.blockstate((context, provider) -> provider.wallBlock(context.get(), name + "_wall", provider.modLoc("block/obsidians/" + name))).item().transform(itemBuilder -> itemBuilder.model((context, provider) -> provider.wallInventory(context.getName(), provider.modLoc("block/obsidians/" + name)))).tag(ItemTags.WALLS).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> obsidianBlockProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::obsidian).tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> cryingObsidianBlockProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::cryingObsidian).tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> cryingObsidianLetterProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::cryingObsidian).tag(BlockTags.MINEABLE_WITH_PICKAXE).tag(BlockTags.ENCHANTMENT_POWER_PROVIDER);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> obsidianSlabProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::obsidian).tag(BlockTags.MINEABLE_WITH_PICKAXE).tag(BlockTags.SLABS);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> obsidianStairsProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::obsidian).tag(BlockTags.STAIRS);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> obsidianWallProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::obsidian).tag(BlockTags.WALLS);
    }
}
