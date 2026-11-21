package net.ty.createcraftedbeginning.gas.network.solver.endpoint;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereState;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.endpoint.AtmosphericGasEndpoint;
import net.ty.createcraftedbeginning.gas.network.endpoint.GasConnectionEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.FlowResistance;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasEndpointDiscovery {
    private GasEndpointDiscovery() {
    }

    static DiscoveredEndpoints discover(Level level, Snapshot topology) {
        List<StorageAccess> storage = new ArrayList<>();
        List<PressureBoundaryAccess> boundaries = new ArrayList<>();
        for (BlockFace face : topology.endpointFaces()) {
            BlockPos pos = face.getConnectedPos();
            if (!level.isLoaded(pos)) {
                continue;
            }

            GasHandler handler = level.getCapability(GasCapabilities.BLOCK, pos, face.getOppositeFace());
            if (handler instanceof GasStorageHandler tanks) {
                int count = tanks.getTanks();
                for (int index = 0; index < count; index++) {
                    GasPressureCompartment compartment = tanks.getPressureCompartment(index);
                    if (compartment.getVolume() <= 0) {
                        continue;
                    }

                    storage.add(new StorageAccess(face, compartment, tanks));
                }
                continue;
            }

            if (!(handler instanceof GasPressureBoundary boundary) || boundary.getTanks() != 1) {
                continue;
            }

            boundaries.add(new PressureBoundaryAccess(face, boundary));
        }
        for (BlockPos pipePos : topology.state().devicePositions()) {
            GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pipePos);
            if (behaviour == null) {
                continue;
            }

            for (Direction face : Iterate.directions) {
                GasPressureBoundary boundary = behaviour.getInternalPressureBoundary(face);
                if (boundary == null || behaviour.getConnection(face) == null) {
                    continue;
                }

                boundaries.add(new PressureBoundaryAccess(new BlockFace(pipePos, face), boundary));
            }
        }
        return new DiscoveredEndpoints(storage, boundaries, discoverAtmosphericAccesses(level, topology));
    }

    @Nullable
    static AtmosphericAccess getManagedAtmosphericEndpoint(Level level, BlockFace face) {
        if (!level.isLoaded(face.getConnectedPos())) {
            return null;
        }

        GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, face.getPos());
        if (behaviour == null) {
            return null;
        }

        GasPipeConnection connection = behaviour.getConnection(face.getFace());
        if (connection == null) {
            return null;
        }

        GasConnectionEndpoint endpoint = connection.getEndpoint();
        if (!(endpoint instanceof AtmosphericGasEndpoint)) {
            if (!connection.resolveEndpoint(level, face.getPos())) {
                return null;
            }

            endpoint = connection.getEndpoint();
        }
        if (!(endpoint instanceof AtmosphericGasEndpoint atmosphericEndpoint)) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(face.getPos());
        if (blockEntity == null) {
            return null;
        }

        atmosphericEndpoint.bind(level, blockEntity);
        ICapabilityProvider<GasHandler> provider = atmosphericEndpoint.getGasHandlerProvider();
        GasHandler handler = provider.getCapability();
        if (handler == null) {
            return null;
        }

        AtmosphereState atmosphereState = atmosphericEndpoint.getAtmosphereState();
        GasStack atmosphericGas = atmosphericEndpoint.canExtractAtmosphericGas() && !atmosphereState.gas().isEmpty() ? new GasStack(atmosphereState.gas(), Long.MAX_VALUE) : GasStack.EMPTY;
        return new AtmosphericAccess(face, handler, atmosphereState, atmosphericGas);
    }

    private static @Unmodifiable List<AtmosphericAccess> discoverAtmosphericAccesses(Level level, Snapshot topology) {
        List<BlockFace> atmosphericFaces = topology.atmosphericFaces();
        List<AtmosphericAccess> accesses = new ArrayList<>(atmosphericFaces.size());
        for (BlockFace face : atmosphericFaces) {
            AtmosphericAccess access = getManagedAtmosphericEndpoint(level, face);
            if (access == null) {
                continue;
            }

            accesses.add(access);
        }
        return List.copyOf(accesses);
    }

    record DiscoveredEndpoints(List<StorageAccess> storageAccesses, List<PressureBoundaryAccess> pressureBoundaryAccesses, List<AtmosphericAccess> atmosphericAccesses) {
        DiscoveredEndpoints {
            storageAccesses = List.copyOf(storageAccesses);
            pressureBoundaryAccesses = List.copyOf(pressureBoundaryAccesses);
            atmosphericAccesses = List.copyOf(atmosphericAccesses);
        }
    }

    record AtmosphericAccess(BlockFace face, GasHandler handler, AtmosphereState atmosphereState, GasStack atmosphericGas) {
        long simulateFillAmount(GasStack gas) {
            return GasTransferExecutor.simulateFillAmount(handler, gas, Long.MAX_VALUE, GasPressureLimits.HARD_PRESSURE_PA);
        }
    }

    record StorageAccess(BlockFace pipeFace, GasPressureCompartment compartment, FlowResistance contact) {
        StorageAccess(BlockFace pipeFace, GasPressureCompartment compartment, GasStorageHandler handler) {
            this(pipeFace, compartment, handler.usesStandardPipePort() ? FlowResistance.STANDARD_PIPE_PORT : FlowResistance.NONE);
        }

        long simulateDrainAmount(GasStack gas, long maxAmount) {
            return GasTransferExecutor.simulateDrainAmount(compartment, gas, maxAmount);
        }

        long simulateFillAmount(GasStack gas, long maxAmount) {
            return GasTransferExecutor.simulateFillAmount(compartment, gas, maxAmount, GasPressureLimits.HARD_PRESSURE_PA);
        }
    }

    record PressureBoundaryAccess(BlockFace pipeFace, GasPressureBoundary handler) {}
}
