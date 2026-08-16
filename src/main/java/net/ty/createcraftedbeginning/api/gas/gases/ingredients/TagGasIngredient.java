package net.ty.createcraftedbeginning.api.gas.gases.ingredients;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

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
    protected Stream<GasStack> generateStacks() {
        return GasRegistries.GAS_REGISTRY.getTag(tag).stream().flatMap(HolderSet::stream).map(gas -> new GasStack(gas, FluidType.BUCKET_VOLUME));
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public GasIngredientType<?> getType() {
        return GasRegistries.TAG_GAS_INGREDIENT_TYPE.get();
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
    public boolean equals(Object obj) {
        return this == obj || obj instanceof TagGasIngredient other && tag.equals(other.tag());
    }

    public TagKey<Gas> tag() {
        return tag;
    }
}
