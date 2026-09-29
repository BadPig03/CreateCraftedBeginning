package net.ty.createcraftedbeginning.gametests.content.airtights.balloon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.balloon.ChainBalloonPose;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChainBalloonPoseGameTests {
    private ChainBalloonPoseGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void hangingPoseKeepsOffsetAndYawWithoutSwing(GameTestHelper helper) {
        ChainBalloonPose pose = ChainBalloonPose.calculate(new Vec3(12, 30, 43), new Vec3(12, 30, 43), 90, new BlockPos(10, 20, 40));
        helper.assertTrue(pose.offset().equals(new Vec3(2, 10, 3)), "Conveyor-relative offset changed");
        helper.assertTrue(pose.yaw() == 90 && Math.abs(pose.xRotation()) < 0.001 && Math.abs(pose.zRotation()) < 0.001, "Vertical hanging pose introduced swing");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void swingIsClampedAndRotatesWithConveyorYaw(GameTestHelper helper) {
        ChainBalloonPose front = ChainBalloonPose.calculate(new Vec3(10, 0, 0), Vec3.ZERO, 0, BlockPos.ZERO);
        ChainBalloonPose back = ChainBalloonPose.calculate(new Vec3(-10, 0, 0), Vec3.ZERO, 0, BlockPos.ZERO);
        ChainBalloonPose turned = ChainBalloonPose.calculate(new Vec3(10, 0, 0), Vec3.ZERO, 90, BlockPos.ZERO);
        helper.assertTrue(front.zRotation() == 25 && back.zRotation() == -25, "Swing limit or direction changed");
        helper.assertTrue(Math.abs(turned.xRotation()) == 25 && Math.abs(turned.zRotation()) < 0.001, "Yaw did not rotate swing into the other axis");
        helper.succeed();
    }
}
