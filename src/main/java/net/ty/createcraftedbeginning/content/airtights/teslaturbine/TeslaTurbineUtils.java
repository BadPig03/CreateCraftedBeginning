package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class TeslaTurbineUtils {
    public static final int BASE_STRESS_CAPACITY = 4096;
    public static final int BASE_ROTATION_SPEED = 16;
    public static final int MAX_LEVEL = 16;

    static final int MAX_ROTORS = 8;
    static final int LEVELS_PER_ROTOR = 2;
    static final int FLOW_SAMPLE_RATE = 5;
    static final int FLOW_SAMPLE_COUNT = 10;
    static final float MIN_GAS_SUPPLY_THRESHOLD = 0.01f;

    static final int LAZY_TICK_RATE = 4;
    static final int MAX_NOZZLES_PER_DIRECTION = 8;

    private static final List<NozzlePort> NOZZLE_PORTS = List.copyOf(Arrays.asList(NozzlePort.values()));
    private static final List<NozzlePort> CLOCKWISE_NOZZLE_PORTS = NOZZLE_PORTS.stream().filter(NozzlePort::clockwise).toList();
    private static final List<NozzlePort> COUNTER_CLOCKWISE_NOZZLE_PORTS = NOZZLE_PORTS.stream().filter(port -> !port.clockwise()).toList();

    private TeslaTurbineUtils() {
    }

    public static BlockPos calculateStructurePos(BlockPos turbinePos, Axis axis, int u, int v) {
        return switch (axis) {
            case X -> new BlockPos(turbinePos.getX(), turbinePos.getY() + v, turbinePos.getZ() + u);
            case Z -> new BlockPos(turbinePos.getX() + u, turbinePos.getY() + v, turbinePos.getZ());
            default -> new BlockPos(turbinePos.getX() + u, turbinePos.getY(), turbinePos.getZ() + v);
        };
    }

    public static @Nullable NozzlePort findNozzlePort(BlockPos masterPos, Axis axis, BlockPos nozzlePos) {
        for (NozzlePort port : NOZZLE_PORTS) {
            if (!port.getWorldPosition(masterPos, axis).equals(nozzlePos)) {
                continue;
            }

            return port;
        }
        return null;
    }

    static List<NozzlePort> getNozzlePorts() {
        return NOZZLE_PORTS;
    }

    static List<NozzlePort> getNozzlePorts(boolean clockwise) {
        return clockwise ? CLOCKWISE_NOZZLE_PORTS : COUNTER_CLOCKWISE_NOZZLE_PORTS;
    }

    public enum NozzlePort {
        CLOCKWISE_U2_V1(2, 1, true),
        CLOCKWISE_U_NEG1_V2(-1, 2, true),
        CLOCKWISE_U1_V_NEG2(1, -2, true),
        CLOCKWISE_U_NEG2_V_NEG1(-2, -1, true),
        COUNTER_CLOCKWISE_U_NEG2_V1(-2, 1, false),
        COUNTER_CLOCKWISE_U_NEG1_V_NEG2(-1, -2, false),
        COUNTER_CLOCKWISE_U1_V2(1, 2, false),
        COUNTER_CLOCKWISE_U2_V_NEG1(2, -1, false);

        private final int u;
        private final int v;
        private final boolean clockwise;

        NozzlePort(int u, int v, boolean clockwise) {
            this.u = u;
            this.v = v;
            this.clockwise = clockwise;
        }

        public boolean clockwise() {
            return clockwise;
        }

        BlockPos getWorldPosition(BlockPos masterPos, Axis axis) {
            return calculateStructurePos(masterPos, axis, u, v);
        }

        Direction getOutwardDirection(Axis axis) {
            if (Math.abs(u) > Math.abs(v)) {
                return switch (axis) {
                    case X -> u > 0 ? Direction.SOUTH : Direction.NORTH;
                    case Y, Z -> u > 0 ? Direction.EAST : Direction.WEST;
                };
            }
            return switch (axis) {
                case Y -> v > 0 ? Direction.SOUTH : Direction.NORTH;
                case X, Z -> v > 0 ? Direction.UP : Direction.DOWN;
            };
        }
    }
}
