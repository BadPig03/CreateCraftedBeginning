package net.ty.createcraftedbeginning.gas.network.solver.endpoint;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner.Port;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasZonePorts {
    private final List<BlockFace> external;
    private final List<BlockFace> atmosphere;
    private final List<BlockFace> internal;

    public GasZonePorts(List<BlockFace> external, List<BlockFace> atmosphere, List<BlockFace> internal) {
        this.external = external;
        this.atmosphere = atmosphere;
        this.internal = internal;
    }

    public int size() {
        return external.size() + atmosphere.size() + internal.size();
    }

    public GasEndpointSlot source(@Nullable BlockFace visited) {
        int index = externalIndex(visited);
        if (index < 0) {
            return GasEndpointSlot.FIRST;
        }
        return new GasEndpointSlot(index, 0);
    }

    public GasEndpointSlot afterTarget(@Nullable BlockFace visited) {
        int index = externalIndex(visited);
        if (index < 0) {
            return GasEndpointSlot.FIRST;
        }

        return new GasEndpointSlot(index, 0).after(1, size());
    }

    public BlockFace face(GasEndpointSlot slot) {
        int i = slot.face();
        if (i < external.size()) {
            return external.get(i);
        }
        i -= external.size();
        if (i < atmosphere.size()) {
            return atmosphere.get(i);
        }

        return internal.get(i - atmosphere.size());
    }

    public Port read(Level level, GasEndpointSlot slot) {
        BlockFace face = face(slot);
        if (slot.face() >= external.size() + atmosphere.size()) {
            return GasEndpointPlanner.readInternalPort(level, face);
        }
        if (slot.face() >= external.size()) {
            return GasEndpointPlanner.readZoneMouth(level, face);
        }
        return GasEndpointPlanner.readPort(level, face, false, slot.tank());
    }

    private int externalIndex(@Nullable BlockFace face) {
        if (face == null) {
            return -1;
        }

        int low = 0;
        int high = external.size() - 1;
        while (low <= high) {
            int middle = low + (high - low >>> 1);
            int order = GasNetworkTopology.compareBlockFaces(external.get(middle), face);
            if (order < 0) {
                low = middle + 1;
                continue;
            }
            if (order > 0) {
                high = middle - 1;
                continue;
            }

            return middle;
        }

        return -(low + 1);
    }
}
