package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface BasinTransactionAccess {
    SmartFluidTankBehaviour ccb$getTransactionOutputTank();

    List<FluidStack> ccb$getTransactionFluidOverflow();
}
