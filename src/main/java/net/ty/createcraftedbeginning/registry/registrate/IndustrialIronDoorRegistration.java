package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class IndustrialIronDoorRegistration {
    public static final BlockSetType INDUSTRIAL_IRON = BlockSetType.register(new BlockSetType(CCBAPI.asResource("industrial_iron").toString(), true, BlockSetType.IRON.canOpenByWindCharge(), BlockSetType.IRON.canButtonBeActivatedByArrows(), BlockSetType.IRON.pressurePlateSensitivity(), BlockSetType.IRON.soundType(), BlockSetType.IRON.doorClose(), BlockSetType.IRON.doorOpen(), BlockSetType.IRON.trapdoorClose(), BlockSetType.IRON.trapdoorOpen(), BlockSetType.IRON.pressurePlateClickOff(), BlockSetType.IRON.pressurePlateClickOn(), BlockSetType.IRON.buttonClickOff(), BlockSetType.IRON.buttonClickOn()));

    private IndustrialIronDoorRegistration() {
    }

    @Contract(pure = true)
    public static <B extends DoorBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> industrialIronDoor(String textureFolder) {
        return builder -> builder.initialProperties(() -> Blocks.IRON_DOOR).tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.DOORS).blockstate((context, provider) -> {
            String name = context.getName();
            String texturePath = "block/industrial_iron/" + textureFolder + '/';
            String[] suffixes = {"bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open", "top_left", "top_left_open", "top_right", "top_right_open"};
            ModelFile[] models = new ModelFile[suffixes.length];
            for (int index = 0; index < suffixes.length; index++) {
                String suffix = suffixes[index];
                models[index] = provider.models().withExistingParent(name + '_' + suffix, provider.modLoc("block/industrial_iron/door/" + suffix)).texture("bottom", provider.modLoc(texturePath + "bottom")).texture("top", provider.modLoc(texturePath + "top")).texture("side", provider.modLoc(texturePath + "side"));
            }
            provider.doorBlock(context.get(), models[0], models[1], models[2], models[3], models[4], models[5], models[6], models[7]);
        }).loot((provider, block) -> provider.add(block, provider.createDoorTable(block))).item(DoubleHighBlockItem::new).tag(ItemTags.DOORS).model((context, provider) -> {
            String name = context.getName();
            provider.basicItem(provider.modLoc(name));
        }).build();
    }

    @Contract(pure = true)
    public static <B extends TrapDoorBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> industrialIronTrapdoor(String textureFolder) {
        return builder -> builder.initialProperties(() -> Blocks.IRON_TRAPDOOR).tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.TRAPDOORS).blockstate((context, provider) -> {
            String name = context.getName();
            String texturePath = "block/industrial_iron/" + textureFolder + '/';
            String[] suffixes = {"bottom", "top", "open"};
            ModelFile[] models = new ModelFile[suffixes.length];
            for (int index = 0; index < suffixes.length; index++) {
                String suffix = suffixes[index];
                models[index] = provider.models().withExistingParent(name + '_' + suffix, provider.modLoc("block/industrial_iron/trapdoor/" + suffix)).texture("texture", provider.modLoc(texturePath + "trapdoor")).texture("side", provider.modLoc(texturePath + "side"));
            }
            provider.trapdoorBlock(context.get(), models[0], models[1], models[2], false);
        }).loot(RegistrateBlockLootTables::dropSelf).item().tag(ItemTags.TRAPDOORS).model((context, provider) -> {
            String name = context.getName();
            provider.withExistingParent(name, provider.modLoc("block/" + name + "_bottom"));
        }).build();
    }
}
