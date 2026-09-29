package net.ty.createcraftedbeginning.gametests;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.MushroomCow.MushroomType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBEntityFlags;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FungalInfectionGameTests {
    private FungalInfectionGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void rejectsImmuneMobs(GameTestHelper helper) {
        Bogged bogged = helper.spawnWithNoFreeWill(EntityType.BOGGED, new BlockPos(1, 1, 1));
        MushroomCow mushroomCow = helper.spawnWithNoFreeWill(EntityType.MOOSHROOM, new BlockPos(3, 1, 1));
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 1, 3));
        helper.assertTrue(CCBEntityFlags.IMMUNE_TO_FUNGAL_INFECTION.matches(bogged), "Bogged is missing from the immunity tag");
        helper.assertTrue(CCBEntityFlags.IMMUNE_TO_FUNGAL_INFECTION.matches(mushroomCow), "Mooshroom is missing from the immunity tag");
        helper.assertTrue(!bogged.addEffect(new MobEffectInstance(CCBMobEffects.FUNGAL_INFECTION, 600)), "Bogged accepted fungal infection");
        helper.assertTrue(!mushroomCow.addEffect(new MobEffectInstance(CCBMobEffects.FUNGAL_INFECTION, 600)), "Mooshroom accepted fungal infection");
        mushroomCow.setVariant(MushroomType.BROWN);
        helper.assertTrue(!mushroomCow.addEffect(new MobEffectInstance(CCBMobEffects.FUNGAL_INFECTION, 600)), "Brown mooshroom accepted fungal infection");
        helper.assertTrue(pig.addEffect(new MobEffectInstance(CCBMobEffects.FUNGAL_INFECTION, 600)), "Ordinary pig rejected fungal infection");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 200)
    public static void damagesOnceEveryEightyTicks(GameTestHelper helper) {
        MobEffect infectionEffect = CCBMobEffects.FUNGAL_INFECTION.get();
        for (int duration : List.of(79, 80, 81, 159, 160, 161)) {
            boolean shouldDamage = duration == 80 || duration == 160;
            for (int amplifier : List.of(0, 2)) {
                helper.assertValueEqual(infectionEffect.shouldApplyEffectTickThisTick(duration, amplifier), shouldDamage, "infection damage scheduling at duration " + duration + " and amplifier " + amplifier);
            }
        }

        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 2));
        pig.setNoGravity(true);
        float initialHealth = pig.getHealth();
        pig.addEffect(new MobEffectInstance(CCBMobEffects.FUNGAL_INFECTION, 239, 2));
        helper.runAfterDelay(70, () -> helper.assertTrue(pig.getHealth() == initialHealth, "Infection dealt damage too early"));
        helper.runAfterDelay(90, () -> helper.assertTrue(pig.getHealth() == initialHealth - 3, "Level three infection did not deal three damage after eighty ticks"));
        helper.runAfterDelay(170, () -> {
            helper.assertTrue(pig.getHealth() == initialHealth - 6, "Infection did not preserve level-scaled damage and the eighty-tick interval");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_8x3x7")
    public static void lethalExplosionSpreadsThroughDyingTarget(GameTestHelper helper) {
        Pig source = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 3));
        Pig intermediate = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
        IronGolem target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(6, 1, 3));
        source.setNoGravity(true);
        intermediate.setNoGravity(true);
        target.setNoGravity(true);
        AttributeInstance intermediateResistance = intermediate.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        if (intermediateResistance == null) {
            throw new NullPointerException("Expected explosion knockback resistance on the intermediate pig at " + intermediate.blockPosition() + '.');
        }

        intermediateResistance.setBaseValue(1);
        AttributeInstance targetResistance = target.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        if (targetResistance == null) {
            throw new NullPointerException("Expected explosion knockback resistance on the target iron golem at " + target.blockPosition() + '.');
        }

        targetResistance.setBaseValue(1);
        intermediate.setHealth(1);
        BlockPos preservedBlock = new BlockPos(1, 0, 3);
        helper.setBlock(preservedBlock, Blocks.STONE);
        source.addEffect(new MobEffectInstance(CCBMobEffects.FUNGAL_INFECTION, 600, 2));
        source.hurt(source.damageSources().genericKill(), Float.MAX_VALUE);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(!intermediate.isAlive(), "Fungal explosion did not kill the nearby low-health pig");
            helper.assertTrue(intermediate.hasEffect(CCBMobEffects.FUNGAL_INFECTION), "Lethal explosion failed to infect its target before death");
            helper.assertTrue(!target.hasEffect(CCBMobEffects.FUNGAL_INFECTION), "First explosion infected an out-of-range target");
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(target.hasEffect(CCBMobEffects.FUNGAL_INFECTION), "Infected dying target did not spread infection through its own explosion");
            MobEffectInstance infection = target.getEffect(CCBMobEffects.FUNGAL_INFECTION);
            if (infection == null) {
                throw new NullPointerException("Expected fungal infection on the chain explosion target at " + target.blockPosition() + '.');
            }

            helper.assertTrue(infection.getAmplifier() == 2, "Chain infection did not preserve level three");
            helper.assertTrue(infection.getDuration() > 1750 && infection.getDuration() <= 1800, "Level three infection did not spread with ninety seconds of duration");
            helper.assertBlockPresent(Blocks.STONE, preservedBlock);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void infectedCowImmediatelyBecomesHealthyMooshroom(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 1, 2));
        cow.setAge(-1200);
        cow.setHealth(6);
        cow.setCustomName(Component.literal("Fungal conversion"));
        AABB bounds = cow.getBoundingBox().inflate(1);
        cow.addEffect(new MobEffectInstance(CCBMobEffects.FUNGAL_INFECTION, 600, 2));
        helper.assertTrue(cow.isRemoved(), "Infected ordinary cow was not immediately replaced");
        List<MushroomCow> mushroomCows = helper.getLevel().getEntitiesOfClass(MushroomCow.class, bounds);
        helper.assertTrue(mushroomCows.size() == 1, "Conversion did not create exactly one mooshroom");
        MushroomCow mushroomCow = mushroomCows.getFirst();
        helper.assertTrue(mushroomCow.getAge() == -1200 && mushroomCow.getHealth() == 6, "Conversion changed age or health");
        helper.assertTrue(Component.literal("Fungal conversion").equals(mushroomCow.getCustomName()), "Conversion lost the custom name");
        helper.assertTrue(!mushroomCow.hasEffect(CCBMobEffects.FUNGAL_INFECTION), "Converted mooshroom retained fungal infection");
        helper.succeed();
    }
}
