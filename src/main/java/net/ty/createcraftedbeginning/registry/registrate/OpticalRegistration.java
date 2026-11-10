package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder.ConditionGroup;
import net.ty.createcraftedbeginning.content.opticalpower.amethystantenna.AmethystAntennaBlockItem;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlockItem;
import net.ty.createcraftedbeginning.foundation.block.CCBSharedProperties;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalRegistration {
    private OpticalRegistration() {
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> opticalFiber() {
        return builder -> builder.properties(Properties::dynamicShape).blockstate((context, provider) -> {
            BlockModelProvider models = provider.models();
            MultiPartBlockStateBuilder multipart = provider.getMultipartBuilder(context.getEntry());
            PartBuilder junction = multipart.part().modelFile(models.getExistingFile(provider.modLoc("block/optical_fiber/junction"))).addModel().useOr();
            ConditionGroup isolated = junction.nestedGroup();
            for (Direction direction : Iterate.directions) {
                isolated.condition(OpticalFiberBlock.PROPERTY_BY_DIRECTION.get(direction), false);
                for (Direction other : Iterate.directions) {
                    if (direction.ordinal() >= other.ordinal() || direction.getAxis() == other.getAxis()) {
                        continue;
                    }

                    junction.nestedGroup().condition(OpticalFiberBlock.PROPERTY_BY_DIRECTION.get(direction), true).condition(OpticalFiberBlock.PROPERTY_BY_DIRECTION.get(other), true).end();
                }
            }
            isolated.end();
            junction.end();
            for (Direction direction : Iterate.directions) {
                String name = direction.getSerializedName();
                PartBuilder end = multipart.part().modelFile(models.getExistingFile(provider.modLoc("block/optical_fiber/end/" + name))).addModel();
                for (Direction other : Iterate.directions) {
                    end.condition(OpticalFiberBlock.PROPERTY_BY_DIRECTION.get(other), direction == other);
                }
                end.end();

                PartBuilder arm = multipart.part().modelFile(models.getExistingFile(provider.modLoc("block/optical_fiber/connection/" + name))).addModel();
                arm.nestedGroup().condition(OpticalFiberBlock.PROPERTY_BY_DIRECTION.get(direction), true).end();
                ConditionGroup perpendicular = arm.nestedGroup().useOr();
                for (Direction other : Iterate.directions) {
                    if (direction.getAxis() == other.getAxis()) {
                        continue;
                    }

                    perpendicular.condition(OpticalFiberBlock.PROPERTY_BY_DIRECTION.get(other), true);
                }
                perpendicular.end();
                arm.end();

                if (direction.getAxisDirection() != AxisDirection.POSITIVE) {
                    continue;
                }

                PartBuilder straight = multipart.part().modelFile(models.getExistingFile(provider.modLoc("block/optical_fiber/straight/" + direction.getAxis().getSerializedName()))).addModel();
                for (Direction other : Iterate.directions) {
                    straight.condition(OpticalFiberBlock.PROPERTY_BY_DIRECTION.get(other), direction.getAxis() == other.getAxis());
                }
                straight.end();
            }
        }).item(OpticalFiberBlockItem::new).transform(ModelGen.customItemModel("optical_fiber", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> amethystCollectorPanel() {
        return builder -> builder.tag(CCBBlockTags.AMETHYST_COLLECTOR_SKY_OCCLUDERS.tag).properties(Properties::dynamicShape).blockstate((context, provider) -> provider.simpleBlock(context.getEntry(), AssetLookup.partialBaseModel(context, provider))).item().transform(ModelGen.customItemModel("amethyst_collector_panel", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> laserEmitter() {
        return builder -> builder.blockstate((context, provider) -> provider.directionalBlock(context.getEntry(), provider.models().getExistingFile(provider.modLoc("block/laser_emitter/block")))).item().transform(ModelGen.customItemModel("laser_emitter", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> amethystMirror() {
        return builder -> builder.properties(Properties::dynamicShape).blockstate((context, provider) -> provider.directionalBlock(context.getEntry(), provider.models().getExistingFile(provider.modLoc("block/amethyst_mirror/block")))).item().transform(ModelGen.customItemModel("amethyst_mirror", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> amethystLens() {
        return builder -> builder.properties(Properties::dynamicShape).blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/" + context.getName() + "/block"));
            provider.getVariantBuilder(context.getEntry()).forAllStates(state -> {
                Direction facing = state.getValue(DirectionalBlock.FACING);
                int xRotation = 0;
                int yRotation = 0;
                if (facing == Direction.UP) {
                    xRotation = 270;
                }
                else if (facing == Direction.DOWN) {
                    xRotation = 90;
                }
                else {
                    yRotation = ((int) facing.toYRot() + 180) % 360;
                }
                return ConfiguredModel.builder().modelFile(model).rotationX(xRotation).rotationY(yRotation).build();
            });
        }).item().model((context, provider) -> {
            String name = context.getName();
            provider.withExistingParent(name, provider.modLoc("block/" + name + "/item"));
        }).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> amethystSoundSuppressor() {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.getEntry(), provider.models().getExistingFile(provider.modLoc("block/amethyst_sound_suppressor/block")))).item().tag(AllItemTags.CONTRAPTION_CONTROLLED.tag).transform(ModelGen.customItemModel("amethyst_sound_suppressor", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> amethystLoader() {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.getEntry(), provider.models().getExistingFile(provider.modLoc("block/amethyst_loader/block")))).item().transform(ModelGen.customItemModel("amethyst_loader", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> amethystAntenna() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/amethyst_antenna/block"));
            provider.getVariantBuilder(context.getEntry()).forAllStates(state -> {
                Direction facing = state.getValue(DirectionalBlock.FACING);
                int x = 270;
                int y = (int) facing.toYRot();
                if (facing == Direction.UP) {
                    x = 0;
                    y = 0;
                }
                else if (facing == Direction.DOWN) {
                    x = 180;
                    y = 0;
                }
                return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
            });
        }).loot((loot, block) -> loot.add(block, loot.createSingleItemTable(block).apply(CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY).include(CCBDataComponents.LOADING_NETWORK)))).item(AmethystAntennaBlockItem::new).transform(ModelGen.customItemModel("amethyst_antenna", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> experienceCrystal(NonNullSupplier<? extends Block> reference, boolean mature) {
        return builder -> builder.initialProperties(reference).properties(properties -> properties.mapColor(MapColor.COLOR_LIGHT_GREEN)).tag(BlockTags.MINEABLE_WITH_PICKAXE).blockstate((context, provider) -> provider.directionalBlock(context.getEntry(), provider.models().cross(context.getName(), provider.modLoc("block/experience_crystal/" + context.getName())).renderType("minecraft:cutout"))).loot((loot, block) -> {
            if (!mature) {
                loot.dropWhenSilkTouch(block);
                return;
            }

            loot.add(block, loot.createSilkTouchDispatchTable(block, loot.applyExplosionDecay(block, LootItem.lootTableItem(AllItems.EXP_NUGGET.get()).apply(SetItemCountFunction.setCount(ConstantValue.exactly(4))).apply(ApplyBonusCount.addOreBonusCount(loot.getRegistries().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE))))));
        }).item().model((context, provider) -> provider.generated(context, provider.modLoc("block/experience_crystal/" + context.getName()))).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> photothermalReceiver() {
        return builder -> builder.properties(Properties::dynamicShape).blockstate((context, provider) -> provider.horizontalBlock(context.getEntry(), provider.models().getExistingFile(provider.modLoc("block/photothermal_receiver/block")))).item().transform(ModelGen.customItemModel("photothermal_receiver", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> powderedAmethystBlock() {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.get(), provider.models().cubeAll(context.getName(), provider.modLoc("block/powdered_amethyst_block")))).item().build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> opticalComponentProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::stone).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(properties -> properties.mapColor(MapColor.COLOR_PURPLE).noOcclusion());
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> photothermalReceiverProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::stone).tag(BlockTags.MINEABLE_WITH_PICKAXE).onRegister(block -> BoilerHeater.REGISTRY.register(block, BoilerHeater.BLAZE_BURNER)).properties(properties -> properties.mapColor(MapColor.COLOR_PURPLE).noOcclusion().lightLevel(state -> switch (state.getValue(BlazeBurnerBlock.HEAT_LEVEL)) {
            case KINDLED -> 8;
            case SEETHING -> 12;
            default -> 0;
        }));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> powderedAmethystBlockProperties() {
        return builder -> builder.tag(BlockTags.MINEABLE_WITH_SHOVEL).tag(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS).properties(properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(0.5F).sound(SoundType.SAND));
    }
}
