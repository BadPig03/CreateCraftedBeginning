package net.ty.createcraftedbeginning.core;

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
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
            throw new IllegalArgumentException("Item snapshot slot count mismatch: expected %d slots to match target inventory, but snapshot has %d".formatted(slotCount, itemSnapshot.size()));
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

    public static @Unmodifiable List<GasStack> copyGases(IGasHandler gasHandler) {
        int tankCount = gasHandler.getTanks();
        List<GasStack> gases = new ArrayList<>(tankCount);
        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            gases.add(gasHandler.getGasInTank(tankIndex).copy());
        }
        return List.copyOf(gases);
    }

    public static boolean matchesGases(IGasHandler gasHandler, List<GasStack> expectedGases) {
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

    public static GasStack copyGas(GasTank tank) {
        return tank.getGasStack().copy();
    }

    public static void restoreGas(GasTank tank, GasStack snapshot) {
        tank.setGasStack(snapshot.copy());
    }

    public static GasTankSnapshot snapshotGasTanks(SmartGasTankBehaviour... behaviours) {
        List<List<GasStack>> behaviourSnapshots = new ArrayList<>(behaviours.length);
        for (SmartGasTankBehaviour behaviour : behaviours) {
            int tankCount = behaviour.getTanks().length;
            List<GasStack> tankSnapshots = new ArrayList<>(tankCount);
            IGasHandler gasHandler = behaviour.getCapability();
            for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
                tankSnapshots.add(gasHandler.getGasInTank(tankIndex).copy());
            }
            behaviourSnapshots.add(List.copyOf(tankSnapshots));
        }
        return new GasTankSnapshot(List.copyOf(behaviourSnapshots));
    }

    public static void restoreGasTanks(GasTankSnapshot snapshot, SmartGasTankBehaviour... behaviours) {
        List<List<GasStack>> snapshotBehaviours = snapshot.behaviours;
        if (snapshotBehaviours.size() != behaviours.length) {
            throw new IllegalArgumentException("Gas tank snapshot behaviour count mismatch: expected %d to match target, but snapshot has %d".formatted(behaviours.length, snapshotBehaviours.size()));
        }

        for (int behaviourIndex = 0; behaviourIndex < behaviours.length; behaviourIndex++) {
            SmartGasTankBehaviour behaviour = behaviours[behaviourIndex];
            int tankCount = behaviour.getTanks().length;
            List<GasStack> tankSnapshots = snapshotBehaviours.get(behaviourIndex);
            if (tankSnapshots.size() != tankCount) {
                throw new IllegalArgumentException("Gas tank snapshot tank count mismatch at behaviour index %d: expected %d to match target, but snapshot has %d".formatted(behaviourIndex, tankCount, tankSnapshots.size()));
            }

            GasStack[] restoredContents = new GasStack[tankCount];
            for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
                restoredContents[tankIndex] = tankSnapshots.get(tankIndex).copy();
            }

            behaviour.beginMutation();
            boolean contentsChanged;
            try {
                behaviour.replaceContents(restoredContents, 0);
            } finally {
                contentsChanged = behaviour.endMutation();
            }
            if (!contentsChanged) {
                continue;
            }

            behaviour.sendDataImmediately();
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
            throw new IllegalArgumentException("Fluid tank snapshot behaviour count mismatch: expected %d to match target, but snapshot has %d".formatted(behaviours.length, snapshotBehaviours.size()));
        }

        for (int behaviourIndex = 0; behaviourIndex < behaviours.length; behaviourIndex++) {
            SmartFluidTankBehaviour behaviour = behaviours[behaviourIndex];
            TankSegment[] tanks = behaviour.getTanks();
            int tankCount = tanks.length;
            List<CompoundTag> tankSnapshots = snapshotBehaviours.get(behaviourIndex);
            if (tankSnapshots.size() != tankCount) {
                throw new IllegalArgumentException("Fluid tank snapshot tank count mismatch at behaviour index %d: expected %d to match target, but snapshot has %d".formatted(behaviourIndex, tankCount, tankSnapshots.size()));
            }

            for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
                tanks[tankIndex].readNBT(tankSnapshots.get(tankIndex).copy(), provider, false);
            }
        }
    }

    public static final class FluidTankSnapshot {
        private final List<List<CompoundTag>> behaviours;

        private FluidTankSnapshot(List<List<CompoundTag>> behaviours) {
            this.behaviours = Objects.requireNonNull(behaviours, "Fluid tank snapshot behaviours must not be null");
        }
    }

    public static final class GasTankSnapshot {
        private final List<List<GasStack>> behaviours;

        private GasTankSnapshot(List<List<GasStack>> behaviours) {
            this.behaviours = Objects.requireNonNull(behaviours, "Gas tank snapshot behaviours must not be null");
        }
    }
}
