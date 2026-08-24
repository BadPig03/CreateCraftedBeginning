package net.ty.createcraftedbeginning.content.airtights.gas.transaction;

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

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class MachineResourceSnapshots {
    private MachineResourceSnapshots() {
    }

    public static @Unmodifiable List<ItemStack> copyItems(IItemHandler itemHandler) {
        List<ItemStack> items = new ArrayList<>(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            items.add(itemHandler.getStackInSlot(slot).copy());
        }
        return List.copyOf(items);
    }

    public static boolean matchesItems(IItemHandler itemHandler, List<ItemStack> expectedItems) {
        if (itemHandler.getSlots() != expectedItems.size()) {
            return false;
        }

        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            if (ItemStack.matches(itemHandler.getStackInSlot(slot), expectedItems.get(slot))) {
                continue;
            }

            return false;
        }
        return true;
    }

    public static void restoreItems(IItemHandlerModifiable itemHandler, List<ItemStack> itemSnapshot) {
        if (itemHandler.getSlots() != itemSnapshot.size()) {
            throw new IllegalArgumentException("Item snapshot slot count does not match inventory");
        }
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            itemHandler.setStackInSlot(slot, itemSnapshot.get(slot).copy());
        }
    }

    public static @Unmodifiable List<FluidStack> copyFluids(IFluidHandler fluidHandler) {
        List<FluidStack> fluids = new ArrayList<>(fluidHandler.getTanks());
        for (int tankIndex = 0; tankIndex < fluidHandler.getTanks(); tankIndex++) {
            fluids.add(fluidHandler.getFluidInTank(tankIndex).copy());
        }
        return List.copyOf(fluids);
    }

    public static boolean matchesFluids(IFluidHandler fluidHandler, List<FluidStack> expectedFluids) {
        if (fluidHandler.getTanks() != expectedFluids.size()) {
            return false;
        }

        for (int tankIndex = 0; tankIndex < fluidHandler.getTanks(); tankIndex++) {
            FluidStack currentFluid = fluidHandler.getFluidInTank(tankIndex);
            FluidStack expectedFluid = expectedFluids.get(tankIndex);
            if (currentFluid.isEmpty() || expectedFluid.isEmpty()) {
                if (currentFluid.isEmpty() != expectedFluid.isEmpty()) {
                    return false;
                }

                continue;
            }

            if (currentFluid.getAmount() != expectedFluid.getAmount() || !FluidStack.isSameFluidSameComponents(currentFluid, expectedFluid)) {
                return false;
            }
        }
        return true;
    }

    public static @Unmodifiable List<GasStack> copyGases(IGasHandler gasHandler) {
        List<GasStack> gases = new ArrayList<>(gasHandler.getTanks());
        for (int tankIndex = 0; tankIndex < gasHandler.getTanks(); tankIndex++) {
            gases.add(gasHandler.getGasInTank(tankIndex).copy());
        }
        return List.copyOf(gases);
    }

    public static boolean matchesGases(IGasHandler gasHandler, List<GasStack> expectedGases) {
        if (gasHandler.getTanks() != expectedGases.size()) {
            return false;
        }

        for (int tankIndex = 0; tankIndex < gasHandler.getTanks(); tankIndex++) {
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

    public static GasContentsSnapshot snapshotGasContents(SmartGasTankBehaviour... behaviours) {
        List<List<GasStack>> behaviourSnapshots = new ArrayList<>(behaviours.length);
        for (SmartGasTankBehaviour behaviour : behaviours) {
            List<GasStack> tankSnapshots = new ArrayList<>(behaviour.getTanks().length);
            IGasHandler gasHandler = behaviour.getCapability();
            for (int tankIndex = 0; tankIndex < behaviour.getTanks().length; tankIndex++) {
                tankSnapshots.add(gasHandler.getGasInTank(tankIndex).copy());
            }
            behaviourSnapshots.add(List.copyOf(tankSnapshots));
        }
        return new GasContentsSnapshot(List.copyOf(behaviourSnapshots));
    }

    public static void restoreGasContents(GasContentsSnapshot snapshot, SmartGasTankBehaviour... behaviours) {
        if (snapshot.behaviours().size() != behaviours.length) {
            throw new IllegalArgumentException("Gas snapshot behaviour count does not match target");
        }
        for (int behaviourIndex = 0; behaviourIndex < behaviours.length; behaviourIndex++) {
            SmartGasTankBehaviour behaviour = behaviours[behaviourIndex];
            List<GasStack> tankSnapshots = snapshot.behaviours().get(behaviourIndex);
            if (tankSnapshots.size() != behaviour.getTanks().length) {
                throw new IllegalArgumentException("Gas snapshot tank count does not match target");
            }

            GasStack[] restoredContents = new GasStack[tankSnapshots.size()];
            for (int tankIndex = 0; tankIndex < tankSnapshots.size(); tankIndex++) {
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
            List<CompoundTag> tankSnapshots = new ArrayList<>(behaviour.getTanks().length);
            for (TankSegment tankSegment : behaviour.getTanks()) {
                tankSnapshots.add(tankSegment.writeNBT(provider).copy());
            }
            behaviourSnapshots.add(List.copyOf(tankSnapshots));
        }
        return new FluidTankSnapshot(List.copyOf(behaviourSnapshots));
    }

    public static void restoreFluidTanks(Provider provider, FluidTankSnapshot snapshot, SmartFluidTankBehaviour... behaviours) {
        if (snapshot.behaviours().size() != behaviours.length) {
            throw new IllegalArgumentException("Fluid snapshot behaviour count does not match target");
        }
        for (int behaviourIndex = 0; behaviourIndex < behaviours.length; behaviourIndex++) {
            SmartFluidTankBehaviour behaviour = behaviours[behaviourIndex];
            List<CompoundTag> tankSnapshots = snapshot.behaviours().get(behaviourIndex);
            if (tankSnapshots.size() != behaviour.getTanks().length) {
                throw new IllegalArgumentException("Fluid snapshot tank count does not match target");
            }
            for (int tankIndex = 0; tankIndex < behaviour.getTanks().length; tankIndex++) {
                behaviour.getTanks()[tankIndex].readNBT(tankSnapshots.get(tankIndex).copy(), provider, false);
            }
        }
    }

    public static GasTankSnapshot snapshotGasTanks(Provider provider, SmartGasTankBehaviour... behaviours) {
        List<List<CompoundTag>> behaviourSnapshots = new ArrayList<>(behaviours.length);
        for (SmartGasTankBehaviour behaviour : behaviours) {
            List<CompoundTag> tankSnapshots = new ArrayList<>(behaviour.getTanks().length);
            for (SmartGasTankBehaviour.TankSegment tankSegment : behaviour.getTanks()) {
                tankSnapshots.add(tankSegment.write(provider).copy());
            }
            behaviourSnapshots.add(List.copyOf(tankSnapshots));
        }
        return new GasTankSnapshot(List.copyOf(behaviourSnapshots));
    }

    public static void restoreGasTanks(Provider provider, GasTankSnapshot snapshot, SmartGasTankBehaviour... behaviours) {
        if (snapshot.behaviours().size() != behaviours.length) {
            throw new IllegalArgumentException("Gas snapshot behaviour count does not match target");
        }
        for (int behaviourIndex = 0; behaviourIndex < behaviours.length; behaviourIndex++) {
            SmartGasTankBehaviour behaviour = behaviours[behaviourIndex];
            List<CompoundTag> tankSnapshots = snapshot.behaviours().get(behaviourIndex);
            if (tankSnapshots.size() != behaviour.getTanks().length) {
                throw new IllegalArgumentException("Gas snapshot tank count does not match target");
            }
            for (int tankIndex = 0; tankIndex < behaviour.getTanks().length; tankIndex++) {
                behaviour.getTanks()[tankIndex].read(tankSnapshots.get(tankIndex).copy(), provider);
            }
        }
    }

    public record FluidTankSnapshot(List<List<CompoundTag>> behaviours) {}

    public record GasTankSnapshot(List<List<CompoundTag>> behaviours) {}

    public record GasContentsSnapshot(List<List<GasStack>> behaviours) {}
}
