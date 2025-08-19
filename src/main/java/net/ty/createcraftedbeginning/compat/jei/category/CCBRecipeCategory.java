package net.ty.createcraftedbeginning.compat.jei.category;

import com.simibubi.create.AllFluids;
import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.compat.jei.JEIPlugin;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static mezz.jei.api.recipe.RecipeType.createRecipeHolderType;

@SuppressWarnings("unused")
public abstract class CCBRecipeCategory<T extends Recipe<?>> implements IRecipeCategory<RecipeHolder<T>> {
    private static final IDrawable BASIC_SLOT = asDrawable(AllGuiTextures.JEI_SLOT);
    private static final IDrawable CHANCE_SLOT = asDrawable(AllGuiTextures.JEI_CHANCE_SLOT);

    protected final RecipeType<RecipeHolder<T>> type;
    protected final Component title;
    protected final IDrawable background;
    protected final IDrawable icon;

    private final Supplier<List<RecipeHolder<T>>> recipes;
    private final List<Supplier<? extends ItemStack>> catalysts;

    public CCBRecipeCategory(Info<T> info) {
        this.type = info.recipeType();
        this.title = info.title();
        this.background = info.background();
        this.icon = info.icon();
        this.recipes = info.recipes();
        this.catalysts = info.catalysts();
    }

    public static IDrawable getRenderedSlot() {
        return BASIC_SLOT;
    }

    public static IDrawable getRenderedSlot(ProcessingOutput output) {
        return getRenderedSlot(output.getChance());
    }

    public static IDrawable getRenderedSlot(float chance) {
        if (chance == 1) {
            return BASIC_SLOT;
        }

        return CHANCE_SLOT;
    }

