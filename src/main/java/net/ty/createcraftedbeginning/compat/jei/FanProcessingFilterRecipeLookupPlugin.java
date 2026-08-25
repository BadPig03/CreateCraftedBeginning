package net.ty.createcraftedbeginning.compat.jei;

import com.simibubi.create.Create;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.compat.createdragonsplus.CreateDragonsPlusCompat;
import net.ty.createcraftedbeginning.compat.dndesires.DnDesiresCompat;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FanProcessingFilterRecipeLookupPlugin implements IRecipeManagerPlugin {
    private static final Map<ResourceLocation, ResourceLocation> PROCESSING_TYPES = new HashMap<>();

    static {
        PROCESSING_TYPES.put(Create.asResource("splashing"), Create.asResource("fan_washing"));
        PROCESSING_TYPES.put(Create.asResource("smoking"), Create.asResource("fan_smoking"));
        PROCESSING_TYPES.put(Create.asResource("blasting"), Create.asResource("fan_blasting"));
        PROCESSING_TYPES.put(Create.asResource("haunting"), Create.asResource("fan_haunting"));
        PROCESSING_TYPES.put(CCBAPI.asResource("chilling"), CCBAPI.asResource("chilling"));
        if (CCBCompatMods.CREATE_DRAGONS_PLUS.isLoaded()) {
            CreateDragonsPlusCompat.registerJeiFanProcessingCategories(PROCESSING_TYPES::put);
        }
        if (CCBCompatMods.DNDESIRES.isLoaded()) {
            DnDesiresCompat.registerJeiFanProcessingCategories(PROCESSING_TYPES::put);
        }
    }

    private final Supplier<IJeiRuntime> runtimeSupplier;

    public FanProcessingFilterRecipeLookupPlugin(Supplier<IJeiRuntime> runtimeSupplier) {
        this.runtimeSupplier = runtimeSupplier;
    }

    private static @Nullable LookupTarget readTarget(IFocus<?> focus) {
        if (focus.getRole() != RecipeIngredientRole.INPUT) {
            return null;
        }

        Optional<ItemStack> focusedStack = focus.getTypedValue().getItemStack();
        ResourceLocation typeId = focusedStack.flatMap(GasInjectionChamberUtils::getFanProcessingTypeId).orElse(null);
        if (typeId == null) {
            return null;
        }

        ResourceLocation categoryId = PROCESSING_TYPES.get(typeId);
        if (categoryId == null) {
            return null;
        }
        return new LookupTarget(categoryId);
    }

    private @Nullable IRecipeCategory<?> findCategory(ResourceLocation categoryId) {
        IJeiRuntime runtime = runtimeSupplier.get();
        if (runtime == null) {
            return null;
        }
        return runtime.getRecipeManager().createRecipeCategoryLookup().get().filter(category -> category.getRecipeType().getUid().equals(categoryId)).findFirst().orElse(null);
    }

    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        LookupTarget target = readTarget(focus);
        if (target == null) {
            return List.of();
        }

        IRecipeCategory<?> category = findCategory(target.categoryId());
        if (category == null) {
            return List.of();
        }
        return List.of(category.getRecipeType());
    }

    @Override
    public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        LookupTarget target = readTarget(focus);
        if (target == null || !recipeCategory.getRecipeType().getUid().equals(target.categoryId())) {
            return List.of();
        }

        IJeiRuntime runtime = runtimeSupplier.get();
        if (runtime == null) {
            return List.of();
        }
        return runtime.getRecipeManager().createRecipeLookup(recipeCategory.getRecipeType()).get().toList();
    }

    @Override
    public <T> List<T> getRecipes(IRecipeCategory<T> recipeCategory) {
        return List.of();
    }

    private record LookupTarget(ResourceLocation categoryId) {}
}
