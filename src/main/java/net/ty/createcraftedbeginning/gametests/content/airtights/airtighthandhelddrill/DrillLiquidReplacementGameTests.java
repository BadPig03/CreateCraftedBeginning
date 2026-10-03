package net.ty.createcraftedbeginning.gametests.content.airtights.airtighthandhelddrill;

import com.mojang.authlib.GameProfile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
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
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.LiquidReplacementUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.MagnetUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DrillLiquidReplacementGameTests {
    private static final int INITIAL_GAS_AMOUNT = 1000;
    private static final int VANILLA_LIQUID_HARDNESS = 100;
    private static final int EXPECTED_LIQUID_HARDNESS = 10;
    private static final int HARVEST_PROGRESS_DIVISOR = 30;
    private static final float HARDNESS_TOLERANCE = 0.0001F;
    private static final float PROGRESS_TOLERANCE = 0.000001F;

    private DrillLiquidReplacementGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void primaryWaterloggedBlockLeavesAir(GameTestHelper helper) {
        verifyWaterloggedMining(helper, true, true, false);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void disabledReplacementLeavesPrimaryWater(GameTestHelper helper) {
        verifyWaterloggedMining(helper, false, true, false);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void uninstalledReplacementLeavesPrimaryWater(GameTestHelper helper) {
        verifyWaterloggedMining(helper, true, false, false);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void areaReplacementClearsPrimaryAdditionalAndLiquidBlocks(GameTestHelper helper) {
        verifyWaterloggedMining(helper, true, true, true);
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void sourceAndFlowingLiquidsUseNormalMiningProgress(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        int maxBuildHeight = level.getMaxBuildHeight();
        for (BlockState state : List.of(Blocks.WATER.defaultBlockState(), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 5), Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 5))) {
            FakePlayer player = createLiquidMiner(helper, true, INITIAL_GAS_AMOUNT);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            float progress = state.getDestroyProgress(player, level, pos);
            helper.assertTrue(Math.abs(state.getDestroySpeed(level, pos) - VANILLA_LIQUID_HARDNESS) < HARDNESS_TOLERANCE, "Global liquid hardness must remain unchanged");
            helper.assertTrue(Math.abs(progress - player.getDigSpeed(state, pos) / EXPECTED_LIQUID_HARDNESS / HARVEST_PROGRESS_DIVISOR) < PROGRESS_TOLERANCE, "Drill liquid mining must use effective hardness 10");
            helper.assertTrue(progress > 0 && progress < 1, "Liquid must use gradual mining, not instant removal");

            player.gameMode.handleBlockBreakAction(pos, Action.START_DESTROY_BLOCK, Direction.UP, maxBuildHeight, 0);
            helper.assertTrue(level.getBlockState(pos).equals(state), "Starting mining must not immediately clear liquid");
            int miningTicks = (int) Math.ceil(1 / progress);
            for (int tick = 0; tick < miningTicks; tick++) {
                player.gameMode.tick();
            }
            player.gameMode.handleBlockBreakAction(pos, Action.STOP_DESTROY_BLOCK, Direction.UP, maxBuildHeight, 1);
            helper.assertTrue(level.getBlockState(pos).isAir(), "Completing normal mining must remove pure liquid");
            assertGasUsed(helper, player, calculateLiquidGasCost());
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void pureLiquidRejectsDisabledUpgradeAndMissingFuel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (FakePlayer player : List.of(createLiquidMiner(helper, false, INITIAL_GAS_AMOUNT), createLiquidMiner(helper, true, 0))) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertTrue(level.getBlockState(pos).getDestroyProgress(player, level, pos) == 0, "Inactive or unfueled drill must not progress through liquid");
            helper.assertTrue(!player.gameMode.destroyBlock(pos), "Server must reject unsupported liquid mining");
            helper.assertTrue(level.getBlockState(pos).is(Blocks.WATER), "Rejected mining must preserve water");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void pureLiquidCanStartMixedAreaWithoutGeneratingExperience(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = createLiquidMiner(helper, true, INITIAL_GAS_AMOUNT);
        ItemStack drill = player.getMainHandItem();
        drill.set(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(3, 1, 1));
        drill.set(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, List.of(new AirtightUpgradeStatus(LiquidReplacementUpgrade.INSTANCE.getID(), true, true), new AirtightUpgradeStatus(ExperienceConversionUpgrade.INSTANCE.getID(), true, true), new AirtightUpgradeStatus(MagnetUpgrade.INSTANCE.getID(), true, true)));
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 1));
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pos.east(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pos.east(2), Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertTrue(player.gameMode.destroyBlock(pos), "Pure liquid must be a valid area origin");
        helper.assertTrue(level.getBlockState(pos).isAir() && level.getBlockState(pos.east()).isAir() && level.getBlockState(pos.east(2)).isAir(), "Mixed area must clear liquid and mine solid blocks");
        helper.assertValueEqual(player.totalExperience, 1, "Only stone may convert to experience; pure liquids must not generate experience");
        AirtightHandheldDrill config = CCBConfig.server().equipment.airtightHandheldDrill;
        float solidCost = config.gasPerBlock.get() * ExperienceConversionUpgrade.BASE_GAS_MULTIPLIER * config.experienceConversionGasMultiplier.getF() * MagnetUpgrade.BASE_GAS_MULTIPLIER * config.magnetGasMultiplier.getF();
        assertGasUsed(helper, player, (long) Math.ceil(2 * calculateLiquidGasCost() + solidCost));
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void cancelledLiquidBreakPreservesPrimaryAndAreaTargets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = createLiquidMiner(helper, true, INITIAL_GAS_AMOUNT);
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 1));
        BlockPos protectedPos = pos.east();
        Consumer<BreakEvent> protection = event -> {
            if (event.getPlayer() != player || !event.getPos().equals(protectedPos)) {
                return;
            }

            event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, protection);
        try {
            level.setBlock(protectedPos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertTrue(!player.gameMode.destroyBlock(protectedPos), "Cancelled primary liquid break must fail");
            assertGasUsed(helper, player, 0);
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            player.getMainHandItem().set(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(2, 1, 1));
            helper.assertTrue(player.gameMode.destroyBlock(pos), "Unprotected primary liquid must still be mined");
            helper.assertTrue(level.getBlockState(pos).isAir(), "Unprotected primary must become air");
            helper.assertTrue(level.getBlockState(protectedPos).is(Blocks.WATER), "Cancelled additional liquid break must preserve water");
            assertGasUsed(helper, player, calculateLiquidGasCost());
        }
        finally {
            NeoForge.EVENT_BUS.unregister(protection);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void waterloggedSolidKeepsItsOwnHardness(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = createLiquidMiner(helper, true, INITIAL_GAS_AMOUNT);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState state = Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        float expectedProgress = player.getDigSpeed(state, pos) / state.getDestroySpeed(level, pos) / HARVEST_PROGRESS_DIVISOR;
        helper.assertTrue(Math.abs(state.getDestroyProgress(player, level, pos) - expectedProgress) < PROGRESS_TOLERANCE, "Waterlogged solid must retain its own hardness");
        helper.succeed();
    }

    private static FakePlayer createLiquidMiner(GameTestHelper helper, boolean enabled, int gasAmount) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "drill-liquid-test"));
        player.setGameMode(GameType.SURVIVAL);
        player.setOnGround(true);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        ItemStack drill = new ItemStack(CCBItems.AIRTIGHT_HANDHELD_DRILL.get());
        drill.set(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, List.of(new AirtightUpgradeStatus(LiquidReplacementUpgrade.INSTANCE.getID(), enabled, true)));
        drill.set(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(1, 1, 1));
        drill.set(CCBDataComponents.DRILL_MINING_DIRECTION, DrillMiningDirection.SOUTH);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        ItemStack canister = new ItemStack(CCBItems.GAS_CANISTER.get());
        GasCanisterContainer container = canister.getCapability(CanisterCapabilities.ITEM);
        if (container == null) {
            throw new NullPointerException("Expected a gas canister capability for the liquid mining test at " + pos + '.');
        }

        container.fill(0, new GasStack(CCBGases.NATURAL_AIR.get(), gasAmount), GasAction.EXECUTE);
        player.getInventory().setItem(1, canister);
        return player;
    }

    private static long calculateLiquidGasCost() {
        AirtightHandheldDrill config = CCBConfig.server().equipment.airtightHandheldDrill;
        return (long) Math.ceil(config.gasPerBlock.get() * config.liquidReplacementGasMultiplier.getF());
    }

    private static void assertGasUsed(GameTestHelper helper, FakePlayer player, long expected) {
        GasCanisterContainer canister = player.getInventory().getItem(1).getCapability(CanisterCapabilities.ITEM);
        if (canister == null) {
            throw new NullPointerException("Expected a gas canister capability in inventory slot 1 after liquid mining.");
        }

        long remainingGas = canister.getGasInTank(0).getAmount();
        helper.assertValueEqual(INITIAL_GAS_AMOUNT - remainingGas, expected, "Mining must charge only successfully cleared targets");
    }

    private static void verifyWaterloggedMining(GameTestHelper helper, boolean enabled, boolean installed, boolean area) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "drill-liquid-test"));
        player.setGameMode(GameType.SURVIVAL);
        ItemStack drill = new ItemStack(CCBItems.AIRTIGHT_HANDHELD_DRILL.get());
        drill.set(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, List.of(new AirtightUpgradeStatus(LiquidReplacementUpgrade.INSTANCE.getID(), enabled, installed)));
        drill.set(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(area ? 3 : 1, 1, 1));
        drill.set(CCBDataComponents.DRILL_MINING_DIRECTION, DrillMiningDirection.SOUTH);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        ItemStack canisterStack = new ItemStack(CCBItems.GAS_CANISTER.get());
        GasCanisterContainer canister = canisterStack.getCapability(CanisterCapabilities.ITEM);
        if (canister == null) {
            throw new NullPointerException("Expected a gas canister capability for the waterlogged block mining test.");
        }

        canister.fill(0, new GasStack(CCBGases.NATURAL_AIR.get(), INITIAL_GAS_AMOUNT), GasAction.EXECUTE);
        player.getInventory().setItem(1, canisterStack);
        long initialGas = canister.getGasInTank(0).getAmount();

        BlockPos primary = helper.absolutePos(new BlockPos(0, 1, 1));
        player.setPos(primary.getX() + 0.5, primary.getY() + 1, primary.getZ() + 0.5);
        level.setBlockAndUpdate(primary, Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        if (area) {
            level.setBlockAndUpdate(primary.east(), Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
            level.setBlockAndUpdate(primary.east(2), Blocks.WATER.defaultBlockState());
        }

        helper.assertTrue(player.gameMode.destroyBlock(primary), "Primary block must be successfully mined");
        boolean replacesLiquid = enabled && installed;
        if (replacesLiquid) {
            helper.assertTrue(level.getBlockState(primary).isAir(), "Primary waterlogged block must leave air instead of water");
        }
        else {
            helper.assertTrue(level.getBlockState(primary).is(Blocks.WATER), "Inactive replacement must preserve primary water");
            helper.assertTrue(level.getFluidState(primary).is(Fluids.WATER), "The original water must remain");
        }
        if (area) {
            helper.assertTrue(level.getBlockState(primary.east()).isAir(), "Additional waterlogged block must leave air");
            helper.assertTrue(level.getBlockState(primary.east(2)).isAir(), "Pure liquid in the area must be cleared");
        }

        AirtightHandheldDrill config = CCBConfig.server().equipment.airtightHandheldDrill;
        int baseCost = config.gasPerBlock.get();
        float liquidCost = baseCost * config.liquidReplacementGasMultiplier.getF();
        float miningCost;
        if (area) {
            miningCost = 2 * (baseCost + liquidCost) + liquidCost;
        }
        else if (replacesLiquid) {
            miningCost = baseCost + liquidCost;
        }
        else {
            miningCost = baseCost;
        }

        long expectedCost = (long) Math.ceil(miningCost);
        GasCanisterContainer remainingCanister = player.getInventory().getItem(1).getCapability(CanisterCapabilities.ITEM);
        if (remainingCanister == null) {
            throw new NullPointerException("Expected a gas canister capability in inventory slot 1 after waterlogged block mining.");
        }

        long remainingGas = remainingCanister.getGasInTank(0).getAmount();
        helper.assertValueEqual(initialGas - remainingGas, expectedCost, "Liquid replacement must be charged exactly once per target");
        helper.succeed();
    }
}
