package net.ty.createcraftedbeginning.recipe.gas.ingredient;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TagGasIngredient extends GasIngredient {
    public static final MapCodec<TagGasIngredient> CODEC = TagKey.codec(GasRegistries.GAS_REGISTRY_KEY).xmap(TagGasIngredient::new, TagGasIngredient::tag).fieldOf("tag");

    private final TagKey<Gas> tag;

    public TagGasIngredient(TagKey<Gas> tag) {
        this.tag = tag;
    }

    @Override
    public boolean test(GasStack gasStack) {
        return gasStack.is(tag);
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof TagGasIngredient other && tag.equals(other.tag());
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public GasIngredientType<?> getType() {
        return GasIngredientTypes.TAG.get();
    }

    @Override
    protected Stream<GasStack> generateStacks() {
        return GasRegistries.GAS_REGISTRY.getTag(tag).stream().flatMap(HolderSet::stream).map(gas -> new GasStack(gas, GasUnits.GU_PER_KGU));
    }

    public TagKey<Gas> tag() {
        return tag;
    }
}
