package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelOutput.Limitation;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystCollectorPanelPower {
    private static final float BASE_RAIN_OUTPUT_MULTIPLIER = 0.5F;
    private static final double SKY_SAMPLE_HEIGHT = 0.6251;

    static AmethystCollectorPanelOutput calculateOutput(Level level, AmethystCollectorPanelRectangle rectangle) {
        if (!level.dimensionType().hasSkyLight()) {
            return new AmethystCollectorPanelOutput(0, Limitation.NO_SKYLIGHT);
        }
        if (!level.isDay()) {
            return new AmethystCollectorPanelOutput(0, Limitation.NIGHT);
        }
        if (!hasOpenSky(level, rectangle)) {
            return new AmethystCollectorPanelOutput(0, Limitation.OBSTRUCTED);
        }

        int powerLp = getRatedPowerLp(rectangle);
        BlockPos anchor = rectangle.anchor();
        if (!SubLevelBridge.isRainingAtWorld(level, getSkySample(anchor.getX(), anchor.getY(), anchor.getZ()))) {
            return new AmethystCollectorPanelOutput(powerLp, Limitation.NONE);
        }

        float rainMultiplier = Mth.clamp(BASE_RAIN_OUTPUT_MULTIPLIER * CCBConfig.server().opticalPower.amethystCollectorPanel.rainOutputMultiplier.getF(), 0.0F, 1.0F);
        int rainPowerLp = Mth.floor(powerLp * rainMultiplier);
        if (rainPowerLp < powerLp) {
            return new AmethystCollectorPanelOutput(rainPowerLp, Limitation.RAIN);
        }

        return new AmethystCollectorPanelOutput(powerLp, Limitation.NONE);
    }

    private static boolean hasOpenSky(Level level, AmethystCollectorPanelRectangle rectangle) {
        int y = rectangle.anchor().getY();
        for (int x = rectangle.minX(); x <= rectangle.maxX(); x++) {
            for (int z = rectangle.minZ(); z <= rectangle.maxZ(); z++) {
                if (SubLevelBridge.canSeeWorldSky(level, getSkySample(x, y, z))) {
                    continue;
                }

                return false;
            }
        }
        return true;
    }

    private static Vec3 getSkySample(int x, int y, int z) {
        return new Vec3(x + 0.5, y + SKY_SAMPLE_HEIGHT, z + 0.5);
    }

    private static int getRatedPowerLp(AmethystCollectorPanelRectangle rectangle) {
        int shortSide = Math.min(rectangle.width(), rectangle.depth());
        return 1 << Mth.clamp(shortSide - 1, 0, 4);
    }
}
