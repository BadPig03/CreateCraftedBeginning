package net.ty.createcraftedbeginning.compat.jei.category.gas;

import com.google.common.base.MoreObjects;
import mezz.jei.api.helpers.IColorHelper;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.data.CCBGasRegistry;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.compat.jei.JEIPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class GasStackHelper implements IIngredientHelper<GasStack> {
    @Nullable
    private IColorHelper colorHelper;

    public void setColorHelper(@Nullable IColorHelper colorHelper) {
        this.colorHelper = colorHelper;
    }

    @Override
    @SuppressWarnings("removal")
    public @NotNull String getUniqueId(@NotNull GasStack ingredient, @NotNull UidContext context) {
        return "gas:" + ingredient.getGas();
    }

    @Override
    public @NotNull IIngredientType<GasStack> getIngredientType() {
        return JEIPlugin.TYPE_GAS;
    }

    @Override
    public @NotNull String getDisplayName(@NotNull GasStack ingredient) {
        return ingredient.getTranslationKey();
    }

    @Override
    public @NotNull Object getUid(@NotNull GasStack ingredient, @NotNull UidContext context) {
        return ingredient.getGas();
    }

    @Override
    public @NotNull Iterable<Integer> getColors(@NotNull GasStack ingredient) {
        if (colorHelper == null) {
            return IIngredientHelper.super.getColors(ingredient);
        }
        return colorHelper.getColors(Gas.getGasTexture(ingredient.getGasHolder()), ingredient.getGasTint(), 1);
    }

    @Override
    public @NotNull ResourceLocation getResourceLocation(@NotNull GasStack ingredient) {
        Holder<Gas> holder = ingredient.getGasHolder();
        ResourceKey<?> key = holder.getKey();
        if (key == null) {
            return CCBGasRegistry.GAS_REGISTRY.getKey(holder.value());
        }
        return key.location();
    }

    @Override
    public @NotNull GasStack copyIngredient(@NotNull GasStack ingredient) {
        return ingredient.copy();
    }

    @Override
    public @NotNull GasStack normalizeIngredient(@NotNull GasStack ingredient) {
        return ingredient.copyWithAmount(FluidType.BUCKET_VOLUME);
    }

    @Override
    public boolean isValidIngredient(@NotNull GasStack ingredient) {
        return !ingredient.isEmpty();
    }

    @Override
    public @NotNull Stream<ResourceLocation> getTagStream(@NotNull GasStack ingredient) {
        return ingredient.getTags().map(TagKey::location);
    }

    @Override
    public @NotNull String getErrorInfo(@Nullable GasStack ingredient) {
        if (ingredient == null) {
            ingredient = GasStack.EMPTY;
        }
        MoreObjects.ToStringHelper toStringHelper = MoreObjects.toStringHelper(GasStack.class);
        Holder<Gas> gasHolder = ingredient.getGasHolder();
        toStringHelper.add("Gas", gasHolder.is(CCBGasRegistry.EMPTY_GAS_KEY) ? "none" : ingredient.getTranslationKey());
        if (!ingredient.isEmpty()) {
            toStringHelper.add("Amount", ingredient.getAmount());
        }
        return toStringHelper.toString();
    }

    @Override
    public @NotNull Optional<TagKey<?>> getTagKeyEquivalent(@NotNull Collection<GasStack> stacks) {
        if (stacks.size() < 2) {
            return Optional.empty();
        }
        List<Holder<Gas>> values = stacks.stream().map(GasStack::getGasHolder).distinct().toList();
        int expected = values.size();
        if (expected != stacks.size()) {
            return Optional.empty();
        }
        for (TagKey<Gas> tagKey : values.getFirst().tags().toList()) {
            Optional<HolderSet.Named<Gas>> optionalTag = CCBGasRegistry.GAS_REGISTRY.getTag(tagKey);
            if (optionalTag.isPresent()) {
                HolderSet.Named<Gas> tag = optionalTag.get();
                if (tag.size() == expected && values.stream().allMatch(tag::contains)) {
                    return Optional.of(tagKey);
                }
            }
        }
        return Optional.empty();
    }
}
