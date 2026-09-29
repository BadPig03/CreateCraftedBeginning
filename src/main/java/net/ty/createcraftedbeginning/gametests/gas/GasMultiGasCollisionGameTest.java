package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasMultiGasCollisionGameTest {
    private static final long SOURCE_PRESSURE_PA = GasPressure.pascals(2);

    private static final BlockPos WEST_SOURCE_POS = new BlockPos(0, 1, 1);
    private static final BlockPos NORTH_SOURCE_POS = new BlockPos(1, 1, 0);
    private static final BlockPos COLLISION_MANIFOLD_POS = new BlockPos(1, 1, 1);

    private GasMultiGasCollisionGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 120)
    public static void differentGasesDestroySharedTransportOnCollision(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AirtightTankBlockEntity westSource = placeFiniteSource(helper, WEST_SOURCE_POS, CCBGases.ULTRAWARM_AIR.get());
        AirtightTankBlockEntity northSource = placeFiniteSource(helper, NORTH_SOURCE_POS, CCBGases.ETHEREAL_AIR.get());
        helper.setBlock(COLLISION_MANIFOLD_POS, collisionManifoldState());

        helper.assertTrue(!GasStack.isSameGasSameComponents(westSource.getTankInventory().getGasStack(), northSource.getTankInventory().getGasStack()), "Multi-gas collision sources were not different gas types");
        helper.assertValueEqual(westSource.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "multi-gas west source pressure");
        helper.assertValueEqual(northSource.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "multi-gas north source pressure");
        helper.assertTrue(level.getBlockState(helper.absolutePos(COLLISION_MANIFOLD_POS)).is(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get()), "Multi-gas collision manifold was not placed before solving");

        helper.succeedWhen(() -> {
            BlockState collisionState = level.getBlockState(helper.absolutePos(COLLISION_MANIFOLD_POS));
            helper.assertTrue(collisionState.isAir(), "Different gases shared the manifold without triggering the default GasCollisionEvent destruction");

            GasStack westGas = westSource.getTankInventory().getGasStack();
            GasStack northGas = northSource.getTankInventory().getGasStack();
            helper.assertTrue(!westGas.isEmpty() && westGas.is(CCBGases.ULTRAWARM_AIR.get()), "West source was emptied or contaminated during the multi-gas collision");
            helper.assertTrue(!northGas.isEmpty() && northGas.is(CCBGases.ETHEREAL_AIR.get()), "North source was emptied or contaminated during the multi-gas collision");
            helper.assertTrue(!GasStack.isSameGasSameComponents(westGas, northGas), "Multi-gas collision incorrectly merged the two source gas types");
        });
    }

    private static AirtightTankBlockEntity placeFiniteSource(GameTestHelper helper, BlockPos pos, Gas gas) {
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof AirtightTankBlockEntity, "Airtight tank block entity was not initialized at " + pos);
        if (!(blockEntity instanceof AirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Airtight tank block entity missing at " + pos + '.');
        }

        long volume = tank.getTankInventory().getVolume();
        helper.assertTrue(volume > 0, "Multi-gas source tank reported zero physical volume at " + pos);
        helper.assertTrue(tank.getTankInventory().getMaxPressurePa() >= SOURCE_PRESSURE_PA, "Multi-gas source tank pressure rating was below the 2 atm test pressure at " + pos);

        long initialAmount = GasPressure.amount(volume, SOURCE_PRESSURE_PA);
        tank.getTankInventory().tryReplaceContents(new GasStack(gas, initialAmount)).requireAccepted();
        helper.assertValueEqual(tank.getTankInventory().getStoredAmount(), initialAmount, "multi-gas source amount at " + pos);
        return tank;
    }

    private static BlockState collisionManifoldState() {
        BlockState state = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState();
        for (Direction face : new Direction[]{Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            state = state.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(face), true);
        }
        return state;
    }
}
