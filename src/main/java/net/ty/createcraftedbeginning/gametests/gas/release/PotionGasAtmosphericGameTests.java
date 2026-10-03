package net.ty.createcraftedbeginning.gametests.gas.release;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.gas.network.endpoint.AtmosphericGasEndpoint;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PotionGasAtmosphericGameTests {
    private static final BlockPos PIPE_POS = new BlockPos(3, 2, 3);
    private static final BlockPos TARGET_POS = new BlockPos(5, 2, 3);
    private static final BlockPos CONTROL_POS = new BlockPos(1, 2, 3);

    private PotionGasAtmosphericGameTests() {
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 160)
    public static void sustainedOutletKeepsTenGasUnitsPerSecondAcrossReload(GameTestHelper helper) {
        Villager target = helper.spawnWithNoFreeWill(EntityType.VILLAGER, TARGET_POS);
        Villager control = helper.spawnWithNoFreeWill(EntityType.VILLAGER, CONTROL_POS);
        target.setNoGravity(true);
        control.setNoGravity(true);
        AtmosphericGasEndpoint[] outlet = {createEndpoint(helper, PIPE_POS, Direction.EAST)};
        GasStack gas = CCBGases.POTION_GAS.get().createStack(1, new PotionContents(Potions.SWIFTNESS));
        GasHandler handler = requireHandler(outlet[0]);
        helper.assertValueEqual(handler.fill(gas.copyWithAmount(20), GasAction.SIMULATE), 20L, "simulated sustained outlet acceptance");
        helper.assertTrue(!target.hasEffect(MobEffects.MOVEMENT_SPEED), "Simulated atmospheric output must not apply effects.");
        for (int tick = 2; tick <= 80; tick += 2) {
            helper.runAtTickTime(tick, () -> helper.assertValueEqual(requireHandler(outlet[0]).fill(gas, GasAction.EXECUTE), 1L, "sustained outlet gas consumed"));
        }
        helper.runAtTickTime(21, () -> outlet[0] = reloadEndpoint(helper, outlet[0]));
        helper.runAtTickTime(39, () -> helper.assertTrue(!target.hasEffect(MobEffects.MOVEMENT_SPEED), "Simulation and outlet reload must not grant or duplicate the partial sustained budget."));
        for (int tick = 41; tick <= 100; tick++) {
            helper.runAtTickTime(tick, () -> {
                helper.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SPEED), "Ten GU per second must maintain continuous coverage after the first pulse.");
                helper.assertTrue(!control.hasEffect(MobEffects.MOVEMENT_SPEED), "Directional potion output must not affect a target behind the pipe.");
            });
        }
        helper.runAtTickTime(143, () -> {
            helper.assertTrue(!target.hasEffect(MobEffects.MOVEMENT_SPEED), "Sustained outlet effects must expire after the supply stops.");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 180)
    public static void instantOutletsKeepFiftyGasUnitsPerSecondAndSharedCooldownAcrossReload(GameTestHelper helper) {
        Villager target = helper.spawnWithNoFreeWill(EntityType.VILLAGER, TARGET_POS);
        Villager control = helper.spawnWithNoFreeWill(EntityType.VILLAGER, CONTROL_POS);
        target.setNoGravity(true);
        control.setNoGravity(true);
        target.setHealth(1);
        control.setHealth(1);
        AtmosphericGasEndpoint[] outlet = {createEndpoint(helper, PIPE_POS, Direction.EAST)};
        AtmosphericGasEndpoint overlapping = createEndpoint(helper, new BlockPos(5, 2, 1), Direction.SOUTH);
        GasStack gas = CCBGases.POTION_GAS.get().createStack(5, new PotionContents(Potions.HEALING));
        helper.assertValueEqual(requireHandler(outlet[0]).fill(gas.copyWithAmount(100), GasAction.SIMULATE), 100L, "simulated instant outlet acceptance");
        helper.assertTrue(target.getHealth() == 1, "Simulated instant output must not heal.");
        for (int tick = 2; tick <= 120; tick += 2) {
            helper.runAtTickTime(tick, () -> helper.assertValueEqual(requireHandler(outlet[0]).fill(gas, GasAction.EXECUTE), 5L, "instant outlet gas consumed"));
        }
        helper.runAtTickTime(21, () -> outlet[0] = reloadEndpoint(helper, outlet[0]));
        helper.runAtTickTime(39, () -> helper.assertTrue(target.getHealth() == 1, "Instant outlet reload must preserve the partial budget without an early pulse."));
        helper.runAtTickTime(41, () -> {
            helper.assertTrue(target.getHealth() == 5, "Fifty GU per second must produce the first healing pulse after 40 ticks.");
            outlet[0] = reloadEndpoint(helper, outlet[0]);
            helper.assertValueEqual(requireHandler(outlet[0]).fill(gas.copyWithAmount(100), GasAction.EXECUTE), 100L, "gas consumed during restored source cooldown");
            helper.assertValueEqual(requireHandler(overlapping).fill(gas.copyWithAmount(100), GasAction.EXECUTE), 100L, "gas consumed by overlapping outlet");
            helper.assertTrue(target.getHealth() == 5, "Reloading a zero-remainder outlet and using another outlet must not bypass the cooldown.");
        });
        helper.runAtTickTime(79, () -> helper.assertTrue(target.getHealth() == 5, "Instant gas received during cooldown must not queue an early extra pulse."));
        helper.runAtTickTime(81, () -> helper.assertTrue(target.getHealth() == 9, "The second full instant budget must heal once after 40 more ticks."));
        helper.runAtTickTime(121, () -> helper.assertTrue(target.getHealth() == 13 && target.getActiveEffects().isEmpty() && control.getHealth() == 1, "The third instant pulse must retain direction, strength and instant-only application."));
        helper.runAtTickTime(163, () -> {
            helper.assertTrue(target.getHealth() == 13, "Stopped instant outlets must not replay gas discarded during cooldown.");
            helper.succeed();
        });
    }

    private static AtmosphericGasEndpoint createEndpoint(GameTestHelper helper, BlockPos pos, Direction direction) {
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, direction.getAxis()));
        BlockEntity pipe = helper.getBlockEntity(pos);
        AtmosphericGasEndpoint endpoint = new AtmosphericGasEndpoint(new BlockFace(helper.absolutePos(pos), direction));
        endpoint.bind(helper.getLevel(), pipe);
        return endpoint;
    }

    private static AtmosphericGasEndpoint reloadEndpoint(GameTestHelper helper, AtmosphericGasEndpoint endpoint) {
        ServerLevel level = helper.getLevel();
        BlockEntity pipe = helper.getBlockEntity(PIPE_POS);
        Provider provider = level.registryAccess();
        AtmosphericGasEndpoint restored = AtmosphericGasEndpoint.read(endpoint.write(provider), provider, new BlockFace(helper.absolutePos(PIPE_POS), Direction.EAST));
        restored.bind(level, pipe);
        return restored;
    }

    private static GasHandler requireHandler(AtmosphericGasEndpoint endpoint) {
        GasHandler handler = endpoint.getGasHandlerProvider().getCapability();
        if (handler == null) {
            throw new NullPointerException("Missing gas handler for the potion atmospheric outlet.");
        }

        return handler;
    }
}
