package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.BasinTransactionAccess;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BasinBlockEntityMixin implements BasinTransactionAccess {
    @Shadow
    protected SmartFluidTankBehaviour outputTank;
    @Shadow
    protected List<FluidStack> spoutputFluidBuffer;
    @Shadow
    private boolean contentsChanged;

    @Override
    public SmartFluidTankBehaviour ccb$getTransactionOutputTank() {
        return outputTank;
    }

    @Override
    public List<FluidStack> ccb$getTransactionFluidOverflow() {
        return spoutputFluidBuffer;
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "tick", at = @At("HEAD"))
    private void ccb$tick(CallbackInfo ci) {
        if (!contentsChanged) {
            return;
        }

        BasinBlockEntity basin = (BasinBlockEntity) (Object) this;
        Level level = basin.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(basin.getBlockPos().above(2));
        if (!(blockEntity instanceof GasInjectionChamberBlockEntity chamber)) {
            return;
        }

        chamber.scheduleBasinCheck();
    }
}
