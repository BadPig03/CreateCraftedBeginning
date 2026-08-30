package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AirtightReactorKettleFluidPortHandler(IFluidHandler input, IFluidHandler output) implements IFluidHandler {
    @Override
    public int getTanks() {
        return input.getTanks() + output.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return getHandler(tank).getFluidInTank(getLocalTank(tank));
    }

    @Override
    public int getTankCapacity(int tank) {
        return getHandler(tank).getTankCapacity(getLocalTank(tank));
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        getHandler(tank);
        return tank < input.getTanks() && input.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return input.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return output.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return output.drain(maxDrain, action);
    }

    private IFluidHandler getHandler(int tank) {
        if (tank < 0 || tank >= getTanks()) {
            throw new IndexOutOfBoundsException("Tank " + tank + " not in valid range [0," + getTanks() + ')');
        }

        if (tank < input.getTanks()) {
            return input;
        }
        return output;
    }

    private int getLocalTank(int tank) {
        if (tank < input.getTanks()) {
            return tank;
        }
        return tank - input.getTanks();
    }
}
