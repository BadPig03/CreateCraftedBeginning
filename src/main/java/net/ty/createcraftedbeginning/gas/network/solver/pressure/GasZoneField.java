package net.ty.createcraftedbeginning.gas.network.solver.pressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasZoneField {
    private static final int SWEEPS = 8;
    private static final double MIN_LOSSLESS_WEIGHT = (double) SWEEPS / GasPressure.REFERENCE_PRESSURE_PA;
    private final GasZoneLayout layout;
    private final int[] groups;
    private final double[] fixedSum;
    private final int[] anchors;
    private final double[] pressure;
    private final double[] net;
    private final double[] slope;
    private final List<Arc> arcs = new ArrayList<>();
    private final List<LosslessLink> losslessLinks = new ArrayList<>();
    private double @Nullable [] losslessWeights;
    private int cursor;
    private int sweep;
    private Stage stage = Stage.SEED;

    GasZoneField(GasZoneLayout layout, @Nullable GasZoneField previous) {
        this.layout = layout;
        int size = layout.nodes();
        groups = new int[size];
        Arrays.fill(groups, -1);
        fixedSum = new double[size];
        anchors = new int[size];
        pressure = previous == null ? new double[size] : previous.pressure.clone();
        if (previous == null) {
            Arrays.fill(pressure, Double.NaN);
        }
        net = new double[size];
        slope = new double[size];
    }

    void boundary(int node, double pascals, double resistance, long limit, boolean supply) {
        if (limit <= 0 || node < 0) {
            return;
        }

        int root = root(node);
        fixedSum[root] += pascals;
        anchors[root]++;
        arcs.add(new Arc(supply ? -1 : node, supply ? node : -1, pascals, limit, 1 / resistance, null, -1));
    }

    void device(int from, int to, GasTransportEdgeProperties edge, long assistedFlow) {
        if (from == to || edge.flowRateLimit() <= 0) {
            return;
        }

        int a = root(from);
        int b = root(to);
        if (a != b) {
            if (groups[a] > groups[b]) {
                int swap = a;
                a = b;
                b = swap;
            }
            groups[a] += groups[b];
            groups[b] = a;
            fixedSum[a] += fixedSum[b];
            anchors[a] += anchors[b];
        }
        if (edge.isLossless()) {
            if (losslessWeights == null) {
                losslessWeights = new double[pressure.length];
            }
            losslessLinks.add(new LosslessLink(from, to));
            return;
        }

        double conductance = 0;
        if (assistedFlow < 0) {
            double configuredConductance = edge.conductancePerPascal();
            conductance = Double.isFinite(configuredConductance) ? configuredConductance : GasFlowResistance.conductancePerPascal(edge.resistanceUnits());
        }

        arcs.add(new Arc(from, to, 0, edge.flowRateLimit(), conductance, edge, assistedFlow));
    }

    boolean step() {
        if (pressure.length == 0) {
            return true;
        }

        if (stage == Stage.SEED) {
            int i = cursor++;
            int group = root(i);
            if (anchors[group] == 0) {
                pressure[i] = Double.NaN;
            }
            else if (!Double.isFinite(pressure[i])) {
                pressure[i] = fixedSum[group] / anchors[group];
            }
            if (cursor == pressure.length) {
                stage = Stage.BALANCE;
                cursor = 0;
            }
            return false;
        }

        if (stage == Stage.BALANCE && cursor < arcs.size()) {
            Arc arc = arcs.get(cursor++);
            double inlet = arc.from < 0 ? arc.fixed : pressure[arc.from];
            double outlet = arc.to < 0 ? arc.fixed : pressure[arc.to];
            if (!Double.isFinite(inlet) || !Double.isFinite(outlet)) {
                return false;
            }

            double flow = arc.assistedFlow;
            if (flow < 0) {
                double head = arc.device == null ? inlet : arc.device.pressureDrive().drivenOutletPressurePa(inlet);
                double limit = arc.limit;
                if (arc.device != null && arc.device.flowLimitReferencePressurePa() > 0) {
                    limit *= Math.min(1, inlet / arc.device.flowLimitReferencePressurePa());
                }

                flow = Math.min(limit, Math.max(0, head - outlet) * arc.conductance);
            }

            if (arc.from >= 0) {
                net[arc.from] -= flow;
                slope[arc.from] += arc.conductance;
            }
            if (arc.to >= 0) {
                net[arc.to] += flow;
                slope[arc.to] += arc.conductance;
            }
            return false;
        }

        if (stage == Stage.BALANCE) {
            cursor = 0;
            stage = Stage.UPDATE;
        }

        double[] weights = losslessWeights;
        if (stage == Stage.EQUALIZE && weights != null) {
            LosslessLink link = losslessLinks.get(cursor++);
            double inlet = pressure[link.from];
            double outlet = pressure[link.to];
            if (inlet > outlet) {
                double inletWeight = weights[link.from];
                double outletWeight = weights[link.to];
                double equalPressure = outlet + (inlet - outlet) * inletWeight / (inletWeight + outletWeight);
                pressure[link.from] = equalPressure;
                pressure[link.to] = equalPressure;
            }
            if (cursor < losslessLinks.size()) {
                return false;
            }

            cursor = 0;
            stage = Stage.BALANCE;
            return ++sweep == SWEEPS;
        }

        int node = cursor++;
        double capacitance = (double) layout.volume(node) / GasPressure.REFERENCE_PRESSURE_PA;
        double weight = slope[node] + SWEEPS * capacitance;
        if (slope[node] > 0 && Double.isFinite(pressure[node])) {
            pressure[node] = GasPressureLimits.clampToHardLimit(pressure[node] + 0.5 * net[node] / weight);
        }
        if (weights != null) {
            weights[node] = Math.max(MIN_LOSSLESS_WEIGHT, weight);
        }
        net[node] = 0;
        slope[node] = 0;
        if (cursor < pressure.length) {
            return false;
        }

        cursor = 0;
        if (weights != null) {
            stage = Stage.EQUALIZE;
            return false;
        }

        stage = Stage.BALANCE;
        return ++sweep == SWEEPS;
    }

    double at(int node) {
        if (node < 0) {
            return Double.NaN;
        }

        return pressure[node];
    }

    private int root(int index) {
        while (groups[index] >= 0) {
            index = groups[index];
        }
        return index;
    }

    private record Arc(int from, int to, double fixed, long limit, double conductance, @Nullable GasTransportEdgeProperties device, long assistedFlow) {}

    private record LosslessLink(int from, int to) {}

    private enum Stage {
        SEED, BALANCE, UPDATE, EQUALIZE
    }
}
