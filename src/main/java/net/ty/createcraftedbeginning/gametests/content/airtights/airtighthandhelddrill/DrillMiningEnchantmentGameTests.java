package net.ty.createcraftedbeginning.gametests.content.airtights.airtighthandhelddrill;

import com.mojang.authlib.GameProfile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.config.CCBEquipment.AirtightHandheldDrill;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.DrillMiningDirection;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.ExperienceConversionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.HarvestOptimizationUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.MagnetUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DrillMiningEnchantmentGameTests {
    private static final int INITIAL_GAS_AMOUNT = 10000;
    private static final int AREA_BLOCK_COUNT = 3;

    private DrillMiningEnchantmentGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void installedDisabledUpgradeGivesFortuneThreeAcrossArea(GameTestHelper helper) {
        verifyAreaDrops(helper, true, false, false, false);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void enabledUpgradeGivesSilkTouchAcrossArea(GameTestHelper helper) {
        verifyAreaDrops(helper, true, true, false, false);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void fortuneModeOverridesStoredSilkTouchWithoutChangingDrill(GameTestHelper helper) {
        verifyAreaDrops(helper, true, false, true, false);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void uninstalledUpgradePreservesStoredEnchantments(GameTestHelper helper) {
        verifyAreaDrops(helper, false, false, true, false);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void experienceConversionOverridesFortuneMode(GameTestHelper helper) {
        verifyAreaDrops(helper, true, false, false, true);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void experienceConversionOverridesSilkTouchMode(GameTestHelper helper) {
        verifyAreaDrops(helper, true, true, false, true);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void togglingInstalledUpgradeSwitchesDropModesImmediately(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = createMiner(helper);
        ItemStack drill = player.getMainHandItem();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (boolean silkTouch : List.of(false, true, false)) {
            drill.set(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, List.of(new AirtightUpgradeStatus(HarvestOptimizationUpgrade.INSTANCE.getID(), silkTouch, true), new AirtightUpgradeStatus(MagnetUpgrade.INSTANCE.getID(), true, true)));
            level.setBlock(pos, Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertTrue(player.gameMode.destroyBlock(pos), "The drill must mine gravel after each toggle.");
        }
        helper.assertValueEqual(player.getInventory().countItem(Items.FLINT), 2, "Both disabled-mode breaks must use Fortune III.");
        helper.assertValueEqual(player.getInventory().countItem(Items.GRAVEL), 1, "The enabled-mode break must use Silk Touch.");
        helper.succeed();
    }

    private static FakePlayer createMiner(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "drill-loot-test"));
        player.setGameMode(GameType.SURVIVAL);
        ItemStack drill = new ItemStack(CCBItems.AIRTIGHT_HANDHELD_DRILL.get());
        drill.set(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(1, 1, 1));
        drill.set(CCBDataComponents.DRILL_MINING_DIRECTION, DrillMiningDirection.SOUTH);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        ItemStack canisterStack = new ItemStack(CCBItems.GAS_CANISTER.get());
        GasCanisterContainer canister = canisterStack.getCapability(CanisterCapabilities.ITEM);
        if (canister == null) {
            throw new NullPointerException("Expected a gas canister capability for the drill enchantment test.");
        }

        canister.fill(0, new GasStack(CCBGases.NATURAL_AIR.get(), INITIAL_GAS_AMOUNT), GasAction.EXECUTE);
        player.getInventory().setItem(1, canisterStack);
        return player;
    }

    private static void verifyAreaDrops(GameTestHelper helper, boolean installed, boolean enabled, boolean storedSilkTouch, boolean experienceConversion) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = createMiner(helper);
        ItemStack drill = player.getMainHandItem();
        drill.set(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(AREA_BLOCK_COUNT, 1, 1));
        drill.set(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, List.of(new AirtightUpgradeStatus(HarvestOptimizationUpgrade.INSTANCE.getID(), enabled, installed), new AirtightUpgradeStatus(MagnetUpgrade.INSTANCE.getID(), true, true), new AirtightUpgradeStatus(ExperienceConversionUpgrade.INSTANCE.getID(), experienceConversion, experienceConversion)));
        Registry<Enchantment> enchantmentRegistry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> fortune = enchantmentRegistry.getHolderOrThrow(Enchantments.FORTUNE);
        drill.enchant(fortune, 1);
        drill.enchant(enchantmentRegistry.getHolderOrThrow(Enchantments.EFFICIENCY), 2);
        if (storedSilkTouch) {
            drill.enchant(enchantmentRegistry.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        }

        ItemEnchantments originalEnchantments = drill.getTagEnchantments();
        BlockPos primary = helper.absolutePos(new BlockPos(0, 1, 1));
        for (int offset = 0; offset < AREA_BLOCK_COUNT; offset++) {
            level.setBlock(primary.east(offset), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        helper.assertTrue(player.gameMode.destroyBlock(primary), "The drill must mine the primary gravel block.");
        for (int offset = 0; offset < AREA_BLOCK_COUNT; offset++) {
            helper.assertTrue(level.getBlockState(primary.east(offset)).isAir(), "Every gravel block in the mining area must be removed.");
        }

        int expectedFlint = 0;
        int expectedGravel = 0;
        int expectedExperience = 0;
        if (experienceConversion) {
            expectedExperience = AREA_BLOCK_COUNT;
        }
        else if (enabled || !installed && storedSilkTouch) {
            expectedGravel = AREA_BLOCK_COUNT;
        }
        else {
            expectedFlint = AREA_BLOCK_COUNT;
        }

        helper.assertValueEqual(player.getInventory().countItem(Items.FLINT), expectedFlint, "Fortune III must make each gravel block drop flint.");
        helper.assertValueEqual(player.getInventory().countItem(Items.GRAVEL), expectedGravel, "Silk Touch must make each gravel block drop itself.");
        helper.assertValueEqual(player.totalExperience, expectedExperience, "Experience Conversion must retain priority over both mining enchantment modes.");
        helper.assertTrue(drill.getTagEnchantments().equals(originalEnchantments), "Virtual mining enchantments must not alter the drill's stored enchantments.");

        AirtightHandheldDrill config = CCBConfig.server().equipment.airtightHandheldDrill;
        float gasPerBlock = config.gasPerBlock.get();
        if (installed && enabled && !experienceConversion) {
            gasPerBlock *= config.harvestOptimizationGasMultiplier.getF();
        }
        gasPerBlock *= MagnetUpgrade.BASE_GAS_MULTIPLIER * config.magnetGasMultiplier.getF();
        if (experienceConversion) {
            gasPerBlock *= ExperienceConversionUpgrade.BASE_GAS_MULTIPLIER * config.experienceConversionGasMultiplier.getF();
        }

        GasCanisterContainer remainingCanister = player.getInventory().getItem(1).getCapability(CanisterCapabilities.ITEM);
        if (remainingCanister == null) {
            throw new NullPointerException("Expected a gas canister capability in inventory slot 1 after drill enchantment mining.");
        }

        long expectedGas = (long) Math.ceil(gasPerBlock * AREA_BLOCK_COUNT);
        helper.assertValueEqual(INITIAL_GAS_AMOUNT - remainingCanister.getGasInTank(0).getAmount(), expectedGas, "Mining enchantment modes must retain the existing upgrade gas costs.");
        helper.succeed();
    }
}
