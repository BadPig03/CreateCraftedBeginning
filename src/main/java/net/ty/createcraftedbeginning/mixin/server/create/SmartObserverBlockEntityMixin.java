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
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasManipulationBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPipeConnection.GasFlow;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = SmartObserverBlockEntity.class, remap = false)
public abstract class SmartObserverBlockEntityMixin extends SmartBlockEntity {
    @Unique
    private GasManipulationBehaviour ccb$observedGasTank;
    @Unique
    private ItemStack ccb$compiledGasFilterStack = ItemStack.EMPTY;
    @Unique
    private Predicate<GasStack> ccb$compiledGasFilter = GasFilterUtils.compile(ItemStack.EMPTY);

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
            Predicate<GasStack> filterTest = ccb$getCompiledGasFilter();
            for (Direction side : Iterate.directions) {
                GasFlow flow = transportBehaviour.getFlow(side);
                if (flow == null || !flow.inbound) {
                    continue;
                }

                if (!filterTest.test(flow.gas)) {
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

    @Unique
    private Predicate<GasStack> ccb$getCompiledGasFilter() {
        ItemStack filterStack = filtering.getFilter();
        if (ItemStack.isSameItemSameComponents(ccb$compiledGasFilterStack, filterStack)) {
            return ccb$compiledGasFilter;
        }

        ccb$compiledGasFilterStack = GasFilterUtils.normalizeStack(filterStack);
        ccb$compiledGasFilter = GasFilterUtils.compile(ccb$compiledGasFilterStack);
        return ccb$compiledGasFilter;
    }
}
