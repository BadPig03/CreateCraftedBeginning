package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public interface BasinTransactionAccess {
    SmartFluidTankBehaviour ccb$getTransactionOutputTank();

    List<FluidStack> ccb$getTransactionFluidOverflow();
}
