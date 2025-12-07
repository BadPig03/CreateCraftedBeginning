package net.ty.createcraftedbeginning.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.ty.createcraftedbeginning.api.gas.recipes.GasIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasRecipeSerializer;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unused")
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

    public static <I extends RecipeInput, R extends ProcessingWithGasRecipe<I, ?>> @NotNull Optional<RecipeHolder<R>> getRecipe(Level world, I inv, RecipeType<R> type, Class<R> recipeClass) {
        return getRecipe(world, inv, type, recipeClass, r -> r.value().matches(inv, world));
    }

    public static <I extends RecipeInput, R extends ProcessingWithGasRecipe<I, ?>> @NotNull Optional<RecipeHolder<R>> getRecipe(Level world, @NotNull I inv, RecipeType<R> type, Class<R> recipeClass, Predicate<? super RecipeHolder<R>> recipeFilter) {
        return getRecipes(world, inv.getItem(0), type, recipeClass).filter(recipeFilter).findFirst();
    }

    public static <R extends ProcessingWithGasRecipe<?, ?>> @NotNull Stream<RecipeHolder<R>> getRecipes(@NotNull Level world, ItemStack item, RecipeType<R> type, Class<R> recipeClass) {
        List<RecipeHolder<SequencedAssemblyWithGasRecipe>> all = world.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS.getType());
        List<RecipeHolder<R>> result = new ArrayList<>();
        for (RecipeHolder<SequencedAssemblyWithGasRecipe> holder : all) {
            if (holder.value().appliesTo(holder.id(), item)) {
                ProcessingWithGasRecipe<?, ?> recipe = holder.value().getNextRecipe(item).getRecipe();
                if (recipe.getType() != type || !recipeClass.isInstance(recipe)) {
                    continue;
                }

                recipe.enforceNextResult(() -> holder.value().advance(holder.id(), item));
                result.add(new RecipeHolder<>(holder.id(), recipeClass.cast(recipe)));
            }
        }

        return result.stream();
    }

    public static <R extends ProcessingWithGasRecipe<?, ?>> Optional<RecipeHolder<R>> getRecipe(@NotNull Level world, ItemStack item, RecipeType<R> type, Class<R> recipeClass) {
        List<RecipeHolder<SequencedAssemblyWithGasRecipe>> all = world.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS.getType());
        for (RecipeHolder<SequencedAssemblyWithGasRecipe> sequencedAssemblyRecipe : all) {
            if (!sequencedAssemblyRecipe.value().appliesTo(sequencedAssemblyRecipe.id(), item)) {
                continue;
            }

            SequencedWithGasRecipe<?> nextRecipe = sequencedAssemblyRecipe.value().getNextRecipe(item);
            ProcessingWithGasRecipe<?, ?> recipe = nextRecipe.getRecipe();
            if (recipe.getType() != type || !recipeClass.isInstance(recipe)) {
                continue;
            }

            recipe.enforceNextResult(() -> sequencedAssemblyRecipe.value().advance(sequencedAssemblyRecipe.id(), item));
            return Optional.of(new RecipeHolder<>(sequencedAssemblyRecipe.id(), recipeClass.cast(recipe)));
        }

        return Optional.empty();
    }

    @SuppressWarnings({"RedundantCast", "DataFlowIssue"})
    @OnlyIn(Dist.CLIENT)
    public static void addToTooltip(@NotNull ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.has(CCBDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS)) {
            return;
        }

        SequencedAssemblyWithGas sequencedAssemblyWithGas = stack.get(CCBDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS);
        Optional<RecipeHolder<? extends Recipe<?>>> optionalRecipe = (Optional<RecipeHolder<?>>) Minecraft.getInstance().level.getRecipeManager().byKey(sequencedAssemblyWithGas.id());
        if (optionalRecipe.isEmpty()) {
            return;
        }

        Recipe<?> recipe = optionalRecipe.get().value();
        if (!(recipe instanceof SequencedAssemblyWithGasRecipe sequencedAssemblyRecipe)) {
            return;
        }

        int length = sequencedAssemblyRecipe.sequence.size();
        int step = getStep(stack);
        int total = length * sequencedAssemblyRecipe.loops;
        List<Component> tooltip = event.getToolTip();
        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CreateLang.translateDirect("recipe.sequenced_assembly").withStyle(ChatFormatting.GRAY));
        tooltip.add(CreateLang.translateDirect("recipe.assembly.progress", step, total).withStyle(ChatFormatting.DARK_GRAY));
        int remaining = total - step;
        for (int i = 0; i < length; i++) {
            if (i >= remaining) {
                break;
            }

            Component textComponent = sequencedAssemblyRecipe.sequence.get((i + step) % length).getAsAssemblyRecipe().getDescriptionForAssembly();
            tooltip.add(i == 0 ? CreateLang.translateDirect("recipe.assembly.next", textComponent).withStyle(ChatFormatting.AQUA) : Component.literal("-> ").append(textComponent).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private ItemStack advance(ResourceLocation id, ItemStack input) {
        int step = getStep(input);
        if ((step + 1) / sequence.size() >= loops) {
            return rollResult();
        }

        ItemStack advancedItem = getTransitionalItem().copyWithCount(1);
        SequencedAssemblyWithGas sequencedAssemblyWithGas = new SequencedAssemblyWithGas(id, step + 1, (step + 1.0f) / (sequence.size() * loops));
        advancedItem.set(CCBDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS, sequencedAssemblyWithGas);
        return advancedItem;
    }

    private ItemStack rollResult() {
        float totalWeight = 0;
        for (ProcessingOutput entry : resultPool) {
            totalWeight += entry.getChance();
        }
        float number = new Random().nextFloat() * totalWeight;
        for (ProcessingOutput entry : resultPool) {
            number -= entry.getChance();
            if (number < 0) {
                return entry.getStack().copy();
            }
        }

        return ItemStack.EMPTY;
    }

    @SuppressWarnings("DataFlowIssue")
    private boolean appliesTo(ResourceLocation id, ItemStack input) {
        return ingredient.test(input) || getTransitionalItem().getItem() == input.getItem() && input.has(CCBDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS) && input.get(CCBDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS).id().equals(id);
    }

    public ItemStack getTransitionalItem() {
        return transitionalItem.getStack();
    }

    private SequencedWithGasRecipe<?> getNextRecipe(ItemStack input) {
        return sequence.get(getStep(input) % sequence.size());
    }

    @SuppressWarnings("DataFlowIssue")
    private static int getStep(@NotNull ItemStack input) {
        return input.has(CCBDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS) ? input.get(CCBDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS).step() : 0;
    }

    public int getLoops() {
        return loops;
    }

    public void addAdditionalIngredientsAndMachines(@NotNull List<Ingredient> list) {
        sequence.forEach(recipe -> recipe.getAsAssemblyRecipe().addAssemblyIngredients(list));
        Set<ItemLike> machines = new HashSet<>();
        sequence.forEach(recipe -> recipe.getAsAssemblyRecipe().addRequiredMachines(machines));
        machines.stream().map(Ingredient::of).forEach(list::add);
    }

    public void addAdditionalFluidIngredients(List<SizedFluidIngredient> list) {
        sequence.forEach(recipe -> recipe.getAsAssemblyRecipe().addAssemblyFluidIngredients(list));
    }

    public void addAdditionalGasIngredients(List<GasIngredient> list) {
        sequence.forEach(recipe -> recipe.getAsAssemblyRecipe().addAssemblyGasIngredients(list));
    }

    @Override
    public boolean matches(@NotNull RecipeWrapper input, @NotNull Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeWrapper input, @NotNull Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull Provider registries) {
        return resultPool.getFirst().getStack();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS.getType();
    }

    public float getOutputChance() {
        float totalWeight = 0;
        for (ProcessingOutput entry : resultPool) {
            totalWeight += entry.getChance();
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
        public static final Codec<SequencedAssemblyWithGas> CODEC = RecordCodecBuilder.create(i -> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(SequencedAssemblyWithGas::id), Codec.INT.fieldOf("step").forGetter(SequencedAssemblyWithGas::step), Codec.FLOAT.fieldOf("progress").forGetter(SequencedAssemblyWithGas::progress)).apply(i, SequencedAssemblyWithGas::new));
        public static final StreamCodec<ByteBuf, SequencedAssemblyWithGas> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, SequencedAssemblyWithGas::id, ByteBufCodecs.INT, SequencedAssemblyWithGas::step, ByteBufCodecs.FLOAT, SequencedAssemblyWithGas::progress, SequencedAssemblyWithGas::new);
    }
}
