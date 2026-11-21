package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasMeshFactor.Row;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasMeshPotential {
    private static final double RESIDUAL_LIMIT = 1e-24;
    private final GasMeshFactor factor;
    private final double[] source;
    private final double[] potential;
    private final double[] direction;
    private final double[] product;
    private final double[] diagonal;
    private int cursor;
    private double residual;
    private double nextResidual;
    private double dot;
    private double scale;
    private Phase phase = Phase.FORWARD;

    GasMeshPotential(GasMeshFactor factor, int inlet, int outlet) {
        this.factor = factor;
        int count = factor.rows.size();
        source = new double[count];
        potential = new double[count];
        source[inlet] = 1;
        source[outlet] = -1;
        cursor = count - 1;
        int reduced = factor.remaining() + 1;
        direction = new double[reduced];
        product = new double[reduced];
        diagonal = new double[reduced];
    }

    double at(int index) {
        return potential[index];
    }

    boolean advance() {
        int remaining = factor.remaining();
        if (phase == Phase.FORWARD) {
            if (cursor > remaining) {
                factor.rows.get(cursor).distribute(source, cursor);
                cursor--;
                return false;
            }
            cursor = 1;
            phase = remaining > 0 ? Phase.INITIALIZE : Phase.BACKWARD;
        }
        if (phase == Phase.BACKWARD) {
            if (cursor < source.length) {
                potential[cursor] = factor.rows.get(cursor).recover(potential, source[cursor]);
                cursor++;
            }
            return cursor == source.length;
        }

        int index = cursor++;
        Row row = factor.rows.get(index);
        switch (phase) {
            case INITIALIZE -> {
                diagonal[index] = row.sum();
                direction[index] = source[index] / diagonal[index];
                residual += source[index] * direction[index];
            }
            case MULTIPLY -> {
                double value = diagonal[index] * direction[index];
                for (Map.Entry<Integer, Double> entry : row.weights.entrySet()) {
                    value -= entry.getValue() * direction[entry.getKey()];
                }
                product[index] = value;
                dot += direction[index] * value;
            }
            case UPDATE -> {
                potential[index] += scale * direction[index];
                source[index] -= scale * product[index];
                nextResidual += source[index] * source[index] / diagonal[index];
            }
            case DIRECTION -> direction[index] = source[index] / diagonal[index] + scale * direction[index];
        }
        if (cursor <= remaining) {
            return false;
        }

        cursor = 1;
        switch (phase) {
            case INITIALIZE -> phase = residual <= RESIDUAL_LIMIT ? Phase.BACKWARD : Phase.MULTIPLY;
            case MULTIPLY -> {
                scale = residual / dot;
                dot = 0;
                phase = Phase.UPDATE;
            }
            case UPDATE -> {
                scale = nextResidual / residual;
                residual = nextResidual;
                nextResidual = 0;
                phase = residual <= RESIDUAL_LIMIT ? Phase.BACKWARD : Phase.DIRECTION;
            }
            case DIRECTION -> phase = Phase.MULTIPLY;
        }
        if (phase == Phase.BACKWARD) {
            cursor = remaining + 1;
            return cursor == source.length;
        }
        return false;
    }

    private enum Phase {
        FORWARD, INITIALIZE, MULTIPLY, UPDATE, DIRECTION, BACKWARD
    }
}
