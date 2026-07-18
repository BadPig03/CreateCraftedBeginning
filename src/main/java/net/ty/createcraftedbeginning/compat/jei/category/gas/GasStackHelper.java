package net.ty.createcraftedbeginning.compat.jei.category.gas;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import mezz.jei.api.helpers.IColorHelper;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasStackHelper implements IIngredientHelper<GasStack> {
    @Nullable
    private IColorHelper colorHelper;

    public void setColorHelper(@Nullable IColorHelper colorHelper) {
        this.colorHelper = colorHelper;
    }

    @Override
    public IIngredientType<GasStack> getIngredientType() {
        return CCBJEIPlugin.GAS_STACK;
    }

    @Override
    public String getDisplayName(GasStack ingredient) {
        return ingredient.getTranslationKey();
    }

    @Override
    @SuppressWarnings("removal")
    public String getUniqueId(GasStack ingredient, UidContext context) {
        return "gas:" + getResourceLocation(ingredient);
    }

    @Override
    public Object getUid(GasStack ingredient, UidContext context) {
        return getResourceLocation(ingredient);
    }

    @Override
    public Iterable<Integer> getColors(GasStack ingredient) {
        if (colorHelper == null) {
            return IIngredientHelper.super.getColors(ingredient);
        }

        return colorHelper.getColors(Gas.getGasTexture(ingredient.getGasHolder()), ingredient.getHint(), 1);
    }

    @Override
    public ResourceLocation getResourceLocation(GasStack ingredient) {
        Holder<Gas> holder = ingredient.getGasHolder();
        ResourceKey<?> key = holder.getKey();
        if (key != null) {
            return key.location();
        }
        return CCBGasRegistries.GAS_REGISTRY.getKey(holder.value());
    }

    @Override
    public GasStack copyIngredient(GasStack ingredient) {
        return ingredient.copy();
    }

    @Override
    public GasStack normalizeIngredient(GasStack ingredient) {
        return ingredient.copyWithAmount(FluidType.BUCKET_VOLUME);
    }

    @Override
    public boolean isValidIngredient(GasStack ingredient) {
        return !ingredient.isEmpty();
    }

    @Override
    public Stream<ResourceLocation> getTagStream(GasStack ingredient) {
        return ingredient.getTags().map(TagKey::location);
    }

    @Override
    public String getErrorInfo(@Nullable GasStack ingredient) {
        GasStack stack = ingredient == null ? GasStack.EMPTY : ingredient;
        ToStringHelper helper = MoreObjects.toStringHelper(GasStack.class);
        Holder<Gas> gasHolder = stack.getGasHolder();
        helper.add("Gas", gasHolder.value().isEmpty() ? "none" : stack.getTranslationKey());
        if (!stack.isEmpty()) {
            helper.add("Amount", stack.getAmount());
        }
        return helper.toString();
    }
}
