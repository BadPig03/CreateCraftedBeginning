package net.ty.createcraftedbeginning.gametests.content.airtights.gascanister.container;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlers;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasBuilder;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ElytraUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.VisionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.HandheldDrillAttackModeButton;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightItemUpgrades;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.GlobalAirtightUpgradesConsumptionManager;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients.DisplayedGasState;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasEquipmentEligibilityGameTests {
    private GasEquipmentEligibilityGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void eligibilityDefaultsAndStartupOverridesApplyToRegisteredGas(GameTestHelper helper) {
        GasBuilder builder = GasBuilder.builder();
        helper.assertTrue(builder.isUsableInEquipment() && CCBGases.NATURAL_AIR.get().isUsableInEquipment(), "Existing and newly defined gases must allow equipment by default.");
        helper.assertTrue(!builder.usableInEquipment(false).isUsableInEquipment() && builder.usableInEquipment(true).isUsableInEquipment(), "The gas builder must accept both explicit eligibility values.");
        helper.assertTrue(!CCBGases.POTION_GAS.get().isUsableInEquipment(), "Every potion gas variant must share the disabled equipment flag.");
        if (Boolean.getBoolean("createcraftedbeginning.test_equipment_gases")) {
            Map<String, Boolean> expected = Map.of("equipment_test:disabled_air", false, "equipment_test:enabled_air", true, "equipment_test:default_air", true);
            for (Entry<String, Boolean> entry : expected.entrySet()) {
                String gasId = entry.getKey();
                boolean usableInEquipment = entry.getValue();
                Gas gas = Gas.findById(ResourceLocation.parse(gasId));
                helper.assertTrue(!gas.isEmpty() && gas.isUsableInEquipment() == usableInEquipment, "KubeJS registration did not retain eligibility for " + gasId + '.');
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.getInventory().setItem(0, canister(new GasStack(gas, 100), false));
                helper.assertTrue(CanisterContainerConsumers.findAffordableFuel(player, context -> AirtightDrillHandlers.resolveForEquipment(context.gasType()).getConsumptionMultiplier()).isPresent() == usableInEquipment, "KubeJS equipment handlers must not override the gas eligibility flag.");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void mixedCanistersSkipDisabledGasAndConsumeTheNextSupply(GameTestHelper helper) {
        GasStack potion = CCBGases.POTION_GAS.get().createStack(100, new PotionContents(Potions.STRONG_HEALING));
        for (boolean packed : new boolean[]{false, true}) {
            for (boolean creativeCanister : new boolean[]{false, true}) {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                ItemStack first = canister(potion, creativeCanister);
                ItemStack second = canister(new GasStack(CCBGases.NATURAL_AIR.get(), 100), false);
                ItemStack supply = first;
                if (packed) {
                    supply = new ItemStack(CCBItems.GAS_CANISTER_PACK.get());
                    supply.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(List.of(first, second)));
                }
                else {
                    player.getInventory().setItem(2, second);
                }
                player.setItemInHand(InteractionHand.OFF_HAND, supply);
                GasCanisterContainer contents = requireContents(supply);
                GasStack before = contents.getGasInTank(0).copy();
                helper.assertTrue(GasStack.isSameGasSameComponents(before, potion), "Canisters must retain disabled gas and all potion components.");
                helper.assertTrue(CanisterContainerSuppliers.getFirstAvailableGasContent(player).is(CCBGases.NATURAL_AIR), "Equipment supply selection must skip the first disabled gas.");
                AffordableFuel fuel = CanisterContainerConsumers.findAffordableFuel(player, context -> {
                    helper.assertTrue(context.gasType().isUsableInEquipment(), "Disabled gas must be skipped before evaluating equipment effects or cost.");
                    return 5;
                }).orElseThrow(() -> new IllegalStateException("Expected the later natural air canister to power equipment."));
                helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, true), "The later eligible supply must support simulation.");
                GasCanisterContainer secondContents = packed ? contents : requireContents(second);
                int secondTank = packed ? 1 : 0;
                helper.assertValueEqual(secondContents.getGasInTank(secondTank).getAmount(), 100L, "simulation must preserve eligible gas");
                helper.assertTrue(CanisterContainerConsumers.interactContainer(player, fuel, () -> true, false), "The later eligible supply must support actual consumption.");
                contents = requireContents(supply);
                secondContents = packed ? contents : requireContents(second);
                helper.assertValueEqual(secondContents.getGasInTank(secondTank).getAmount(), 95L, "eligible gas after equipment consumption");
                helper.assertTrue(GasStack.matches(before, contents.getGasInTank(0)), "Equipment must leave the skipped potion gas untouched.");
                helper.assertTrue(GasStack.isSameGasSameComponents(contents.drain(0, 20, GasAction.SIMULATE), potion), "Disabled equipment gas must remain available to ordinary storage extraction.");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void disabledGasCannotBypassConsumptionWithZeroCostOrCreativeMode(GameTestHelper helper) {
        GasStack potion = CCBGases.POTION_GAS.get().createStack(100, new PotionContents(Potions.SWIFTNESS));
        for (GameType mode : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
            for (boolean creativeCanister : new boolean[]{false, true}) {
                Player player = helper.makeMockPlayer(mode);
                ItemStack supply = canister(potion, creativeCanister);
                player.getInventory().setItem(0, supply);
                GasCanisterContainer contents = requireContents(supply);
                GasStack before = contents.getGasInTank(0).copy();
                helper.assertTrue(CanisterContainerSuppliers.getFirstAvailableGasContent(player).isEmpty(), "Disabled gas must not satisfy supply-only equipment checks.");
                helper.assertTrue(CanisterContainerConsumers.findAffordableFuel(player, context -> 0).isEmpty(), "Zero cost must still require an eligible gas.");
                helper.assertTrue(CanisterContainerConsumers.findAffordableFuel(player, potion.getGasType(), context -> 0).isEmpty(), "Explicit gas selection must respect equipment eligibility.");
                int[] callbacks = {0};
                long sourcePressurePa = contents.getTankPressurePa(0);
                for (long cost : new long[]{0, 1}) {
                    AffordableFuel forged = new AffordableFuel(before, sourcePressurePa, cost);
                    for (boolean simulate : new boolean[]{false, true}) {
                        boolean accepted = CanisterContainerConsumers.interactContainer(player, forged, () -> {
                            callbacks[0]++;
                            return true;
                        }, simulate);
                        helper.assertTrue(!accepted, "A directly supplied disabled fuel must be rejected before creative or zero-cost bypasses.");
                    }
                }
                helper.assertTrue(callbacks[0] == 0 && GasStack.matches(before, contents.getGasInTank(0)), "Rejected equipment fuel must neither execute actions nor drain storage.");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void toolAndArmorPowerChecksRequireEligibleGas(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack potionCanister = canister(CCBGases.POTION_GAS.get().createStack(100, new PotionContents(Potions.SWIFTNESS)), false);
        player.setItemInHand(InteractionHand.OFF_HAND, potionCanister);
        ItemStack drill = new ItemStack(CCBItems.AIRTIGHT_HANDHELD_DRILL.get());
        drill.set(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, List.of(new AirtightUpgradeStatus(HandheldDrillAttackModeButton.INSTANCE.getID(), true, true)));
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        helper.assertTrue(drill.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL && !player.isUsingItem(), "A drill must not enter its powered attack mode with only potion gas.");
        ItemStack helmet = new ItemStack(CCBItems.AIRTIGHT_HELMET.get());
        AirtightUpgrade spectral = AirtightItemUpgrades.getAllUpgrades(helmet).stream().filter(upgrade -> upgrade.getID().equals(CCBAPI.asResource("spectral"))).findFirst().orElseThrow(() -> new IllegalStateException("Expected the supply-only spectral helmet upgrade."));
        helmet.set(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, List.of(new AirtightUpgradeStatus(spectral.getID(), true, true), new AirtightUpgradeStatus(VisionUpgrade.INSTANCE.getID(), true, true)));
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        ItemStack chestplate = new ItemStack(CCBItems.AIRTIGHT_CHESTPLATE.get());
        chestplate.set(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, List.of(new AirtightUpgradeStatus(ElytraUpgrade.INSTANCE.getID(), true, true)));
        player.setItemSlot(EquipmentSlot.CHEST, chestplate);
        try {
            GlobalAirtightUpgradesConsumptionManager.tick(player);
            helper.assertTrue(!spectral.isActive(player, helmet) && !VisionUpgrade.INSTANCE.isActive(player, helmet) && !ElytraUpgrade.INSTANCE.canFly(player, chestplate), "Disabled gas must not power supply-only, continuous or flight equipment.");
            helper.assertTrue(!GlobalAirtightUpgradesConsumptionManager.tryConsumeGas(player, VisionUpgrade.INSTANCE, EquipmentSlot.HEAD, 0), "A zero-cost armor action must still reject disabled gas.");
            player.getInventory().setItem(2, canister(new GasStack(CCBGases.NATURAL_AIR.get(), 1000), false));
            CanisterContainerSuppliers.invalidateCache(player);
            GlobalAirtightUpgradesConsumptionManager.tick(player);
            helper.assertTrue(spectral.isActive(player, helmet) && VisionUpgrade.INSTANCE.isActive(player, helmet) && ElytraUpgrade.INSTANCE.canFly(player, chestplate), "A later eligible gas must restore ordinary armor operation.");
            helper.assertTrue(drill.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "A later eligible gas must restore powered drill use.");
            helper.assertValueEqual(requireContents(potionCanister).getGasInTank(0).getAmount(), 100L, "potion gas after armor and drill checks");
        }
        finally {
            player.stopUsingItem();
            GlobalAirtightUpgradesConsumptionManager.clearTracking(player);
            CanisterContainerSuppliers.invalidateCache(player);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void storedEquipmentDisplayCannotSelectDisabledGas(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        NBTHelper.writeResourceLocation(player.getPersistentData(), CanisterContainerClients.COMPOUND_KEY_STORED_GAS_TYPE, CCBGases.POTION_GAS.getId());
        helper.assertTrue(CanisterContainerClients.getStoredGasType(player).isEmpty(), "A persisted potion selection must not become the current equipment gas.");
        GasStack potion = CCBGases.POTION_GAS.get().createStack(100, new PotionContents(Potions.HEALING));
        DisplayedGasState disabledDisplay = new DisplayedGasState(potion, 1000, 100000, 1, true, true);
        helper.assertTrue(disabledDisplay.content().isEmpty() && disabledDisplay.maxAmount() <= 0 && !disabledDisplay.creative() && disabledDisplay.synced(), "Equipment overlays and item bars must not display disabled gas as a usable supply.");
        GasStack air = new GasStack(CCBGases.NATURAL_AIR.get(), 100);
        DisplayedGasState enabledDisplay = new DisplayedGasState(air, 1000, 100000, 1, false, true);
        helper.assertTrue(GasStack.matches(air, enabledDisplay.content()) && enabledDisplay.maxAmount() == 1000 && enabledDisplay.pressurePa() == 100000 && enabledDisplay.packType() == 1, "Eligible display snapshots must retain amount, pressure and canister metadata.");
        NBTHelper.writeResourceLocation(player.getPersistentData(), CanisterContainerClients.COMPOUND_KEY_STORED_GAS_TYPE, CCBGases.NATURAL_AIR.getId());
        helper.assertTrue(CanisterContainerClients.getStoredGasType(player) == CCBGases.NATURAL_AIR.get(), "A persisted eligible selection must remain usable.");
        helper.succeed();
    }

    private static ItemStack canister(GasStack gas, boolean creative) {
        if (creative) {
            ItemStack stack = new ItemStack(CCBItems.CREATIVE_GAS_CANISTER.get());
            stack.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, gas.copyWithAmount(1));
            return stack;
        }

        ItemStack stack = new ItemStack(CCBItems.GAS_CANISTER.get());
        requireContents(stack).fill(0, gas, GasAction.EXECUTE);
        return stack;
    }

    private static GasCanisterContainer requireContents(ItemStack stack) {
        GasCanisterContainer contents = stack.getCapability(CanisterCapabilities.ITEM);
        if (contents == null) {
            throw new NullPointerException("Expected canister storage for equipment eligibility tests.");
        }

        return contents;
    }
}
