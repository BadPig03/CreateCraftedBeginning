package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder.ConditionGroup;
import net.ty.createcraftedbeginning.config.CCBStress;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;
import net.ty.createcraftedbeginning.foundation.block.CCBSharedProperties;
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
        return builder -> builder.properties(Block.Properties::dynamicShape).blockstate((context, provider) -> {
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
        }).item().transform(ModelGen.customItemModel("optical_fiber", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> amethystCollectorPanel() {
        return builder -> builder.properties(Block.Properties::dynamicShape).blockstate((context, provider) -> provider.simpleBlock(context.getEntry(), AssetLookup.partialBaseModel(context, provider))).item().transform(ModelGen.customItemModel("amethyst_collector_panel", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> laserEmitter() {
        return builder -> builder.blockstate((context, provider) -> provider.directionalBlock(context.getEntry(), provider.models().getExistingFile(provider.modLoc("block/laser_emitter/block")))).item().transform(ModelGen.customItemModel("laser_emitter", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> laserReceiver() {
        return builder -> builder.blockstate((context, provider) -> provider.directionalBlock(context.getEntry(), provider.models().getExistingFile(provider.modLoc("block/laser_receiver/block")))).item().transform(ModelGen.customItemModel("laser_receiver", "item"));
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
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> laserReceiverProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::stone).tag(BlockTags.MINEABLE_WITH_PICKAXE).transform(CCBStress.setCapacity(128)).properties(properties -> properties.mapColor(MapColor.COLOR_PURPLE).noOcclusion());
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> powderedAmethystBlockProperties() {
        return builder -> builder.tag(BlockTags.MINEABLE_WITH_SHOVEL).tag(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS).properties(properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(0.5F).sound(SoundType.SAND));
    }
}
