package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.foundation.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.recipe.FractionationTowerCraftPlanner;
import net.ty.createcraftedbeginning.recipe.FractionationTowerCraftPlanner.InputPlan;
import net.ty.createcraftedbeginning.recipe.FractionationTowerOutput;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe;
import net.ty.createcraftedbeginning.recipe.PotionFractionationRecipes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightFractionationTowerCrafting {
    private static final int RECIPE_SEARCH_INTERVAL = 10;
    private static final int PROGRESS_SYNC_INTERVAL = 4;
    private static final String COMPOUND_KEY_RECIPE = "Recipe";
    private static final String COMPOUND_KEY_PROGRESS = "Progress";
    private static final String COMPOUND_KEY_DURATION = "Duration";
    private static final String COMPOUND_KEY_PAUSED = "Paused";
    private static final String COMPOUND_KEY_PAUSE_REASON = "PauseReason";
    private static final String COMPOUND_KEY_POTION_INPUT = "PotionInput";

    private final AirtightFractionationTowerBlockEntity tower;
    private @Nullable ResourceLocation recipeId;
    private @Nullable FractionationTowerRecipe activeRecipe;
    private FluidStack potionInput = FluidStack.EMPTY;
    private @Nullable List<RecipeHolder<FractionationTowerRecipe>> potionRecipeGeneration;
    private int progress;
    private int duration;
    private boolean paused;
    private PauseReason pauseReason = PauseReason.OTHER;
    private int searchCooldown;

    AirtightFractionationTowerCrafting(AirtightFractionationTowerBlockEntity tower) {
        this.tower = tower;
    }

    void tick() {
        Level level = tower.getLevel();
        if (level == null || level.isClientSide || !tower.isTowerController() || tower.isRemoved()) {
            return;
        }

        ResourceLocation previousRecipe = recipeId;
        int previousProgress = progress;
        int previousDuration = duration;
        boolean previouslyPaused = paused;
        PauseReason previousPauseReason = pauseReason;
        paused = recipeId != null;
        pauseReason = PauseReason.OTHER;
        advance(level);
        if (Objects.equals(previousRecipe, recipeId) && previousDuration == duration && previouslyPaused == paused && previousPauseReason == pauseReason && (previousProgress == progress || progress % PROGRESS_SYNC_INTERVAL != 0)) {
            return;
        }

        tower.notifyUpdate();
    }

    int getProgress() {
        return progress;
    }

    int getDuration() {
        return duration;
    }

    boolean isPaused() {
        return paused;
    }

    PauseReason getPauseReason() {
        return pauseReason;
    }

    CompoundTag write(Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (recipeId == null) {
            return tag;
        }

        tag.putString(COMPOUND_KEY_RECIPE, recipeId.toString());
        tag.putInt(COMPOUND_KEY_PROGRESS, progress);
        tag.putInt(COMPOUND_KEY_DURATION, duration);
        tag.putBoolean(COMPOUND_KEY_PAUSED, paused);
        tag.putString(COMPOUND_KEY_PAUSE_REASON, pauseReason.name());
        if (!potionInput.isEmpty()) {
            tag.put(COMPOUND_KEY_POTION_INPUT, potionInput.save(provider));
        }
        return tag;
    }

    void read(CompoundTag tag, Provider provider) {
        clear();
        if (!tower.isTowerController()) {
            return;
        }

        String savedRecipe = tag.getString(COMPOUND_KEY_RECIPE);
        if (savedRecipe.isEmpty()) {
            return;
        }

        recipeId = ResourceLocation.tryParse(savedRecipe);
        if (recipeId == null || recipeId.getPath().isEmpty()) {
            recipeId = null;
            return;
        }

        if (tag.contains(COMPOUND_KEY_POTION_INPUT)) {
            potionInput = FluidStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_POTION_INPUT));
            activeRecipe = PotionFractionationRecipes.create(potionInput);
            if (!PotionFractionationRecipes.RECIPE_ID.equals(recipeId) || activeRecipe == null) {
                clear();
                return;
            }
        }

        duration = Math.max(0, tag.getInt(COMPOUND_KEY_DURATION));
        progress = Math.clamp(tag.getInt(COMPOUND_KEY_PROGRESS), 0, duration);
        paused = tag.getBoolean(COMPOUND_KEY_PAUSED);
        if (!paused) {
            return;
        }

        pauseReason = switch (tag.getString(COMPOUND_KEY_PAUSE_REASON)) {
            case "TEMPERATURE" -> PauseReason.TEMPERATURE;
            case "OUTPUT" -> PauseReason.OUTPUT;
            case "STRUCTURE" -> PauseReason.STRUCTURE;
            default -> PauseReason.OTHER;
        };
    }

    void clear() {
        recipeId = null;
        activeRecipe = null;
        potionInput = FluidStack.EMPTY;
        potionRecipeGeneration = null;
        progress = 0;
        duration = 0;
        paused = false;
        pauseReason = PauseReason.OTHER;
        searchCooldown = 0;
    }

    private void advance(Level level) {
        if (recipeId == null && searchCooldown > 0) {
            searchCooldown--;
            return;
        }

        List<Layer> layers = findLayers(level);
        if (layers.isEmpty()) {
            if (paused) {
                pauseReason = PauseReason.STRUCTURE;
            }
            return;
        }

        RecipeManager recipes = level.getRecipeManager();
        List<RecipeHolder<FractionationTowerRecipe>> candidates = AirtightFractionationTowerRecipeLookup.getRecipes(recipes);
        if (recipeId != null) {
            FractionationTowerRecipe recipe;
            if (potionInput.isEmpty()) {
                recipe = AirtightFractionationTowerRecipeLookup.findRecipe(recipes, recipeId);
            }
            else {
                if (potionRecipeGeneration != null && potionRecipeGeneration != candidates) {
                    clear();
                    tower.setChanged();
                    return;
                }

                potionRecipeGeneration = candidates;
                recipe = activeRecipe;
            }
            if (recipe == null || activeRecipe != null && activeRecipe != recipe) {
                clear();
                tower.setChanged();
                return;
            }

            activeRecipe = recipe;
            AirtightFractionationTowerMode mode = tower.getProcessingMode();
            if (mode != AirtightFractionationTowerMode.NONE && (mode == AirtightFractionationTowerMode.CONDENSATION) != recipe.isCondensation()) {
                clear();
                tower.setChanged();
            }
        }
        if (activeRecipe == null) {
            searchCooldown = RECIPE_SEARCH_INTERVAL;
            for (RecipeHolder<FractionationTowerRecipe> holder : candidates) {
                FractionationTowerRecipe recipe = holder.value();
                if (recipe.getRequiredHeight() > layers.size() || !recipe.getTemperatureRecipeData().test(tower.getRecipeTemperature())) {
                    continue;
                }

                int inputIndex = recipe.isCondensation() ? layers.size() - 1 : 0;
                if (layers.get(inputIndex).planner().planInputs(recipe).isEmpty()) {
                    continue;
                }

                recipeId = holder.id();
                activeRecipe = recipe;
                progress = 0;
                break;
            }

            if (activeRecipe == null) {
                IFluidHandler fluids = layers.getFirst().inventory().getFluids();
                if (fluids != null) {
                    for (int tank = 0; tank < fluids.getTanks(); tank++) {
                        FluidStack fluid = fluids.getFluidInTank(tank);
                        FractionationTowerRecipe potionRecipe = PotionFractionationRecipes.create(fluid);
                        if (potionRecipe == null || potionRecipe.getRequiredHeight() > layers.size() || !potionRecipe.getTemperatureRecipeData().test(tower.getRecipeTemperature())) {
                            continue;
                        }

                        potionInput = fluid.copyWithAmount(PotionFractionationRecipes.BATCH_AMOUNT);
                        potionRecipeGeneration = candidates;
                        recipeId = PotionFractionationRecipes.RECIPE_ID;
                        activeRecipe = potionRecipe;
                        progress = 0;
                        break;
                    }
                }
            }
        }

        FractionationTowerRecipe recipe = activeRecipe;
        if (recipe == null) {
            return;
        }

        if (recipe.getRequiredHeight() > layers.size()) {
            clear();
            tower.setChanged();
            return;
        }

        duration = recipe.getProcessingDuration();
        paused = true;
        int inputIndex = recipe.isCondensation() ? layers.size() - 1 : 0;
        int direction = recipe.isCondensation() ? -1 : 1;
        Layer input = layers.get(inputIndex);
        Optional<InputPlan> plannedInputs = input.planner().planInputs(recipe);
        if (plannedInputs.isEmpty()) {
            clear();
            tower.setChanged();
            return;
        }

        if (!recipe.getTemperatureRecipeData().test(tower.getRecipeTemperature())) {
            pauseReason = PauseReason.TEMPERATURE;
            return;
        }

        for (FractionationTowerOutput output : recipe.getLayerOutputs()) {
            Layer layer = layers.get(inputIndex + direction * output.layer());
            if (!layer.inventory().transferOutput(output, true)) {
                pauseReason = PauseReason.OUTPUT;
                return;
            }
        }

        paused = false;
        progress = Math.min(progress, duration - 1) + 1;
        tower.setChanged();
        if (progress < duration) {
            return;
        }

        InputPlan inputPlan = plannedInputs.get();
        Provider provider = level.registryAccess();
        ResourceTransaction transaction = new ResourceTransaction().add(ResourceTransaction.participant(inputPlan::canExecute, () -> input.inventory().snapshot(provider), inputPlan::execute, snapshot -> input.inventory().restore(provider, snapshot)));
        for (FractionationTowerOutput output : recipe.getLayerOutputs()) {
            Layer layer = layers.get(inputIndex + direction * output.layer());
            transaction.add(ResourceTransaction.participant(() -> layer.inventory().transferOutput(output, true), () -> layer.inventory().snapshot(provider), () -> layer.inventory().transferOutput(output, false), snapshot -> layer.inventory().restore(provider, snapshot)));
        }
        if (!transaction.commit()) {
            paused = true;
            return;
        }

        clear();
        for (Layer layer : layers) {
            layer.controller().notifyUpdate();
        }
    }

    private List<Layer> findLayers(Level level) {
        BlockPos origin = tower.getOrigin();
        if (origin == null) {
            return List.of();
        }

        int height = tower.getHeight();
        List<Layer> layers = new ArrayList<>(height);
        for (int layer = 0; layer < height; layer++) {
            BlockPos min = origin.above(layer);
            if (!level.isLoaded(min) || !level.isLoaded(min.offset(2, 0, 0)) || !level.isLoaded(min.offset(0, 0, 2)) || !level.isLoaded(min.offset(2, 0, 2))) {
                return List.of();
            }

            BlockPos center = min.offset(1, 0, 1);
            if (!(level.getBlockEntity(center) instanceof AirtightFractionationTowerBlockEntity controller) || controller.isRemoved() || !origin.equals(controller.getOrigin()) || height != controller.getHeight()) {
                return List.of();
            }

            AirtightFractionationTowerInventory inventory = controller.getInventory();
            FractionationTowerCraftPlanner planner = inventory.createPlanner();
            if (planner == null) {
                return List.of();
            }

            layers.add(new Layer(controller, inventory, planner));
        }
        return layers;
    }

    enum PauseReason {
        OTHER,
        TEMPERATURE,
        OUTPUT,
        STRUCTURE
    }

    private record Layer(AirtightFractionationTowerBlockEntity controller, AirtightFractionationTowerInventory inventory, FractionationTowerCraftPlanner planner) {}
}
