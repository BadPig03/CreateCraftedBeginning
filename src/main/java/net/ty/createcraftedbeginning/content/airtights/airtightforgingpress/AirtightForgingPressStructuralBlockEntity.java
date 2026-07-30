package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressStructuralBlockEntity extends SmartBlockEntity implements ThresholdSwitchObservable {
    private FilteringBehaviour filteringBehaviour;
    private boolean syncingFilter;

    public AirtightForgingPressStructuralBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL.get(), (structural, context) -> structural.getItemCapability());
    }

    public static boolean isLowerStore(BlockState blockState) {
        return blockState.getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION).isLowerStore();
    }

    @Nullable
    public AirtightForgingPressBlockEntity getMasterBlockEntity() {
        BlockPos masterPos = AirtightForgingPressUtils.getMaster(getBlockPos(), getBlockState());
        if (level == null || !(level.getBlockEntity(masterPos) instanceof AirtightForgingPressBlockEntity master)) {
            return null;
        }

        return master;
    }

    public @Nullable IItemHandler getItemCapability() {
        AirtightForgingPressBlockEntity master = getMasterBlockEntity();
        if (master == null || !isLowerStore(getBlockState())) {
            return null;
        }

        return master.getInputOutputCapability();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        AirtightForgingPressStructuralPosition position = getBlockState().getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION);
        if (!position.isFilter()) {
            return;
        }

        filteringBehaviour = new FilteringBehaviour(this, new AirtightForgingPressValueBox()).withCallback(this::onFilterChanged).forRecipes();
        behaviours.add(filteringBehaviour);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (filteringBehaviour == null) {
            return;
        }

        AirtightForgingPressBlockEntity master = getMasterBlockEntity();
        if (master == null) {
            return;
        }

        syncFilterFromMaster(master.getRecipeFilter());
    }

    private void onFilterChanged(ItemStack stack) {
        if (syncingFilter) {
            return;
        }

        AirtightForgingPressUtils.updateRecipeFilter(this, stack);
    }

    void syncFilterFromMaster(ItemStack stack) {
        if (filteringBehaviour == null || ItemStack.matches(filteringBehaviour.getFilter(), stack)) {
            return;
        }

        syncingFilter = true;
        try {
            filteringBehaviour.setFilter(stack);
        } finally {
            syncingFilter = false;
        }
    }

    @Override
    public int getMaxValue() {
        AirtightForgingPressBlockEntity master = getMasterBlockEntity();
        if (master == null || !isLowerStore(getBlockState())) {
            return 0;
        }

        IItemHandler items = getItemCapability();
        if (items == null) {
            return 0;
        }

        long maxValue = 0;
        for (int i = 0; i < items.getSlots(); i++) {
            maxValue += items.getSlotLimit(i);
        }
        return Math.clamp(maxValue, 0, Integer.MAX_VALUE);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        AirtightForgingPressBlockEntity master = getMasterBlockEntity();
        if (master == null || !isLowerStore(getBlockState())) {
            return 0;
        }

        IItemHandler items = getItemCapability();
        if (items == null) {
            return 0;
        }

        long currentValue = 0;
        for (int i = 0; i < items.getSlots(); i++) {
            currentValue += items.getStackInSlot(i).getCount();
        }
        return Math.clamp(currentValue, 0, Integer.MAX_VALUE);
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.items")).component();
    }

    private static class AirtightForgingPressValueBox extends Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 7, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            AirtightForgingPressStructuralPosition position = state.getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION);
            if (!position.isFilter()) {
                return false;
            }

            Direction filterDirection = position.getDirection();
            return filterDirection.getAxis() != Axis.Y && direction == filterDirection;
        }
    }
}
