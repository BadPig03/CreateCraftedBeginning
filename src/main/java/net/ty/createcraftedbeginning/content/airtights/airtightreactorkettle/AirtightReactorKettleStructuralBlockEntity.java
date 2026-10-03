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
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.logistics.GasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleStructuralBlockEntity extends SmartBlockEntity implements ThresholdSwitchObservable, GasInventoryIdentifierProvider {
    private FilteringBehaviour filteringBehaviour;
    private boolean syncingFilter;

    public AirtightReactorKettleStructuralBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL.get(), (blockEntity, direction) -> blockEntity.getItemCapability());
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL.get(), (blockEntity, direction) -> blockEntity.getFluidCapability());
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL.get(), (blockEntity, direction) -> blockEntity.getGasCapability());
    }

    public static boolean canStore(BlockState state) {
        return state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION).canStore();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        AirtightReactorKettleStructuralPosition structuralPosition = getBlockState().getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION);
        if (!structuralPosition.isFilter()) {
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

        AirtightReactorKettleBlockEntity kettle = getMasterBlockEntity();
        if (kettle == null) {
            return;
        }

        if (!kettle.hasAuthoritativeRecipeFilter() && !filteringBehaviour.getFilter().isEmpty()) {
            kettle.setRecipeFilter(filteringBehaviour.getFilter());
            return;
        }

        syncFilterFromMaster(kettle.getRecipeFilter());
    }

    @Override
    public int getMaxValue() {
        AirtightReactorKettleBlockEntity kettle = getMasterBlockEntity();
        if (kettle == null || !canStore(getBlockState())) {
            return 0;
        }

        IItemHandler items = getItemCapability();
        IFluidHandler fluids = getFluidCapability();
        GasHandler gases = getGasCapability();
        if (items == null || fluids == null || gases == null) {
            return 0;
        }

        long totalCapacity = 0;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            totalCapacity += items.getSlotLimit(slot);
        }
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            totalCapacity += fluids.getTankCapacity(tank);
        }
        if (gases instanceof GasStorageHandler storageGases) {
            for (int tank = 0; tank < storageGases.getTanks(); tank++) {
                totalCapacity += storageGases.getTankMaxAmount(tank);
            }
        }
        return BoundedMath.clampToNonNegativeInt(totalCapacity);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        AirtightReactorKettleBlockEntity kettle = getMasterBlockEntity();
        if (kettle == null || !canStore(getBlockState())) {
            return 0;
        }

        IItemHandler items = getItemCapability();
        IFluidHandler fluids = getFluidCapability();
        GasHandler gases = getGasCapability();
        if (items == null || fluids == null || gases == null) {
            return 0;
        }

        long storedAmount = 0;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            storedAmount += items.getStackInSlot(slot).getCount();
        }
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            storedAmount += fluids.getFluidInTank(tank).getAmount();
        }
        for (int tank = 0; tank < gases.getTanks(); tank++) {
            storedAmount += gases.getGasInTank(tank).getAmount();
        }
        return BoundedMath.clampToNonNegativeInt(storedAmount);
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(String.valueOf(value) + ' ').add(CCBLang.translate("gui.threshold.items")).component();
    }

    @Override
    public InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        BlockPos masterPos = AirtightReactorKettleStructural.getMaster(getBlockPos(), getBlockState());
        return new Single(masterPos);
    }

    @Nullable AirtightReactorKettleBlockEntity getMasterBlockEntity() {
        BlockPos masterPos = AirtightReactorKettleStructural.getMaster(getBlockPos(), getBlockState());
        if (level == null || !(level.getBlockEntity(masterPos) instanceof AirtightReactorKettleBlockEntity masterBlockEntity)) {
            return null;
        }

        return masterBlockEntity;
    }

    void syncFilterFromMaster(ItemStack filterStack) {
        if (filteringBehaviour == null || ItemStack.matches(filteringBehaviour.getFilter(), filterStack)) {
            return;
        }

        syncingFilter = true;
        try {
            filteringBehaviour.setFilter(filterStack);
        }
        finally {
            syncingFilter = false;
        }
    }

    private @Nullable IItemHandler getItemCapability() {
        AirtightReactorKettleBlockEntity kettle = getMasterBlockEntity();
        if (kettle == null || !canStore(getBlockState())) {
            return null;
        }

        return kettle.getItemPortCapability();
    }

    private @Nullable IFluidHandler getFluidCapability() {
        AirtightReactorKettleBlockEntity kettle = getMasterBlockEntity();
        if (kettle == null || !canStore(getBlockState())) {
            return null;
        }

        return kettle.getFluidPortCapability();
    }

    private @Nullable GasHandler getGasCapability() {
        AirtightReactorKettleBlockEntity kettle = getMasterBlockEntity();
        if (kettle == null || !canStore(getBlockState())) {
            return null;
        }

        return kettle.getGasPortCapability();
    }

    private void onFilterChanged(ItemStack filterStack) {
        if (syncingFilter) {
            return;
        }

        AirtightReactorKettleInteraction.updateRecipeFilter(this, filterStack);
    }

    private static class AirtightReactorKettleValueBox extends Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            AirtightReactorKettleStructuralPosition structuralPosition = state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION);
            if (!structuralPosition.isFilter()) {
                return false;
            }

            Direction filterDirection = structuralPosition.getDirection();
            return filterDirection.getAxis() != Axis.Y && direction == filterDirection;
        }
    }
}
