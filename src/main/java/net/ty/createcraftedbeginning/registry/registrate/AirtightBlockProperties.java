package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.ty.createcraftedbeginning.config.CCBStress;
import net.ty.createcraftedbeginning.foundation.block.CCBSharedProperties;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightBlockProperties {
    private AirtightBlockProperties() {
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightComponent() {
        return AirtightBlockProperties::applyComponent;
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightComponentWithImpact(double impact) {
        return builder -> applyComponent(builder).transform(CCBStress.setImpact(impact));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightComponentWithCapacity(double capacity) {
        return builder -> applyComponent(builder).transform(CCBStress.setCapacity(capacity));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightStructural() {
        return AirtightBlockProperties::applyStructural;
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightStructuralWithImpact(double impact) {
        return builder -> applyStructural(builder).transform(CCBStress.setImpact(impact));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightMetal() {
        return builder -> builder.initialProperties(CCBSharedProperties::airtightMetal).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(properties -> properties.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).requiresCorrectToolForDrops());
    }

    static <B extends Block> BlockBuilder<B, CreateRegistrate> applyComponent(BlockBuilder<B, CreateRegistrate> builder) {
        return builder.initialProperties(CCBSharedProperties::airtightMetal).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(properties -> properties.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).requiresCorrectToolForDrops().noOcclusion()).tag(CCBBlockTags.AIRTIGHT_COMPONENTS.tag);
    }

    private static <B extends Block> BlockBuilder<B, CreateRegistrate> applyStructural(BlockBuilder<B, CreateRegistrate> builder) {
        return builder.initialProperties(CCBSharedProperties::airtightMetal).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(properties -> properties.mapColor(MapColor.METAL).sound(SoundType.EMPTY).requiresCorrectToolForDrops().noOcclusion()).tag(CCBBlockTags.AIRTIGHT_COMPONENTS.tag);
    }
}
