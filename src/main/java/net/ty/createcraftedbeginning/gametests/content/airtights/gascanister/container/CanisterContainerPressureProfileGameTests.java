package net.ty.createcraftedbeginning.gametests.content.airtights.gascanister.container;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
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
import java.util.Objects;

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
        return Objects.requireNonNull(stack.getCapability(CanisterCapabilities.ITEM));
    }
}
