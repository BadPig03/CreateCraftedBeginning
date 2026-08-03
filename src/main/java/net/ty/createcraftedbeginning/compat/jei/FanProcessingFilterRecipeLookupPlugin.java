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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
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
    public static final Map<ResourceLocation, ResourceLocation> PROCESSING_TYPES = new HashMap<>();

    static {
        PROCESSING_TYPES.put(Create.asResource("splashing"), Create.asResource("fan_washing"));
        PROCESSING_TYPES.put(Create.asResource("smoking"), Create.asResource("fan_smoking"));
        PROCESSING_TYPES.put(Create.asResource("blasting"), Create.asResource("fan_blasting"));
        PROCESSING_TYPES.put(Create.asResource("haunting"), Create.asResource("fan_haunting"));
        PROCESSING_TYPES.put(CreateCraftedBeginning.asResource("chilling"), CreateCraftedBeginning.asResource("chilling"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_white"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_light_gray"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_gray"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_black"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_brown"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_red"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_orange"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_yellow"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_lime"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_green"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_cyan"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_light_blue"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_blue"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_purple"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_magenta"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_pink"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "ending"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "ending"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "sanding"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "sanding"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "freezing"), ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "freezing"));
        PROCESSING_TYPES.put(ResourceLocation.fromNamespaceAndPath("dndesires", "seething"), Create.asResource("fan_seething"));
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
