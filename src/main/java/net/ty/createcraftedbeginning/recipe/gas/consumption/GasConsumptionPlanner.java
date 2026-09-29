package net.ty.createcraftedbeginning.recipe.gas.consumption;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan.TankDrain;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasConsumptionPlanner {
    private GasConsumptionPlanner() {
    }

    public static Optional<GasConsumptionPlan> plan(List<GasRecipeRequirement> requirements, GasStorageHandler storage) {
        return plan(requirements, storage, 1);
    }

    public static Optional<GasConsumptionPlan> plan(List<GasRecipeRequirement> requirements, GasStorageHandler storage, long multiplier) {
        if (multiplier <= 0) {
            return Optional.empty();
        }

        List<Source> sources = new ArrayList<>(storage.getTanks());
        for (int tankIndex = 0; tankIndex < storage.getTanks(); tankIndex++) {
            sources.add(new Source(tankIndex, storage.getPressureCompartment(tankIndex)));
        }
        return planScaled(requirements, sources, storage.getTanks(), multiplier);
    }

    public static Optional<GasConsumptionPlan> plan(GasRecipeRequirement requirement, GasPressureCompartment compartment) {
        return plan(requirement, compartment, 1);
    }

    public static Optional<GasConsumptionPlan> plan(GasRecipeRequirement requirement, GasPressureCompartment compartment, long multiplier) {
        if (multiplier <= 0) {
            return Optional.empty();
        }

        return planScaled(List.of(requirement), List.of(new Source(0, compartment)), 1, multiplier);
    }

    public static int findMaximumMultiplier(GasRecipeRequirement requirement, GasPressureCompartment compartment, int maximumMultiplier) {
        return (int) findMaximumMultiplier(requirement, compartment, (long) maximumMultiplier);
    }

    public static long findMaximumMultiplier(GasRecipeRequirement requirement, GasPressureCompartment compartment, long maximumMultiplier) {
        if (maximumMultiplier <= 0) {
            return 0;
        }

        long low = 1;
        long high = maximumMultiplier;
        long best = 0;
        while (low <= high) {
            long candidate = low + (high - low) / 2;
            if (plan(requirement, compartment, candidate).isEmpty()) {
                high = candidate - 1;
                continue;
            }

            best = candidate;
            if (candidate == Long.MAX_VALUE) {
                break;
            }

            low = candidate + 1;
        }
        return best;
    }

    private static Optional<GasConsumptionPlan> planScaled(List<GasRecipeRequirement> requirements, List<Source> sources, int tankCount, long multiplier) {
        if (requirements.isEmpty()) {
            return Optional.of(GasConsumptionPlan.empty(tankCount));
        }

        if (sources.isEmpty()) {
            return Optional.empty();
        }

        List<ScaledRequirement> scaledRequirements = new ArrayList<>(requirements.size());
        long totalRequired = 0;
        for (GasRecipeRequirement requirement : requirements) {
            long amountPerCraft = requirement.amount();
            if (amountPerCraft <= 0 || amountPerCraft > Long.MAX_VALUE / multiplier) {
                return Optional.empty();
            }

            long requiredAmount = amountPerCraft * multiplier;
            if (Long.MAX_VALUE - totalRequired < requiredAmount) {
                return Optional.empty();
            }

            scaledRequirements.add(new ScaledRequirement(requirement, requiredAmount));
            totalRequired += requiredAmount;
        }

        List<long[]> floorOptions = new ArrayList<>(sources.size());
        for (Source source : sources) {
            floorOptions.add(createFloorOptions(source, scaledRequirements));
        }

        long[] selectedFloors = new long[sources.size()];
        return searchFloorAssignments(0, selectedFloors, floorOptions, sources, scaledRequirements, tankCount, totalRequired);
    }

    private static long[] createFloorOptions(Source source, List<ScaledRequirement> requirements) {
        if (source.compartment.getPressureModel() == PressureModel.FIXED) {
            return new long[] {GasPressure.VACUUM_PA};
        }

        GasStack sourceGas = source.compartment.getGasStack();
        if (sourceGas.isEmpty()) {
            return new long[] {GasPressure.VACUUM_PA};
        }

        long sourcePressurePa = source.compartment.getPressurePa();
        Set<Long> floors = new LinkedHashSet<>();
        floors.add(GasPressure.VACUUM_PA);
        for (ScaledRequirement scaledRequirement : requirements) {
            GasRecipeRequirement requirement = scaledRequirement.requirement;
            if (!requirement.ingredient().test(sourceGas)) {
                continue;
            }

            if (requirement.pressure().hasMaximumPressure() && sourcePressurePa > requirement.pressure().maximumPressurePaOrUnbounded()) {
                continue;
            }

            floors.add(requirement.pressure().minimumPressurePaOrVacuum());
        }

        return floors.stream().mapToLong(Long::longValue).sorted().toArray();
    }

    private static Optional<GasConsumptionPlan> searchFloorAssignments(int sourceIndex, long[] selectedFloors, List<long[]> floorOptions, List<Source> sources, List<ScaledRequirement> requirements, int tankCount, long totalRequired) {
        if (sourceIndex >= sources.size()) {
            return buildPlan(selectedFloors, sources, requirements, tankCount, totalRequired);
        }

        for (long floor : floorOptions.get(sourceIndex)) {
            selectedFloors[sourceIndex] = floor;
            Optional<GasConsumptionPlan> plan = searchFloorAssignments(sourceIndex + 1, selectedFloors, floorOptions, sources, requirements, tankCount, totalRequired);
            if (plan.isPresent()) {
                return plan;
            }
        }
        return Optional.empty();
    }

    private static Optional<GasConsumptionPlan> buildPlan(long[] selectedFloors, List<Source> sources, List<ScaledRequirement> requirements, int tankCount, long totalRequired) {
        int sourceCount = sources.size();
        int requirementCount = requirements.size();
        int sourceNode = 0;
        int tankOffset = 1;
        int requirementOffset = tankOffset + sourceCount;
        int sinkNode = requirementOffset + requirementCount;
        long[][] residual = new long[sinkNode + 1][sinkNode + 1];
        long[] sourceCapacities = new long[sourceCount];

        for (int tankIndex = 0; tankIndex < sourceCount; tankIndex++) {
            Source source = sources.get(tankIndex);
            long sourceCapacity = getSourceCapacity(source, selectedFloors[tankIndex], totalRequired);
            sourceCapacities[tankIndex] = sourceCapacity;
            residual[sourceNode][tankOffset + tankIndex] = sourceCapacity;
        }

        for (int requirementIndex = 0; requirementIndex < requirementCount; requirementIndex++) {
            ScaledRequirement scaledRequirement = requirements.get(requirementIndex);
            residual[requirementOffset + requirementIndex][sinkNode] = scaledRequirement.amount;
            for (int tankIndex = 0; tankIndex < sourceCount; tankIndex++) {
                if (sourceCapacities[tankIndex] <= 0 || !canSourceSatisfyRequirement(sources.get(tankIndex), selectedFloors[tankIndex], scaledRequirement.requirement)) {
                    continue;
                }

                residual[tankOffset + tankIndex][requirementOffset + requirementIndex] = Math.min(sourceCapacities[tankIndex], scaledRequirement.amount);
            }
        }

        long totalFlow = findMaximumFlow(residual, sourceNode, sinkNode, totalRequired);
        if (totalFlow != totalRequired) {
            return Optional.empty();
        }

        List<TankDrain> drains = new ArrayList<>();
        for (int tankIndex = 0; tankIndex < sourceCount; tankIndex++) {
            long drainedAmount = sourceCapacities[tankIndex] - residual[sourceNode][tankOffset + tankIndex];
            if (drainedAmount <= 0) {
                continue;
            }

            OptionalLong minimumPressure = OptionalLong.empty();
            OptionalLong maximumPressure = OptionalLong.empty();
            for (int requirementIndex = 0; requirementIndex < requirementCount; requirementIndex++) {
                long assignedAmount = residual[requirementOffset + requirementIndex][tankOffset + tankIndex];
                if (assignedAmount <= 0) {
                    continue;
                }

                PressureRequirement pressure = requirements.get(requirementIndex).requirement.pressure();
                if (pressure.hasMinimumPressure()) {
                    long minimum = pressure.minimumPressurePaOrVacuum();
                    minimumPressure = OptionalLong.of(minimumPressure.isPresent() ? Math.max(minimumPressure.getAsLong(), minimum) : minimum);
                }
                if (!pressure.hasMaximumPressure()) {
                    continue;
                }

                long maximum = pressure.maximumPressurePaOrUnbounded();
                maximumPressure = OptionalLong.of(maximumPressure.isPresent() ? Math.min(maximumPressure.getAsLong(), maximum) : maximum);
            }

            PressureRequirement combinedPressure = new PressureRequirement(minimumPressure.isPresent() ? Optional.of(minimumPressure.getAsLong()) : Optional.empty(), maximumPressure.isPresent() ? Optional.of(maximumPressure.getAsLong()) : Optional.empty());
            Source source = sources.get(tankIndex);
            drains.add(new TankDrain(source.tankIndex, source.compartment, source.compartment.getGasStack(), drainedAmount, combinedPressure));
        }

        GasConsumptionPlan plan = new GasConsumptionPlan(tankCount, drains);
        if (!plan.canExecute()) {
            return Optional.empty();
        }

        return Optional.of(plan);
    }

    private static long getSourceCapacity(Source source, long minimumPressurePa, long totalRequired) {
        GasPressureCompartment compartment = source.compartment;
        GasStack sourceGas = compartment.getGasStack();
        if (sourceGas.isEmpty()) {
            return 0;
        }

        long requestedCapacity;
        if (compartment.getPressureModel() == PressureModel.FIXED) {
            requestedCapacity = totalRequired;
        }
        else {
            requestedCapacity = Math.min(sourceGas.getAmount(), GasPressure.amountAbovePressureFloor(compartment.getStoredAmount(), compartment.getVolume(), minimumPressurePa));
            requestedCapacity = Math.min(requestedCapacity, totalRequired);
        }
        if (requestedCapacity <= 0) {
            return 0;
        }

        GasStack simulatedDrain = compartment.drain(sourceGas.copyWithAmount(requestedCapacity), GasAction.SIMULATE);
        if (simulatedDrain.isEmpty() || !GasStack.isSameGasSameComponents(simulatedDrain, sourceGas)) {
            return 0;
        }

        return Math.min(requestedCapacity, simulatedDrain.getAmount());
    }

    private static boolean canSourceSatisfyRequirement(Source source, long selectedFloorPa, GasRecipeRequirement requirement) {
        GasPressureCompartment compartment = source.compartment;
        GasStack sourceGas = compartment.getGasStack();
        if (sourceGas.isEmpty() || !requirement.ingredient().test(sourceGas)) {
            return false;
        }

        long sourcePressurePa = compartment.getPressurePa();
        if (compartment.getPressureModel() == PressureModel.FIXED) {
            return requirement.pressure().allowsPressure(sourcePressurePa);
        }

        return (!requirement.pressure().hasMaximumPressure() || sourcePressurePa <= requirement.pressure().maximumPressurePaOrUnbounded()) && selectedFloorPa >= requirement.pressure().minimumPressurePaOrVacuum();
    }

    private static long findMaximumFlow(long[][] residual, int sourceNode, int sinkNode, long targetFlow) {
        long totalFlow = 0;
        int[] parent = new int[residual.length];
        while (totalFlow < targetFlow) {
            Arrays.fill(parent, -1);
            parent[sourceNode] = sourceNode;
            ArrayDeque<Integer> pendingNodes = new ArrayDeque<>();
            pendingNodes.add(sourceNode);
            while (!pendingNodes.isEmpty() && parent[sinkNode] == -1) {
                int currentNode = pendingNodes.removeFirst();
                for (int nextNode = 0; nextNode < residual.length; nextNode++) {
                    if (parent[nextNode] != -1 || residual[currentNode][nextNode] <= 0) {
                        continue;
                    }

                    parent[nextNode] = currentNode;
                    pendingNodes.addLast(nextNode);
                }
            }
            if (parent[sinkNode] == -1) {
                break;
            }

            long pathFlow = targetFlow - totalFlow;
            for (int node = sinkNode; node != sourceNode; node = parent[node]) {
                pathFlow = Math.min(pathFlow, residual[parent[node]][node]);
            }
            for (int node = sinkNode; node != sourceNode; node = parent[node]) {
                int previousNode = parent[node];
                residual[previousNode][node] -= pathFlow;
                residual[node][previousNode] += pathFlow;
            }
            totalFlow += pathFlow;
        }
        return totalFlow;
    }

    private record Source(int tankIndex, GasPressureCompartment compartment) {
    }

    private record ScaledRequirement(GasRecipeRequirement requirement, long amount) {
    }
}
