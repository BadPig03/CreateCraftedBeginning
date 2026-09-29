package net.ty.createcraftedbeginning.gametests.gas;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasLinearPressureSolver;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasLinearPressureSolver.Result;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeConstraintState;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.EdgeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowEdge;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.FlowGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.PressureNode;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureFlowGraph.SolverWork;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver.PreparedGraph;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureResiduals;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureResiduals.FlowResidual;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransportFlowBudget;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasParallelPumpNumericalDiagnostics {
    private static final int MAX_DIAGNOSTIC_ACTIVE_SET_ITERATIONS = 16;
    private static final Gson GSON = new GsonBuilder().serializeSpecialFloatingPointValues().setPrettyPrinting().create();

    private GasParallelPumpNumericalDiagnostics() {
    }

    static void report(ServerLevel level, BlockPos origin, String label) {
        if (!Boolean.getBoolean("createcraftedbeginning.parallel_pump_numerical_diagnostics") || !(label.endsWith("all-forward") || label.endsWith("flip-3"))) {
            return;
        }

        Snapshot topology = GasNetworkTopology.get(level, origin);
        GasEndpointPlanner planner = GasEndpointPlanner.prepare(level, topology);
        GasStack gas = planner.gasGroups().getFirst();
        PreparedGraph prepared = GasPressureGraphSolver.prepare(level, topology, gas);
        Object templateObject = readField(prepared, "template");
        PreparedGraph isolated;
        try {
            Constructor<PreparedGraph> constructor = PreparedGraph.class.getDeclaredConstructor(Level.class, templateObject.getClass(), GasStack.class);
            constructor.setAccessible(true);
            isolated = constructor.newInstance(level, templateObject, gas);
        }
        catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Failed to create isolated numerical diagnostic graph.", failure);
        }
        isolated.solveHypothetical(planner.planPressureEndpoints(level, gas), new GasTransportFlowBudget());
        GasPressureFlowGraph source = (GasPressureFlowGraph) readField(isolated, "graph");
        JsonArray nodes = GSON.toJsonTree(source.getNodes()).getAsJsonArray();
        JsonArray edges = GSON.toJsonTree(source.getEdges()).getAsJsonArray();
        JsonObject template = GSON.toJsonTree(templateObject).getAsJsonObject();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("label", label);
        report.put("nodes", nodes);
        report.put("edges", edges);
        report.put("template", template);

        GasPressureFlowGraph cold = copy(nodes, edges);
        int staticCount = requireField(template, "staticNodeCount").getAsInt();
        double defaultPressure = (GasPressure.pascals(16) + GasPressure.REFERENCE_PRESSURE_PA) / 2.0;
        for (int i = 0; i < staticCount; i++) {
            cold.setNodePressure(i, defaultPressure);
        }
        for (int i = staticCount; i < nodes.size(); i += 2) {
            cold.setNodePressure(i + 1, cold.getNodePressure(i));
        }
        report.put("coldSolve", solveReport(cold));
        report.put("warmSolve", solveReport(copy(nodes, edges)));

        if (label.endsWith("all-forward")) {
            GasPressureFlowGraph equilibrium = copy(nodes, edges);
            JsonArray ports = requireField(template, "ports").getAsJsonArray();
            for (int i = 0; i < ports.size(); i++) {
                JsonObject port = ports.get(i).getAsJsonObject();
                int z = requireField(requireField(port, "pos").getAsJsonObject(), "z").getAsInt();
                boolean upper = z <= origin.getZ() || z == origin.getZ() + 1 && requireField(port, "face").getAsString().equals("NORTH");
                equilibrium.setNodePressure(i, upper ? GasPressure.REFERENCE_PRESSURE_PA : 0);
            }
            for (JsonElement element : requireField(template, "manifoldSpokes").getAsJsonArray()) {
                JsonObject spoke = element.getAsJsonObject();
                int z = requireField(requireField(spoke, "pipePos").getAsJsonObject(), "z").getAsInt();
                equilibrium.setNodePressure(requireField(spoke, "manifoldNode").getAsInt(), z <= origin.getZ() ? GasPressure.REFERENCE_PRESSURE_PA : 0);
            }
            for (int i = staticCount; i < nodes.size(); i += 2) {
                equilibrium.setNodePressure(i + 1, equilibrium.getNodePressure(i) == GasPressure.REFERENCE_PRESSURE_PA ? GasPressure.REFERENCE_PRESSURE_PA : 0);
            }
            report.put("equilibriumWithActualConstraintStates", linearizeFromActualState(equilibrium));
            report.put("exactZeroFlowEquilibrium", solveReport(equilibrium));
        }
        GasPressureFlowGraph active = copy(nodes, edges);
        for (int i = 0; i < staticCount; i++) {
            active.setNodePressure(i, defaultPressure);
        }
        for (int i = staticCount; i < nodes.size(); i += 2) {
            active.setNodePressure(i + 1, active.getNodePressure(i));
        }
        report.put("initialActiveSetSteps", traceActiveSet(active));
        try {
            Path directory = Path.of("parallel-pump-numerical-diagnostics");
            Files.createDirectories(directory);
            Files.writeString(directory.resolve(label + ".json"), GSON.toJson(report));
        }
        catch (IOException failure) {
            throw new IllegalStateException("Failed to export numerical diagnostics for '" + label + "'.", failure);
        }
    }

    private static JsonElement requireField(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null) {
            throw new NullPointerException("Numerical diagnostic snapshot field '" + name + "' is missing.");
        }

        return value;
    }

    private static Object readField(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(owner);
            if (value == null) {
                throw new NullPointerException("Numerical diagnostic field '" + name + "' is null.");
            }

            return value;
        }
        catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Failed to read numerical diagnostic field '" + name + "'.", failure);
        }
    }

    private static GasPressureFlowGraph copy(JsonArray nodes, JsonArray edges) {
        GasPressureFlowGraph graph = new GasPressureFlowGraph();
        for (JsonElement element : nodes) {
            JsonObject node = element.getAsJsonObject();
            double pressure = requireField(node, "pressurePa").getAsDouble();
            if (requireField(node, "fixed").getAsBoolean()) {
                graph.addFixedNode(pressure);
                continue;
            }

            graph.addUnknownNode(pressure);
        }
        for (JsonElement element : edges) {
            JsonObject edge = element.getAsJsonObject();
            GasTransportPressureDrive drive = GasTransportPressureDrive.pressureBoost(requireField(requireField(edge, "pressureDrive").getAsJsonObject(), "pressureBoostPa").getAsLong());
            GasTransportEdgeProperties properties = new GasTransportEdgeProperties(requireField(edge, "resistanceUnits").getAsLong(), drive, requireField(edge, "flowRateLimit").getAsLong(), requireField(edge, "flowLimitReferencePressurePa").getAsLong(), requireField(edge, "conductanceOverride").getAsDouble(), false, false);
            graph.addEdge(requireField(edge, "from").getAsInt(), requireField(edge, "to").getAsInt(), properties, null, null);
        }
        return graph;
    }

    private static Map<String, Object> solveReport(GasPressureFlowGraph graph) {
        GasPressureResiduals residuals = new GasPressureResiduals(graph);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("before", residuals.calculateUnknownResidual(actualFlows(graph), false));
        long start = System.nanoTime();
        FlowGraphSolution solution = graph.solve();
        result.put("elapsedNanos", System.nanoTime() - start);
        result.put("converged", solution.converged());
        result.put("work", solution.work());
        result.put("after", residuals.calculateUnknownResidual(actualFlows(graph), false));
        result.put("pressures", solution.pressures());
        return result;
    }

    private static EdgeFlow[] actualFlows(GasPressureFlowGraph graph) {
        List<PressureNode> nodes = graph.getNodes();
        return graph.getEdges().stream().map(edge -> edge.calculate(nodes)).toArray(EdgeFlow[]::new);
    }

    private static Map<String, Object> linearizeFromActualState(GasPressureFlowGraph graph) {
        List<FlowEdge> edges = graph.getEdges();
        List<PressureNode> nodes = graph.getNodes();
        EdgeFlow[] flows = new EdgeFlow[edges.size()];
        for (int i = 0; i < flows.length; i++) {
            FlowEdge edge = edges.get(i);
            EdgeConstraintState state = edge.nextConstraintState(nodes, EdgeConstraintState.LINEAR);
            flows[i] = edge.linearize(nodes, state);
        }
        GasPressureResiduals residuals = new GasPressureResiduals(graph);
        SolverWork work = new SolverWork();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("before", residuals.calculateUnknownResidual(actualFlows(graph), false));
        result.put("linearResidual", residuals.calculateUnknownResidual(flows, true));
        long start = System.nanoTime();
        result.put("result", new GasLinearPressureSolver(graph, residuals).solve(flows, work));
        result.put("elapsedNanos", System.nanoTime() - start);
        result.put("after", residuals.calculateUnknownResidual(actualFlows(graph), false));
        result.put("work", work);
        return result;
    }

    private static List<Map<String, Object>> traceActiveSet(GasPressureFlowGraph graph) {
        List<FlowEdge> edges = graph.getEdges();
        List<PressureNode> nodes = graph.getNodes();
        EdgeConstraintState[] states = new EdgeConstraintState[edges.size()];
        Arrays.fill(states, EdgeConstraintState.LINEAR);
        GasPressureResiduals residuals = new GasPressureResiduals(graph);
        GasLinearPressureSolver linear = new GasLinearPressureSolver(graph, residuals);
        List<Map<String, Object>> steps = new ArrayList<>();
        List<EdgeFlow[]> attempted = new ArrayList<>();
        for (int iteration = 0; iteration < MAX_DIAGNOSTIC_ACTIVE_SET_ITERATIONS; iteration++) {
            EdgeFlow[] flows = new EdgeFlow[edges.size()];
            for (int i = 0; i < edges.size(); i++) {
                flows[i] = edges.get(i).linearize(nodes, states[i]);
            }
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("iteration", iteration);
            step.put("statesBefore", states.clone());
            boolean repeated = attempted.stream().anyMatch(previous -> sameLinearization(previous, flows));
            if (repeated) {
                step.put("stop", "repeated linearization; production proceeds to recovery");
                steps.add(step);
                break;
            }

            attempted.add(flows);
            SolverWork work = new SolverWork();
            Result result = linear.solve(flows, work);
            FlowResidual actual = residuals.calculateUnknownResidual(actualFlows(graph), false);
            step.put("linearResult", result);
            step.put("linearResidual", residuals.calculateUnknownResidual(flows, true));
            step.put("actualResidual", actual);
            step.put("tolerance", residuals.flowResidualTolerance(flows));
            step.put("work", work);
            double[] pressures = new double[nodes.size()];
            for (int i = 0; i < pressures.length; i++) {
                pressures[i] = graph.getNodePressure(i);
            }
            step.put("pressures", pressures);
            step.put("linearizedFlows", flows);
            steps.add(step);
            if (result == Result.INCONSISTENT) {
                break;
            }

            for (int i = 0; i < edges.size(); i++) {
                states[i] = edges.get(i).nextConstraintState(nodes, states[i]);
            }
        }
        return steps;
    }

    private static boolean sameLinearization(EdgeFlow[] first, EdgeFlow[] second) {
        for (int i = 0; i < first.length; i++) {
            if (first[i].fromPressureConductance() != second[i].fromPressureConductance() || first[i].toPressureConductance() != second[i].toPressureConductance() || first[i].constantFlowRate() != second[i].constantFlowRate()) {
                return false;
            }
        }
        return true;
    }
}
