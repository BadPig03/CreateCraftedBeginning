package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
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
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleStructuralBlockEntity extends SmartBlockEntity implements ThresholdSwitchObservable, IGasInventoryIdentifierProvider {
    private FilteringBehaviour filteringBehaviour;
    private boolean syncingFilter;

    public AirtightReactorKettleStructuralBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL.get(), (blockEntity, direction) -> blockEntity.getItemCapability());
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL.get(), (blockEntity, direction) -> blockEntity.getFluidCapability());
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL.get(), (blockEntity, direction) -> blockEntity.getGasCapability());
    }

    public static boolean canStore(BlockState state) {
        return state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION).canStore();
    }

    @Nullable
    public AirtightReactorKettleBlockEntity getMasterBlockEntity() {
        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(getBlockPos(), getBlockState());
        if (level == null || !(level.getBlockEntity(masterPos) instanceof AirtightReactorKettleBlockEntity masterBlockEntity)) {
            return null;
        }
        return masterBlockEntity;
    }

    public @Nullable IItemHandler getItemCapability() {
        AirtightReactorKettleBlockEntity master = getMasterBlockEntity();
        if (master == null || !canStore(getBlockState())) {
            return null;
        }
        return master.getItemPortCapability();
    }

    public @Nullable IFluidHandler getFluidCapability() {
        AirtightReactorKettleBlockEntity master = getMasterBlockEntity();
        if (master == null || !canStore(getBlockState())) {
            return null;
        }
        return master.getFluidPortCapability();
    }

    public @Nullable IGasHandler getGasCapability() {
        AirtightReactorKettleBlockEntity master = getMasterBlockEntity();
        if (master == null || !canStore(getBlockState())) {
            return null;
        }
        return master.getGasPortCapability();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        AirtightReactorKettleStructuralPosition position = getBlockState().getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION);
        if (!position.isFilter()) {
            return;
        }

        filteringBehaviour = new FilteringBehaviour(this, new AirtightReactorKettleValueBox()).withCallback(this::onFilterChanged).forRecipes();
        behaviours.add(filteringBehaviour);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (filteringBehaviour == null) {
            return;
        }

        AirtightReactorKettleBlockEntity master = getMasterBlockEntity();
        if (master == null) {
            return;
        }

        if (!master.hasAuthoritativeRecipeFilter() && !filteringBehaviour.getFilter().isEmpty()) {
            master.setRecipeFilter(filteringBehaviour.getFilter());
            return;
        }

        syncFilterFromMaster(master.getRecipeFilter());
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

    private void onFilterChanged(ItemStack stack) {
        if (syncingFilter) {
            return;
        }

        AirtightReactorKettleUtils.updateRecipeFilter(this, stack);
    }

    @Override
    public int getMaxValue() {
        AirtightReactorKettleBlockEntity master = getMasterBlockEntity();
        if (master == null || !canStore(getBlockState())) {
            return 0;
        }

        IItemHandler items = getItemCapability();
        IFluidHandler fluids = getFluidCapability();
        IGasHandler gases = getGasCapability();
        if (items == null || fluids == null || gases == null) {
            return 0;
        }

        long capacity = 0;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            capacity += items.getSlotLimit(slot);
        }
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            capacity += fluids.getTankCapacity(tank);
        }
        for (int tank = 0; tank < gases.getTanks(); tank++) {
            capacity += gases.getTankCapacity(tank);
        }
        return Math.clamp(capacity, 0, Integer.MAX_VALUE);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        AirtightReactorKettleBlockEntity master = getMasterBlockEntity();
        if (master == null || !canStore(getBlockState())) {
            return 0;
        }

        IItemHandler items = getItemCapability();
        IFluidHandler fluids = getFluidCapability();
        IGasHandler gases = getGasCapability();
        if (items == null || fluids == null || gases == null) {
            return 0;
        }

        long stored = 0;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            stored += items.getStackInSlot(slot).getCount();
        }
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            stored += fluids.getFluidInTank(tank).getAmount();
        }
        for (int tank = 0; tank < gases.getTanks(); tank++) {
            stored += gases.getGasInTank(tank).getAmount();
        }
        return Math.clamp(stored, 0, Integer.MAX_VALUE);
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.items")).component();
    }

    @Override
    public InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        BlockPos masterPos = AirtightReactorKettleUtils.getMaster(getBlockPos(), getBlockState());
        return new Single(masterPos);
    }

    private static class AirtightReactorKettleValueBox extends Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            AirtightReactorKettleStructuralPosition position = state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION);
            if (!position.isFilter()) {
                return false;
            }

            Direction filterDirection = position.getDirection();
            return filterDirection.getAxis() != Axis.Y && direction == filterDirection;
        }
    }
}
