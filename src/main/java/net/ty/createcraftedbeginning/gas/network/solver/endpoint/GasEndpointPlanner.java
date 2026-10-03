package net.ty.createcraftedbeginning.gas.network.solver.endpoint;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointDiscovery.AtmosphericAccess;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointDiscovery.DiscoveredEndpoints;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointDiscovery.PressureBoundaryAccess;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointDiscovery.StorageAccess;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.FlowResistance;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PredictionAccess;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PressureState;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.Recovery;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.TransferAccess;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.TransferLimits;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasEndpointPlanner {
    private final List<StorageEndpointGroup> storageEndpoints;
    private final List<PressureBoundaryEndpointGroup> pressureBoundaries;
    private final List<AtmosphericAccess> atmosphericAccesses;
    private final List<GasStack> gasGroups;

    private GasEndpointPlanner(List<StorageEndpointGroup> storageEndpoints, List<PressureBoundaryEndpointGroup> pressureBoundaries, List<AtmosphericAccess> atmosphericAccesses) {
        this.storageEndpoints = List.copyOf(storageEndpoints);
        this.pressureBoundaries = List.copyOf(pressureBoundaries);
        this.atmosphericAccesses = List.copyOf(atmosphericAccesses);
        gasGroups = collectGasGroups();
    }

    public static GasEndpointPlanner prepare(Level level, Snapshot topology) {
        return prepare(GasEndpointDiscovery.discover(level, topology));
    }

    @Internal
    public static GasEndpointPlanner prepare(DiscoveredEndpoints discovered) {
        return new GasEndpointPlanner(groupStorageEndpoints(discovered.storageAccesses()), groupPressureBoundaries(discovered.pressureBoundaryAccesses()), discovered.atmosphericAccesses());
    }

    private static void addGasGroup(List<GasStack> groups, GasStack candidate) {
        if (candidate.isEmpty()) {
            return;
        }

        for (GasStack group : groups) {
            if (!GasStack.isSameGasSameComponents(group, candidate)) {
                continue;
            }

            return;
        }
        groups.add(candidate.copyWithAmount(1));
    }

    private static @Unmodifiable List<StorageEndpointGroup> groupStorageEndpoints(List<StorageAccess> accesses) {
        List<StorageEndpointGroup> endpoints = new ArrayList<>();
        for (StorageAccess access : accesses) {
            StorageEndpointGroup matchingEndpoint = null;
            for (StorageEndpointGroup endpoint : endpoints) {
                if (!endpoint.matches(access)) {
                    continue;
                }

                matchingEndpoint = endpoint;
                break;
            }

            if (matchingEndpoint == null) {
                endpoints.add(new StorageEndpointGroup(access));
                continue;
            }

            matchingEndpoint.add(access);
        }

        for (StorageEndpointGroup endpoint : endpoints) {
            endpoint.sortAccesses();
        }
        endpoints.sort((first, second) -> GasNetworkTopology.compareBlockFaces(first.canonicalPipeFace(), second.canonicalPipeFace()));
        return List.copyOf(endpoints);
    }

    private static @Unmodifiable List<PressureBoundaryEndpointGroup> groupPressureBoundaries(List<PressureBoundaryAccess> accesses) {
        List<PressureBoundaryEndpointGroup> endpoints = new ArrayList<>();
        for (PressureBoundaryAccess access : accesses) {
            PressureBoundaryEndpointGroup matchingEndpoint = null;
            for (PressureBoundaryEndpointGroup endpoint : endpoints) {
                if (endpoint.handler != access.handler()) {
                    continue;
                }

                matchingEndpoint = endpoint;
                break;
            }

            if (matchingEndpoint == null) {
                endpoints.add(new PressureBoundaryEndpointGroup(access));
                continue;
            }

            matchingEndpoint.accesses.add(access);
        }

        for (PressureBoundaryEndpointGroup endpoint : endpoints) {
            endpoint.sortAccesses();
        }
        endpoints.sort((first, second) -> GasNetworkTopology.compareBlockFaces(first.canonicalPipeFace(), second.canonicalPipeFace()));
        return List.copyOf(endpoints);
    }

    @Nullable
    private static GasNetworkPressureEndpoint createAtmosphericPressureEndpoint(AtmosphericAccess boundary, GasStack gas) {
        long drainLimit = 0;
        if (!boundary.atmosphericGas().isEmpty() && GasStack.isSameGasSameComponents(boundary.atmosphericGas(), gas)) {
            drainLimit = Long.MAX_VALUE;
        }

        long fillLimit = boundary.simulateFillAmount(gas);
        if (drainLimit <= 0 && fillLimit <= 0) {
            return null;
        }

        List<BlockFace> drainFaces = drainLimit > 0 ? List.of(boundary.face()) : List.of();
        List<BlockFace> fillFaces = fillLimit > 0 ? List.of(boundary.face()) : List.of();
        GasHandler drainHandler = null;
        if (drainLimit > 0) {
            drainHandler = boundary.handler();
        }
        GasHandler fillHandler = null;
        if (fillLimit > 0) {
            fillHandler = boundary.handler();
        }
        return new GasNetworkPressureEndpoint(new TransferAccess(drainHandler, fillHandler, drainFaces, fillFaces), new PressureState(GasPressureLimits.clampToHardLimit(boundary.atmosphereState().pressurePa()), true, 0, Long.MAX_VALUE, GasPressureLimits.HARD_PRESSURE_PA, Long.MAX_VALUE), new TransferLimits(drainLimit, fillLimit), Recovery.DISCARD);
    }

    public List<GasStack> gasGroups() {
        return gasGroups;
    }

    public @Unmodifiable List<GasNetworkPressureEndpoint> planPressureEndpoints(Level level, GasStack gas) {
        List<GasNetworkPressureEndpoint> endpoints = new ArrayList<>();
        for (StorageEndpointGroup endpoint : storageEndpoints) {
            GasNetworkPressureEndpoint pressureEndpoint = endpoint.createPressureEndpoint(level, gas);
            if (pressureEndpoint == null) {
                continue;
            }

            endpoints.add(pressureEndpoint);
        }
        for (PressureBoundaryEndpointGroup endpoint : pressureBoundaries) {
            endpoint.appendPressureEndpoints(level, gas, endpoints);
        }
        for (AtmosphericAccess atmosphericAccess : atmosphericAccesses) {
            GasNetworkPressureEndpoint pressureEndpoint = createAtmosphericPressureEndpoint(atmosphericAccess, gas);
            if (pressureEndpoint == null) {
                continue;
            }

            endpoints.add(pressureEndpoint);
        }
        return List.copyOf(endpoints);
    }

    private @Unmodifiable List<GasStack> collectGasGroups() {
        List<GasStack> groups = new ArrayList<>();
        for (StorageEndpointGroup endpoint : storageEndpoints) {
            addGasGroup(groups, endpoint.currentGas());
        }
        for (PressureBoundaryEndpointGroup endpoint : pressureBoundaries) {
            addGasGroup(groups, endpoint.currentGas());
        }
        for (AtmosphericAccess atmosphericAccess : atmosphericAccesses) {
            addGasGroup(groups, atmosphericAccess.atmosphericGas());
        }
        return List.copyOf(groups);
    }

    private static final class StorageEndpointGroup {
        private final List<StorageAccess> accesses = new ArrayList<>();
        private final Object compartmentIdentity;

        private StorageEndpointGroup(StorageAccess firstAccess) {
            compartmentIdentity = firstAccess.compartment().getCompartmentIdentity();
            accesses.add(firstAccess);
        }

        private boolean matches(StorageAccess access) {
            return compartmentIdentity == access.compartment().getCompartmentIdentity();
        }

        private void add(StorageAccess access) {
            accesses.add(access);
        }

        private void sortAccesses() {
            accesses.sort((first, second) -> GasNetworkTopology.compareBlockFaces(first.pipeFace(), second.pipeFace()));
        }

        private BlockFace canonicalPipeFace() {
            return accesses.getFirst().pipeFace();
        }

        private GasStack currentGas() {
            for (StorageAccess access : accesses) {
                GasStack gas = access.compartment().getGasStack();
                if (gas.isEmpty()) {
                    continue;
                }

                return gas;
            }
            return accesses.getFirst().compartment().getGasStack();
        }

        @Nullable
        private GasNetworkPressureEndpoint createPressureEndpoint(Level level, GasStack gas) {
            List<StorageAccess> usableAccesses = new ArrayList<>();
            for (StorageAccess access : accesses) {
                BlockFace pipeFace = access.pipeFace();
                GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pipeFace.getPos());
                GasPipeConnection connection = behaviour == null ? null : behaviour.getConnection(pipeFace.getFace());
                if (connection == null || connection.hasPendingTransfer() && !connection.tryRecoverPendingTransfer(level, pipeFace.getPos())) {
                    continue;
                }

                usableAccesses.add(access);
            }
            if (usableAccesses.isEmpty()) {
                return null;
            }

            GasPressureCompartment physicalCompartment = usableAccesses.getFirst().compartment();
            GasStack currentGas = physicalCompartment.getGasStack();
            PressureModel pressureModel = physicalCompartment.getPressureModel();
            List<BlockFace> drainFaces = new ArrayList<>();
            List<BlockFace> fillFaces = new ArrayList<>();
            StorageAccess bestDrainAccess = null;
            GasPipeConnection bestDrainConnection = null;
            long bestDrain = 0;
            StorageAccess bestFillAccess = null;
            long bestFill = 0;
            long currentAmount = currentGas.getAmount();

            for (StorageAccess access : usableAccesses) {
                BlockFace pipeFace = access.pipeFace();
                GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pipeFace.getPos());
                GasPipeConnection connection = behaviour == null ? null : behaviour.getConnection(pipeFace.getFace());
                GasPressureCompartment compartment = access.compartment();
                if (!currentGas.isEmpty()) {
                    long requestedDrain = pressureModel == PressureModel.FIXED ? Long.MAX_VALUE : currentAmount;
                    long drain = access.simulateDrainAmount(gas, requestedDrain);
                    if (drain > 0) {
                        drainFaces.add(pipeFace);
                    }
                    if (drain > bestDrain) {
                        bestDrain = drain;
                        bestDrainAccess = access;
                        bestDrainConnection = connection;
                    }
                }

                long requestedFill = pressureModel == PressureModel.FIXED ? Long.MAX_VALUE : BoundedMath.saturatedSubtract(compartment.getMaxAmount(), currentAmount);
                if (requestedFill <= 0) {
                    continue;
                }

                long fill = access.simulateFillAmount(gas, requestedFill);
                if (fill > 0) {
                    fillFaces.add(pipeFace);
                }
                if (fill <= bestFill) {
                    continue;
                }

                bestFill = fill;
                bestFillAccess = access;
            }

            if (drainFaces.isEmpty() && fillFaces.isEmpty()) {
                return null;
            }

            StorageAccess physicalAccess = bestDrainAccess != null ? bestDrainAccess : bestFillAccess;
            if (physicalAccess == null) {
                return null;
            }

            physicalCompartment = physicalAccess.compartment();
            GasStack physicalGas = physicalCompartment.getGasStack();
            currentAmount = physicalGas.isEmpty() ? 0 : physicalGas.getAmount();
            long volume = physicalCompartment.getVolume();
            long maxPressurePa = GasPressureLimits.clampToHardLimit(physicalCompartment.getMaxPressurePa());
            long maxAmount = physicalCompartment.getMaxAmount();
            double pressurePa = GasPressureLimits.clampToHardLimit(pressureModel == PressureModel.FIXED ? physicalCompartment.getPressurePa() : GasPressure.pressureExact(currentAmount, volume));
            List<GasPressureCompartment> recoveryCompartments = accesses.stream().map(StorageAccess::compartment).toList();
            List<PredictionAccess> predictionAccesses = usableAccesses.stream().map(access -> new PredictionAccess(access.compartment(), List.of(access.pipeFace()))).toList();
            GasPressureCompartment drainCompartment = null;
            BlockPos drainPos = null;
            if (bestDrainAccess != null) {
                drainCompartment = bestDrainAccess.compartment();
                drainPos = bestDrainAccess.pipeFace().getPos();
            }
            GasPressureCompartment fillCompartment = null;
            if (bestFillAccess != null) {
                fillCompartment = bestFillAccess.compartment();
            }
            return new GasNetworkPressureEndpoint(new TransferAccess(drainCompartment, fillCompartment, drainFaces, fillFaces), new PressureState(pressurePa, pressureModel == PressureModel.FIXED, currentAmount, volume, maxPressurePa, maxAmount), new TransferLimits(bestDrain, bestFill), FlowResistance.NONE, new Recovery(recoveryCompartments, bestDrainConnection, drainPos, false), predictionAccesses);
        }
    }

    private static final class PressureBoundaryEndpointGroup {
        private final List<PressureBoundaryAccess> accesses = new ArrayList<>();
        private final GasPressureBoundary handler;

        private PressureBoundaryEndpointGroup(PressureBoundaryAccess firstAccess) {
            handler = firstAccess.handler();
            accesses.add(firstAccess);
        }

        private static long additionalBoundaryResistanceUnits(double resistanceFactor) {
            if (!Double.isFinite(resistanceFactor) || resistanceFactor <= 0) {
                return 0;
            }

            return GasFlowResistance.fromFactor(resistanceFactor);
        }

        private void sortAccesses() {
            accesses.sort((first, second) -> GasNetworkTopology.compareBlockFaces(first.pipeFace(), second.pipeFace()));
        }

        private BlockFace canonicalPipeFace() {
            return accesses.getFirst().pipeFace();
        }

        private GasStack currentGas() {
            return handler.getGasInTank(0);
        }

        private void appendPressureEndpoints(Level level, GasStack gas, List<GasNetworkPressureEndpoint> endpoints) {
            List<BlockFace> usableFaces = new ArrayList<>();
            GasPipeConnection recoveryConnection = null;
            BlockPos recoveryPipePos = null;
            for (PressureBoundaryAccess access : accesses) {
                BlockFace pipeFace = access.pipeFace();
                GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pipeFace.getPos());
                GasPipeConnection connection = behaviour == null ? null : behaviour.getConnection(pipeFace.getFace());
                if (connection == null || connection.hasPendingTransfer() && !connection.tryRecoverPendingTransfer(level, pipeFace.getPos())) {
                    continue;
                }

                usableFaces.add(pipeFace);
                if (recoveryConnection != null) {
                    continue;
                }

                recoveryConnection = connection;
                recoveryPipePos = pipeFace.getPos();
            }
            if (usableFaces.isEmpty()) {
                return;
            }

            List<BlockFace> faces = List.copyOf(usableFaces);
            long drainLimit = GasTransferExecutor.simulateDrainAmount(handler, gas, Long.MAX_VALUE);
            long fillLimit = GasTransferExecutor.simulateFillAmount(handler, gas, Long.MAX_VALUE, GasPressureLimits.HARD_PRESSURE_PA);
            if (drainLimit > 0) {
                long drainPressurePa = GasPressureLimits.clampToHardLimit(handler.getDrainPressurePa(0, gas));
                long drainResistanceUnits = additionalBoundaryResistanceUnits(handler.getDrainFlowResistanceFactor(0, gas));
                endpoints.add(new GasNetworkPressureEndpoint(new TransferAccess(handler, null, faces, List.of()), new PressureState(drainPressurePa, true, 0, Long.MAX_VALUE, GasPressureLimits.HARD_PRESSURE_PA, Long.MAX_VALUE), new TransferLimits(drainLimit, 0), new FlowResistance(drainResistanceUnits, 0), new Recovery(List.of(), recoveryConnection, recoveryPipePos, false)));
            }
            if (fillLimit <= 0) {
                return;
            }

            long fillPressurePa = GasPressureLimits.clampToHardLimit(handler.getFillPressurePa(0, gas));
            long fillResistanceUnits = additionalBoundaryResistanceUnits(handler.getFillFlowResistanceFactor(0, gas));
            endpoints.add(new GasNetworkPressureEndpoint(new TransferAccess(null, handler, List.of(), faces), new PressureState(fillPressurePa, true, 0, Long.MAX_VALUE, GasPressureLimits.HARD_PRESSURE_PA, Long.MAX_VALUE), new TransferLimits(0, fillLimit), new FlowResistance(0, fillResistanceUnits), Recovery.NONE));
        }
    }
}
