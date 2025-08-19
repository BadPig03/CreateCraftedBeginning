package net.ty.createcraftedbeginning.content.compressedair;

import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.data.CCBTags;

import java.util.function.Consumer;

public class LiquidOnlyFluidTank extends SmartFluidTank {
    public LiquidOnlyFluidTank(int capacity, Consumer<FluidStack> updateCallback) {
        super(capacity, updateCallback);
    }
	
    @Override
    public boolean isFluidValid(FluidStack stack) {
        return !stack.is(CCBTags.commonFluidTag("compressed_air"));
    }
}
