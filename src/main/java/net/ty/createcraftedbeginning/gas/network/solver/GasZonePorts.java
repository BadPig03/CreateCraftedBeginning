package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner.Port;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasZonePorts {
    private final List<BlockFace> external;
    private final List<BlockFace> atmosphere;
    private final List<BlockFace> internal;

    final boolean observeOnly;

    GasZonePorts(Snapshot topology, List<BlockFace> internal, boolean observeOnly) {
        this.observeOnly = observeOnly;
        external = topology.endpointFaces();
        atmosphere = topology.atmosphericFaces();
        this.internal = internal;
    }

    int size() {
        return external.size() + atmosphere.size() + internal.size();
    }

    GasEndpointSlot source(@Nullable BlockFace visited) {
        int index = externalIndex(visited);
        if (index < 0) {
            return GasEndpointSlot.FIRST;
        }
        return new GasEndpointSlot(index, 0);
    }

    GasEndpointSlot afterTarget(@Nullable BlockFace visited) {
        int index = externalIndex(visited);
        if (index < 0) {
            return GasEndpointSlot.FIRST;
        }

        return new GasEndpointSlot(index, 0).after(1, size());
    }

    BlockFace face(GasEndpointSlot slot) {
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

    Port read(Level level, GasEndpointSlot slot) {
        BlockFace face = face(slot);
        if (slot.face() >= external.size() + atmosphere.size()) {
            return GasEndpointPlanner.readInternalPort(level, face, observeOnly);
        }
        if (slot.face() >= external.size()) {
            return GasEndpointPlanner.readZoneMouth(level, face, observeOnly);
        }
        return GasEndpointPlanner.readPort(level, face, false, slot.tank(), observeOnly);
    }

    private int externalIndex(@Nullable BlockFace face) {
        if (face == null) {
            return -1;
        }
        return Collections.binarySearch(external, face, GasNetworkTopology::compareBlockFaces);
    }
}
