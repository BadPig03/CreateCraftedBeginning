package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.decoration.MetalScaffoldingBlock;
import com.simibubi.create.content.decoration.MetalScaffoldingBlockItem;
import com.simibubi.create.content.decoration.MetalScaffoldingCTBehaviour;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.ty.createcraftedbeginning.client.render.CCBSpriteShifts;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ScaffoldingRegistration {
    private ScaffoldingRegistration() {
    }

    @Contract(pure = true)
    public static <B extends MetalScaffoldingBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> industrialIronBlockScaffolding() {
        return builder -> builder.blockstate((context, provider) -> {
            String blockName = context.getName();
            String texturePath = "block/industrial_iron_block_scaffolding/";
            provider.getVariantBuilder(context.get()).forAllStatesExcept(state -> {
                String suffix = "";
                if (state.getValue(MetalScaffoldingBlock.BOTTOM)) {
                    suffix = "_horizontal";
                }

                return ConfiguredModel.builder().modelFile(provider.models().withExistingParent(blockName + suffix, Create.asResource("block/scaffold/block" + suffix)).texture("top", provider.modLoc(texturePath + "top")).texture("inside", provider.modLoc(texturePath + "inside")).texture("side", provider.modLoc(texturePath + "side")).texture("casing", provider.modLoc(texturePath + "top")).texture("particle", provider.modLoc(texturePath + "side")).renderType("minecraft:cutout")).build();
            }, MetalScaffoldingBlock.WATERLOGGED, MetalScaffoldingBlock.DISTANCE);
        }).loot(RegistrateBlockLootTables::dropSelf).onRegister(CreateRegistrate.connectedTextures(() -> new MetalScaffoldingCTBehaviour(CCBSpriteShifts.INDUSTRIAL_IRON_BLOCK_SCAFFOLD, CCBSpriteShifts.INDUSTRIAL_IRON_BLOCK_SCAFFOLD_INSIDE, CCBSpriteShifts.INDUSTRIAL_IRON_BLOCK_SCAFFOLD_TOP))).item(MetalScaffoldingBlockItem::new).recipe((context, provider) -> provider.stonecutting(DataIngredient.items(AllBlocks.INDUSTRIAL_IRON_BLOCK.get()), RecipeCategory.DECORATIONS, context, 2)).model((context, provider) -> {
            String itemName = context.getName();
            provider.withExistingParent(itemName, provider.modLoc("block/" + itemName));
        }).build();
    }

    @Contract(pure = true)
    public static <B extends MetalScaffoldingBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> industrialIronBlockScaffoldingProperties() {
        return builder -> builder.initialProperties(() -> Blocks.SCAFFOLDING).properties(properties -> properties.mapColor(MapColor.METAL).sound(SoundType.NETHERITE_BLOCK)).tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.CLIMBABLE);
    }
}
