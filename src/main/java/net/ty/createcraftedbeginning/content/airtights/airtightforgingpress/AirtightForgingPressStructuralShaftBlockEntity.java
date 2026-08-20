package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressStructuralShaftBlockEntity extends KineticBlockEntity implements ThresholdSwitchObservable, IGasInventoryIdentifierProvider {
    public AirtightForgingPressStructuralShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get(), (shaft, ignoredContext) -> shaft.getItemCapability());
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get(), (shaft, ignoredDirection) -> shaft.getFluidCapability());
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get(), (shaft, ignoredDirection) -> shaft.getGasCapability());
    }

    public static boolean isUpperStore(BlockState blockState) {
        AirtightForgingPressStructuralPosition structuralPosition = blockState.getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION);
        return structuralPosition.isUpperStore() && structuralPosition == AirtightForgingPressStructuralPosition.TOP_CENTER;
    }

    @Override
    public int getMaxValue() {
        AirtightForgingPressBlockEntity press = getMasterBlockEntity();
        if (press == null || !isUpperStore(getBlockState())) {
            return 0;
        }

        IItemHandlerModifiable itemHandler = getItemCapability();
        IFluidHandler fluidHandler = getFluidCapability();
        IGasHandler gasHandler = getGasCapability();
        if (itemHandler == null || fluidHandler == null || gasHandler == null) {
            return 0;
        }

        long totalCapacity = 0;
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            totalCapacity += itemHandler.getSlotLimit(slot);
        }
        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            totalCapacity += fluidHandler.getTankCapacity(tank);
        }
        for (int tank = 0; tank < gasHandler.getTanks(); tank++) {
            totalCapacity += gasHandler.getTankCapacity(tank);
        }
        return Math.clamp(totalCapacity, 0, Integer.MAX_VALUE);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        AirtightForgingPressBlockEntity press = getMasterBlockEntity();
        if (press == null || !isUpperStore(getBlockState())) {
            return 0;
        }

        IItemHandlerModifiable itemHandler = getItemCapability();
        IFluidHandler fluidHandler = getFluidCapability();
        IGasHandler gasHandler = getGasCapability();
        if (itemHandler == null || fluidHandler == null || gasHandler == null) {
            return 0;
        }

        long storedAmount = 0;
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            storedAmount += itemHandler.getStackInSlot(slot).getCount();
        }
        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            storedAmount += fluidHandler.getFluidInTank(tank).getAmount();
        }
        for (int tank = 0; tank < gasHandler.getTanks(); tank++) {
            storedAmount += gasHandler.getGasInTank(tank).getAmount();
        }
        return Math.clamp(storedAmount, 0, Integer.MAX_VALUE);
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.items")).component();
    }

    @Override
    public @Nullable InventoryIdentifier getGasInventoryIdentifier(Direction ignoredDirection) {
        BlockPos masterPos = AirtightForgingPressUtils.getMaster(getBlockPos(), getBlockState());
        return new Single(masterPos);
    }

    @Nullable AirtightForgingPressBlockEntity getMasterBlockEntity() {
        BlockPos masterPos = AirtightForgingPressUtils.getMaster(getBlockPos(), getBlockState());
        if (level == null || !(level.getBlockEntity(masterPos) instanceof AirtightForgingPressBlockEntity press)) {
            return null;
        }
        return press;
    }

    boolean getOverstressed() {
        return overStressed;
    }

    private @Nullable IItemHandlerModifiable getItemCapability() {
        AirtightForgingPressBlockEntity press = getMasterBlockEntity();
        if (press == null || !isUpperStore(getBlockState())) {
            return null;
        }
        return press.getAdditionInventory();
    }

    private @Nullable IFluidHandler getFluidCapability() {
        AirtightForgingPressBlockEntity press = getMasterBlockEntity();
        if (press == null || !isUpperStore(getBlockState())) {
            return null;
        }
        return press.getFluidCapability();
    }

    private @Nullable IGasHandler getGasCapability() {
        AirtightForgingPressBlockEntity press = getMasterBlockEntity();
        if (press == null || !isUpperStore(getBlockState())) {
            return null;
        }
        return press.getGasCapability();
    }
}
