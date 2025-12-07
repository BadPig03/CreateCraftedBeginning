package net.ty.createcraftedbeginning.compat.jei.category.gas;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import mezz.jei.api.helpers.IColorHelper;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.jei.JEIPlugin;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
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
    public @NotNull IIngredientType<GasStack> getIngredientType() {
        return JEIPlugin.GAS_STACK;
    }

    @Override
    public @NotNull String getDisplayName(@NotNull GasStack ingredient) {
        return ingredient.getTranslationKey();
    }

    @Override
    @SuppressWarnings("removal")
    public @NotNull String getUniqueId(@NotNull GasStack ingredient, @NotNull UidContext context) {
        return "gas:" + ingredient.getGas();
    }

    @Override
    public @NotNull Object getUid(@NotNull GasStack ingredient, @NotNull UidContext context) {
        return ingredient.getGas();
    }

    @Override
    public @NotNull Iterable<Integer> getColors(@NotNull GasStack ingredient) {
        return colorHelper == null ? IIngredientHelper.super.getColors(ingredient) : colorHelper.getColors(Gas.getGasTexture(ingredient.getGasHolder()), ingredient.getHint(), 1);
    }

    @Override
    public @NotNull ResourceLocation getResourceLocation(@NotNull GasStack ingredient) {
        Holder<Gas> holder = ingredient.getGasHolder();
        ResourceKey<?> key = holder.getKey();
        return key == null ? CCBGasRegistries.GAS_REGISTRY.getKey(holder.value()) : key.location();
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
        ToStringHelper stringHelper = MoreObjects.toStringHelper(GasStack.class);
        Holder<Gas> gasHolder = ingredient.getGasHolder();
        stringHelper.add("Gas", gasHolder.value().isEmpty() ? "none" : ingredient.getTranslationKey());
        if (!ingredient.isEmpty()) {
            stringHelper.add("Amount", ingredient.getAmount());
        }
        return stringHelper.toString();
    }

    @Override
    public @NotNull Optional<TagKey<?>> getTagKeyEquivalent(@NotNull Collection<GasStack> stacks) {
        List<Holder<Gas>> gasHolders = stacks.stream().map(GasStack::getGasHolder).distinct().toList();
        if (gasHolders.isEmpty()) {
            return Optional.empty();
        }

        Holder<Gas> firstGas = gasHolders.getFirst();
        List<TagKey<Gas>> candidateTags = firstGas.tags().toList();
        for (TagKey<Gas> tagKey : candidateTags) {
            Optional<Named<Gas>> optionalTag = CCBGasRegistries.GAS_REGISTRY.getTag(tagKey);
            if (optionalTag.isEmpty() || !gasHolders.stream().allMatch(optionalTag.get()::contains)) {
                continue;
            }

            return Optional.of(tagKey);
        }

        return Optional.empty();
    }
}
