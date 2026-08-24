package net.ty.createcraftedbeginning.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import io.netty.buffer.ByteBuf;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.SequencedAssemblyWithGasRecipeSerializer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SequencedAssemblyWithGasRecipe implements Recipe<RecipeWrapper> {
    public final List<ProcessingOutput> resultPool;
    public SequencedAssemblyWithGasRecipeSerializer serializer;
    public Ingredient ingredient;
    public List<SequencedWithGasRecipe<?>> sequence;
    public int loops;
    public ProcessingOutput transitionalItem;

    public SequencedAssemblyWithGasRecipe(SequencedAssemblyWithGasRecipeSerializer serializer) {
        this.serializer = serializer;
        sequence = new ArrayList<>();
        resultPool = new ArrayList<>();
        loops = 5;
    }

    public static <I extends RecipeInput, R extends ProcessingWithGasRecipe<I, ?>> @NotNull Optional<RecipeHolder<R>> getRecipe(Level level, I input, RecipeType<R> type, Class<R> recipeClass) {
        return getRecipe(level, input, type, recipeClass, recipeHolder -> recipeHolder.value().matches(input, level));
    }

    public static <I extends RecipeInput, R extends ProcessingWithGasRecipe<I, ?>> @NotNull Optional<RecipeHolder<R>> getRecipe(Level level, I input, RecipeType<R> type, Class<R> recipeClass, Predicate<? super RecipeHolder<R>> recipeFilter) {
        return getRecipes(level, input.getItem(0), type, recipeClass).filter(recipeFilter).findFirst();
    }

    public static <R extends ProcessingWithGasRecipe<?, ?>> @NotNull Stream<RecipeHolder<R>> getRecipes(Level level, ItemStack itemStack, RecipeType<R> type, Class<R> recipeClass) {
        List<RecipeHolder<SequencedAssemblyWithGasRecipe>> assemblyRecipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS.getType());
        List<RecipeHolder<R>> matchingRecipes = new ArrayList<>();
        for (RecipeHolder<SequencedAssemblyWithGasRecipe> assemblyHolder : assemblyRecipes) {
            SequencedAssemblyWithGasRecipe assemblyRecipe = assemblyHolder.value();
            if (!assemblyRecipe.appliesTo(assemblyHolder.id(), itemStack)) {
                continue;
            }

            ProcessingWithGasRecipe<?, ?> stepRecipe = assemblyRecipe.getNextRecipe(itemStack).getRecipe();
            if (stepRecipe.getType() != type || !recipeClass.isInstance(stepRecipe)) {
                continue;
            }

            stepRecipe.enforceNextResult(() -> assemblyRecipe.advance(assemblyHolder.id(), itemStack));
            matchingRecipes.add(new RecipeHolder<>(assemblyHolder.id(), recipeClass.cast(stepRecipe)));
        }

        return matchingRecipes.stream();
    }

    public static <R extends ProcessingWithGasRecipe<?, ?>> Optional<RecipeHolder<R>> getRecipe(Level level, ItemStack itemStack, RecipeType<R> type, Class<R> recipeClass) {
        for (RecipeHolder<SequencedAssemblyWithGasRecipe> assemblyHolder : level.getRecipeManager().<RecipeWrapper, SequencedAssemblyWithGasRecipe>getAllRecipesFor(CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS.getType())) {
            SequencedAssemblyWithGasRecipe assemblyRecipe = assemblyHolder.value();
            if (!assemblyRecipe.appliesTo(assemblyHolder.id(), itemStack)) {
                continue;
            }

            ProcessingWithGasRecipe<?, ?> stepRecipe = assemblyRecipe.getNextRecipe(itemStack).getRecipe();
            if (stepRecipe.getType() != type || !recipeClass.isInstance(stepRecipe)) {
                continue;
            }

            stepRecipe.enforceNextResult(() -> assemblyRecipe.advance(assemblyHolder.id(), itemStack));
            return Optional.of(new RecipeHolder<>(assemblyHolder.id(), recipeClass.cast(stepRecipe)));
        }
        return Optional.empty();
    }

    private static int getStep(ItemStack input) {
        SequencedAssemblyWithGas assemblyData = input.get(CCBRecipeDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS);
        if (assemblyData == null) {
            return 0;
        }
        return assemblyData.step();
    }

    private ItemStack advance(ResourceLocation assemblyId, ItemStack input) {
        int step = getStep(input);
        int nextStep = step + 1;
        if (nextStep / sequence.size() >= loops) {
            return rollResult();
        }

        ItemStack advancedItem = getTransitionalItem().copyWithCount(1);
        float progress = (step + 1.0f) / (sequence.size() * loops);
        SequencedAssemblyWithGas assemblyData = new SequencedAssemblyWithGas(assemblyId, nextStep, progress);
        advancedItem.set(CCBRecipeDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS, assemblyData);
        return advancedItem;
    }

    private ItemStack rollResult() {
        float totalWeight = 0;
        for (ProcessingOutput output : resultPool) {
            totalWeight += output.getChance();
        }

        float remainingWeight = new Random().nextFloat() * totalWeight;
        for (ProcessingOutput output : resultPool) {
            remainingWeight -= output.getChance();
            if (remainingWeight < 0) {
                return output.getStack().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean appliesTo(ResourceLocation assemblyId, ItemStack input) {
        if (ingredient.test(input)) {
            return true;
        }

        if (getTransitionalItem().getItem() != input.getItem()) {
            return false;
        }

        SequencedAssemblyWithGas assemblyData = input.get(CCBRecipeDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS);
        return assemblyData != null && assemblyData.id().equals(assemblyId);
    }

    public ItemStack getTransitionalItem() {
        return transitionalItem.getStack();
    }

    private SequencedWithGasRecipe<?> getNextRecipe(ItemStack input) {
        return sequence.get(getStep(input) % sequence.size());
    }

    public int getLoops() {
        return loops;
    }

    @Override
    public boolean matches(RecipeWrapper input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeWrapper input, Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(Provider registries) {
        return resultPool.getFirst().getStack();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Override
    public RecipeType<?> getType() {
        return CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS.getType();
    }

    public float getOutputChance() {
        float totalWeight = 0;
        for (ProcessingOutput output : resultPool) {
            totalWeight += output.getChance();
        }
        return resultPool.getFirst().getChance() / totalWeight;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public List<SequencedWithGasRecipe<?>> getSequence() {
        return sequence;
    }

    public record SequencedAssemblyWithGas(ResourceLocation id, int step, float progress) {
        public static final Codec<SequencedAssemblyWithGas> CODEC = RecordCodecBuilder.create(instance -> instance.group(ResourceLocation.CODEC.fieldOf("id").forGetter(SequencedAssemblyWithGas::id), Codec.INT.fieldOf("step").forGetter(SequencedAssemblyWithGas::step), Codec.FLOAT.fieldOf("progress").forGetter(SequencedAssemblyWithGas::progress)).apply(instance, SequencedAssemblyWithGas::new));
        public static final StreamCodec<ByteBuf, SequencedAssemblyWithGas> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, SequencedAssemblyWithGas::id, ByteBufCodecs.INT, SequencedAssemblyWithGas::step, ByteBufCodecs.FLOAT, SequencedAssemblyWithGas::progress, SequencedAssemblyWithGas::new);
    }
}
