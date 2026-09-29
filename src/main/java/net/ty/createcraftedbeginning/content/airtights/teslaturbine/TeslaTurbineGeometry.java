package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class TeslaTurbineGeometry {
    private static final List<NozzlePort> NOZZLE_PORTS = List.copyOf(Arrays.asList(NozzlePort.values()));
    private static final List<NozzlePort> CLOCKWISE_NOZZLE_PORTS = NOZZLE_PORTS.stream().filter(NozzlePort::clockwise).toList();
    private static final List<NozzlePort> COUNTER_CLOCKWISE_NOZZLE_PORTS = NOZZLE_PORTS.stream().filter(port -> !port.clockwise()).toList();

    private TeslaTurbineGeometry() {
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
        if (!clockwise) {
            return COUNTER_CLOCKWISE_NOZZLE_PORTS;
        }

        return CLOCKWISE_NOZZLE_PORTS;
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
            if (Mth.abs(u) > Mth.abs(v)) {
                return switch (axis) {
                    case X -> {
                        if (u > 0) {
                            yield Direction.SOUTH;
                        }

                        yield Direction.NORTH;
                    }
                    case Y, Z -> {
                        if (u > 0) {
                            yield Direction.EAST;
                        }

                        yield Direction.WEST;
                    }
                };
            }

            return switch (axis) {
                case Y -> {
                    if (v > 0) {
                        yield Direction.SOUTH;
                    }

                    yield Direction.NORTH;
                }
                case X, Z -> {
                    if (v > 0) {
                        yield Direction.UP;
                    }

                    yield Direction.DOWN;
                }
            };
        }
    }
}
