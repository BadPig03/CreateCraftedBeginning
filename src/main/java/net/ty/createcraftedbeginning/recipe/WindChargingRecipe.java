package net.ty.createcraftedbeginning.recipe;

import com.google.common.base.Joiner;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodProperties.PossibleEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WindChargingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    private static final TagKey<Item> WIND_CHARGING_EXCLUDED = ItemTags.create(CCBAPI.asResource("wind_charging_excluded"));
    private static final WindChargingData EMPTY = new WindChargingData(WindChargingAction.CHARGE, 0, 0, ItemStack.EMPTY);

    protected final ProcessingRecipeParams recipeParams;
    protected final WindChargingAction action;

    public WindChargingRecipe(ProcessingRecipeParams params) {
        this(params, WindChargingAction.CHARGE);
    }

    public WindChargingRecipe(ProcessingRecipeParams params, WindChargingAction action) {
        super(CCBRecipeTypes.WIND_CHARGING, params);
        recipeParams = params;
        this.action = action;
    }

    private static @Nullable WindChargingRecipe findRecipe(Level level, ItemStack itemStack) {
        List<RecipeHolder<WindChargingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.WIND_CHARGING.getType());
        for (RecipeHolder<WindChargingRecipe> holder : recipes) {
            WindChargingRecipe recipe = holder.value();
            if (!recipe.getIngredient().test(itemStack)) {
                continue;
            }

            return recipe;
        }
        return null;
    }

    public static WindChargingData getWindChargingData(Level level, ItemStack itemStack) {
        WindChargingRecipe recipe = findRecipe(level, itemStack);
        if (recipe != null) {
            int chargingTime = recipe.action == WindChargingAction.CHARGE ? recipe.processingDuration : 0;
            ItemStack recipeResult = recipe.getResultItem(level.registryAccess()).copy();
            return new WindChargingData(recipe.action, chargingTime, 1, recipeResult);
        }

        return getAutomaticWindChargingTime(itemStack);
    }

    public static WindChargingData getAutomaticWindChargingTime(ItemStack stack) {
        Item item = stack.getItem();
        FoodProperties properties = item.getFoodProperties(stack, null);
        if (properties == null || stack.is(WIND_CHARGING_EXCLUDED)) {
            return EMPTY;
        }

        double foodValue = 0.5 * properties.nutrition() + properties.saturation();
        if (foodValue <= 0) {
            return EMPTY;
        }

        double effectScore = getEffectScore(properties.effects());
        double multiplier = getChargeMultiplier(effectScore);
        double calculatedTime = Math.pow(foodValue, 1.39858) * 100 * Math.abs(multiplier);
        int magnitude = !GasConsumptions.isFinite(calculatedTime) || calculatedTime >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Mth.ceil(calculatedTime);
        if (magnitude <= 0) {
            return EMPTY;
        }

        int chargingTime = multiplier < 0 ? -magnitude : magnitude;
        return new WindChargingData(WindChargingAction.CHARGE, chargingTime, 1, ItemStack.EMPTY);
    }

    private static double getEffectScore(List<PossibleEffect> effects) {
        double score = 0;
        for (PossibleEffect possibleEffect : effects) {
            MobEffectInstance instance = possibleEffect.effect();
            MobEffectCategory category = instance.getEffect().value().getCategory();
            double sign = switch (category) {
                case BENEFICIAL -> 1;
                case HARMFUL -> -1;
                default -> 0;
            };
            if (sign == 0) {
                continue;
            }

            double probability = Math.clamp(possibleEffect.probability(), 0, 1);
            if (probability <= 0) {
                continue;
            }

            double level = instance.getAmplifier() + 1;
            score += sign * level * probability * getDurationFactor(instance);
        }
        return Math.abs(score) < 1.0E-9 ? 0 : score;
    }

    private static double getDurationFactor(MobEffectInstance instance) {
        if (instance.getEffect().value().isInstantenous()) {
            return 1;
        }

        if (instance.isInfiniteDuration()) {
            return 2;
        }

        double seconds = Math.max(1, instance.getDuration() / 20.0);
        return Math.min(2, Math.log1p(seconds) / Math.log1p(30));
    }

    private static double getChargeMultiplier(double effectScore) {
        if (effectScore >= 0) {
            return 1 + effectScore;
        }
        return -2 * Math.min(1, -effectScore) * (1 - effectScore);
    }

    public Ingredient getIngredient() {
        return ingredients.getFirst();
    }

    public WindChargingAction getAction() {
        return action;
    }

    public boolean isBadFood() {
        return action == WindChargingAction.CHARGE && processingDuration < 0;
    }

    protected ProcessingRecipeParams getRecipeParams() {
        return recipeParams;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !input.isEmpty() && getIngredient().test(input.getItem(0));
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    public List<String> validate() {
        List<String> errors = super.validate();
        if (action == WindChargingAction.CHARGE && processingDuration == 0) {
            errors.add("Wind Charging recipes with action 'charge' must specify a non-zero processing_time.");
        }
        else if (action != WindChargingAction.CHARGE && processingDuration != 0) {
            errors.add("Wind Charging recipes with action '" + action.getSerializedName() + "' must not specify processing_time.");
        }
        return errors;
    }

    public enum WindChargingAction implements StringRepresentable {
        CHARGE,
        CLEAR_ILL,
        CYCLE_CREATIVE;

        public static final Codec<WindChargingAction> CODEC = StringRepresentable.fromEnum(WindChargingAction::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, WindChargingAction> STREAM_CODEC = StreamCodec.of(FriendlyByteBuf::writeEnum, buffer -> buffer.readEnum(WindChargingAction.class));

        @Override
        public String getSerializedName() {
            return Lang.asId(name());
        }
    }

    public static class Serializer implements RecipeSerializer<WindChargingRecipe> {
        private static final MapCodec<WindChargingRecipe> CODEC = RecordCodecBuilder.<WindChargingRecipe>mapCodec(instance -> instance.group(ProcessingRecipeParams.CODEC.forGetter(WindChargingRecipe::getRecipeParams), WindChargingAction.CODEC.fieldOf("action").forGetter(WindChargingRecipe::getAction)).apply(instance, WindChargingRecipe::new)).validate(Serializer::validateRecipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, WindChargingRecipe> STREAM_CODEC = StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, WindChargingRecipe::getRecipeParams, WindChargingAction.STREAM_CODEC, WindChargingRecipe::getAction, WindChargingRecipe::new);

        private static DataResult<WindChargingRecipe> validateRecipe(WindChargingRecipe recipe) {
            List<String> errors = recipe.validate();
            if (errors.isEmpty()) {
                return DataResult.success(recipe);
            }

            errors.addFirst(recipe.getClass().getSimpleName() + " failed validation:");
            return DataResult.error(() -> Joiner.on('\n').join(errors), recipe);
        }

        @Override
        public MapCodec<WindChargingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WindChargingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public record WindChargingData(WindChargingAction action, int time, int amount, ItemStack recipeResult) {}
}
