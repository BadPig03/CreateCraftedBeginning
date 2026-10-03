package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserBehaviour;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalReceiverBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LaserEmitterRangeGameTests {
    private static final String COMPOUND_KEY_LASER_RANGE = "LaserRange";
    private static final String COMPOUND_KEY_RECEIVED_POWER_LP = "ReceivedPowerLp";
    private static final double LENGTH_TOLERANCE = 1.0E-5;

    private LaserEmitterRangeGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void rangeSettingsPersistClampAndSync(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState());
        LaserEmitterBlockEntity emitter = helper.getBlockEntity(pos);
        ScrollValueBehaviour range = requireRange(emitter);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        Provider registries = helper.getLevel().registryAccess();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(emitter.getBlockPos()), Direction.EAST, emitter.getBlockPos(), false);
        ValueSettingsBoard board = range.createBoard(player, hit);
        helper.assertTrue(emitter.getLaserRange() == 32 && !range.onlyVisibleWithWrench(), "Laser range must default to 32 and remain accessible without a wrench.");
        helper.assertTrue(board.maxValue() == 31 && "1".equals(board.formatter().format(new ValueSettings(0, 0)).getString()) && "32".equals(board.formatter().format(new ValueSettings(0, 31)).getString()), "Laser range board did not display the inclusive 1 to 32 range.");
        range.setValueSettings(player, new ValueSettings(0, 0), false);
        helper.assertTrue(emitter.getLaserRange() == 1, "First laser range setting did not select one block.");
        range.setValueSettings(player, new ValueSettings(0, 31), false);
        helper.assertTrue(emitter.getLaserRange() == 32, "Last laser range setting did not select 32 blocks.");
        range.setValueSettings(player, new ValueSettings(0, 6), false);
        helper.assertTrue(emitter.getLaserRange() == 7 && range.getValueSettings().value() == 6, "Laser range board did not preserve its selected step.");
        CompoundTag saved = emitter.saveWithoutMetadata(registries);
        helper.assertTrue(saved.getInt(COMPOUND_KEY_LASER_RANGE) == 7, "Saved laser emitter did not retain its selected range.");
        LaserEmitterBlockEntity restored = new LaserEmitterBlockEntity(CCBBlockEntities.LASER_EMITTER.get(), emitter.getBlockPos(), emitter.getBlockState());
        restored.loadWithComponents(saved, registries);
        helper.assertTrue(restored.getLaserRange() == 7, "Reloaded laser emitter lost its selected range.");
        range.setValue(12);
        restored.handleUpdateTag(emitter.getUpdateTag(registries), registries);
        helper.assertTrue(restored.getLaserRange() == 12, "Laser emitter update tag did not synchronize its range.");
        saved.putInt(COMPOUND_KEY_LASER_RANGE, -1);
        restored.loadWithComponents(saved, registries);
        helper.assertTrue(restored.getLaserRange() == 1, "Negative saved laser range was not clamped to one block.");
        saved.putInt(COMPOUND_KEY_LASER_RANGE, 100);
        restored.loadWithComponents(saved, registries);
        helper.assertTrue(restored.getLaserRange() == 32, "Oversized saved laser range exceeded 32 blocks.");
        saved.remove(COMPOUND_KEY_LASER_RANGE);
        restored.loadWithComponents(saved, registries);
        helper.assertTrue(restored.getLaserRange() == 32, "Missing saved laser range did not use the default.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void rangeControlsFollowFourSidePanels(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState());
        LaserEmitterBlockEntity emitter = helper.getBlockEntity(pos);
        ScrollValueBehaviour range = requireRange(emitter);
        helper.assertTrue(range.getSlotPositioning() instanceof Sided, "Laser range control requires a sided value box.");
        Sided box = (Sided) range.getSlotPositioning();
        BlockPos absolutePos = emitter.getBlockPos();
        for (Direction facing : Iterate.directions) {
            BlockState state = emitter.getBlockState().setValue(DirectionalBlock.FACING, facing);
            int visibleSides = 0;
            for (Direction side : Iterate.directions) {
                box.fromSide(side);
                if (side.getAxis() == facing.getAxis()) {
                    helper.assertTrue(!box.shouldRender(helper.getLevel(), absolutePos, state) && !box.testHit(helper.getLevel(), absolutePos, state, new Vec3(0.5, 0.5, 0.5)), "Laser range control appeared on the emission or input face for " + facing + '.');
                    continue;
                }

                Vec3 offset = box.getLocalOffset(helper.getLevel(), absolutePos, state);
                if (offset == null) {
                    throw new NullPointerException("Missing laser range panel position for " + facing + " on " + side + '.');
                }

                Vec3 fromCenter = offset.subtract(0.5, 0.5, 0.5);
                helper.assertTrue(Math.abs(fromCenter.dot(Vec3.atLowerCornerOf(side.getNormal())) - 0.5) < LENGTH_TOLERANCE && Math.abs(fromCenter.dot(Vec3.atLowerCornerOf(facing.getNormal())) + 0.125) < LENGTH_TOLERANCE, "Laser range control missed the textured panel center for " + facing + " on " + side + '.');
                helper.assertTrue(box.shouldRender(helper.getLevel(), absolutePos, state) && box.testHit(helper.getLevel(), absolutePos, state, offset), "Laser range panel did not accept interaction for " + facing + " on " + side + '.');
                visibleSides++;
            }
            helper.assertTrue(visibleSides == 4, "Laser range control did not expose exactly four side panels for " + facing + '.');
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void changingRangeUpdatesBeamAndClearsOldHit(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        BlockPos targetPos = pos.above(6);
        helper.setBlock(pos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.UP));
        BlockState[] originalColumn = new BlockState[LaserBehaviour.MAX_RANGE];
        for (int index = 0; index < originalColumn.length; index++) {
            originalColumn[index] = helper.getBlockState(pos.above(index + 1));
        }
        try {
            for (int index = 0; index < originalColumn.length; index++) {
                helper.setBlock(pos.above(index + 1), Blocks.AIR);
            }
            helper.setBlock(targetPos, Blocks.STONE);
            LaserEmitterBlockEntity emitter = helper.getBlockEntity(pos);
            ScrollValueBehaviour range = requireRange(emitter);
            LaserBehaviour laser = emitter.getBehaviour(LaserBehaviour.TYPE);
            if (laser == null) {
                throw new NullPointerException("Missing range test laser behaviour at " + emitter.getBlockPos() + '.');
            }

            emitter.applyOpticalPowerAllocation(16);
            emitter.tick();
            helper.assertTrue(laser.getHitResult() != null && Math.abs(emitter.getBeamLength() - 5) < LENGTH_TOLERANCE, "Default laser range did not stop at the nearby obstacle.");
            range.setValue(4);
            emitter.tick();
            helper.assertTrue(laser.getHitResult() == null && Math.abs(emitter.getBeamLength() - 4) < LENGTH_TOLERANCE, "Shortening laser range retained a hit beyond its endpoint.");
            range.setValue(6);
            emitter.tick();
            helper.assertTrue(laser.getHitResult() != null && Math.abs(emitter.getBeamLength() - 5) < LENGTH_TOLERANCE, "Extending laser range did not restore the obstacle hit.");
            range.setValue(1);
            emitter.tick();
            helper.assertTrue(laser.getHitResult() == null && Math.abs(emitter.getBeamLength() - 1) < LENGTH_TOLERANCE && emitter.isLaserActive(), "Minimum laser range changed power or retained an out-of-range hit.");
            helper.setBlock(targetPos, Blocks.AIR);
            range.setValue(32);
            emitter.tick();
            helper.assertTrue(laser.getHitResult() == null && Math.abs(emitter.getBeamLength() - 32) < LENGTH_TOLERANCE, "Unobstructed laser did not reach the maximum selected range: length=" + emitter.getBeamLength() + ", active=" + emitter.isLaserActive() + ", hit=" + laser.getHitResult() + '.');
        }
        finally {
            for (int index = 0; index < originalColumn.length; index++) {
                helper.setBlock(pos.above(index + 1), originalColumn[index]);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 40)
    public static void shorteningRangeStopsReceiverPowerAndExtendingRestoresIt(GameTestHelper helper) {
        BlockPos emitterPos = new BlockPos(4, 2, 4);
        BlockPos receiverPos = emitterPos.above(6);
        helper.setBlock(emitterPos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.UP));
        helper.setBlock(receiverPos, CCBBlocks.PHOTOTHERMAL_RECEIVER_BLOCK.getDefaultState());
        LaserEmitterBlockEntity emitter = helper.getBlockEntity(emitterPos);
        PhotothermalReceiverBlockEntity receiver = helper.getBlockEntity(receiverPos);
        ScrollValueBehaviour range = requireRange(emitter);
        Provider registries = helper.getLevel().registryAccess();
        range.setValue(6);
        helper.onEachTick(() -> {
            emitter.applyOpticalPowerAllocation(16);
            emitter.tick();
        });
        helper.startSequence().thenIdle(5).thenExecute(() -> {
            helper.assertTrue(receiver.getUpdateTag(registries).getInt(COMPOUND_KEY_RECEIVED_POWER_LP) == 16, "Receiver did not receive power within the selected laser range.");
            range.setValue(4);
        }).thenIdle(5).thenExecute(() -> {
            helper.assertTrue(receiver.getUpdateTag(registries).getInt(COMPOUND_KEY_RECEIVED_POWER_LP) == 0, "Receiver continued receiving power beyond the shortened laser range.");
            range.setValue(6);
        }).thenIdle(5).thenExecute(() -> helper.assertTrue(receiver.getUpdateTag(registries).getInt(COMPOUND_KEY_RECEIVED_POWER_LP) == 16, "Extending laser range did not restore receiver power.")).thenSucceed();
    }

    private static ScrollValueBehaviour requireRange(LaserEmitterBlockEntity emitter) {
        ScrollValueBehaviour range = emitter.getBehaviour(ScrollValueBehaviour.TYPE);
        if (range == null) {
            throw new NullPointerException("Missing laser range control at " + emitter.getBlockPos() + '.');
        }

        return range;
    }
}
