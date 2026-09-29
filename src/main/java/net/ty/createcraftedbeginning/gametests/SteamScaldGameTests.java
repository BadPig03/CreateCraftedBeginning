package net.ty.createcraftedbeginning.gametests;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.steam.SteamAirCannonHandler;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SteamScaldGameTests {
    private SteamScaldGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 140)
    public static void dryScaldKeepsDurationAndContinuesDamage(GameTestHelper helper) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 2));
        pig.setNoGravity(true);
        pig.addEffect(new MobEffectInstance(CCBMobEffects.STEAM_SCALD, 100));
        pig.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40));
        float initialHealth = pig.getHealth();
        float[] firstObservedHealth = {initialHealth};
        helper.runAfterDelay(60, () -> {
            MobEffectInstance scald = pig.getEffect(CCBMobEffects.STEAM_SCALD);
            if (scald == null) {
                throw new NullPointerException("Expected steam scald during the first dry observation at " + pig.blockPosition() + '.');
            }

            float health = pig.getHealth();
            helper.assertTrue(!pig.isInWater(), "Scald test target entered water during the first dry observation");
            helper.assertValueEqual(scald.getDuration(), 100, "scald duration during the first dry observation");
            helper.assertTrue(health < initialHealth && health > 1, "Dry scald did not deal initial damage above the nonlethal floor");
            firstObservedHealth[0] = health;
        });
        helper.runAfterDelay(120, () -> {
            MobEffectInstance scald = pig.getEffect(CCBMobEffects.STEAM_SCALD);
            if (scald == null) {
                throw new NullPointerException("Expected steam scald during the second dry observation at " + pig.blockPosition() + '.');
            }

            float health = pig.getHealth();
            helper.assertTrue(!pig.isInWater(), "Scald test target entered water during the second dry observation");
            helper.assertTrue(scald.getDuration() == 100, "Dry scald duration decreased");
            helper.assertTrue(health < firstObservedHealth[0] && health >= 1, "Paused scald did not continue nonlethal damage after the first observation");
            helper.assertTrue(!pig.hasEffect(MobEffects.GLOWING), "Scald paused an unrelated effect");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 200)
    public static void waterConsumesDurationAndLeavingWaterPausesIt(GameTestHelper helper) {
        BlockPos waterPos = new BlockPos(1, 1, 1);
        helper.setBlock(waterPos, Blocks.WATER);
        helper.setBlock(waterPos.above(), Blocks.WATER);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, waterPos);
        pig.setNoGravity(true);
        pig.addEffect(new MobEffectInstance(CCBMobEffects.STEAM_SCALD, 100));
        helper.runAfterDelay(30, () -> {
            MobEffectInstance scald = pig.getEffect(CCBMobEffects.STEAM_SCALD);
            if (scald == null) {
                throw new NullPointerException("Expected steam scald during the first water exposure at " + pig.blockPosition() + '.');
            }

            helper.assertTrue(pig.isInWater() && scald.getDuration() < 100, "Water did not consume scald duration");
            pig.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 4, 4))));
            pig.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAfterDelay(40, () -> {
            MobEffectInstance scald = pig.getEffect(CCBMobEffects.STEAM_SCALD);
            if (scald == null) {
                throw new NullPointerException("Expected steam scald after leaving water at " + pig.blockPosition() + '.');
            }

            int remainingDuration = scald.getDuration();
            helper.assertTrue(!pig.isInWater(), "Scald test target did not leave water");
            helper.runAfterDelay(30, () -> {
                MobEffectInstance pausedScald = pig.getEffect(CCBMobEffects.STEAM_SCALD);
                if (pausedScald == null) {
                    throw new NullPointerException("Expected paused steam scald before returning to water at " + pig.blockPosition() + '.');
                }

                helper.assertTrue(pausedScald.getDuration() == remainingDuration, "Scald kept counting down after leaving water");
                pig.setPos(Vec3.atBottomCenterOf(helper.absolutePos(waterPos)));
                pig.setDeltaMovement(Vec3.ZERO);
            });
        });
        helper.runAfterDelay(185, () -> {
            helper.assertTrue(pig.isAlive() && !pig.hasEffect(CCBMobEffects.STEAM_SCALD), "Scald did not expire after sufficient time in water");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void damageScalesEveryLevelWithoutKilling(GameTestHelper helper) {
        IronGolem target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 1, 2));
        for (int amplifier = 0; amplifier < 6; amplifier++) {
            target.setHealth(100);
            target.invulnerableTime = 0;
            target.tickCount = 25;
            CCBMobEffects.STEAM_SCALD.get().applyEffectTick(target, amplifier);
            float expectedDamage = 1 + amplifier * 0.5F;
            helper.assertTrue(target.getHealth() == 100 - expectedDamage, "Scald damage did not increase by half a point per level");
        }

        target.setHealth(1.5F);
        target.invulnerableTime = 0;
        CCBMobEffects.STEAM_SCALD.get().applyEffectTick(target, 5);
        helper.assertTrue(target.isAlive() && target.getHealth() == 1, "Scald damage crossed the half-heart floor");
        target.setHealth(100);
        target.invulnerableTime = 0;
        target.tickCount = 26;
        CCBMobEffects.STEAM_SCALD.get().applyEffectTick(target, 0);
        helper.assertTrue(target.getHealth() == 100, "Scald dealt damage between its twenty-five-tick intervals");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_8x3x7")
    public static void steamExplosionAppliesFiveSecondScaldOnlyInRange(GameTestHelper helper) {
        Pig nearby = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 1, 2));
        Pig distant = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 2));
        Pig owner = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 2));
        Vec3 center = nearby.getBoundingBox().getCenter();
        Holder<Gas> gas = CCBGases.STEAM.get().getHolder();
        AirtightCannonWindChargeProjectileEntity projectile = new AirtightCannonWindChargeProjectileEntity(helper.getLevel(), gas, Vec3.ZERO);
        projectile.setPos(center);
        AirtightCannonShotContext context = new AirtightCannonShotContext(projectile, owner, gas, 101325, 1, 0, false);
        new SteamAirCannonHandler().explode(helper.getLevel(), center, context);
        MobEffectInstance scald = nearby.getEffect(CCBMobEffects.STEAM_SCALD);
        if (scald == null) {
            throw new NullPointerException("Expected steam scald on the nearby blast target at " + nearby.blockPosition() + '.');
        }

        helper.assertTrue(scald.getDuration() == 100 && scald.getAmplifier() == 0, "Steam blast did not apply five seconds of level-one scald");
        helper.assertTrue(!distant.hasEffect(CCBMobEffects.STEAM_SCALD), "Steam blast scalded an out-of-range target");
        helper.assertTrue(!owner.hasEffect(CCBMobEffects.STEAM_SCALD), "Steam blast scalded its owner");
        helper.succeed();
    }
}
