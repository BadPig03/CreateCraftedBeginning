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
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressStructuralShaftBlockEntity extends KineticBlockEntity implements ThresholdSwitchObservable, IGasInventoryIdentifierProvider {
    public AirtightForgingPressStructuralShaftBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get(), (be, context) -> be.getItemCapability());
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get(), (be, direction) -> be.getFluidCapability());
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get(), (be, direction) -> be.getGasCapability());
    }

    public static boolean isUpperStore(BlockState blockState) {
        AirtightForgingPressStructuralPosition structuralPosition = blockState.getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION);
        return structuralPosition.isUpperStore() && structuralPosition == AirtightForgingPressStructuralPosition.TOP_CENTER;
    }

    @Nullable
    public AirtightForgingPressBlockEntity getMasterBlockEntity() {
        BlockPos masterPos = AirtightForgingPressUtils.getMaster(getBlockPos(), getBlockState());
        if (level == null || !(level.getBlockEntity(masterPos) instanceof AirtightForgingPressBlockEntity masterBlockEntity)) {
            return null;
        }

        return masterBlockEntity;
    }

    public @Nullable IItemHandlerModifiable getItemCapability() {
        AirtightForgingPressBlockEntity master = getMasterBlockEntity();
        if (master == null || !isUpperStore(getBlockState())) {
            return null;
        }

        return master.getProcessingInventories().getSecond();
    }

    public @Nullable IFluidHandler getFluidCapability() {
        AirtightForgingPressBlockEntity master = getMasterBlockEntity();
        if (master == null || !isUpperStore(getBlockState())) {
            return null;
        }

        return master.getFluidCapability();
    }

    public @Nullable IGasHandler getGasCapability() {
        AirtightForgingPressBlockEntity master = getMasterBlockEntity();
        if (master == null || !isUpperStore(getBlockState())) {
            return null;
        }

        return master.getGasCapability();
    }

    public boolean getOverstressed() {
        return overStressed;
    }

    @Override
    public int getMaxValue() {
        AirtightForgingPressBlockEntity master = getMasterBlockEntity();
        if (master == null || !isUpperStore(getBlockState())) {
            return 0;
        }

        IItemHandlerModifiable item = getItemCapability();
        IFluidHandler fluid = getFluidCapability();
        IGasHandler gas = getGasCapability();
        if (item == null || fluid == null || gas == null) {
            return 0;
        }

        long maxValue = 0;
        for (int i = 0; i < item.getSlots(); i++) {
            maxValue += item.getSlotLimit(i);
        }
        for (int i = 0; i < fluid.getTanks(); i++) {
            maxValue += fluid.getTankCapacity(i);
        }
        for (int i = 0; i < gas.getTanks(); i++) {
            maxValue += gas.getTankCapacity(i);
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
        if (master == null || !isUpperStore(getBlockState())) {
            return 0;
        }

        IItemHandlerModifiable item = getItemCapability();
        IFluidHandler fluid = getFluidCapability();
        IGasHandler gas = getGasCapability();
        if (item == null || fluid == null || gas == null) {
            return 0;
        }

        long currentValue = 0;
        for (int i = 0; i < item.getSlots(); i++) {
            currentValue += item.getStackInSlot(i).getCount();
        }
        for (int i = 0; i < fluid.getTanks(); i++) {
            currentValue += fluid.getFluidInTank(i).getAmount();
        }
        for (int i = 0; i < gas.getTanks(); i++) {
            currentValue += gas.getGasInTank(i).getAmount();
        }
        return Math.clamp(currentValue, 0, Integer.MAX_VALUE);
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.items")).component();
    }

    @Override
    public @Nullable InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        BlockPos masterPos = AirtightForgingPressUtils.getMaster(getBlockPos(), getBlockState());
        return new Single(masterPos);
    }
}
