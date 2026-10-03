package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalReceiverBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalReceiverPort;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhotothermalReceiverPortGameTests {
    private static final String COMPOUND_KEY_RECEIVED_POWER_LP = "ReceivedPowerLp";

    private PhotothermalReceiverPortGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void portsRequireGeometricHitsWithoutExtendingInteractionShape(GameTestHelper helper) {
        VoxelShape laserShape = PhotothermalReceiverPort.getLaserShape();
        Vec3 center = new Vec3(0.5, 0.5, 0.5);
        for (PhotothermalReceiverPort port : PhotothermalReceiverPort.values()) {
            Direction direction = port.getDirection();
            Vec3 start = center.add(Vec3.atLowerCornerOf(direction.getNormal()).scale(2));
            BlockHitResult hit = laserShape.clip(start, center, BlockPos.ZERO);
            if (hit == null) {
                throw new NullPointerException("Missing laser hit on receiver port " + port + '.');
            }

            helper.assertTrue(PhotothermalReceiverPort.findHit(hit) == port, "Laser did not identify receiver port " + port + '.');
        }

        BlockHitResult bodyHit = laserShape.clip(new Vec3(-1, 0.25, 0.25), new Vec3(0.5, 0.25, 0.25), BlockPos.ZERO);
        BlockHitResult topHit = laserShape.clip(new Vec3(0.5, 2, 0.5), center, BlockPos.ZERO);
        BlockHitResult bottomMiss = laserShape.clip(new Vec3(0.25, -1, 0.25), new Vec3(0.25, 0.5, 0.25), BlockPos.ZERO);
        BlockHitResult ordinaryHit = CCBShapes.PHOTOTHERMAL_RECEIVER.clip(new Vec3(-1, 0.5, 0.5), center, BlockPos.ZERO);
        if (bodyHit == null || topHit == null || bottomMiss == null || ordinaryHit == null) {
            throw new NullPointerException("Missing receiver body hit for port rejection test.");
        }

        helper.assertTrue(PhotothermalReceiverPort.findHit(bodyHit) == null, "Receiver body accepted an off-center laser.");
        helper.assertTrue(PhotothermalReceiverPort.findHit(topHit) == null, "Receiver accepted a laser from above.");
        helper.assertTrue(PhotothermalReceiverPort.findHit(bottomMiss) == null, "Receiver bottom accepted a laser outside its port.");
        helper.assertTrue(ordinaryHit.getLocation().x == 0, "Ordinary interaction shape includes a protruding port.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 80)
    public static void fiveLaserPortsIlluminateIndependentlyAndTopIsRejected(GameTestHelper helper) {
        BlockPos receiverPos = new BlockPos(5, 4, 5);
        helper.setBlock(receiverPos, CCBBlocks.PHOTOTHERMAL_RECEIVER_BLOCK.getDefaultState());
        PhotothermalReceiverBlockEntity receiver = helper.getBlockEntity(receiverPos);
        Map<PhotothermalReceiverPort, LaserEmitterBlockEntity> emitters = new EnumMap<>(PhotothermalReceiverPort.class);
        for (PhotothermalReceiverPort port : PhotothermalReceiverPort.values()) {
            Direction direction = port.getDirection();
            BlockPos emitterPos = receiverPos.relative(direction, 3);
            helper.setBlock(emitterPos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, direction.getOpposite()));
            LaserEmitterBlockEntity emitter = helper.getBlockEntity(emitterPos);
            emitters.put(port, emitter);
        }

        BlockPos topEmitterPos = receiverPos.above(3);
        helper.setBlock(topEmitterPos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.DOWN));
        LaserEmitterBlockEntity topEmitter = helper.getBlockEntity(topEmitterPos);
        int[] ticks = {0};
        helper.onEachTick(() -> {
            int tick = ++ticks[0];
            for (LaserEmitterBlockEntity emitter : emitters.values()) {
                int power = 16;
                if (tick >= 40) {
                    power = 0;
                }

                emitter.applyOpticalPowerAllocation(power);
                emitter.tick();
            }
            topEmitter.applyOpticalPowerAllocation(48);
            topEmitter.tick();
            if (tick == 15) {
                for (PhotothermalReceiverPort port : PhotothermalReceiverPort.values()) {
                    helper.assertTrue(receiver.isPortIlluminated(port), "Active laser did not illuminate receiver port " + port + '.');
                }

                helper.setBlock(receiverPos.west(), Blocks.STONE.defaultBlockState());
            }
            if (tick == 30) {
                for (PhotothermalReceiverPort port : PhotothermalReceiverPort.values()) {
                    helper.assertTrue(receiver.isPortIlluminated(port) == (port != PhotothermalReceiverPort.WEST), "Occlusion changed the wrong receiver port " + port + '.');
                }
            }
            if (tick < 50) {
                return;
            }

            for (PhotothermalReceiverPort port : PhotothermalReceiverPort.values()) {
                helper.assertTrue(!receiver.isPortIlluminated(port), "Receiver port retained light after its laser stopped: " + port + '.');
            }
            helper.assertTrue(receiver.getUpdateTag(helper.getLevel().registryAccess()).getInt(COMPOUND_KEY_RECEIVED_POWER_LP) == 0, "Laser from above supplied receiver power.");
            helper.succeed();
        });
    }
}
