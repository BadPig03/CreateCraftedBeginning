package net.ty.createcraftedbeginning.compat.jei.category;

import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.createmod.catnip.config.ConfigBase.ConfigBool;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static mezz.jei.api.recipe.RecipeType.createRecipeHolderType;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CCBRecipeCategory<T extends Recipe<?>> implements IRecipeCategory<RecipeHolder<T>> {
    protected static final IDrawable BASIC_SLOT = asDrawable(AllGuiTextures.JEI_SLOT);
    protected static final IDrawable CHANCE_SLOT = asDrawable(AllGuiTextures.JEI_CHANCE_SLOT);

    protected final RecipeType<RecipeHolder<T>> type;
    protected final Component title;
    protected final IDrawable background;
    protected final IDrawable icon;

    private final Supplier<List<RecipeHolder<T>>> recipes;
    private final List<Supplier<? extends ItemStack>> catalysts;

    public CCBRecipeCategory(Info<T> info) {
        type = info.recipeType();
        title = info.title();
        background = info.background();
        icon = info.icon();
        recipes = info.recipes();
        catalysts = info.catalysts();
    }

    public static IDrawable getRenderedSlot() {
        return BASIC_SLOT;
    }

    public static IDrawable getRenderedSlot(ProcessingOutput output) {
        return getRenderedSlot(output.getChance());
    }

    public static IDrawable getRenderedSlot(float chance) {
        return chance == 1 ? BASIC_SLOT : CHANCE_SLOT;
    }

    public static ItemStack getResultItem(Recipe<?> recipe) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return ItemStack.EMPTY;
        }

        return recipe.getResultItem(level.registryAccess());
    }

    @Contract(value = "_ -> new", pure = true)
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
            public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
                texture.render(graphics, xOffset, yOffset);
            }
        };
    }

    @Override
    public RecipeType<RecipeHolder<T>> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<T> holder, IFocusGroup focuses) {
        setRecipe(builder, holder.value(), focuses);
    }

    @Override
    public void draw(RecipeHolder<T> holder, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY) {
        draw(holder.value(), recipeSlotsView, gui, mouseX, mouseY);
    }

    @Override
    public void onDisplayedIngredientsUpdate(RecipeHolder<T> holder, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
        onDisplayedIngredientsUpdate(holder.value(), recipeSlots, focuses);
    }

    @SuppressWarnings("removal")
    @Override
    public List<Component> getTooltipStrings(RecipeHolder<T> holder, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        return getTooltipStrings(holder.value(), recipeSlotsView, mouseX, mouseY);
    }

    protected void onDisplayedIngredientsUpdate(T recipe, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
    }

    protected List<Component> getTooltipStrings(T recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        return List.of();
    }

    protected abstract void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY);

    protected abstract void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses);

    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(type, recipes.get());
    }

    public void registerCatalysts(IRecipeCatalystRegistration registration) {
        catalysts.forEach(s -> registration.addRecipeCatalyst(s.get(), type));
    }

    @FunctionalInterface
    public interface Factory<T extends Recipe<?>> {
        CCBRecipeCategory<T> create(Info<T> info);
    }

    public record Info<T extends Recipe<?>>(RecipeType<RecipeHolder<T>> recipeType, Component title, IDrawable background, IDrawable icon, Supplier<List<RecipeHolder<T>>> recipes, List<Supplier<? extends ItemStack>> catalysts) {}

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

        public Builder<T> enableWhen(Supplier<Boolean> predicate) {
            config = predicate;
            return this;
        }

        public Builder<T> enableWhen(ConfigBool configValue) {
            config = configValue::get;
            return this;
        }

        public Builder<T> addRecipeListConsumer(Consumer<List<RecipeHolder<T>>> consumer) {
            recipeListConsumers.add(consumer);
            return this;
        }

        public Builder<T> addRecipes(Supplier<Collection<? extends RecipeHolder<T>>> collection) {
            return addRecipeListConsumer(recipes -> recipes.addAll(collection.get()));
        }

        public Builder<T> addAllRecipesIf(Predicate<RecipeHolder<?>> pred, Function<RecipeHolder<?>, RecipeHolder<T>> converter) {
            return addRecipeListConsumer(recipes -> CCBJEIPlugin.consumeAllRecipes(recipe -> {
                if (pred.test(recipe)) {
                    recipes.add(converter.apply(recipe));
                }
            }));
        }

        public Builder<T> addTypedRecipes(IRecipeTypeInfo recipeTypeEntry) {
            return addTypedRecipes(recipeTypeEntry::getType);
        }

        @SuppressWarnings("unchecked")
        public <I extends RecipeInput, R extends Recipe<I>> Builder<T> addTypedRecipes(Supplier<net.minecraft.world.item.crafting.RecipeType<R>> recipeType) {
            return addRecipeListConsumer(recipes -> CCBJEIPlugin.consumeTypedRecipes(recipe -> {
                if (!recipeClass.isInstance(recipe.value())) {
                    return;
                }

                recipes.add((RecipeHolder<T>) recipe);
            }, recipeType.get()));
        }

        public Builder<T> catalyst(Supplier<ItemLike> supplier) {
            return catalystStack(() -> new ItemStack(supplier.get().asItem()));
        }

        public Builder<T> catalystStack(Supplier<ItemStack> supplier) {
            catalysts.add(supplier);
            return this;
        }

        public Builder<T> itemIcon(ItemLike item) {
            icon(new ItemIcon(() -> new ItemStack(item)));
            return this;
        }

        public Builder<T> icon(IDrawable newIcon) {
            icon = newIcon;
            return this;
        }

        public Builder<T> doubleItemIcon(ItemLike item1, ItemLike item2) {
            icon(new DoubleItemIcon(() -> new ItemStack(item1), () -> new ItemStack(item2)));
            return this;
        }

        public Builder<T> emptyBackground(int width, int height) {
            background = new EmptyBackground(width, height);
            return this;
        }

        public CCBRecipeCategory<T> build(String name, Factory<T> factory) {
            return build(CreateCraftedBeginning.asResource(name), factory);
        }

        public CCBRecipeCategory<T> build(ResourceLocation id, Factory<T> factory) {
            Supplier<List<RecipeHolder<T>>> recipesSupplier = config.get() ? () -> {
                List<RecipeHolder<T>> recipes = new ArrayList<>();
                for (Consumer<List<RecipeHolder<T>>> consumer : recipeListConsumers) {
                    consumer.accept(recipes);
                }
                return recipes;
            } : Collections::emptyList;

            Info<T> info = new Info<>(createRecipeHolderType(id), Component.translatable(id.getNamespace() + ".recipe." + id.getPath()), background, icon, recipesSupplier, catalysts);
            return factory.create(info);
        }
    }
}
