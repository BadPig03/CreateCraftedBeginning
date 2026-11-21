package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasMeshFactor {
    private static final int MAX_PIVOT_WIDTH = 6;
    private static final int MAX_ROW_WIDTH = 12;
    final List<Row> rows = new ArrayList<>();
    private int remaining = -1;
    private boolean stopped;
    private int column;

    void addRow() {
        rows.add(new Row());
        remaining++;
    }

    void connect(int first, int second) {
        if (first != 0) {
            rows.get(first).weights.merge(second, 1.0, Double::sum);
        }
        if (second != 0) {
            rows.get(second).weights.merge(first, 1.0, Double::sum);
        }
    }

    int remaining() {
        return remaining;
    }

    boolean ready() {
        if (remaining == 0 || stopped) {
            return true;
        }

        Row pivot = rows.get(remaining);
        if (pivot.ports.length == 0) {
            int width = pivot.weights.size();
            if (width > MAX_PIVOT_WIDTH) {
                stopped = true;
                return true;
            }
            for (int port : pivot.weights.keySet()) {
                if (port != 0 && rows.get(port).weights.size() + width - 2 > MAX_ROW_WIDTH) {
                    stopped = true;
                    return true;
                }
            }
            pivot.ports = new int[width];
            pivot.gains = new double[width];
            pivot.diagonal = pivot.sum();
            int index = 0;
            for (Map.Entry<Integer, Double> entry : pivot.weights.entrySet()) {
                pivot.ports[index] = entry.getKey();
                pivot.gains[index++] = entry.getValue() / pivot.diagonal;
            }
        }

        int port = pivot.ports[column];
        if (port != 0) {
            Row row = rows.get(port);
            double weight = row.weights.remove(remaining);
            for (int i = 0; i < pivot.ports.length; i++) {
                if (i != column) {
                    row.weights.merge(pivot.ports[i], weight * pivot.gains[i], Double::sum);
                }
            }
        }
        if (++column == pivot.ports.length) {
            pivot.weights.clear();
            column = 0;
            remaining--;
        }
        return remaining == 0;
    }

    static final class Row {
        private static final int[] NO_PORTS = {};
        private static final double[] NO_GAINS = {};
        final Map<Integer, Double> weights = new LinkedHashMap<>();
        private int[] ports = NO_PORTS;
        private double[] gains = NO_GAINS;
        private double diagonal;

        double sum() {
            double value = 0;
            for (double weight : weights.values()) {
                value += weight;
            }
            return value;
        }

        void distribute(double[] source, int index) {
            for (int i = 0; i < ports.length; i++) {
                source[ports[i]] += gains[i] * source[index];
            }
        }

        double recover(double[] potential, double source) {
            double value = source / diagonal;
            for (int i = 0; i < ports.length; i++) {
                value += gains[i] * potential[ports[i]];
            }
            return value;
        }
    }
}
