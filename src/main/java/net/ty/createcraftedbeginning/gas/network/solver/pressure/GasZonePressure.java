package net.ty.createcraftedbeginning.gas.network.solver.pressure;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner.Port;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointSlot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasZonePorts;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout;
import net.ty.createcraftedbeginning.gas.telemetry.GasPressureTelemetryTarget;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongBiFunction;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasZonePressure {
    private final GasZoneLayout layout;
    private final GasZonePorts ports;
    private final List<Gauge> gauges;
    private final ToLongBiFunction<BlockPos, GasStack> assistedPumpFlow;
    private final Set<GasStack> species = new LinkedHashSet<>();
    private Map<GasStack, GasZoneField> published = Map.of();
    private Map<GasStack, GasZoneField> building = new HashMap<>();
    private GasEndpointSlot slot = GasEndpointSlot.FIRST;
    private List<GasStack> gases = List.of();
    private int speciesIndex;
    private int device;
    private int gauge;
    private int gaugeGas;
    private int observedFaces;
    private long minimum = Long.MAX_VALUE;
    private long maximum;
    private Stage stage = Stage.DISCOVER;
    private int lastWork;

    public GasZonePressure(GasZoneLayout layout, GasZonePorts ports, List<Gauge> gauges, ToLongBiFunction<BlockPos, GasStack> assistedPumpFlow) {
        this.layout = layout;
        this.ports = ports;
        this.gauges = gauges;
        this.assistedPumpFlow = assistedPumpFlow;
    }

    public static void clearTelemetry(Level level, List<Gauge> gauges) {
        for (Gauge gauge : gauges) {
            if (!(level.getBlockEntity(gauge.pos()) instanceof GasPressureTelemetryTarget target)) {
                continue;
            }

            target.clearPressureTelemetry();
        }
    }

    public void tick(Level level, int budget) {
        lastWork = 0;
        while (lastWork < budget) {
            lastWork++;
            if (step(level)) {
                break;
            }
        }
    }

    public int nodes() {
        return layout.nodes();
    }

    public int lastWork() {
        return lastWork;
    }

    double pressure(BlockFace face, GasStack gas) {
        GasZoneField field = published.get(gas.getAmount() == 1 ? gas : gas.copyWithAmount(1));
        if (field == null) {
            return Double.NaN;
        }

        return field.at(layout.node(face));
    }

    private boolean step(Level level) {
        if (stage == Stage.DISCOVER) {
            if (ports.size() > 0) {
                Port port = ports.read(level, slot);
                GasStack offered = port.offeredGas();
                if (!offered.isEmpty()) {
                    species.add(offered.copyWithAmount(1));
                }
                slot = slot.after(port.tanks(), ports.size());
                if (!slot.equals(GasEndpointSlot.FIRST)) {
                    return false;
                }
            }
            gases = List.copyOf(species);
            species.clear();
            speciesIndex = 0;
            stage = gases.isEmpty() ? Stage.PUBLISH : Stage.PORTS;
            return false;
        }
        if (stage == Stage.PUBLISH) {
            if (gauge < gauges.size()) {
                sampleGauge(level, gauges.get(gauge));
                return false;
            }
            published = building;
            building = new HashMap<>();
            gauge = 0;
            stage = Stage.DISCOVER;
            return true;
        }

        GasStack gas = gases.get(speciesIndex);
        GasZoneField field = building.computeIfAbsent(gas, key -> new GasZoneField(layout, published.get(key)));
        if (stage == Stage.PORTS) {
            BlockFace face = ports.face(slot);
            Port port = ports.read(level, slot);
            int node = layout.node(face);
            if (node >= 0 && port.planner() != null) {
                GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, face.getPos());
                BlockState state = level.getBlockState(face.getPos());
                if (behaviour != null && behaviour.canObservePressureOnFace(state, face.getFace())) {
                    for (GasNetworkPressureEndpoint endpoint : port.planner().planPressureEndpoints(level, gas)) {
                        if (behaviour.acceptsInboundGas(state, face.getFace(), gas)) {
                            field.boundary(node, endpoint.pressureState().pressurePa(), GasFlowResistance.endpointResistance(endpoint.flowResistance().drainResistanceUnits()), endpoint.transferLimits().drainLimit(), true);
                        }
                        if (behaviour.allowsOutboundGas(state, face.getFace(), gas) || behaviour.getInternalPressureBoundary(face.getFace()) == endpoint.access().fillHandler() && endpoint.access().fillHandler() != null) {
                            field.boundary(node, endpoint.pressureState().pressurePa(), GasFlowResistance.endpointResistance(endpoint.flowResistance().fillResistanceUnits()), endpoint.transferLimits().fillLimit(), false);
                        }
                    }
                }
            }
            slot = slot.after(port.tanks(), ports.size());
            if (slot.equals(GasEndpointSlot.FIRST)) {
                stage = Stage.DEVICES;
            }
            return false;
        }
        if (stage == Stage.DEVICES && device < layout.deviceCount()) {
            BlockPos pos = layout.devicePosition(device++);
            GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pos);
            if (behaviour == null) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            for (Direction in : Iterate.directions) {
                int from = layout.node(new BlockFace(pos, in));
                if (from < 0 || !behaviour.acceptsInboundGas(state, in, gas)) {
                    continue;
                }
                for (Direction out : Iterate.directions) {
                    int to = layout.node(new BlockFace(pos, out));
                    if (in == out || to < 0 || !behaviour.allowsOutboundGas(state, out, gas)) {
                        continue;
                    }

                    GasTransportEdgeProperties edge = behaviour.getTransportEdgeProperties(state, in, out, gas);
                    long assistedFlow = -1;
                    if (edge.flowRateLimit() > 0 && behaviour.passesNaturalForwardFlow()) {
                        assistedFlow = assistedPumpFlow.applyAsLong(pos, gas);
                        if (assistedFlow < 0) {
                            edge = GasTransportEdgeProperties.passive(0);
                        }
                    }
                    field.device(from, to, edge, assistedFlow);
                }
            }
            return false;
        }

        stage = Stage.RELAX;
        if (field.step()) {
            device = 0;
            stage = ++speciesIndex == gases.size() ? Stage.PUBLISH : Stage.PORTS;
        }
        return false;
    }

    private void sampleGauge(Level level, Gauge meter) {
        int faces = 0;
        GasZoneField field = null;
        if (gaugeGas < gases.size()) {
            field = building.get(gases.get(gaugeGas++));
        }
        for (Direction side : Iterate.directions) {
            int bit = 1 << side.ordinal();
            if ((meter.connectedFaces() & bit) == 0) {
                continue;
            }

            int node = layout.node(new BlockFace(meter.pos(), side));
            if (node < 0) {
                continue;
            }

            faces |= bit;
            if (field == null || !Double.isFinite(field.at(node))) {
                continue;
            }

            observedFaces |= bit;
            long reading = GasPressure.round(field.at(node));
            minimum = Math.min(minimum, reading);
            maximum = Math.max(maximum, reading);
        }
        if (gaugeGas < gases.size()) {
            return;
        }

        publishGauge(level, meter.pos(), faces != 0 && observedFaces == faces);
        gauge++;
        gaugeGas = 0;
        observedFaces = 0;
        minimum = Long.MAX_VALUE;
        maximum = 0;
    }

    private void publishGauge(Level level, BlockPos pos, boolean known) {
        if (!(level.getBlockEntity(pos) instanceof GasPressureTelemetryTarget target)) {
            return;
        }
        if (!known) {
            target.clearPressureTelemetry();
            return;
        }

        target.acceptPressureTelemetry(minimum, maximum);
    }

    public record Gauge(BlockPos pos, int connectedFaces) {}

    private enum Stage {
        DISCOVER, PORTS, DEVICES, RELAX, PUBLISH
    }
}
