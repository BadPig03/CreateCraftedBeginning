package net.ty.createcraftedbeginning.content.airtights.creativegascanister;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.ICreativeGasContainer;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreativeGasCanisterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ICreativeGasContainer {
    private static final String COMPOUND_KEY_CANISTER = "Canister";

    private ItemStack canister = ItemStack.EMPTY;
    private SmartGasTankBehaviour tankBehaviour;

    public CreativeGasCanisterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.CREATIVE_GAS_CANISTER.get(), (be, context) -> be.tankBehaviour.getCapability());
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, CreativeGasCanisterContainerContents.getDefaultCapacity()).forbidInsertion().forbidExtraction();
        behaviours.add(tankBehaviour);
    }

    @Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_CANISTER, canister.saveOptional(provider));
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!compoundTag.contains(COMPOUND_KEY_CANISTER)) {
            return;
        }

        canister = ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_CANISTER));
        updateCapacity();
    }

    public void setCanisterContent(@NotNull ItemStack itemStack) {
        canister = itemStack.copy();
        if (!(canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents canisterContents)) {
            return;
        }

        tankBehaviour.getPrimaryHandler().setCapacity(canisterContents.getTankCapacity(0));
        tankBehaviour.getInternalGasHandler().forceFill(canisterContents.getGasInTank(0), GasAction.EXECUTE);
        notifyUpdate();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        SmartGasTank gasTank = tankBehaviour.getPrimaryHandler();
        if (gasTank == null) {
            return false;
        }

        CCBLang.translate("gui.goggles.gas_container").forGoggles(tooltip);
        GasStack stack = gasTank.getGasStack();
        if (stack.isEmpty()) {
            CCBLang.translate("gui.tooltips.creative_gas_canister.empty").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.gasName(stack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.translate("gui.goggles.gas_container.infinity").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        return true;
    }

    public ItemStack getCanister() {
        return canister;
    }

    private void updateCapacity() {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents canisterContents)) {
            return;
        }

        long newCapacity = canisterContents.getTankCapacity(0);
        if (tankBehaviour.getPrimaryHandler().getCapacity() == newCapacity) {
            return;
        }

        tankBehaviour.getPrimaryHandler().setCapacity(newCapacity);
    }

    @Override
    public boolean isCreative(Level level, BlockState blockState, BlockPos blockPos) {
        return true;
    }
}
