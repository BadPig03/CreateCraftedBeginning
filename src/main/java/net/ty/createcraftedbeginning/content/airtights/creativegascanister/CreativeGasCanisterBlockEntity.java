package net.ty.createcraftedbeginning.content.airtights.creativegascanister;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.SmartGasTank;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.ICreativeGasContainer;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeGasCanisterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ICreativeGasContainer {
    private static final String COMPOUND_KEY_CANISTER = "Canister";

    private ItemStack canister = ItemStack.EMPTY;
    private SmartGasTankBehaviour tankBehaviour;

    public CreativeGasCanisterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.CREATIVE_GAS_CANISTER.get(), (be, context) -> be.tankBehaviour.getCapability());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, CreativeGasCanisterContainerContents.getDefaultCapacity()).forbidInsertion().forbidExtraction();
        behaviours.add(tankBehaviour);
    }

    @Override
    protected void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.write(tag, provider, clientPacket);
        tag.put(COMPOUND_KEY_CANISTER, canister.saveOptional(provider));
    }

    @Override
    protected void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.read(tag, provider, clientPacket);
        if (!tag.contains(COMPOUND_KEY_CANISTER)) {
            return;
        }

        canister = ItemStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_CANISTER));
        updateCapacity();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    public void setCanisterContent(ItemStack stack) {
        canister = stack.copy();
        if (!(canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents contents)) {
            return;
        }

        SmartGasTank tank = tankBehaviour.getPrimaryHandler();
        tank.setCapacity(contents.getTankCapacity(0));
        tankBehaviour.getInternalGasHandler().forceFill(contents.getGasInTank(0), GasAction.EXECUTE);
        notifyUpdate();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        SmartGasTank tank = tankBehaviour.getPrimaryHandler();
        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gas = tank.getGasStack();
        if (gas.isEmpty()) {
            CCBLang.translate("gui.creative_gas_canister.empty").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CCBLang.translate("gui.gas_container.infinity").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }

    public ItemStack getCanister() {
        return canister;
    }

    private void updateCapacity() {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents contents)) {
            return;
        }

        SmartGasTank tank = tankBehaviour.getPrimaryHandler();
        long capacity = contents.getTankCapacity(0);
        if (tank.getCapacity() == capacity) {
            return;
        }

        tank.setCapacity(capacity);
    }

    @Override
    public boolean isCreative(Level level, BlockState state, BlockPos pos) {
        return true;
    }
}
