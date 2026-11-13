package net.ty.createcraftedbeginning.content.opticalpower.laser;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record PropagationBudget(double range, int maxSegments, int maxDepth, int maxBeams, double minFluxLm, int maxCandidates, int maxShapeTests) {
    private static final int DEFAULT_MAX_SEGMENTS = 4096;
    private static final int DEFAULT_MAX_DEPTH = 64;
    private static final int DEFAULT_MAX_BEAMS = 4096;
    private static final double DEFAULT_MIN_FLUX_LM = 1.0E-9;
    private static final int DEFAULT_MAX_CANDIDATES = 65536;
    private static final int DEFAULT_MAX_SHAPE_TESTS = 65536;

    public PropagationBudget(double range, int maxSegments, int maxDepth, int maxBeams, double minFluxLm) {
        this(range, maxSegments, maxDepth, maxBeams, minFluxLm, DEFAULT_MAX_CANDIDATES, DEFAULT_MAX_SHAPE_TESTS);
    }

    public PropagationBudget {
        if (!Double.isFinite(range) || range <= 0) {
            throw new IllegalArgumentException("Propagation range must be finite and positive; got " + range + " blocks.");
        }

        if (maxSegments < 1 || maxDepth < 1 || maxBeams < 1 || maxCandidates < 1 || maxShapeTests < 1) {
            throw new IllegalArgumentException("Propagation limits must be positive; got segments=" + maxSegments + ", depth=" + maxDepth + ", beams=" + maxBeams + ", candidates=" + maxCandidates + ", shape tests=" + maxShapeTests + '.');
        }

        if (!Double.isFinite(minFluxLm) || minFluxLm < 0) {
            throw new IllegalArgumentException("Minimum propagation power must be finite and non-negative; got " + minFluxLm + " lm.");
        }
    }

    public static PropagationBudget standard(double range) {
        return new PropagationBudget(range, DEFAULT_MAX_SEGMENTS, DEFAULT_MAX_DEPTH, DEFAULT_MAX_BEAMS, DEFAULT_MIN_FLUX_LM);
    }
}
