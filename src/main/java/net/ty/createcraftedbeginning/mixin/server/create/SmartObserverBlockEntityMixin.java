package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlock;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection.AirFlow;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasManipulationBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.IGasFilter;
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
    private GasManipulationBehaviour ccb$observedGasTank;

    @Shadow
    private FilteringBehaviour filtering;

    private SmartObserverBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    public abstract void activate();

    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void ccb$addGasBehaviour(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        ccb$observedGasTank = new GasManipulationBehaviour(this, (w, p, s) -> new BlockFace(p, DirectedDirectionalBlock.getTargetDirection(s))).bypassSidedness();
        behaviours.add(ccb$observedGasTank);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ccb$tick(CallbackInfo ci) {
        BlockPos targetPos = worldPosition.relative(SmartObserverBlock.getTargetDirection(getBlockState()));
        GasTransportBehaviour transportBehaviour = BlockEntityBehaviour.get(level, targetPos, GasTransportBehaviour.TYPE);
        if (transportBehaviour != null) {
            for (Direction side : Iterate.directions) {
                AirFlow flow = transportBehaviour.getFlow(side);
                if (flow == null || !flow.inbound) {
                    continue;
                }

                ItemStack filterStack = filtering.getFilter();
                if (!filterStack.isEmpty() && (!(filterStack.getItem() instanceof IGasFilter gasFilter) || !gasFilter.test(filterStack, flow.gas))) {
                    continue;
                }

                activate();
                return;
            }
            return;
        }

        if (ccb$observedGasTank == null || ccb$observedGasTank.simulate().extractAny().isEmpty()) {
            return;
        }

        activate();
    }
}
