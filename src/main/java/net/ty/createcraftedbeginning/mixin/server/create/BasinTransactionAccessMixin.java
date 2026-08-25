package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.platform.access.BasinTransactionAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BasinTransactionAccessMixin implements BasinTransactionAccess {
    @Shadow
    protected List<FluidStack> spoutputFluidBuffer;

    @Override
    public List<FluidStack> ccb$copyTransactionFluidOverflow() {
        return spoutputFluidBuffer.stream().map(FluidStack::copy).toList();
    }

    @Override
    public void ccb$restoreTransactionFluidOverflow(List<FluidStack> snapshot) {
        spoutputFluidBuffer.clear();
        snapshot.stream().map(FluidStack::copy).forEach(spoutputFluidBuffer::add);
    }
}
