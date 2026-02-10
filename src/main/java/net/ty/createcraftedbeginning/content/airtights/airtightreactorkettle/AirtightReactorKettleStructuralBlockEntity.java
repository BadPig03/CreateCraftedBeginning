package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AirtightReactorKettleStructuralBlockEntity extends SmartBlockEntity implements ThresholdSwitchObservable {
    private FilteringBehaviour filteringBehaviour;

    public AirtightReactorKettleStructuralBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL.get(), (be, context) -> be.getItemCapability());
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL.get(), (be, context) -> be.getFluidCapability());
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL.get(), (be, context) -> be.getGasCapability());
    }

    @Nullable
    public AirtightReactorKettleBlockEntity getMasterBlockEntity() {
        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(getBlockPos(), getBlockState());
        if (level == null || !(level.getBlockEntity(masterPos) instanceof AirtightReactorKettleBlockEntity masterBlockEntity)) {
            return null;
        }

        return masterBlockEntity;
    }

    public boolean canStore() {
        return getBlockState().getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION).canStore();
    }

    public IItemHandlerModifiable getItemCapability() {
        AirtightReactorKettleBlockEntity master = getMasterBlockEntity();
        if (master == null || !canStore()) {
            return null;
        }

        return master.getItemCapability();
    }

    public IFluidHandler getFluidCapability() {
        AirtightReactorKettleBlockEntity master = getMasterBlockEntity();
        if (master == null || !canStore()) {
            return null;
        }

        return master.getFluidCapability();
    }

    public IGasHandler getGasCapability() {
        AirtightReactorKettleBlockEntity master = getMasterBlockEntity();
        if (master == null || !canStore()) {
            return null;
        }

        return master.getGasCapability();
    }

    public FilteringBehaviour getFilteringBehaviour() {
        return filteringBehaviour;
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        AirtightReactorKettleStructuralPosition structuralPosition = getBlockState().getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION);
        if (!structuralPosition.isFilter()) {
            return;
        }

        filteringBehaviour = new FilteringBehaviour(this, new AirtightReactorKettleValueBox()).withCallback(stack -> AirtightReactorKettleUtils.refreshOtherFilters(this, stack)).onlyActiveWhen(() -> AirtightReactorKettleUtils.canModifyFilter(this)).forRecipes();
        behaviours.add(filteringBehaviour);
    }

    @Override
    public int getMaxValue() {
        AirtightReactorKettleBlockEntity master = getMasterBlockEntity();
        if (master == null || !canStore()) {
            return 0;
        }

        long maxValue = 0;
        for (int i = 0; i < master.getItemCapability().getSlots(); i++) {
            maxValue += master.getItemCapability().getSlotLimit(i);
        }
        for (int i = 0; i < master.getFluidCapability().getTanks(); i++) {
            maxValue += master.getFluidCapability().getTankCapacity(i);
        }
        for (int i = 0; i < master.getGasCapability().getTanks(); i++) {
            maxValue += master.getGasCapability().getTankCapacity(i);
        }

        return Math.clamp(maxValue, 0, Integer.MAX_VALUE);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        AirtightReactorKettleBlockEntity master = getMasterBlockEntity();
        if (master == null || !canStore()) {
            return 0;
        }

        long currentValue = 0;
        for (int i = 0; i < master.getItemCapability().getSlots(); i++) {
            currentValue += master.getItemCapability().getStackInSlot(i).getCount();
        }
        for (int i = 0; i < master.getFluidCapability().getTanks(); i++) {
            currentValue += master.getFluidCapability().getFluidInTank(i).getAmount();
        }
        for (int i = 0; i < master.getGasCapability().getTanks(); i++) {
            currentValue += master.getGasCapability().getGasInTank(i).getAmount();
        }

        return Math.clamp(currentValue, 0, Integer.MAX_VALUE);
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.items")).component();
    }

    private static class AirtightReactorKettleValueBox extends Sided {
        @Override
        protected @NotNull Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 16.05);
        }

        @Override
        protected boolean isSideActive(@NotNull BlockState state, @NotNull Direction direction) {
            AirtightReactorKettleStructuralPosition structuralPosition = state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION);
            if (!structuralPosition.isFilter()) {
                return false;
            }

            Direction filterDirection = structuralPosition.getDirection();
            return filterDirection.getAxis() != Axis.Y && direction == filterDirection;
        }
    }
}
