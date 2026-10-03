package net.ty.createcraftedbeginning.gametests.gas.release;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.gas.release.GasReleaseState;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PotionGasReleaseGameTests {
    private static final BlockPos RELEASE_POS = new BlockPos(2, 2, 2);

    private PotionGasReleaseGameTests() {
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 160)
    public static void lowFlowMaintainsEffectThenExpiresAfterSupplyStops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(RELEASE_POS);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, RELEASE_POS);
        pig.setNoGravity(true);
        GasStack gas = CCBGases.POTION_GAS.get().createStack(1, new PotionContents(Potions.LONG_SWIFTNESS));
        GasReleaseState state = new GasReleaseState();
        for (int tick = 2; tick <= 80; tick += 2) {
            helper.runAtTickTime(tick, () -> GasReleaseService.release(level, GasReleaseRequest.radial(gas, source, GasReleaseCause.ATMOSPHERIC_OUTLET), state));
        }
        helper.runAtTickTime(39, () -> helper.assertTrue(!pig.hasEffect(MobEffects.MOVEMENT_SPEED), "Less than 20 GU must not apply potion effects."));
        helper.runAtTickTime(81, () -> {
            MobEffectInstance effect = pig.getEffect(MobEffects.MOVEMENT_SPEED);
            if (effect == null) {
                throw new NullPointerException("Expected speed after two paid low-flow applications.");
            }

            helper.assertTrue(effect.getDuration() >= 58 && effect.getDuration() <= 60 && effect.getAmplifier() == 0, "Low-flow refresh must retain potion level and short duration.");
        });
        helper.runAtTickTime(143, () -> {
            helper.assertTrue(!pig.hasEffect(MobEffects.MOVEMENT_SPEED), "Potion gas must expire within three seconds after supply stops.");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_5x12x5")
    public static void partialBudgetSurvivesSaveAndChangesResetIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(RELEASE_POS);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, RELEASE_POS);
        GasStack speed = CCBGases.POTION_GAS.get().createStack(7, new PotionContents(Potions.SWIFTNESS));
        GasReleaseState state = new GasReleaseState();
        GasReleaseService.release(level, GasReleaseRequest.radial(speed, source, GasReleaseCause.ATMOSPHERIC_OUTLET), state);
        CompoundTag saved = state.write(level.registryAccess());
        GasReleaseState restored = GasReleaseState.read(saved, level.registryAccess());
        helper.assertTrue(restored.getEffectProgress() == 7, "Potion release budget must survive saving below the old 1000 GU threshold.");
        GasReleaseService.release(level, GasReleaseRequest.radial(speed.copyWithAmount(13), source, GasReleaseCause.ATMOSPHERIC_OUTLET), restored);
        helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SPEED), "Saved 7 GU plus 13 GU must trigger one effect.");
        GasReleaseService.release(level, GasReleaseRequest.radial(speed.copyWithAmount(19), source, GasReleaseCause.ATMOSPHERIC_OUTLET), state);
        GasStack fire = CCBGases.POTION_GAS.get().createStack(1, new PotionContents(Potions.FIRE_RESISTANCE));
        GasReleaseService.release(level, GasReleaseRequest.radial(fire, source, GasReleaseCause.ATMOSPHERIC_OUTLET), state);
        helper.assertTrue(!pig.hasEffect(MobEffects.FIRE_RESISTANCE) && state.getEffectProgress() == 1, "Switching potions must not spend the previous potion's budget.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5")
    public static void overlappingOutletsCapTargetsAndPreserveStrongerEffects(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(RELEASE_POS);
        List<Pig> pigs = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            pigs.add(helper.spawnWithNoFreeWill(EntityType.PIG, RELEASE_POS));
        }
        GasStack gas = CCBGases.POTION_GAS.get().createStack(20, new PotionContents(Potions.SWIFTNESS));
        GasReleaseRequest request = GasReleaseRequest.radial(gas, source, GasReleaseCause.ATMOSPHERIC_OUTLET);
        GasReleaseService.release(level, request, new GasReleaseState());
        helper.assertTrue(pigs.stream().filter(pig -> pig.hasEffect(MobEffects.MOVEMENT_SPEED)).count() == 4, "One 20 GU application must affect at most four targets.");
        GasReleaseService.release(level, request, new GasReleaseState());
        for (Pig pig : pigs) {
            MobEffectInstance effect = pig.getEffect(MobEffects.MOVEMENT_SPEED);
            if (effect == null) {
                throw new NullPointerException("Expected overlapping paid outlets to cover the remaining target.");
            }

            helper.assertTrue(effect.getDuration() == 60 && effect.getAmplifier() == 0, "Overlapping outlets must not stack duration or level.");
        }
        Pig protectedPig = pigs.getFirst();
        protectedPig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
        GasReleaseService.release(level, request);
        MobEffectInstance stronger = protectedPig.getEffect(MobEffects.MOVEMENT_SPEED);
        if (stronger == null) {
            throw new NullPointerException("Expected existing stronger potion effect to remain.");
        }

        helper.assertTrue(stronger.getDuration() == 600 && stronger.getAmplifier() == 1, "Gas must not shorten or downgrade an existing stronger effect.");
        Pig longEffectPig = pigs.get(1);
        longEffectPig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600));
        GasReleaseService.release(level, GasReleaseRequest.radial(gas.copyWithAmount(20000), source, GasReleaseCause.TANK_REMOVAL));
        MobEffectInstance longer = longEffectPig.getEffect(MobEffects.MOVEMENT_SPEED);
        if (longer == null) {
            throw new NullPointerException("Expected existing long-duration potion effect to remain.");
        }

        helper.assertTrue(longer.getDuration() == 600, "A large one-shot release must not shorten existing same-level effects.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5")
    public static void unsupportedPotionGasDoesNotApplyEffects(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(RELEASE_POS);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, RELEASE_POS);
        pig.setHealth(5);
        List<PotionContents> unsupported = List.of(new PotionContents(Potions.TURTLE_MASTER), PotionContents.EMPTY);
        for (PotionContents contents : unsupported) {
            GasStack gas = CCBGases.POTION_GAS.get().createStack(20000, contents);
            GasReleaseService.release(level, GasReleaseRequest.radial(gas, source, GasReleaseCause.MANUAL_VENT));
        }
        helper.assertTrue(pig.getHealth() == 5 && pig.getActiveEffects().isEmpty(), "Multi-effect and empty potion gas must not bypass supported-effect restrictions.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 90)
    public static void pressureExpandsCoverageAndLeavingTheAreaStopsRefresh(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(RELEASE_POS);
        Pig nearby = helper.spawnWithNoFreeWill(EntityType.PIG, RELEASE_POS);
        Pig distant = helper.spawnWithNoFreeWill(EntityType.PIG, RELEASE_POS.offset(2, 0, 0));
        nearby.setNoGravity(true);
        distant.setNoGravity(true);
        GasStack fire = CCBGases.POTION_GAS.get().createStack(20, new PotionContents(Potions.FIRE_RESISTANCE));
        GasReleaseService.release(level, GasReleaseRequest.radial(fire, source, GasReleaseCause.MANUAL_VENT, GasPressure.REFERENCE_PRESSURE_PA));
        helper.assertTrue(nearby.hasEffect(MobEffects.FIRE_RESISTANCE) && !distant.hasEffect(MobEffects.FIRE_RESISTANCE), "Normal pressure must apply only within its release bounds.");
        GasReleaseService.release(level, GasReleaseRequest.radial(fire, source, GasReleaseCause.MANUAL_VENT, GasPressure.pascals(10)));
        helper.assertTrue(distant.hasEffect(MobEffects.FIRE_RESISTANCE), "High pressure must expand coverage using the normal potion handler.");
        GasStack poison = CCBGases.POTION_GAS.get().createStack(20, new PotionContents(Potions.POISON));
        GasReleaseService.release(level, GasReleaseRequest.radial(poison, source, GasReleaseCause.MANUAL_VENT));
        helper.assertTrue(nearby.hasEffect(MobEffects.POISON), "Negative potion effects must apply as well.");
        nearby.setPos(source.getX() + 0.5, source.getY() + 6, source.getZ() + 0.5);
        helper.runAtTickTime(40, () -> GasReleaseService.release(level, GasReleaseRequest.radial(poison, source, GasReleaseCause.MANUAL_VENT)));
        helper.runAtTickTime(65, () -> {
            helper.assertTrue(nearby.tickCount >= 60, "The departed target must keep ticking inside the test structure.");
            helper.assertTrue(!nearby.hasEffect(MobEffects.POISON) && !nearby.hasEffect(MobEffects.FIRE_RESISTANCE), "Leaving the area must stop refreshing both positive and negative effects.");
            helper.succeed();
        });
    }
}
