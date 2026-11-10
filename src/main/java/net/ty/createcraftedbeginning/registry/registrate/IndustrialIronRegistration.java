package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.SimpleCTBehaviour;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.ty.createcraftedbeginning.client.render.CCBSpriteShifts;
import net.ty.createcraftedbeginning.content.decoration.industrialiron.IndustrialIronPillarCTBehaviour;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class IndustrialIronRegistration {
    private IndustrialIronRegistration() {
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> industrialIronBlock() {
        return builder -> builder.initialProperties(SharedProperties::softMetal).properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()).tag(BlockTags.MINEABLE_WITH_PICKAXE, AllBlockTags.WRENCH_PICKUP.tag).blockstate((context, provider) -> {
            String blockName = context.getName();
            provider.simpleBlock(context.get(), provider.models().cubeAll(blockName, provider.modLoc("block/industrial_iron/" + blockName)));
        }).loot(RegistrateBlockLootTables::dropSelf).item().recipe((context, provider) -> provider.stonecutting(DataIngredient.items(AllBlocks.INDUSTRIAL_IRON_BLOCK.get()), RecipeCategory.DECORATIONS, context, 1)).model((context, provider) -> {
            String itemName = context.getName();
            provider.withExistingParent(itemName, provider.modLoc("block/" + itemName));
        }).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> connectedIndustrialIronBlock() {
        return builder -> builder.transform(industrialIronBlock()).onRegister(CreateRegistrate.connectedTextures(() -> new SimpleCTBehaviour(CCBSpriteShifts.CONNECTED_INDUSTRIAL_IRON_BLOCK)));
    }

    @Contract(pure = true)
    public static <B extends RotatedPillarBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> industrialIronPillar(Supplier<CTSpriteShiftEntry> shift) {
        return builder -> builder.transform(industrialIronBlock()).blockstate((context, provider) -> {
            String texturePath = "block/industrial_iron/" + context.getName() + '/';
            provider.axisBlock(context.get(), provider.modLoc(texturePath + "single"), provider.modLoc(texturePath + "top"));
        }).onRegister(CreateRegistrate.connectedTextures(() -> new IndustrialIronPillarCTBehaviour(shift.get())));
    }
}
