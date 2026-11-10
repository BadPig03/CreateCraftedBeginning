package net.ty.createcraftedbeginning.content.opticalpower.laser.beam;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record BeamProfile(double referenceWidth, double widthSlope) {
    public static final double MIN_WIDTH = 0.03125;

    public BeamProfile(double radius) {
        this(referenceForRadius(radius), 0);
    }

    public BeamProfile {
        if (!Double.isFinite(referenceWidth) || !Double.isFinite(widthSlope) || !Double.isFinite(referenceWidth * referenceWidth)) {
            throw new IllegalArgumentException("Beam reference width and slope must be finite with finite cross-sectional area; got width=" + referenceWidth + ", slope=" + widthSlope + '.');
        }
    }

    private static double referenceForRadius(double radius) {
        double width = 2 * radius;
        if (!Double.isFinite(width) || width < MIN_WIDTH || !Double.isFinite(width * width)) {
            throw new IllegalArgumentException("Beam radius must be finite and at least " + MIN_WIDTH / 2 + " with finite cross-sectional area; got " + radius + " blocks.");
        }

        return width;
    }

    public double radius() {
        return Math.max(MIN_WIDTH, Math.abs(referenceWidth)) / 2;
    }

    public double getArea() {
        double radius = radius();
        return 4 * radius * radius;
    }

    public BeamProfile advance(double distance) {
        if (!Double.isFinite(distance) || distance < 0) {
            throw new IllegalArgumentException("Beam propagation distance must be finite and non-negative; got " + distance + " blocks.");
        }

        if (widthSlope == 0 || distance == 0) {
            return this;
        }

        return new BeamProfile(referenceWidth + widthSlope * distance, widthSlope);
    }

    public @Unmodifiable List<Span> spans(double range) {
        advance(range);
        List<Double> boundaries = new ArrayList<>();
        boundaries.add(0.0);
        if (widthSlope != 0) {
            for (int side : new int[]{-1, 1}) {
                double distance = (side * MIN_WIDTH - referenceWidth) / widthSlope;
                if (distance <= 0 || distance >= range || boundaries.contains(distance)) {
                    continue;
                }

                boundaries.add(distance);
            }
        }
        boundaries.add(range);
        boundaries.sort(Double::compare);
        List<Span> spans = new ArrayList<>();
        for (int index = 1; index < boundaries.size(); index++) {
            double from = boundaries.get(index - 1);
            double to = boundaries.get(index);
            double reference = referenceWidth + widthSlope * (from + (to - from) / 2);
            double slope = 0;
            if (Math.abs(reference) > MIN_WIDTH) {
                slope = Math.signum(reference) * widthSlope / 2;
            }
            spans.add(new Span(from, to, slope));
        }
        return List.copyOf(spans);
    }

    public record Span(double from, double to, double radiusSlope) {}
}
