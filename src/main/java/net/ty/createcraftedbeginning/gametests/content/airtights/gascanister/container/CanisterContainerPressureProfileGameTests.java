package net.ty.createcraftedbeginning.gametests.content.airtights.gascanister.container;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlers;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBEnchantments;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CanisterContainerPressureProfileGameTests {
    private CanisterContainerPressureProfileGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void looseCanistersPoolAcrossPressureProfilesWithoutPartialSimulation(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack full = canister(CCBGases.NATURAL_AIR.get(), Long.MAX_VALUE);
        ItemStack partial = canister(CCBGases.NATURAL_AIR.get(), 30);
        long capacity = contents(full).getGasInTank(0).getAmount();
        helper.assertTrue(contents(full).getTankPressurePa(0) >= GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa(), "Full test canister must begin in the high-pressure profile");
        player.getInventory().setItem(0, full);
        player.getInventory().setItem(1, partial);

        AffordableFuel fuel = CanisterContainerConsumers.findAffordableFuel(player, CCBGases.NATURAL_AIR.get(), context -> capacity + 20).orElseThrow();
        helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, true), "Cross-profile simulation failed");
        helper.assertValueEqual(contents(full).getGasInTank(0).getAmount(), capacity, "Simulation changed the full canister");
        helper.assertValueEqual(contents(partial).getGasInTank(0).getAmount(), 30L, "Simulation changed the partial canister");
        helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, false), "Cross-profile consumption failed");
        helper.assertTrue(contents(full).isEmpty(), "First canister was not drained");
        helper.assertValueEqual(contents(partial).getGasInTank(0).getAmount(), 10L, "Second canister did not cover the remaining cost");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void packedCanistersPoolAcrossProfilesAndKeepPerCanisterEconomize(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack full = canister(CCBGases.NATURAL_AIR.get(), Long.MAX_VALUE);
        full.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(CCBEnchantments.ECONOMIZE), 1);
        long capacity = contents(full).getGasInTank(0).getAmount();
        ItemStack partial = canister(CCBGases.NATURAL_AIR.get(), 30);
        ItemStack pack = new ItemStack(CCBItems.GAS_CANISTER_PACK.get());
        pack.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(List.of(full, partial)));
        player.getInventory().setItem(0, pack);

        long logicalCost = capacity * 100 / 80 + 20;
        AffordableFuel fuel = CanisterContainerConsumers.findAffordableFuel(player, context -> logicalCost).orElseThrow();
        helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, false), "Cross-profile packed consumption failed");
        GasCanisterContainer remaining = contents(pack);
        helper.assertTrue(remaining.getGasInTank(0).isEmpty(), "Economized full canister was not drained");
        helper.assertValueEqual(remaining.getGasInTank(1).getAmount(), 10L, "Second packed canister inherited another canister's economize modifier");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void economizeLevelsApplyDiscountsAndMinimumDrain(GameTestHelper helper) {
        Holder<Enchantment> economize = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(CCBEnchantments.ECONOMIZE);
        helper.assertValueEqual(economize.value().getMaxLevel(), 3, "Economize compatibility must not raise the registered maximum above III.");
        int[] levels = {0, 1, 2, 3, 4, 5, 255};
        long[] expectedDrains = {101, 81, 61, 41, 21, 1, 1};
        for (boolean packed : new boolean[]{false, true}) {
            for (int scenario = 0; scenario < levels.length; scenario++) {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                ItemStack canister = canister(CCBGases.NATURAL_AIR.get(), 200);
                int economizeLevel = levels[scenario];
                if (economizeLevel > 0) {
                    canister.enchant(economize, economizeLevel);
                }

                ItemStack supply = canister;
                if (packed) {
                    supply = new ItemStack(CCBItems.GAS_CANISTER_PACK.get());
                    supply.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(List.of(canister)));
                }

                player.getInventory().setItem(0, supply);
                String context = "Economize level " + economizeLevel + " with packed=" + packed;
                AffordableFuel fuel = CanisterContainerConsumers.findAffordableFuel(player, usage -> 101).orElseThrow(() -> new IllegalStateException(context + " could not pay 101 GU of logical cost."));
                helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, true), context + " failed simulation.");
                helper.assertValueEqual(contents(supply).getGasInTank(0).getAmount(), 200L, context + " changed storage during simulation.");
                helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, false), context + " failed consumption.");
                long expectedRemaining = 200 - expectedDrains[scenario];
                helper.assertValueEqual(contents(supply).getGasInTank(0).getAmount(), expectedRemaining, context + " applied the wrong rounded discount.");

                AffordableFuel minimumFuel = CanisterContainerConsumers.findAffordableFuel(player, usage -> 1).orElseThrow(() -> new IllegalStateException(context + " could not pay 1 GU of logical cost."));
                helper.assertTrue(CanisterContainerConsumers.interactContainer(player, minimumFuel, () -> true, false), context + " failed minimum consumption.");
                helper.assertValueEqual(contents(supply).getGasInTank(0).getAmount(), expectedRemaining - 1, context + " did not consume the minimum 1 GU.");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void economizeFiveAndHigherConsumeTheLastGasUnit(GameTestHelper helper) {
        Holder<Enchantment> economize = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(CCBEnchantments.ECONOMIZE);
        for (boolean packed : new boolean[]{false, true}) {
            for (int economizeLevel : new int[]{5, 6, 255}) {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                ItemStack canister = canister(CCBGases.NATURAL_AIR.get(), 1);
                canister.enchant(economize, economizeLevel);
                ItemStack supply = canister;
                if (packed) {
                    supply = new ItemStack(CCBItems.GAS_CANISTER_PACK.get());
                    supply.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(List.of(canister)));
                }

                player.getInventory().setItem(0, supply);
                String context = "Economize level " + economizeLevel + " with packed=" + packed;
                AffordableFuel fuel = CanisterContainerConsumers.findAffordableFuel(player, usage -> 1000000).orElseThrow(() -> new IllegalStateException(context + " could not pay a large logical cost with 1 GU."));
                helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, true), context + " rejected the last gas unit during simulation.");
                helper.assertValueEqual(contents(supply).getGasInTank(0).getAmount(), 1L, context + " consumed the last gas unit during simulation.");
                helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, false), context + " rejected the last gas unit during execution.");
                helper.assertTrue(contents(supply).isEmpty(), context + " did not consume the last gas unit.");
                helper.assertTrue(CanisterContainerConsumers.findAffordableFuel(player, usage -> 1).isEmpty(), context + " found fuel in an empty canister.");
                helper.assertTrue(!CanisterContainerConsumers.interactContainer(player, fuel, () -> true, true), context + " simulated free consumption from an empty canister.");
                helper.assertTrue(!CanisterContainerConsumers.interactContainer(player, fuel, () -> true, false), context + " executed free consumption from an empty canister.");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void economizeFourPoolsWithOtherLevelsWithoutPartialPayment(GameTestHelper helper) {
        Holder<Enchantment> economize = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(CCBEnchantments.ECONOMIZE);
        Gas gasType = CCBGases.NATURAL_AIR.get();
        for (boolean packed : new boolean[]{false, true}) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack first = canister(gasType, 1);
            first.enchant(economize, 4);
            ItemStack second = canister(gasType, 10);
            second.enchant(economize, 3);
            ItemStack supply = first;
            if (packed) {
                supply = new ItemStack(CCBItems.GAS_CANISTER_PACK.get());
                supply.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(List.of(first, second)));
            }
            else {
                player.getInventory().setItem(1, second);
            }

            player.getInventory().setItem(0, supply);
            helper.assertTrue(CanisterContainerConsumers.findAffordableFuel(player, usage -> 31).isEmpty(), "Mixed economize levels covered more than 30 GU of logical cost.");
            AffordableFuel unaffordable = new AffordableFuel(new GasStack(gasType, 1), contents(supply).getTankPressurePa(0), 31);
            helper.assertTrue(!CanisterContainerConsumers.interactContainer(player, unaffordable, () -> true, false), "Mixed economize levels partially paid an unaffordable cost.");
            GasCanisterContainer failedFirst = contents(supply);
            GasCanisterContainer failedSecond = packed ? failedFirst : contents(second);
            int secondTank = packed ? 1 : 0;
            helper.assertValueEqual(failedFirst.getGasInTank(0).getAmount(), 1L, "Failed payment drained the economize IV canister.");
            helper.assertValueEqual(failedSecond.getGasInTank(secondTank).getAmount(), 10L, "Failed payment drained the economize III canister.");

            AffordableFuel fuel = CanisterContainerConsumers.findAffordableFuel(player, usage -> 26).orElseThrow(() -> new IllegalStateException("Mixed economize levels could not pay 26 GU of logical cost."));
            helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, false), "Mixed economize levels failed to pool gas.");
            GasCanisterContainer remainingFirst = contents(supply);
            GasCanisterContainer remainingSecond = packed ? remainingFirst : contents(second);
            helper.assertTrue(remainingFirst.getGasInTank(0).isEmpty(), "Economize IV did not spend 1 GU to cover the first 5 GU of logical cost.");
            helper.assertValueEqual(remainingSecond.getGasInTank(secondTank).getAmount(), 1L, "Economize III did not spend 9 GU to cover the remaining 21 GU of logical cost.");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void economizeFivePaysOnlyTheRemainingCostInSupplyOrder(GameTestHelper helper) {
        Holder<Enchantment> economize = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(CCBEnchantments.ECONOMIZE);
        Gas gasType = CCBGases.NATURAL_AIR.get();
        for (boolean packed : new boolean[]{false, true}) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack first = canister(gasType, 2);
            first.enchant(economize, 4);
            ItemStack second = canister(gasType, 1);
            second.enchant(economize, 5);
            ItemStack last = canister(gasType, 100);
            ItemStack supply = first;
            if (packed) {
                supply = new ItemStack(CCBItems.GAS_CANISTER_PACK.get());
                supply.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(List.of(first, second, last)));
            }
            else {
                player.getInventory().setItem(1, second);
                player.getInventory().setItem(2, last);
            }

            player.getInventory().setItem(0, supply);
            AffordableFuel fuel = CanisterContainerConsumers.findAffordableFuel(player, usage -> 110).orElseThrow(() -> new IllegalStateException("Economize IV and V could not jointly pay 110 GU of logical cost."));
            helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, false), "Economize IV and V failed to consume gas in supply order.");
            GasCanisterContainer remaining = contents(supply);
            helper.assertTrue(remaining.getGasInTank(0).isEmpty(), "Earlier economize IV canister was skipped.");
            if (packed) {
                helper.assertTrue(remaining.getGasInTank(1).isEmpty(), "Packed economize V canister did not pay its last gas unit.");
                helper.assertValueEqual(remaining.getGasInTank(2).getAmount(), 100L, "Consumption continued after the packed economize V canister paid the remainder.");
                continue;
            }

            helper.assertTrue(contents(second).isEmpty(), "Loose economize V canister did not pay its last gas unit.");
            helper.assertValueEqual(contents(last).getGasInTank(0).getAmount(), 100L, "Consumption continued after the loose economize V canister paid the remainder.");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void poolingNeverCombinesGasTypesOrPartiallyPaysAnUnaffordableCost(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack natural = canister(CCBGases.NATURAL_AIR.get(), Long.MAX_VALUE);
        ItemStack otherGas = canister(CCBGases.ULTRAWARM_AIR.get(), 30);
        long capacity = contents(natural).getGasInTank(0).getAmount();
        player.getInventory().setItem(0, natural);
        player.getInventory().setItem(1, otherGas);

        helper.assertTrue(CanisterContainerConsumers.findAffordableFuel(player, context -> capacity + 1).isEmpty(), "Different gases were combined to cover one cost");
        AffordableFuel staleRequest = new AffordableFuel(new GasStack(CCBGases.NATURAL_AIR.get(), 1), contents(natural).getTankPressurePa(0), capacity + 1);
        helper.assertTrue(!CanisterContainerConsumers.interactContainer(player, staleRequest, () -> true, false), "Unaffordable request succeeded");
        helper.assertValueEqual(contents(natural).getGasInTank(0).getAmount(), capacity, "Failed request partially drained natural air");
        helper.assertValueEqual(contents(otherGas).getGasInTank(0).getAmount(), 30L, "Failed request drained another gas type");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void normalEquipmentCostSurvivesTheFullCanisterThreshold(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack full = canister(CCBGases.NATURAL_AIR.get(), Long.MAX_VALUE);
        long startingAmount = contents(full).getGasInTank(0).getAmount();
        player.getInventory().setItem(0, full);
        for (int shot = 0; shot < 2; shot++) {
            AffordableFuel fuel = CanisterContainerConsumers.findAffordableFuel(player, context -> 50 * AirtightCannonHandlers.resolveForEquipment(context.gasType()).getGasConsumptionMultiplier()).orElseThrow();
            helper.assertValueEqual(fuel.amount(), 50L, "Equipment cost changed across the full-canister threshold");
            helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, false), "Normal-baseline shot failed");
        }
        helper.assertValueEqual(contents(full).getGasInTank(0).getAmount(), startingAmount - 100, "Two shots did not consume the same baseline amount");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void creativeCanisterRetainsInfiniteSupplyAtNormalEquipmentCost(GameTestHelper helper) {
        for (boolean packed : new boolean[]{false, true}) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack creative = new ItemStack(CCBItems.CREATIVE_GAS_CANISTER.get());
            creative.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.NATURAL_AIR.get(), 1));
            ItemStack supply = creative;
            if (packed) {
                supply = new ItemStack(CCBItems.GAS_CANISTER_PACK.get());
                supply.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(List.of(creative)));
            }
            player.getInventory().setItem(0, supply);
            GasStack before = contents(supply).getGasInTank(0);
            AffordableFuel fuel = CanisterContainerConsumers.findAffordableFuel(player, context -> 50 * AirtightCannonHandlers.resolveForEquipment(context.gasType()).getGasConsumptionMultiplier()).orElseThrow();
            helper.assertValueEqual(fuel.amount(), 50L, "Creative canister incorrectly selected the high-pressure equipment discount");
            helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, false), "Creative supply failed");
            helper.assertTrue(GasStack.matches(before, contents(supply).getGasInTank(0)), "Creative supply was depleted");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void affordableFuelKeepsExactPressureAlongsideProfile(GameTestHelper helper) {
        long exactPressurePa = GasPressure.pascals(14.25);
        AffordableFuel fuel = new AffordableFuel(new GasStack(CCBGases.NATURAL_AIR.get(), 100), exactPressurePa, 25);

        helper.assertValueEqual(fuel.sourcePressurePa(), exactPressurePa, "Affordable fuel exact source pressure");
        helper.assertTrue(fuel.pressureProfile().equals(GameplayPressureProfiles.HIGH_PRESSURE), "Affordable fuel did not retain its gameplay pressure profile identity");
        helper.assertValueEqual(fuel.usageContext().sourcePressurePa(), exactPressurePa, "Affordable fuel usage context exact source pressure");
        helper.assertTrue(fuel.usageContext().pressureProfile().equals(GameplayPressureProfiles.HIGH_PRESSURE), "Affordable fuel usage context resolved a different gameplay pressure profile");
        helper.succeed();
    }

    private static ItemStack canister(Gas gas, long amount) {
        ItemStack canister = new ItemStack(CCBItems.GAS_CANISTER.get());
        contents(canister).fill(0, new GasStack(gas, amount), GasAction.EXECUTE);
        return canister;
    }

    private static GasCanisterContainer contents(ItemStack stack) {
        GasCanisterContainer container = stack.getCapability(CanisterCapabilities.ITEM);
        if (container == null) {
            throw new NullPointerException("Expected a gas canister capability for the pressure profile fixture.");
        }

        return container;
    }
}
