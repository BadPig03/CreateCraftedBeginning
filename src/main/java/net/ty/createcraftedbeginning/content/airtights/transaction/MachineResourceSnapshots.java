package net.ty.createcraftedbeginning.content.airtights.transaction;

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

    public static @Unmodifiable List<ItemStack> copyItems(IItemHandler inventory) {
        List<ItemStack> contents = new ArrayList<>(inventory.getSlots());
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            contents.add(inventory.getStackInSlot(slot).copy());
        }
        return List.copyOf(contents);
    }

    public static boolean matchesItems(IItemHandler inventory, List<ItemStack> expected) {
        if (inventory.getSlots() != expected.size()) {
            return false;
        }

        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!ItemStack.matches(inventory.getStackInSlot(slot), expected.get(slot))) {
                return false;
            }
        }
        return true;
    }

    public static void restoreItems(IItemHandlerModifiable inventory, List<ItemStack> snapshot) {
        if (inventory.getSlots() != snapshot.size()) {
            throw new IllegalArgumentException("Item snapshot slot count does not match inventory");
        }
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            inventory.setStackInSlot(slot, snapshot.get(slot).copy());
        }
    }

    public static @Unmodifiable List<FluidStack> copyFluids(IFluidHandler handler) {
        List<FluidStack> contents = new ArrayList<>(handler.getTanks());
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            contents.add(handler.getFluidInTank(tank).copy());
        }
        return List.copyOf(contents);
    }

    public static boolean matchesFluids(IFluidHandler handler, List<FluidStack> expected) {
        if (handler.getTanks() != expected.size()) {
            return false;
        }

        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack current = handler.getFluidInTank(tank);
            FluidStack expectedStack = expected.get(tank);
            if (current.isEmpty() || expectedStack.isEmpty()) {
                if (current.isEmpty() != expectedStack.isEmpty()) {
                    return false;
                }

                continue;
            }

            if (current.getAmount() != expectedStack.getAmount() || !FluidStack.isSameFluidSameComponents(current, expectedStack)) {
                return false;
            }
        }
        return true;
    }

    public static @Unmodifiable List<GasStack> copyGases(IGasHandler handler) {
        List<GasStack> contents = new ArrayList<>(handler.getTanks());
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            contents.add(handler.getGasInTank(tank).copy());
        }
        return List.copyOf(contents);
    }

    public static boolean matchesGases(IGasHandler handler, List<GasStack> expected) {
        if (handler.getTanks() != expected.size()) {
            return false;
        }

        for (int tank = 0; tank < handler.getTanks(); tank++) {
            if (!GasStack.matches(handler.getGasInTank(tank), expected.get(tank))) {
                return false;
            }
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
        List<List<GasStack>> snapshots = new ArrayList<>(behaviours.length);
        for (SmartGasTankBehaviour behaviour : behaviours) {
            List<GasStack> tankSnapshots = new ArrayList<>(behaviour.getTanks().length);
            IGasHandler handler = behaviour.getCapability();
            for (int tank = 0; tank < behaviour.getTanks().length; tank++) {
                tankSnapshots.add(handler.getGasInTank(tank).copy());
            }
            snapshots.add(List.copyOf(tankSnapshots));
        }
        return new GasContentsSnapshot(List.copyOf(snapshots));
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

            GasStack[] replacement = new GasStack[tankSnapshots.size()];
            for (int tank = 0; tank < tankSnapshots.size(); tank++) {
                replacement[tank] = tankSnapshots.get(tank).copy();
            }

            behaviour.beginMutation();
            boolean changed;
            try {
                behaviour.replaceContents(replacement, 0);
            } finally {
                changed = behaviour.endMutation();
            }
            if (changed) {
                behaviour.sendDataImmediately();
            }
        }
    }

    public static FluidTankSnapshot snapshotFluidTanks(Provider provider, SmartFluidTankBehaviour... behaviours) {
        List<List<CompoundTag>> snapshots = new ArrayList<>(behaviours.length);
        for (SmartFluidTankBehaviour behaviour : behaviours) {
            List<CompoundTag> tankSnapshots = new ArrayList<>(behaviour.getTanks().length);
            for (TankSegment tank : behaviour.getTanks()) {
                tankSnapshots.add(tank.writeNBT(provider).copy());
            }
            snapshots.add(List.copyOf(tankSnapshots));
        }
        return new FluidTankSnapshot(List.copyOf(snapshots));
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
            for (int tank = 0; tank < behaviour.getTanks().length; tank++) {
                behaviour.getTanks()[tank].readNBT(tankSnapshots.get(tank).copy(), provider, false);
            }
        }
    }

    public static GasTankSnapshot snapshotGasTanks(Provider provider, SmartGasTankBehaviour... behaviours) {
        List<List<CompoundTag>> snapshots = new ArrayList<>(behaviours.length);
        for (SmartGasTankBehaviour behaviour : behaviours) {
            List<CompoundTag> tankSnapshots = new ArrayList<>(behaviour.getTanks().length);
            for (SmartGasTankBehaviour.TankSegment tank : behaviour.getTanks()) {
                tankSnapshots.add(tank.write(provider).copy());
            }
            snapshots.add(List.copyOf(tankSnapshots));
        }
        return new GasTankSnapshot(List.copyOf(snapshots));
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
            for (int tank = 0; tank < behaviour.getTanks().length; tank++) {
                behaviour.getTanks()[tank].read(tankSnapshots.get(tank).copy(), provider, false);
            }
        }
    }

    public record FluidTankSnapshot(List<List<CompoundTag>> behaviours) {}

    public record GasTankSnapshot(List<List<CompoundTag>> behaviours) {}

    public record GasContentsSnapshot(List<List<GasStack>> behaviours) {}
}
