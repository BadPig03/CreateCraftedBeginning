package net.ty.createcraftedbeginning.gametests.gas.release;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.gas.release.GasReleaseState;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InstantPotionGasReleaseGameTests {
    private static final BlockPos RELEASE_POS = new BlockPos(2, 2, 2);

    private InstantPotionGasReleaseGameTests() {
    }

    @GameTest(template = "gametest/empty_5x12x5")
    public static void lowFlowTriggersOnlyAfterEachHundredGasUnits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(RELEASE_POS);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, RELEASE_POS);
        villager.setNoGravity(true);
        villager.setHealth(1);
        GasStack gas = CCBGases.POTION_GAS.get().createStack(5, new PotionContents(Potions.HEALING));
        GasReleaseState state = new GasReleaseState();
        for (int tick = 2; tick <= 80; tick += 2) {
            helper.runAtTickTime(tick, () -> GasReleaseService.release(level, GasReleaseRequest.radial(gas, source, GasReleaseCause.ATMOSPHERIC_OUTLET), state));
        }
        helper.runAtTickTime(39, () -> helper.assertTrue(villager.getHealth() == 1, "Instant effects must not use the sustained-effect threshold."));
        helper.runAtTickTime(41, () -> helper.assertTrue(villager.getHealth() == 5, "The first 100 GU must apply one full healing effect."));
        helper.runAtTickTime(79, () -> helper.assertTrue(villager.getHealth() == 5, "Partial gas and active cooldown must not apply another effect."));
        helper.runAtTickTime(81, () -> {
            helper.assertTrue(villager.getHealth() == 9 && villager.getActiveEffects().isEmpty(), "The next 100 GU after two seconds must heal once without adding a timed effect.");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_5x12x5")
    public static void partialInstantGasBudgetSurvivesReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(RELEASE_POS);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, RELEASE_POS);
        villager.setHealth(1);
        GasStack gas = CCBGases.POTION_GAS.get().createStack(99, new PotionContents(Potions.HEALING));
        GasReleaseState state = new GasReleaseState();
        GasReleaseService.release(level, GasReleaseRequest.radial(gas, source, GasReleaseCause.ATMOSPHERIC_OUTLET), state);
        GasReleaseState restored = GasReleaseState.read(state.write(level.registryAccess()), level.registryAccess());
        helper.assertTrue(villager.getHealth() == 1 && restored.getEffectProgress() == 99, "Reload must retain the full instant-effect remainder without clamping it to the sustained threshold.");
        GasReleaseRequest remaining = GasReleaseRequest.radial(gas.copyWithAmount(1), source, GasReleaseCause.ATMOSPHERIC_OUTLET);
        helper.assertTrue(GasReleaseService.release(level, remaining, restored).effectDue() && villager.getHealth() == 5, "The saved instant gas budget must need only its final GU to apply one effect.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5")
    public static void burstReleaseCapsTargetsAndOverlappingOutletsDoNotStack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(RELEASE_POS);
        List<Villager> villagers = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, RELEASE_POS);
            villager.setHealth(1);
            villagers.add(villager);
        }
        GasStack gas = CCBGases.POTION_GAS.get().createStack(20000, new PotionContents(Potions.STRONG_HEALING));
        GasReleaseService.release(level, GasReleaseRequest.radial(gas, source, GasReleaseCause.TANK_REMOVAL));
        helper.assertTrue(villagers.stream().filter(villager -> villager.getHealth() == 9).count() == 4 && villagers.stream().filter(villager -> villager.getHealth() == 1).count() == 1, "A large instant release must apply exactly one pulse to at most four targets.");
        GasReleaseService.release(level, GasReleaseRequest.radial(gas.copyWithAmount(100), source.above(), GasReleaseCause.MANUAL_VENT, GasPressure.pascals(10)));
        helper.assertTrue(villagers.stream().allMatch(villager -> villager.getHealth() == 9), "An overlapping outlet must cover only targets outside the shared effect cooldown.");
        GasStack weaker = CCBGases.POTION_GAS.get().createStack(100, new PotionContents(Potions.HEALING));
        GasReleaseService.release(level, GasReleaseRequest.radial(weaker, source, GasReleaseCause.MANUAL_VENT));
        helper.assertTrue(villagers.stream().allMatch(villager -> villager.getHealth() == 9), "Changing effect level must not bypass target cooldown.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 70)
    public static void targetCooldownSurvivesEntityReloadAndDifferentLevels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(RELEASE_POS);
        Villager original = helper.spawnWithNoFreeWill(EntityType.VILLAGER, RELEASE_POS);
        original.setNoGravity(true);
        original.setHealth(1);
        GasStack gas = CCBGases.POTION_GAS.get().createStack(100, new PotionContents(Potions.HEALING));
        GasReleaseService.release(level, GasReleaseRequest.radial(gas, source, GasReleaseCause.MANUAL_VENT));
        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        saved.remove("UUID");
        original.discard();
        Villager restored = helper.spawnWithNoFreeWill(EntityType.VILLAGER, RELEASE_POS);
        restored.load(saved);
        GasStack stronger = CCBGases.POTION_GAS.get().createStack(100, new PotionContents(Potions.STRONG_HEALING));
        helper.runAtTickTime(39, () -> {
            GasReleaseService.release(level, GasReleaseRequest.radial(stronger, source, GasReleaseCause.MANUAL_VENT, GasPressure.pascals(10)));
            helper.assertTrue(restored.getHealth() == 5, "Entity reload, pressure and amplifier changes must preserve the shared cooldown.");
        });
        helper.runAtTickTime(40, () -> {
            GasReleaseService.release(level, GasReleaseRequest.radial(stronger, source, GasReleaseCause.MANUAL_VENT));
            helper.assertTrue(restored.getHealth() == 13, "A saved target cooldown must expire after exactly forty ticks.");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 70)
    public static void outletCooldownSurvivesZeroRemainderSaveAndDropsBlockedPulses(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(RELEASE_POS);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, RELEASE_POS);
        villager.setNoGravity(true);
        villager.setHealth(10);
        GasStack healing = CCBGases.POTION_GAS.get().createStack(100, new PotionContents(Potions.HEALING));
        GasReleaseState state = new GasReleaseState();
        GasReleaseService.release(level, GasReleaseRequest.radial(healing, source, GasReleaseCause.ATMOSPHERIC_OUTLET), state);
        GasReleaseState restored = GasReleaseState.read(state.write(level.registryAccess()), level.registryAccess());
        GasStack harming = CCBGases.POTION_GAS.get().createStack(20000, new PotionContents(Potions.HARMING));
        helper.runAtTickTime(1, () -> {
            helper.assertTrue(!GasReleaseService.release(level, GasReleaseRequest.radial(harming, source, GasReleaseCause.ATMOSPHERIC_OUTLET, GasPressure.pascals(10)), restored).effectDue(), "A zero-remainder save and gas change must not reset the outlet cooldown.");
            helper.assertTrue(villager.getHealth() == 14 && restored.getEffectProgress() == 0, "A blocked high-flow pulse must not apply or accumulate delayed attempts.");
        });
        helper.runAtTickTime(40, () -> {
            GasReleaseRequest smallRelease = GasReleaseRequest.radial(harming.copyWithAmount(1), source, GasReleaseCause.ATMOSPHERIC_OUTLET, GasPressure.pascals(10));
            helper.assertTrue(!GasReleaseService.release(level, smallRelease, restored).effectDue(), "Expired cooldown must not replay previously blocked pulses.");
            GasReleaseRequest rest = GasReleaseRequest.radial(harming.copyWithAmount(99), source, GasReleaseCause.ATMOSPHERIC_OUTLET, GasPressure.pascals(10));
            helper.assertTrue(GasReleaseService.release(level, rest, restored).effectDue() && villager.getHealth() == 8, "A new paid pulse must trigger after the saved outlet cooldown expires.");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_5x12x5")
    public static void nativeInstantEffectsKeepUndeadInversion(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(RELEASE_POS);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, RELEASE_POS);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, RELEASE_POS);
        villager.setHealth(10);
        zombie.setHealth(20);
        GasStack healing = CCBGases.POTION_GAS.get().createStack(100, new PotionContents(Potions.HEALING));
        GasReleaseService.release(level, GasReleaseRequest.radial(healing, source, GasReleaseCause.MANUAL_VENT));
        helper.assertTrue(villager.getHealth() == 14 && zombie.getHealth() == 14, "Instant healing must retain native undead inversion.");
        GasStack harming = CCBGases.POTION_GAS.get().createStack(100, new PotionContents(Potions.HARMING));
        GasReleaseService.release(level, GasReleaseRequest.radial(harming, source, GasReleaseCause.MANUAL_VENT));
        helper.assertTrue(villager.getHealth() == 8 && zombie.getHealth() == 18, "A different instant effect must retain native behavior and use a separate target cooldown.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5")
    public static void arbitraryInstantImplementationRunsOnFullHealthTargets(GameTestHelper helper) {
        MobEffect customEffect = new MobEffect(MobEffectCategory.NEUTRAL, 0x123456) {
            @Override
            public boolean isInstantenous() {
                return true;
            }

            @Override
            public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource, LivingEntity target, int amplifier, double strength) {
                CompoundTag data = target.getPersistentData();
                data.putInt("InstantGasTestCalls", data.getInt("InstantGasTestCalls") + 1);
                data.putInt("InstantGasTestAmplifier", amplifier);
                data.putDouble("InstantGasTestStrength", strength);
            }
        };
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, RELEASE_POS);
        PotionContents contents = new PotionContents(Optional.empty(), Optional.empty(), List.of(new MobEffectInstance(Holder.direct(customEffect), 0, 2)));
        GasStack gas = CCBGases.POTION_GAS.get().createStack(100, contents);
        GasReleaseRequest request = GasReleaseRequest.radial(gas, helper.absolutePos(RELEASE_POS), GasReleaseCause.MANUAL_VENT);
        GasReleaseService.release(helper.getLevel(), request);
        CompoundTag data = villager.getPersistentData();
        helper.assertTrue(data.getInt("InstantGasTestCalls") == 1 && data.getInt("InstantGasTestAmplifier") == 2 && data.getDouble("InstantGasTestStrength") == 1.0, "Custom instant effects must run their own implementation at full strength without a whitelist or duration requirement.");
        helper.assertTrue(villager.getHealth() == villager.getMaxHealth() && villager.getActiveEffects().isEmpty(), "Full health must not exclude an arbitrary instant effect or add a timed status.");
        GasReleaseService.release(helper.getLevel(), request);
        helper.assertTrue(data.getInt("InstantGasTestCalls") == 1, "Custom instant effects must also obey target cooldown.");
        helper.succeed();
    }
}
