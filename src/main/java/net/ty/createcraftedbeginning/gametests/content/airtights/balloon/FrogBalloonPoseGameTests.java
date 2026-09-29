package net.ty.createcraftedbeginning.gametests.content.airtights.balloon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyles;
import net.ty.createcraftedbeginning.content.airtights.balloon.FrogBalloonPose;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FrogBalloonPoseGameTests {
    private FrogBalloonPoseGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void depositingPoseInterpolatesOffsetsAndRespectsAnimation(GameTestHelper helper) {
        ItemStack balloon = BalloonStyles.createDefaultBalloon();
        FrogBalloonPose pose = FrogBalloonPose.calculate(new Vec3(4, 0, 0), 2, balloon, true, true);
        helper.assertTrue(pose.offset().equals(new Vec3(2, -0.75, 0)), "Depositing travel offset");
        helper.assertTrue(Math.abs(pose.baseY() - 0.40625F) < 1.0E-6, "Halfway depositing height");
        helper.assertTrue(Math.abs(pose.hookDistance() - BalloonItem.getHookDistance(balloon) / 2) < 1.0E-6, "Halfway hook offset");
        helper.assertTrue(Math.abs(pose.boxDistance() - BalloonItem.getBoxDistance(balloon) / 2) < 1.0E-6, "Halfway box offset");
        FrogBalloonPose idle = FrogBalloonPose.calculate(new Vec3(4, 0, 0), 2, balloon, true, false);
        helper.assertTrue(idle.offset().equals(new Vec3(2, 0, 0)), "Idle depositing offset included animation drop");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void receivingAndDegenerateTravelKeepFiniteClampedOffsets(GameTestHelper helper) {
        ItemStack balloon = BalloonStyles.createDefaultBalloon();
        FrogBalloonPose receiving = FrogBalloonPose.calculate(new Vec3(0, 0, 4), 8, balloon, false, true);
        helper.assertTrue(receiving.offset().equals(new Vec3(0, 0, 8)), "Receiving travel offset");
        helper.assertValueEqual(receiving.baseY(), 0.1875F, "Receiving base height");
        helper.assertValueEqual(receiving.hookDistance(), BalloonItem.getHookDistance(balloon), "Clamped hook offset");
        FrogBalloonPose zero = FrogBalloonPose.calculate(Vec3.ZERO, 0, balloon, true, true);
        helper.assertTrue(zero.offset().equals(new Vec3(0, -0.75, 0)), "Zero travel produced an invalid direction");
        helper.assertValueEqual(zero.hookDistance(), 0.0F, "Zero travel hook offset");
        helper.succeed();
    }
}
