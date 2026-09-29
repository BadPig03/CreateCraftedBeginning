package net.ty.createcraftedbeginning.gametests.gas;

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
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberCanisterTransfer;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasRecipePressureSpeed;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBEnchantments;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasBalanceValidationGameTests {
    private GasBalanceValidationGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void chargedCanistersSustainRepeatedEquipmentCosts(GameTestHelper helper) {
        int[] canisterCounts = {1, 4, 4};
        int[] economizeLevels = {0, 0, 3};
        int[] expectedUses = {400, 1600, 4000};
        for (int scenario = 0; scenario < canisterCounts.length; scenario++) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            List<ItemStack> canisters = new ArrayList<>();
            for (int index = 0; index < canisterCounts[scenario]; index++) {
                ItemStack canister = new ItemStack(CCBItems.GAS_CANISTER.get());
                if (economizeLevels[scenario] > 0) {
                    canister.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(CCBEnchantments.ECONOMIZE), economizeLevels[scenario]);
                }

                GasCanisterContainer contents = canister.getCapability(CanisterCapabilities.ITEM);
                if (contents == null) {
                    throw new NullPointerException("Expected gas canister capability in endurance validation.");
                }

                GasTank source = new GasTank(10000, GasPressure.pascals(24));
                source.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), 120000)).requireAccepted();
                helper.assertTrue(GasInjectionChamberCanisterTransfer.transferExactly(source, contents, source.getGasStack(), 20000), "Buffered charging failed.");
                helper.assertValueEqual(contents.getTankPressurePa(0), GasPressure.pascals(10), "Charged canister pressure");
                canisters.add(canister);
            }

            ItemStack supply = canisters.getFirst();
            if (canisterCounts[scenario] > 1) {
                supply = new ItemStack(CCBItems.GAS_CANISTER_PACK.get());
                supply.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(canisters));
            }

            player.getInventory().setItem(0, supply);
            for (int use = 0; use < expectedUses[scenario]; use++) {
                Optional<AffordableFuel> fuel = CanisterContainerConsumers.findAffordableFuel(player, CCBGases.NATURAL_AIR.get(), context -> 50);
                helper.assertTrue(fuel.isPresent(), "Equipment fuel ran out before the expected use count.");
                helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel.orElseThrow(), () -> true, false), "Repeated equipment consumption failed.");
            }

            helper.assertTrue(CanisterContainerConsumers.findAffordableFuel(player, CCBGases.NATURAL_AIR.get(), context -> 50).isEmpty(), "Equipment fuel remained after the expected use count.");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void lowPressureChargingCannotProvideFullCanisterEndurance(GameTestHelper helper) {
        for (int pressure : new int[]{1, 3, 5, 10}) {
            ItemStack canister = new ItemStack(CCBItems.GAS_CANISTER.get());
            GasCanisterContainer contents = canister.getCapability(CanisterCapabilities.ITEM);
            if (contents == null) {
                throw new NullPointerException("Expected gas canister capability in pressure charging validation.");
            }

            GasTank source = new GasTank(10000, GasPressure.pascals(24));
            source.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), pressure * 10000L)).requireAccepted();
            long amount = GasInjectionChamberCanisterTransfer.getTransferableAmount(source, contents, source.getGasStack(), Long.MAX_VALUE);
            helper.assertValueEqual(amount, pressure * 2000L, "Pressure-limited canister charge");
            helper.assertTrue(GasInjectionChamberCanisterTransfer.transferExactly(source, contents, source.getGasStack(), amount), "Pressure-limited charging failed.");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void bulkInjectionRequiresPressureAndLosesSpeedAfterLargeDrain(GameTestHelper helper) {
        int[] pressures = {1, 3, 5, 10, 14, 24};
        int[] batchSizes = {5, 15, 25, 50, 64, 64};
        GasRecipeRequirement requirement = GasRecipeRequirement.of(CCBGases.NATURAL_AIR.get(), 2000);
        for (int index = 0; index < pressures.length; index++) {
            GasTank tank = new GasTank(10000, GasPressure.pascals(24));
            tank.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), pressures[index] * 10000L)).requireAccepted();
            long batchSize = GasConsumptionPlanner.findMaximumMultiplier(requirement, tank, 64);
            helper.assertValueEqual(batchSize, (long) batchSizes[index], "Pressure-limited injection batch size");
            GasConsumptionPlan plan = GasConsumptionPlanner.plan(requirement, tank, batchSize).orElseThrow();
            float speed = GasRecipePressureSpeed.multiplier(plan);
            if (pressures[index] == 14) {
                helper.assertTrue(speed > 1 && speed < 1.05F, "Large batch retained an unexpected high-pressure speed bonus.");
            }

            helper.assertTrue(plan.execute(), "Bulk injection gas plan failed.");
            helper.assertValueEqual(tank.getStoredAmount(), pressures[index] * 10000L - batchSize * 2000, "Gas remaining after bulk injection");
        }
        helper.succeed();
    }
}
