package net.ty.createcraftedbeginning.recipe.transaction;

import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class MachineResourceSnapshots {
    private MachineResourceSnapshots() {
    }

    public static @Unmodifiable List<ItemStack> copyItems(IItemHandler itemHandler) {
        int slotCount = itemHandler.getSlots();
        List<ItemStack> items = new ArrayList<>(slotCount);
        for (int slot = 0; slot < slotCount; slot++) {
            items.add(itemHandler.getStackInSlot(slot).copy());
        }
        return List.copyOf(items);
    }

    public static boolean matchesItems(IItemHandler itemHandler, List<ItemStack> expectedItems) {
        int slotCount = itemHandler.getSlots();
        if (slotCount != expectedItems.size()) {
            return false;
        }

        for (int slot = 0; slot < slotCount; slot++) {
            if (ItemStack.matches(itemHandler.getStackInSlot(slot), expectedItems.get(slot))) {
                continue;
            }

            return false;
        }
        return true;
    }

    public static void restoreItems(IItemHandlerModifiable itemHandler, List<ItemStack> itemSnapshot) {
        int slotCount = itemHandler.getSlots();
        if (slotCount != itemSnapshot.size()) {
            throw new IllegalArgumentException("Item snapshot slot count mismatch: expected %d slots to match target inventory, but snapshot has %d.".formatted(slotCount, itemSnapshot.size()));
        }

        for (int slot = 0; slot < slotCount; slot++) {
            itemHandler.setStackInSlot(slot, itemSnapshot.get(slot).copy());
        }
    }

    public static @Unmodifiable List<FluidStack> copyFluids(IFluidHandler fluidHandler) {
        int tankCount = fluidHandler.getTanks();
        List<FluidStack> fluids = new ArrayList<>(tankCount);
        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            fluids.add(fluidHandler.getFluidInTank(tankIndex).copy());
        }
        return List.copyOf(fluids);
    }

    public static boolean matchesFluids(IFluidHandler fluidHandler, List<FluidStack> expectedFluids) {
        int tankCount = fluidHandler.getTanks();
        if (tankCount != expectedFluids.size()) {
            return false;
        }

        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            FluidStack currentFluid = fluidHandler.getFluidInTank(tankIndex);
            FluidStack expectedFluid = expectedFluids.get(tankIndex);
            if (currentFluid.isEmpty() || expectedFluid.isEmpty()) {
                if (currentFluid.isEmpty() != expectedFluid.isEmpty()) {
                    return false;
                }

                continue;
            }

            if (currentFluid.getAmount() == expectedFluid.getAmount() && FluidStack.isSameFluidSameComponents(currentFluid, expectedFluid)) {
                continue;
            }

            return false;
        }
        return true;
    }

    public static @Unmodifiable List<GasStack> copyGases(GasHandler gasHandler) {
        int tankCount = gasHandler.getTanks();
        List<GasStack> gases = new ArrayList<>(tankCount);
        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            gases.add(gasHandler.getGasInTank(tankIndex).copy());
        }
        return List.copyOf(gases);
    }

    public static boolean matchesGases(GasHandler gasHandler, List<GasStack> expectedGases) {
        int tankCount = gasHandler.getTanks();
        if (tankCount != expectedGases.size()) {
            return false;
        }

        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            if (GasStack.matches(gasHandler.getGasInTank(tankIndex), expectedGases.get(tankIndex))) {
                continue;
            }

            return false;
        }
        return true;
    }

    public static GasTankState snapshotGas(GasTank tank) {
        return tank.snapshot();
    }

    public static void restoreGas(GasTank tank, GasTankState snapshot) {
        tank.tryApplyState(snapshot).requireAccepted();
    }

    public static GasTankSnapshot snapshotGasTanks(SmartGasTankBehaviour... behaviours) {
        List<List<GasTankState>> behaviourSnapshots = new ArrayList<>(behaviours.length);
        for (SmartGasTankBehaviour behaviour : behaviours) {
            GasTankState[] states = behaviour.snapshotStates();
            List<GasTankState> tankSnapshots = new ArrayList<>(states.length);
            tankSnapshots.addAll(Arrays.asList(states));
            behaviourSnapshots.add(List.copyOf(tankSnapshots));
        }
        return new GasTankSnapshot(List.copyOf(behaviourSnapshots));
    }

    public static void restoreGasTanks(GasTankSnapshot snapshot, SmartGasTankBehaviour... behaviours) {
        List<List<GasTankState>> snapshotBehaviours = snapshot.behaviours;
        if (snapshotBehaviours.size() != behaviours.length) {
            throw new IllegalArgumentException("Gas tank snapshot behaviour count mismatch: expected %d to match target, but snapshot has %d.".formatted(behaviours.length, snapshotBehaviours.size()));
        }

        GasTankState[][] restoredStates = new GasTankState[behaviours.length][];
        for (int behaviourIndex = 0; behaviourIndex < behaviours.length; behaviourIndex++) {
            SmartGasTankBehaviour behaviour = behaviours[behaviourIndex];
            int tankCount = behaviour.getTanks().length;
            List<GasTankState> tankSnapshots = snapshotBehaviours.get(behaviourIndex);
            if (tankSnapshots.size() != tankCount) {
                throw new IllegalArgumentException("Gas tank snapshot tank count mismatch at behaviour index %d: expected %d to match target, but snapshot has %d.".formatted(behaviourIndex, tankCount, tankSnapshots.size()));
            }

            GasTankState[] behaviourStates = tankSnapshots.toArray(GasTankState[]::new);
            behaviour.validateStates(behaviourStates, 0);
            restoredStates[behaviourIndex] = behaviourStates;
        }

        boolean[] changedBehaviours = new boolean[behaviours.length];
        for (int behaviourIndex = 0; behaviourIndex < behaviours.length; behaviourIndex++) {
            SmartGasTankBehaviour behaviour = behaviours[behaviourIndex];
            behaviour.beginMutation();
            try {
                behaviour.replaceStates(restoredStates[behaviourIndex], 0);
            }
            finally {
                changedBehaviours[behaviourIndex] = behaviour.endMutation();
            }
        }

        for (int behaviourIndex = 0; behaviourIndex < behaviours.length; behaviourIndex++) {
            if (!changedBehaviours[behaviourIndex]) {
                continue;
            }

            behaviours[behaviourIndex].sendDataImmediately();
        }
    }

    public static FluidTankSnapshot snapshotFluidTanks(Provider provider, SmartFluidTankBehaviour... behaviours) {
        List<List<CompoundTag>> behaviourSnapshots = new ArrayList<>(behaviours.length);
        for (SmartFluidTankBehaviour behaviour : behaviours) {
            TankSegment[] tanks = behaviour.getTanks();
            int tankCount = tanks.length;
            List<CompoundTag> tankSnapshots = new ArrayList<>(tankCount);
            for (TankSegment tankSegment : tanks) {
                tankSnapshots.add(tankSegment.writeNBT(provider).copy());
            }
            behaviourSnapshots.add(List.copyOf(tankSnapshots));
        }
        return new FluidTankSnapshot(List.copyOf(behaviourSnapshots));
    }

    public static void restoreFluidTanks(Provider provider, FluidTankSnapshot snapshot, SmartFluidTankBehaviour... behaviours) {
        List<List<CompoundTag>> snapshotBehaviours = snapshot.behaviours;
        if (snapshotBehaviours.size() != behaviours.length) {
            throw new IllegalArgumentException("Fluid tank snapshot behaviour count mismatch: expected %d to match target, but snapshot has %d.".formatted(behaviours.length, snapshotBehaviours.size()));
        }

        for (int behaviourIndex = 0; behaviourIndex < behaviours.length; behaviourIndex++) {
            SmartFluidTankBehaviour behaviour = behaviours[behaviourIndex];
            TankSegment[] tanks = behaviour.getTanks();
            int tankCount = tanks.length;
            List<CompoundTag> tankSnapshots = snapshotBehaviours.get(behaviourIndex);
            if (tankSnapshots.size() != tankCount) {
                throw new IllegalArgumentException("Fluid tank snapshot tank count mismatch at behaviour index %d: expected %d to match target, but snapshot has %d.".formatted(behaviourIndex, tankCount, tankSnapshots.size()));
            }

            for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
                tanks[tankIndex].readNBT(tankSnapshots.get(tankIndex).copy(), provider, false);
            }
        }
    }

    public static final class FluidTankSnapshot {
        private final List<List<CompoundTag>> behaviours;

        private FluidTankSnapshot(List<List<CompoundTag>> behaviours) {
            this.behaviours = behaviours;
        }
    }

    public static final class GasTankSnapshot {
        private final List<List<GasTankState>> behaviours;

        private GasTankSnapshot(List<List<GasTankState>> behaviours) {
            this.behaviours = behaviours;
        }
    }
}
