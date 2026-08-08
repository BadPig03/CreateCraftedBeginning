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
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get(), (shaft, context) -> shaft.getItemCapability());
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get(), (shaft, direction) -> shaft.getFluidCapability());
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get(), (shaft, direction) -> shaft.getGasCapability());
    }

    public static boolean isUpperStore(BlockState blockState) {
        AirtightForgingPressStructuralPosition position = blockState.getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION);
        return position.isUpperStore() && position == AirtightForgingPressStructuralPosition.TOP_CENTER;
    }

    @Nullable
    public AirtightForgingPressBlockEntity getMasterBlockEntity() {
        BlockPos masterPos = AirtightForgingPressUtils.getMaster(getBlockPos(), getBlockState());
        if (level == null || !(level.getBlockEntity(masterPos) instanceof AirtightForgingPressBlockEntity master)) {
            return null;
        }
        return master;
    }

    public @Nullable IItemHandlerModifiable getItemCapability() {
        AirtightForgingPressBlockEntity master = getMasterBlockEntity();
        if (master == null || !isUpperStore(getBlockState())) {
            return null;
        }
        return master.getAdditionInventory();
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

        IItemHandlerModifiable items = getItemCapability();
        IFluidHandler fluids = getFluidCapability();
        IGasHandler gases = getGasCapability();
        if (items == null || fluids == null || gases == null) {
            return 0;
        }

        long maxValue = 0;
        for (int i = 0; i < items.getSlots(); i++) {
            maxValue += items.getSlotLimit(i);
        }
        for (int i = 0; i < fluids.getTanks(); i++) {
            maxValue += fluids.getTankCapacity(i);
        }
        for (int i = 0; i < gases.getTanks(); i++) {
            maxValue += gases.getTankCapacity(i);
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

        IItemHandlerModifiable items = getItemCapability();
        IFluidHandler fluids = getFluidCapability();
        IGasHandler gases = getGasCapability();
        if (items == null || fluids == null || gases == null) {
            return 0;
        }

        long currentValue = 0;
        for (int i = 0; i < items.getSlots(); i++) {
            currentValue += items.getStackInSlot(i).getCount();
        }
        for (int i = 0; i < fluids.getTanks(); i++) {
            currentValue += fluids.getFluidInTank(i).getAmount();
        }
        for (int i = 0; i < gases.getTanks(); i++) {
            currentValue += gases.getGasInTank(i).getAmount();
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
