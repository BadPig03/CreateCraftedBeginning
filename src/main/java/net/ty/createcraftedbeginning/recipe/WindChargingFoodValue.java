package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodProperties.PossibleEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingAction;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipeLookup.WindChargingData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class WindChargingFoodValue {
    private static final double EFFECT_SCORE_EPSILON = 1.0E-9;
    private static final TagKey<Item> WIND_CHARGING_EXCLUDED = ItemTags.create(CCBAPI.asResource("wind_charging_excluded"));
    private static final WindChargingData EMPTY = new WindChargingData(WindChargingAction.CHARGE, 0, 0, ItemStack.EMPTY);
    private final ItemStack stack;

    public WindChargingFoodValue(ItemStack stack) {
        this.stack = stack;
    }

    private static double getEffectScore(List<PossibleEffect> effects) {
        double score = 0;
        for (PossibleEffect possibleEffect : effects) {
            MobEffectInstance instance = possibleEffect.effect();
            MobEffectCategory effectCategory = instance.getEffect().value().getCategory();
            double effectSign = switch (effectCategory) {
                case BENEFICIAL -> 1;
                case HARMFUL -> -1;
                default -> 0;
            };
            if (effectSign == 0) {
                continue;
            }

            double probability = Mth.clamp(possibleEffect.probability(), 0.0F, 1.0F);
            if (probability <= 0) {
                continue;
            }

            double amplifierLevel = instance.getAmplifier() + 1;
            score += effectSign * amplifierLevel * probability * getDurationFactor(instance);
        }
        if (Math.abs(score) < EFFECT_SCORE_EPSILON) {
            return 0;
        }

        return score;
    }

    private static double getDurationFactor(MobEffectInstance instance) {
        if (instance.getEffect().value().isInstantenous()) {
            return 1;
        }

        if (instance.isInfiniteDuration()) {
            return 2;
        }

        double durationSeconds = Math.max(1, instance.getDuration() / 20.0);
        return Math.min(2, Math.log1p(durationSeconds) / Math.log1p(30));
    }

    private static double getChargeMultiplier(double effectScore) {
        if (effectScore >= 0) {
            return 1 + effectScore;
        }

        return -2 * Math.min(1, -effectScore) * (1 - effectScore);
    }

    public WindChargingData calculate() {
        FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, null);
        if (foodProperties == null || stack.is(WIND_CHARGING_EXCLUDED)) {
            return EMPTY;
        }

        double foodValue = 0.5 * foodProperties.nutrition() + foodProperties.saturation();
        if (foodValue <= 0) {
            return EMPTY;
        }

        double effectScore = getEffectScore(foodProperties.effects());
        double chargeMultiplier = getChargeMultiplier(effectScore);
        double calculatedTime = Math.pow(foodValue, 1.39858) * 100 * Math.abs(chargeMultiplier);
        int chargingMagnitude = !GasConsumptionMath.isFinite(calculatedTime) || calculatedTime >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Mth.ceil(calculatedTime);
        if (chargingMagnitude <= 0) {
            return EMPTY;
        }

        int chargingTime = chargeMultiplier < 0 ? -chargingMagnitude : chargingMagnitude;
        return new WindChargingData(WindChargingAction.CHARGE, chargingTime, 1, ItemStack.EMPTY);
    }
}
