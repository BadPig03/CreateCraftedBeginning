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
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
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
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL.get(), (structural, ignoredContext) -> structural.getItemCapability());
    }

    public static boolean isLowerStore(BlockState blockState) {
        return blockState.getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION).isLowerStore();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        AirtightForgingPressStructuralPosition structuralPosition = getBlockState().getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION);
        if (!structuralPosition.isFilter()) {
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

        AirtightForgingPressBlockEntity press = getMasterBlockEntity();
        if (press == null) {
            return;
        }

        syncFilterFromMaster(press.getRecipeFilter());
    }

    @Override
    public int getMaxValue() {
        AirtightForgingPressBlockEntity press = getMasterBlockEntity();
        if (press == null || !isLowerStore(getBlockState())) {
            return 0;
        }

        IItemHandler itemHandler = getItemCapability();
        if (itemHandler == null) {
            return 0;
        }

        long maxItemCount = 0;
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            maxItemCount += itemHandler.getSlotLimit(slot);
        }
        return Math.clamp(maxItemCount, 0, Integer.MAX_VALUE);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        AirtightForgingPressBlockEntity press = getMasterBlockEntity();
        if (press == null || !isLowerStore(getBlockState())) {
            return 0;
        }

        IItemHandler itemHandler = getItemCapability();
        if (itemHandler == null) {
            return 0;
        }

        long storedItemCount = 0;
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            storedItemCount += itemHandler.getStackInSlot(slot).getCount();
        }
        return Math.clamp(storedItemCount, 0, Integer.MAX_VALUE);
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.items")).component();
    }

    void syncFilterFromMaster(ItemStack filterStack) {
        if (filteringBehaviour == null || ItemStack.matches(filteringBehaviour.getFilter(), filterStack)) {
            return;
        }

        syncingFilter = true;
        try {
            filteringBehaviour.setFilter(filterStack);
        } finally {
            syncingFilter = false;
        }
    }

    @Nullable AirtightForgingPressBlockEntity getMasterBlockEntity() {
        BlockPos masterPos = AirtightForgingPressUtils.getMaster(getBlockPos(), getBlockState());
        if (level == null || !(level.getBlockEntity(masterPos) instanceof AirtightForgingPressBlockEntity press)) {
            return null;
        }
        return press;
    }

    private @Nullable IItemHandler getItemCapability() {
        AirtightForgingPressBlockEntity press = getMasterBlockEntity();
        if (press == null || !isLowerStore(getBlockState())) {
            return null;
        }
        return press.getInputOutputCapability();
    }

    private void onFilterChanged(ItemStack filterStack) {
        if (syncingFilter) {
            return;
        }

        AirtightForgingPressUtils.updateRecipeFilter(this, filterStack);
    }

    private static class AirtightForgingPressValueBox extends Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 7, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            AirtightForgingPressStructuralPosition structuralPosition = state.getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION);
            if (!structuralPosition.isFilter()) {
                return false;
            }

            Direction filterDirection = structuralPosition.getDirection();
            return filterDirection.getAxis() != Axis.Y && direction == filterDirection;
        }
    }
}
