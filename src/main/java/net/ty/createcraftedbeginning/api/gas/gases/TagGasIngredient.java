package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class TagGasIngredient extends GasIngredient {
    public static final MapCodec<TagGasIngredient> CODEC = TagKey.codec(CCBRegistries.GAS_REGISTRY_KEY).xmap(TagGasIngredient::new, TagGasIngredient::tag).fieldOf("tag");

    private final TagKey<Gas> tag;

    public TagGasIngredient(TagKey<Gas> tag) {
        this.tag = tag;
    }

    @Override
    public boolean test(@NotNull GasStack gasStack) {
        return gasStack.is(tag);
    }

    @Override
    protected Stream<GasStack> generateStacks() {
        return CCBGasRegistries.GAS_REGISTRY.getTag(tag).stream().flatMap(HolderSet::stream).map(gasHolder -> new GasStack(gasHolder, FluidType.BUCKET_VOLUME));
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public GasIngredientType<?> getType() {
        return CCBGasRegistries.TAG_GAS_INGREDIENT_TYPE.get();
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof TagGasIngredient other && other.tag() == tag;
    }

    public TagKey<Gas> tag() {
        return tag;
    }
}