    public static ItemStack getResultItem(Recipe<?> recipe) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return ItemStack.EMPTY;
        }
        return recipe.getResultItem(level.registryAccess());
    }

    public static IRecipeSlotRichTooltipCallback addStochasticTooltip(ProcessingOutput output) {
        return (view, tooltip) -> {
            float chance = output.getChance();
            if (chance != 1) {
                tooltip.add(CCBLang.translateDirect("recipe.processing.chance", chance < 0.01 ? "<1" : (int) (chance * 100)).withStyle(ChatFormatting.GOLD));
            }
        };
    }

    protected static IDrawable asDrawable(AllGuiTextures texture) {
        return new IDrawable() {
            @Override
            public int getWidth() {
                return texture.getWidth();
            }

            @Override
            public int getHeight() {
                return texture.getHeight();
            }

            @Override
            public void draw(@NotNull GuiGraphics graphics, int xOffset, int yOffset) {
                texture.render(graphics, xOffset, yOffset);
            }
        };
    }

    @SuppressWarnings("removal")
    public static void addFluidSlot(IRecipeLayoutBuilder builder, int x, int y, FluidStack stack) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).setBackground(getRenderedSlot(), -1, -1).addIngredient(NeoForgeTypes.FLUID_STACK, stack).setFluidRenderer(stack.getAmount(), false, 16, 16).addTooltipCallback(CCBRecipeCategory::addPotionTooltip);
    }

    private static void addPotionTooltip(IRecipeSlotView view, List<Component> tooltip) {
        Optional<FluidStack> displayed = view.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK);
        if (displayed.isEmpty()) {
            return;
        }

        FluidStack fluidStack = displayed.get();

        if (fluidStack.getFluid().isSame(AllFluids.POTION.get())) {
            List<Component> potionTooltip = new ArrayList<>();
            PotionFluidHandler.addPotionTooltip(fluidStack, potionTooltip::add, 1);
            tooltip.addAll(1, potionTooltip.stream().toList());
        }
    }

    @NotNull
    @Override
    public RecipeType<RecipeHolder<T>> getRecipeType() {
        return type;
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }

    @SuppressWarnings("removal")
    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, RecipeHolder<T> holder, @NotNull IFocusGroup focuses) {
        setRecipe(builder, holder.value(), focuses);
    }

    @Override
    public void draw(RecipeHolder<T> holder, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics gui, double mouseX, double mouseY) {
        draw(holder.value(), recipeSlotsView, gui, mouseX, mouseY);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltipStrings(RecipeHolder<T> holder, @NotNull IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        return getTooltipStrings(holder.value(), recipeSlotsView, mouseX, mouseY);
    }

    protected abstract void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses);

    protected abstract void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY);

    protected List<Component> getTooltipStrings(T recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        return List.of();
    }

    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(type, recipes.get());
    }

    public void registerCatalysts(IRecipeCatalystRegistration registration) {
        catalysts.forEach(s -> registration.addRecipeCatalyst(s.get(), type));
    }

    public interface Factory<T extends Recipe<?>> {
        CCBRecipeCategory<T> create(CCBRecipeCategory.Info<T> info);
    }

    public record Info<T extends Recipe<?>>(RecipeType<RecipeHolder<T>> recipeType, Component title, IDrawable background, IDrawable icon, Supplier<List<RecipeHolder<T>>> recipes, List<Supplier<? extends ItemStack>> catalysts) {
    }

    public static class Builder<T extends Recipe<? extends RecipeInput>> {
        private final Class<? extends T> recipeClass;
        private final List<Consumer<List<RecipeHolder<T>>>> recipeListConsumers = new ArrayList<>();
        private final List<Supplier<? extends ItemStack>> catalysts = new ArrayList<>();
        private Supplier<Boolean> config = () -> true;
        private IDrawable background;
        private IDrawable icon;

        public Builder(Class<? extends T> recipeClass) {
            this.recipeClass = recipeClass;
        }

        public CCBRecipeCategory.Builder<T> enableWhen(Supplier<Boolean> predicate) {
            this.config = predicate;
            return this;
        }

        public CCBRecipeCategory.Builder<T> enableWhen(ConfigBase.ConfigBool configValue) {
            config = configValue::get;
            return this;
        }

        public CCBRecipeCategory.Builder<T> addRecipeListConsumer(Consumer<List<RecipeHolder<T>>> consumer) {
            recipeListConsumers.add(consumer);
            return this;
        }

        public CCBRecipeCategory.Builder<T> addRecipes(Supplier<Collection<? extends RecipeHolder<T>>> collection) {
            return addRecipeListConsumer(recipes -> recipes.addAll(collection.get()));
        }

        public CCBRecipeCategory.Builder<T> addAllRecipesIf(Predicate<RecipeHolder<T>> predicate) {
            return addRecipeListConsumer(recipes -> consumeAllRecipesOfType(recipe -> {
                if (predicate.test(recipe)) {
                    recipes.add(recipe);
                }
            }));
        }

        public CCBRecipeCategory.Builder<T> addAllRecipesIf(Predicate<RecipeHolder<?>> predicate, Function<RecipeHolder<?>, RecipeHolder<T>> converter) {
            return addRecipeListConsumer(recipes -> JEIPlugin.consumeAllRecipes(recipe -> {
                if (predicate.test(recipe)) {
                    recipes.add(converter.apply(recipe));
                }
            }));
        }

        public CCBRecipeCategory.Builder<T> addTypedRecipes(IRecipeTypeInfo recipeTypeEntry) {
            return addTypedRecipes(recipeTypeEntry::getType);
        }

        @SuppressWarnings("unchecked")
        public <I extends RecipeInput, R extends Recipe<I>> CCBRecipeCategory.Builder<T> addTypedRecipes(Supplier<net.minecraft.world.item.crafting.RecipeType<R>> recipeType) {
            return addRecipeListConsumer(recipes -> JEIPlugin.<T>consumeTypedRecipes(recipe -> {
                if (recipeClass.isInstance(recipe.value())) {
                    recipes.add((RecipeHolder<T>) recipe);
                }
            }, recipeType.get()));
        }

        public CCBRecipeCategory.Builder<T> addTypedRecipes(Supplier<net.minecraft.world.item.crafting.RecipeType<T>> recipeType, Function<RecipeHolder<?>, RecipeHolder<T>> converter) {
            return addRecipeListConsumer(recipes -> JEIPlugin.<T>consumeTypedRecipes(recipe -> recipes.add(converter.apply(recipe)), recipeType.get()));
        }

        public CCBRecipeCategory.Builder<T> addTypedRecipesIf(Supplier<net.minecraft.world.item.crafting.RecipeType<? extends T>> recipeType, Predicate<RecipeHolder<?>> predicate) {
            return addRecipeListConsumer(recipes -> consumeTypedRecipesTyped(recipe -> {
                if (predicate.test(recipe)) {
                    recipes.add(recipe);
                }
            }, recipeType.get()));
        }

        public CCBRecipeCategory.Builder<T> addTypedRecipesExcluding(Supplier<net.minecraft.world.item.crafting.RecipeType<? extends T>> recipeType, Supplier<net.minecraft.world.item.crafting.RecipeType<? extends T>> excluded) {
            return addRecipeListConsumer(recipes -> {
                List<RecipeHolder<?>> excludedRecipes = JEIPlugin.getTypedRecipes(excluded.get());
                consumeTypedRecipesTyped(recipe -> {
                    for (RecipeHolder<?> excludedRecipe : excludedRecipes) {
                        if (JEIPlugin.doInputsMatch(recipe.value(), excludedRecipe.value())) {
                            return;
                        }
                    }
                    recipes.add(recipe);
                }, recipeType.get());
            });
        }

        public CCBRecipeCategory.Builder<T> removeRecipes(Supplier<net.minecraft.world.item.crafting.RecipeType<? extends T>> recipeType) {
            return addRecipeListConsumer(recipes -> {
                List<RecipeHolder<?>> excludedRecipes = JEIPlugin.getTypedRecipes(recipeType.get());
                recipes.removeIf(recipe -> {
                    for (RecipeHolder<?> excludedRecipe : excludedRecipes) {
                        if (JEIPlugin.doInputsMatch(recipe.value(), excludedRecipe.value()) && JEIPlugin.doOutputsMatch(recipe.value(), excludedRecipe.value())) {
                            return true;
                        }
                    }
                    return false;
                });
            });
        }

        public CCBRecipeCategory.Builder<T> catalystStack(Supplier<ItemStack> supplier) {
            catalysts.add(supplier);
            return this;
        }

        public CCBRecipeCategory.Builder<T> catalyst(Supplier<ItemLike> supplier) {
            return catalystStack(() -> new ItemStack(supplier.get().asItem()));
        }

        public CCBRecipeCategory.Builder<T> icon(IDrawable icon) {
            this.icon = icon;
            return this;
        }

        public CCBRecipeCategory.Builder<T> itemIcon(ItemLike item) {
            icon(new ItemIcon(() -> new ItemStack(item)));
            return this;
        }

        public CCBRecipeCategory.Builder<T> doubleItemIcon(ItemLike item1, ItemLike item2) {
            icon(new DoubleItemIcon(() -> new ItemStack(item1), () -> new ItemStack(item2)));
            return this;
        }

        public void background(IDrawable background) {
            this.background = background;
        }

        public CCBRecipeCategory.Builder<T> emptyBackground(int width, int height) {
            background(new EmptyBackground(width, height));
            return this;
        }

        public CCBRecipeCategory<T> build(String name, CCBRecipeCategory.Factory<T> factory) {
            return build(CreateCraftedBeginning.asResource(name), factory);
        }

        public CCBRecipeCategory<T> build(ResourceLocation id, CCBRecipeCategory.Factory<T> factory) {
            Supplier<List<RecipeHolder<T>>> recipesSupplier;
            if (config.get()) {
                recipesSupplier = () -> {
                    List<RecipeHolder<T>> recipes = new ArrayList<>();
                    for (Consumer<List<RecipeHolder<T>>> consumer : recipeListConsumers) {
                        consumer.accept(recipes);
                    }
                    return recipes;
                };
            } else {
                recipesSupplier = Collections::emptyList;
            }

            CCBRecipeCategory.Info<T> info = new CCBRecipeCategory.Info<>(createRecipeHolderType(id), Component.translatable(id.getNamespace() + ".recipe." + id.getPath()), background, icon, recipesSupplier, catalysts);
            return factory.create(info);
        }

        @SuppressWarnings("unchecked")
        private void consumeAllRecipesOfType(Consumer<RecipeHolder<T>> consumer) {
            JEIPlugin.consumeAllRecipes(recipeHolder -> {
                if (recipeClass.isInstance(recipeHolder.value())) {
                    consumer.accept((RecipeHolder<T>) recipeHolder);
                }
            });
        }

        @SuppressWarnings("unchecked")
        private void consumeTypedRecipesTyped(Consumer<RecipeHolder<T>> consumer, net.minecraft.world.item.crafting.RecipeType<?> type) {
            JEIPlugin.consumeTypedRecipes(recipeHolder -> {
                if (recipeClass.isInstance(recipeHolder.value())) {
                    consumer.accept((RecipeHolder<T>) recipeHolder);
                }
            }, type);
        }
    }
}
