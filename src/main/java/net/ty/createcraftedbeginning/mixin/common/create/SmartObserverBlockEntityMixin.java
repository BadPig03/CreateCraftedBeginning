package net.ty.createcraftedbeginning.mixin.common.create;

import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gasobserver.GasObserverBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = SmartObserverBlockEntity.class, remap = false)
public abstract class SmartObserverBlockEntityMixin extends SmartBlockEntity {
    @Unique
    private GasObserverBehaviour ccb$gasObserver;

    @Shadow
    private FilteringBehaviour filtering;

    private SmartObserverBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    public abstract void activate();

    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void ccb$addGasBehaviour(List<BlockEntityBehaviour> behaviours, CallbackInfo callback) {
        ccb$gasObserver = new GasObserverBehaviour(this);
        behaviours.add(ccb$gasObserver);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ccb$tick(CallbackInfo callback) {
        if (!(ccb$gasObserver != null && ccb$gasObserver.hasMatchingGas(filtering))) {
            return;
        }

        activate();
    }
}
