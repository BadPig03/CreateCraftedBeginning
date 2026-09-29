package net.ty.createcraftedbeginning.gas.network.solver.endpoint;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereState;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.logistics.GasInventoryIdentifiers;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.endpoint.AtmosphericGasEndpoint;
import net.ty.createcraftedbeginning.gas.network.endpoint.GasConnectionEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
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
        return new DiscoveredEndpoints(discoverStorageAccesses(level, topology), discoverPressureBoundaryAccesses(level, topology), discoverAtmosphericAccesses(level, topology));
    }

    private static @Unmodifiable List<StorageAccess> discoverStorageAccesses(Level level, Snapshot topology) {
        List<StorageAccess> accesses = new ArrayList<>();
        for (BlockFace endpointFace : topology.endpointFaces()) {
            GasStorageHandler handler = getStorageHandler(level, endpointFace);
            if (handler == null) {
                continue;
            }

            for (int tank = 0; tank < handler.getTanks(); tank++) {
                GasPressureCompartment compartment = handler.getPressureCompartment(tank);
                if (compartment.getVolume() <= 0) {
                    continue;
                }

                accesses.add(new StorageAccess(endpointFace, compartment));
            }
        }
        return List.copyOf(accesses);
    }

    private static @Unmodifiable List<PressureBoundaryAccess> discoverPressureBoundaryAccesses(Level level, Snapshot topology) {
        List<PressureBoundaryAccess> accesses = new ArrayList<>();
        for (BlockFace endpointFace : topology.endpointFaces()) {
            GasPressureBoundary handler = getSingleTankPressureBoundary(level, endpointFace);
            if (handler == null) {
                continue;
            }

            BlockFace inventoryFace = endpointFace.getOpposite();
            InventoryIdentifier identifier = GasInventoryIdentifiers.get(level, inventoryFace);
            accesses.add(new PressureBoundaryAccess(endpointFace, inventoryFace, handler, identifier));
        }
        return List.copyOf(accesses);
    }

    private static @Unmodifiable List<AtmosphericAccess> discoverAtmosphericAccesses(Level level, Snapshot topology) {
        List<AtmosphericAccess> accesses = new ArrayList<>(topology.atmosphericFaces().size());
        for (BlockFace atmosphericFace : topology.atmosphericFaces()) {
            AtmosphericAccess access = getManagedAtmosphericEndpoint(level, atmosphericFace);
            if (access == null) {
                continue;
            }

            accesses.add(access);
        }
        return List.copyOf(accesses);
    }

    @Nullable
    private static AtmosphericAccess getManagedAtmosphericEndpoint(Level level, BlockFace face) {
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

    @Nullable
    private static GasStorageHandler getStorageHandler(Level level, BlockFace endpointFace) {
        BlockPos adjacentPos = endpointFace.getConnectedPos();
        if (!level.isLoaded(adjacentPos)) {
            return null;
        }

        GasHandler rawHandler = level.getCapability(GasCapabilities.BLOCK, adjacentPos, endpointFace.getOppositeFace());
        if (!(rawHandler instanceof GasStorageHandler handler)) {
            return null;
        }

        return handler;
    }

    @Nullable
    private static GasPressureBoundary getSingleTankPressureBoundary(Level level, BlockFace endpointFace) {
        BlockPos adjacentPos = endpointFace.getConnectedPos();
        if (!level.isLoaded(adjacentPos)) {
            return null;
        }

        GasHandler rawHandler = level.getCapability(GasCapabilities.BLOCK, adjacentPos, endpointFace.getOppositeFace());
        if (!(rawHandler instanceof GasPressureBoundary handler) || rawHandler instanceof GasStorageHandler || handler.getTanks() != 1) {
            return null;
        }

        return handler;
    }

    @Internal
    public record DiscoveredEndpoints(List<StorageAccess> storageAccesses, List<PressureBoundaryAccess> pressureBoundaryAccesses, List<AtmosphericAccess> atmosphericAccesses) {
        @Internal
        public DiscoveredEndpoints {
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

    record StorageAccess(BlockFace pipeFace, GasPressureCompartment compartment) {
        long simulateDrainAmount(GasStack gas, long maxAmount) {
            return GasTransferExecutor.simulateDrainAmount(compartment, gas, maxAmount);
        }

        long simulateFillAmount(GasStack gas, long maxAmount) {
            return GasTransferExecutor.simulateFillAmount(compartment, gas, maxAmount, GasPressureLimits.HARD_PRESSURE_PA);
        }
    }

    @Internal
    public record PressureBoundaryAccess(BlockFace pipeFace, BlockFace inventoryFace, GasPressureBoundary handler, @Nullable InventoryIdentifier identifier) {
        long simulateDrainAmount(GasStack gas) {
            return GasTransferExecutor.simulateDrainAmount(handler, gas, Long.MAX_VALUE);
        }

        long simulateFillAmount(GasStack gas) {
            return GasTransferExecutor.simulateFillAmount(handler, gas, Long.MAX_VALUE, GasPressureLimits.HARD_PRESSURE_PA);
        }
    }
}
