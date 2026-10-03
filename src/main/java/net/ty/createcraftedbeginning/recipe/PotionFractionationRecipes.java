package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.AllFluids;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.potiongas.PotionGas;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PotionFractionationRecipes {
    public static final ResourceLocation RECIPE_ID = CCBAPI.asResource("potion_fractionation");
    public static final int BATCH_AMOUNT = 250;
    public static final int PROCESSING_TICKS = 200;
    private static final int NORMALIZED_EFFECT_TICKS = 20;
    private static final int GAS_PER_EFFECT_TICK = 2;
    private static final int INSTANT_GAS_PER_BATCH = 800;
    private static final int MAX_EFFECTS = FractionationTowerOutput.MAX_LAYER_OFFSET - 1;

    private PotionFractionationRecipes() {
    }

    public static @Nullable FractionationTowerRecipe create(FluidStack input) {
        if (!input.is(AllFluids.POTION.get().getSource()) || input.getAmount() < BATCH_AMOUNT) {
            return null;
        }

        PotionContents contents = input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        List<MobEffectInstance> effects = new ArrayList<>();
        Set<Holder<MobEffect>> effectTypes = new HashSet<>();
        for (MobEffectInstance effect : contents.getAllEffects()) {
            Holder<MobEffect> effectType = effect.getEffect();
            if (!PotionGas.isSupportedEffect(effect) || effects.size() >= MAX_EFFECTS || !effectTypes.add(effectType)) {
                return null;
            }

            effects.add(effect);
        }
        if (effects.isEmpty()) {
            return null;
        }

        effects.sort(Comparator.comparing(effect -> effect.getEffect().getRegisteredName()));
        FluidStack batch = input.copyWithAmount(BATCH_AMOUNT);
        Builder builder = new Builder(RECIPE_ID).require(new SizedFluidIngredient(DataComponentFluidIngredient.of(true, batch), BATCH_AMOUNT)).temperatureCondition(TemperatureCondition.HEATED).duration(PROCESSING_TICKS).outputAtLayer(1, new FluidStack(Fluids.WATER, BATCH_AMOUNT));
        int layer = 2;
        for (MobEffectInstance effect : effects) {
            long gasAmount = (long) effect.getDuration() * GAS_PER_EFFECT_TICK;
            if (effect.getEffect().value().isInstantenous()) {
                gasAmount = INSTANT_GAS_PER_BATCH;
            }

            MobEffectInstance normalizedEffect = new MobEffectInstance(effect.getEffect(), NORMALIZED_EFFECT_TICKS, effect.getAmplifier());
            PotionContents normalizedContents = new PotionContents(Optional.empty(), contents.customColor(), List.of(normalizedEffect));
            GasStack gas = CCBGases.POTION_GAS.get().createStack(gasAmount, normalizedContents);
            builder.outputAtLayer(layer++, gas);
        }
        return builder.build();
    }
}
