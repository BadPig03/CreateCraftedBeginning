package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.content.airtights.gas.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ResidueOutletInsertionPlanner {
    private final ResidueOutletBlockEntity outlet;
    private final ResidueOutletInventory inventory;

    public ResidueOutletInsertionPlanner(ResidueOutletBlockEntity outlet, ResidueOutletInventory inventory) {
        this.outlet = outlet;
        this.inventory = inventory;
    }

    @Nullable public ResidueOutletBlockEntity.ResidueInsertionPlan create(FluidStack fluidStack, ItemStack itemStack, int maxAmount) {
        boolean hasFluid = !fluidStack.isEmpty();
        boolean hasItem = !itemStack.isEmpty();
        Level level = outlet.getLevel();
        if (level == null || maxAmount <= 0 || hasFluid == hasItem) {
            return null;
        }

        Provider provider = level.registryAccess();
        return hasFluid ? createFluidInsertionPlan(fluidStack, maxAmount, provider) : createItemInsertionPlan(itemStack, maxAmount, provider);
    }

    private @Nullable ResidueOutletBlockEntity.ResidueInsertionPlan createFluidInsertionPlan(FluidStack fluid, int maxAmount, Provider provider) {
        int plannedAmount = outlet.insertResidueFluid(fluid.copyWithAmount(maxAmount), FluidAction.SIMULATE);
        if (plannedAmount <= 0) {
            return null;
        }

        SmartFluidTankBehaviour fluidTankBehaviour = outlet.getFluidTankBehaviour();
        FluidStack plannedFluid = fluid.copyWithAmount(plannedAmount);
        return new ResidueOutletBlockEntity.ResidueInsertionPlan(plannedAmount, ResourceTransaction.participant(() -> outlet.insertResidueFluid(plannedFluid, FluidAction.SIMULATE) == plannedAmount, () -> MachineResourceSnapshots.snapshotFluidTanks(provider, fluidTankBehaviour), () -> outlet.insertResidueFluid(plannedFluid, FluidAction.EXECUTE) == plannedAmount, snapshot -> {
            MachineResourceSnapshots.restoreFluidTanks(provider, snapshot, fluidTankBehaviour);
            outlet.notifyUpdate();
        }));
    }

    private @Nullable ResidueOutletBlockEntity.ResidueInsertionPlan createItemInsertionPlan(ItemStack item, int maxUnits, Provider provider) {
        int plannedUnits = Math.min(maxUnits, inventory.getItemInsertionCapacityUnits(item));
        if (plannedUnits <= 0) {
            return null;
        }

        ItemStack plannedItem = item.copyWithCount(1);
        return new ResidueOutletBlockEntity.ResidueInsertionPlan(plannedUnits, ResourceTransaction.participant(() -> inventory.getItemInsertionCapacityUnits(plannedItem) >= plannedUnits, () -> inventory.serializeNBT(provider).copy(), () -> inventory.addPartialItemUnits(plannedUnits, plannedItem) == plannedUnits, snapshot -> {
            inventory.deserializeNBT(provider, snapshot.copy());
            outlet.notifyUpdate();
        }));
    }
}
