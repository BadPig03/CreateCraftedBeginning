package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserBeamGeometry;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LaserBeamGeometryGameTests {
    private static final double TOLERANCE = 1.0E-6;

    private LaserBeamGeometryGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void angledBeamCornersEndOnTargetPlaneAcrossRotatedFrames(GameTestHelper helper) {
        Vec3 direction = new Vec3(1, -1, 0).normalize();
        Vec3 lateral = new Vec3(1, 1, 0).normalize().scale(0.09375);
        Vec3 start = new Vec3(2, 5, 3);
        Vec3 planePoint = new Vec3(0, 1, 0);
        Vec3 normal = new Vec3(0, 1, 0);
        Vec3 translation = new Vec3(100, -20, 70);
        double shortest = Double.POSITIVE_INFINITY;
        double longest = 0;
        for (int firstSign : new int[]{-1, 1}) {
            for (int secondSign : new int[]{-1, 1}) {
                Vec3 corner = start.add(lateral.scale(firstSign)).add(0, 0, secondSign * 0.09375);
                double distance = LaserBeamGeometry.intersectPlaneDistance(corner, direction, planePoint, normal, 5, 32);
                Vec3 end = corner.add(direction.scale(distance));
                helper.assertTrue(Math.abs(end.y - 1) < TOLERANCE, "Angled laser corner did not end on the horizontal target plane.");
                shortest = Math.min(shortest, distance);
                longest = Math.max(longest, distance);
                Vec3 rotatedCorner = corner.xRot(0.4F).yRot(0.7F).add(translation);
                Vec3 rotatedDirection = direction.xRot(0.4F).yRot(0.7F);
                Vec3 rotatedPoint = planePoint.xRot(0.4F).yRot(0.7F).add(translation);
                Vec3 rotatedNormal = normal.xRot(0.4F).yRot(0.7F);
                double rotatedDistance = LaserBeamGeometry.intersectPlaneDistance(rotatedCorner, rotatedDirection, rotatedPoint, rotatedNormal, 5, 32);
                helper.assertTrue(Math.abs(rotatedDistance - distance) < TOLERANCE, "Rotating and translating the laser and target changed the clipping distance.");
            }
        }
        helper.assertTrue(longest - shortest > 0.1, "Angled beam retained a perpendicular end instead of a slanted cut.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void parallelAndOutOfRangeCutsStayFiniteAndBounded(GameTestHelper helper) {
        Vec3 direction = new Vec3(1, 0, 0);
        Vec3 normal = new Vec3(0, 1, 0);
        double parallel = LaserBeamGeometry.intersectPlaneDistance(Vec3.ZERO, direction, new Vec3(0, 1, 0), normal, 7, 32);
        double grazing = LaserBeamGeometry.intersectPlaneDistance(Vec3.ZERO, new Vec3(1, 1.0E-10, 0), new Vec3(0, 1, 0), normal, 7, 32);
        double beyondRange = LaserBeamGeometry.intersectPlaneDistance(Vec3.ZERO, direction, new Vec3(100, 0, 0), direction, 7, 32);
        double behindStart = LaserBeamGeometry.intersectPlaneDistance(Vec3.ZERO, direction, new Vec3(-1, 0, 0), direction, 7, 32);
        double perpendicular = LaserBeamGeometry.intersectPlaneDistance(new Vec3(0, 0.1, 0.1), direction, new Vec3(7, 0, 0), direction, 7, 32);
        helper.assertTrue(parallel == 7 && grazing == 7, "Parallel laser cut failed to retain its finite fallback length.");
        helper.assertTrue(beyondRange == 32 && behindStart == 0, "Laser cut escaped its forward range.");
        helper.assertTrue(perpendicular == 7, "Perpendicular laser cut changed its original length.");
        helper.succeed();
    }
}
