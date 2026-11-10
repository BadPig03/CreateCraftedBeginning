package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelOutput.Limitation;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystCollectorPanelPower {
    private static final int[] SQUARE_FLUX_LM = {1, 4, 10, 20, 32};
    private static final int RATED_FLUX_STEP_LM = 1;
    private static final int DAYLIGHT_DARKNESS_LIMIT = 4;
    private static final int MAX_SKY_DARKNESS = 11;
    private static final double BASE_RAIN_OUTPUT_MULTIPLIER = 0.5;
    private static final double SKY_SAMPLE_HEIGHT = 0.6251;
    private static final double[][] SKY_SAMPLE_OFFSETS = {{0.5, 0.5}, {0.125, 0.125}, {0.125, 0.875}, {0.875, 0.125}, {0.875, 0.875}};

    static AmethystCollectorPanelOutput calculateOutput(Level level, AmethystCollectorPanelRectangle rectangle) {
        DimensionType dimensionType = level.dimensionType();
        if (!dimensionType.hasSkyLight()) {
            return new AmethystCollectorPanelOutput(0, Limitation.NO_SKYLIGHT);
        }

        if (dimensionType.hasFixedTime()) {
            return new AmethystCollectorPanelOutput(0, Limitation.NIGHT);
        }

        double rainBrightness = 1.0 - (double) (level.getRainLevel(1) * 5) / 16.0;
        double thunderBrightness = 1.0 - (double) (level.getThunderLevel(1) * 5) / 16.0;
        double daylightBrightness = 0.5 + 2.0 * Mth.clamp(Mth.cos(level.getSunAngle(1)), -0.25, 0.25);
        int skyDarkness = (int) ((1.0 - daylightBrightness * rainBrightness * thunderBrightness) * MAX_SKY_DARKNESS);
        if (skyDarkness >= DAYLIGHT_DARKNESS_LIMIT) {
            return new AmethystCollectorPanelOutput(0, Limitation.NIGHT);
        }

        int visibleSamples = countVisibleSkySamples(level, rectangle);
        int totalSamples = rectangle.area() * SKY_SAMPLE_OFFSETS.length;
        if (visibleSamples == 0) {
            return new AmethystCollectorPanelOutput(0, Limitation.OBSTRUCTED);
        }

        double fluxLm = (double) getRatedFluxLm(rectangle) * visibleSamples / totalSamples;
        Limitation limitation = Limitation.NONE;
        if (visibleSamples < totalSamples) {
            limitation = Limitation.OBSTRUCTED;
        }
        BlockPos anchor = rectangle.anchor();
        Vec3 rainSample = new Vec3(anchor.getX() + 0.5, anchor.getY() + SKY_SAMPLE_HEIGHT, anchor.getZ() + 0.5);
        if (!SubLevelBridge.isRainingAtWorld(level, rainSample)) {
            return new AmethystCollectorPanelOutput(fluxLm, limitation);
        }

        double rainMultiplier = Mth.clamp(BASE_RAIN_OUTPUT_MULTIPLIER * CCBConfig.server().opticalPower.amethystCollectorPanel.rainOutputMultiplier.get(), 0, 1);
        double rainFluxLm = fluxLm * rainMultiplier;
        if (rainFluxLm >= fluxLm) {
            return new AmethystCollectorPanelOutput(fluxLm, limitation);
        }

        if (limitation == Limitation.NONE) {
            limitation = Limitation.RAIN;
        }
        return new AmethystCollectorPanelOutput(rainFluxLm, limitation);
    }

    static int getRatedFluxLm(AmethystCollectorPanelRectangle rectangle) {
        int area = rectangle.area();
        for (int side = 1; side < SQUARE_FLUX_LM.length; side++) {
            int nextSide = side + 1;
            int upperArea = nextSide * nextSide;
            if (area > upperArea) {
                continue;
            }

            int lowerArea = side * side;
            int lowerFluxLm = SQUARE_FLUX_LM[side - 1];
            int upperFluxLm = SQUARE_FLUX_LM[side];
            return lowerFluxLm + (area - lowerArea) * (upperFluxLm - lowerFluxLm) / ((upperArea - lowerArea) * RATED_FLUX_STEP_LM) * RATED_FLUX_STEP_LM;
        }

        return SQUARE_FLUX_LM[SQUARE_FLUX_LM.length - 1];
    }

    private static int countVisibleSkySamples(Level level, AmethystCollectorPanelRectangle rectangle) {
        int y = rectangle.anchor().getY();
        List<Vec3> samples = new ArrayList<>(rectangle.area() * SKY_SAMPLE_OFFSETS.length);
        for (int x = rectangle.minX(); x <= rectangle.maxX(); x++) {
            for (int z = rectangle.minZ(); z <= rectangle.maxZ(); z++) {
                for (double[] offset : SKY_SAMPLE_OFFSETS) {
                    samples.add(new Vec3(x + offset[0], y + SKY_SAMPLE_HEIGHT, z + offset[1]));
                }
            }
        }
        return SubLevelBridge.countVisibleWorldSkySamples(level, samples);
    }
}
