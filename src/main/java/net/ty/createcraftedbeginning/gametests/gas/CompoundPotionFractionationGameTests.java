package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.potiongas.PotionGas;
import net.ty.createcraftedbeginning.recipe.FractionationTowerOutput;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe;
import net.ty.createcraftedbeginning.recipe.PotionFractionationRecipes;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CompoundPotionFractionationGameTests {
    private CompoundPotionFractionationGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void independentYieldsAndLayersIgnoreEffectOrderAndBottleType(GameTestHelper helper) {
        MobEffectInstance speed = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600);
        MobEffectInstance strength = new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9600, 1);
        List<GasStack> previous = List.of();
        for (List<MobEffectInstance> effects : List.of(List.of(speed, strength), List.of(strength, speed))) {
            PotionContents contents = new PotionContents(Optional.empty(), Optional.of(0x123456), effects);
            for (BottleType bottleType : BottleType.values()) {
                FractionationTowerRecipe recipe = PotionFractionationRecipes.create(PotionFluid.of(250, contents, bottleType));
                if (recipe == null) {
                    throw new NullPointerException("Expected compound potion fractionation for " + bottleType + '.');
                }

                List<FractionationTowerOutput> outputs = recipe.getLayerOutputs();
                helper.assertTrue(recipe.validate().isEmpty() && outputs.size() == 3 && recipe.getRequiredHeight() == 4 && recipe.getProcessingDuration() == 200 && recipe.getTemperatureCondition() == TemperatureCondition.HEATED, "Two sustained effects must require four layers and retain the existing processing conditions.");
                helper.assertTrue(outputs.getFirst().layer() == 1 && outputs.getFirst().fluid().is(Fluids.WATER) && outputs.getFirst().fluid().getAmount() == 250, "Compound fractionation must return exactly one batch of water.");
                List<GasStack> gases = new ArrayList<>();
                String previousId = "";
                for (int index = 1; index < outputs.size(); index++) {
                    FractionationTowerOutput output = outputs.get(index);
                    GasStack gas = output.gas();
                    MobEffectInstance effect = PotionGas.findReleaseEffect(gas);
                    if (effect == null) {
                        throw new NullPointerException("Expected a supported single effect at product layer " + output.layer() + '.');
                    }

                    long expectedAmount = 7200;
                    int expectedAmplifier = 0;
                    if (effect.getEffect().equals(MobEffects.DAMAGE_BOOST)) {
                        expectedAmount = 19200;
                        expectedAmplifier = 1;
                    }
                    else {
                        helper.assertTrue(effect.getEffect().equals(MobEffects.MOVEMENT_SPEED), "Compound output must not introduce an unrelated effect.");
                    }

                    String effectId = effect.getEffect().getRegisteredName();
                    helper.assertTrue(output.layer() == index + 1 && previousId.compareTo(effectId) < 0, "Gas outputs must occupy consecutive layers ordered by registered effect ID.");
                    helper.assertTrue(gas.getAmount() == expectedAmount && effect.getAmplifier() == expectedAmplifier && effect.getDuration() == 20, "Each gas must retain its own source yield and level with normalized duration.");
                    PotionContents normalized = PotionGas.getContents(gas);
                    helper.assertTrue(normalized.potion().isEmpty() && normalized.customColor().equals(contents.customColor()) && gas.getGasType().getTooltip(gas).isEmpty(), "Separated gas must preserve custom color without retaining a compound potion reference or warning.");
                    previousId = effectId;
                    gases.add(gas);
                }
                if (!previous.isEmpty()) {
                    helper.assertTrue(GasStack.matches(previous.getFirst(), gases.getFirst()) && GasStack.matches(previous.getLast(), gases.getLast()), "Effect order and bottle type must not change separated gas identities, quantities or layers.");
                }
                previous = gases;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void mixedEffectsRetainIndependentYieldsAcrossInputVariants(GameTestHelper helper) {
        MobEffectInstance speed = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600);
        MobEffectInstance healing = new MobEffectInstance(MobEffects.HEAL, 0, 1);
        Optional<Integer> color = Optional.of(0x123456);
        List<PotionContents> variants = List.of(new PotionContents(Optional.empty(), color, List.of(speed, healing)), new PotionContents(Optional.empty(), color, List.of(healing, speed)), new PotionContents(Optional.of(Potions.SWIFTNESS), color, List.of(healing)), new PotionContents(Optional.empty(), color, List.of(speed, new MobEffectInstance(MobEffects.HEAL, -1, 1))), new PotionContents(Optional.empty(), color, List.of(speed, new MobEffectInstance(MobEffects.HEAL, 9600, 1))));
        GasStack previousHealing = GasStack.EMPTY;
        GasStack previousSpeed = GasStack.EMPTY;
        for (PotionContents contents : variants) {
            for (BottleType bottleType : BottleType.values()) {
                FractionationTowerRecipe recipe = PotionFractionationRecipes.create(PotionFluid.of(250, contents, bottleType));
                if (recipe == null) {
                    throw new NullPointerException("Expected mixed potion fractionation for " + bottleType + '.');
                }

                List<FractionationTowerOutput> outputs = recipe.getLayerOutputs();
                helper.assertTrue(recipe.validate().isEmpty() && recipe.getRequiredHeight() == 4 && recipe.getProcessingDuration() == 200 && recipe.getTemperatureCondition() == TemperatureCondition.HEATED && outputs.size() == 3, "Mixed effects must retain the four-layer batch requirements.");
                FluidStack water = outputs.getFirst().fluid();
                helper.assertTrue(water.is(Fluids.WATER) && water.getAmount() == 250, "Mixed fractionation must return only one water batch.");
                FractionationTowerOutput healingOutput = outputs.get(1);
                FractionationTowerOutput speedOutput = outputs.get(2);
                GasStack healingGas = healingOutput.gas();
                GasStack speedGas = speedOutput.gas();
                MobEffectInstance healingEffect = PotionGas.findReleaseEffect(healingGas);
                MobEffectInstance speedEffect = PotionGas.findReleaseEffect(speedGas);
                if (healingEffect == null || speedEffect == null) {
                    throw new NullPointerException("Expected both normalized effects from mixed fractionation.");
                }

                helper.assertTrue(healingOutput.layer() == 2 && speedOutput.layer() == 3 && healingEffect.getEffect().equals(MobEffects.HEAL) && speedEffect.getEffect().equals(MobEffects.MOVEMENT_SPEED), "Instant health must sort before speed regardless of input order or source potion reference.");
                helper.assertTrue(healingGas.getAmount() == 800 && healingEffect.getAmplifier() == 1 && healingEffect.getDuration() == 20 && speedGas.getAmount() == 7200 && speedEffect.getAmplifier() == 0 && speedEffect.getDuration() == 20, "Instant yield must ignore duration while sustained yield retains its own source duration.");
                helper.assertTrue(PotionGas.getContents(healingGas).customColor().equals(color) && PotionGas.getContents(speedGas).customColor().equals(color) && healingGas.getGasType().getTooltip(healingGas).isEmpty() && speedGas.getGasType().getTooltip(speedGas).isEmpty(), "Mixed products must preserve custom color and remain valid single-effect gases.");
                if (!previousHealing.isEmpty()) {
                    helper.assertTrue(GasStack.matches(previousHealing, healingGas) && GasStack.matches(previousSpeed, speedGas), "Bottle type, effect order and instant duration must not change mixed product identity or quantity.");
                }
                previousHealing = healingGas;
                previousSpeed = speedGas;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void multipleInstantEffectsEachProduceOneFixedGasBatch(GameTestHelper helper) {
        List<MobEffectInstance> effects = List.of(new MobEffectInstance(MobEffects.SATURATION, 9600, 2), new MobEffectInstance(MobEffects.HEAL, 0, 1), new MobEffectInstance(MobEffects.HARM, -1));
        PotionContents contents = new PotionContents(Optional.empty(), Optional.empty(), effects);
        FractionationTowerRecipe recipe = PotionFractionationRecipes.create(PotionFluid.of(250, contents, BottleType.REGULAR));
        if (recipe == null) {
            throw new NullPointerException("Expected fractionation for three distinct instant effects.");
        }

        List<FractionationTowerOutput> outputs = recipe.getLayerOutputs();
        helper.assertTrue(recipe.validate().isEmpty() && recipe.getRequiredHeight() == 5 && outputs.size() == 4 && outputs.getFirst().fluid().getAmount() == 250, "Three instant effects must use five tower layers and one water batch.");
        String previousId = "";
        for (int index = 1; index < outputs.size(); index++) {
            FractionationTowerOutput output = outputs.get(index);
            GasStack gas = output.gas();
            MobEffectInstance effect = PotionGas.findReleaseEffect(gas);
            if (effect == null) {
                throw new NullPointerException("Expected a single instant effect at layer " + output.layer() + '.');
            }

            Holder<MobEffect> effectType = effect.getEffect();
            String effectId = effectType.getRegisteredName();
            MobEffectInstance original = effects.stream().filter(source -> source.getEffect().equals(effectType)).findFirst().orElse(null);
            if (original == null) {
                throw new NullPointerException("Unexpected instant fractionation product " + effectId + '.');
            }

            helper.assertTrue(output.layer() == index + 1 && previousId.compareTo(effectId) < 0 && gas.getAmount() == 800 && effect.getAmplifier() == original.getAmplifier() && effect.getDuration() == 20, "Every distinct instant effect must receive its own sorted 800 GU output, retaining its level.");
            previousId = effectId;
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void turtleMasterSeparatesEveryPositiveAndNegativeEffect(GameTestHelper helper) {
        for (Holder<Potion> potion : List.of(Potions.TURTLE_MASTER, Potions.LONG_TURTLE_MASTER, Potions.STRONG_TURTLE_MASTER)) {
            PotionContents contents = new PotionContents(potion);
            FractionationTowerRecipe recipe = PotionFractionationRecipes.create(PotionFluid.of(250, contents, BottleType.REGULAR));
            if (recipe == null) {
                throw new NullPointerException("Expected compound fractionation for " + potion.getRegisteredName() + '.');
            }

            helper.assertTrue(recipe.getLayerOutputs().size() == 3 && recipe.getRequiredHeight() == 4, "Turtle master must split into water and both effects.");
            for (MobEffectInstance original : contents.getAllEffects()) {
                boolean found = false;
                for (FractionationTowerOutput output : recipe.getLayerOutputs()) {
                    GasStack gas = output.gas();
                    MobEffectInstance separated = PotionGas.findReleaseEffect(gas);
                    if (separated == null || !separated.getEffect().equals(original.getEffect())) {
                        continue;
                    }

                    helper.assertTrue(gas.getAmount() == (long) original.getDuration() * 2 && separated.getAmplifier() == original.getAmplifier(), "Both positive and negative effects must retain their full duration-based yield and level.");
                    found = true;
                    break;
                }
                helper.assertTrue(found, "Compound fractionation must not silently drop an effect.");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void compoundLimitsRejectWholeInvalidInputs(GameTestHelper helper) {
        List<Holder<MobEffect>> types = List.of(MobEffects.MOVEMENT_SPEED, MobEffects.DAMAGE_BOOST, MobEffects.DAMAGE_RESISTANCE, MobEffects.FIRE_RESISTANCE, MobEffects.NIGHT_VISION, MobEffects.WATER_BREATHING, MobEffects.JUMP, MobEffects.REGENERATION);
        List<MobEffectInstance> effects = new ArrayList<>();
        for (Holder<MobEffect> type : types.subList(0, 7)) {
            effects.add(new MobEffectInstance(type, 100));
        }
        PotionContents maximum = new PotionContents(Optional.empty(), Optional.empty(), effects);
        FractionationTowerRecipe recipe = PotionFractionationRecipes.create(PotionFluid.of(250, maximum, BottleType.REGULAR));
        if (recipe == null) {
            throw new NullPointerException("Expected a seven-effect fractionation recipe.");
        }

        helper.assertTrue(recipe.validate().isEmpty() && recipe.getRequiredHeight() == 9 && recipe.getLayerOutputs().size() == 8, "Seven effects must fit exactly within the nine-layer tower limit.");
        effects.add(new MobEffectInstance(types.getLast(), 100));
        MobEffectInstance speed = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100);
        List<List<MobEffectInstance>> invalid = List.of(effects, List.of(speed, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 0)), List.of(speed, new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1)), List.of(new MobEffectInstance(MobEffects.HEAL, 0), new MobEffectInstance(MobEffects.DAMAGE_BOOST, 0)), List.of(new MobEffectInstance(MobEffects.HEAL, 0), new MobEffectInstance(MobEffects.HEAL, 200, 1)), List.of(speed, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1)));
        for (List<MobEffectInstance> entries : invalid) {
            PotionContents contents = new PotionContents(Optional.empty(), Optional.empty(), entries);
            helper.assertTrue(PotionFractionationRecipes.create(PotionFluid.of(250, contents, BottleType.REGULAR)) == null, "Oversized, invalid-duration and duplicate-effect inputs, including instant effects, must be rejected in full.");
        }
        PotionContents duplicateBaseEffect = new PotionContents(Optional.of(Potions.SWIFTNESS), Optional.empty(), List.of(speed));
        helper.assertTrue(PotionFractionationRecipes.create(PotionFluid.of(250, duplicateBaseEffect, BottleType.REGULAR)) == null, "Base and custom entries must not duplicate the yield of the same effect.");
        helper.assertTrue(PotionFractionationRecipes.create(PotionFluid.of(249, new PotionContents(Potions.TURTLE_MASTER), BottleType.REGULAR)) == null, "Compound potions must still require a complete input batch.");
        helper.succeed();
    }
}
