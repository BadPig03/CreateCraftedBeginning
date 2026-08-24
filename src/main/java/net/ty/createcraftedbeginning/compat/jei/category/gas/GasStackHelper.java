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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.client.CCBGasClientTextures;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterContainerContents;
import net.ty.createcraftedbeginning.registry.CCBItems;
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
        return colorHelper.getColors(CCBGasClientTextures.getGasTexture(ingredient.getGasHolder()), ingredient.getHint(), 1);
    }

    @Override
    public ItemStack getCheatItemStack(GasStack ingredient) {
        if (ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack canister = new ItemStack(CCBItems.CREATIVE_GAS_CANISTER.asItem());
        if (!(canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents contents)) {
            return ItemStack.EMPTY;
        }

        contents.setGasInTank(0, ingredient);
        return canister;
    }

    @Override
    public ResourceLocation getResourceLocation(GasStack ingredient) {
        Holder<Gas> holder = ingredient.getGasHolder();
        ResourceKey<?> key = holder.getKey();
        if (key != null) {
            return key.location();
        }
        return GasRegistries.GAS_REGISTRY.getKey(holder.value());
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
        ToStringHelper stringHelper = MoreObjects.toStringHelper(GasStack.class);
        Holder<Gas> gasHolder = stack.getGasHolder();
        stringHelper.add("Gas", gasHolder.value().isEmpty() ? "none" : stack.getTranslationKey());
        if (stack.isEmpty()) {
            return stringHelper.toString();
        }

        stringHelper.add("Amount", stack.getAmount());
        return stringHelper.toString();
    }
}
