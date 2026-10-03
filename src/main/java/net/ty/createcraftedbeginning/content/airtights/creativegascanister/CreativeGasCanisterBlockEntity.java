package net.ty.createcraftedbeginning.content.airtights.creativegascanister;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeGasContainer;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import net.ty.createcraftedbeginning.gas.storage.SmartGasTank;
import net.ty.createcraftedbeginning.gas.visual.GasUnitsTooltips;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeGasCanisterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, CreativeGasContainer {
    private static final String COMPOUND_KEY_CANISTER = "Canister";

    private ItemStack canister = ItemStack.EMPTY;
    private SmartGasTankBehaviour tankBehaviour;

    public CreativeGasCanisterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBBlockEntities.CREATIVE_GAS_CANISTER.get(), (canister, ignoredDirection) -> canister.tankBehaviour.getCapability());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, CreativeGasCanisterContainerContents.VOLUME_LITERS, CreativeGasCanisterContainerContents.PRESSURE_PA).forbidInsertion().forbidExtraction();
        behaviours.add(tankBehaviour);
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

        ItemStack loadedCanister = ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_CANISTER));
        if (!(loadedCanister.getCapability(CanisterCapabilities.ITEM) instanceof CreativeGasCanisterContainerContents canisterContents)) {
            return;
        }

        if (!applyCanisterState(canisterContents)) {
            return;
        }

        canister = loadedCanister;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        SmartGasTank gasTank = tankBehaviour.getPrimaryHandler();
        GasUnitsTooltips.addContainer(tooltip, gasTank, true);
        return true;
    }

    @Override
    public boolean isCreative(Level level, BlockState state, BlockPos pos) {
        return true;
    }

    void setCanisterContent(ItemStack placedCanister) {
        ItemStack installedCanister = placedCanister.copyWithCount(1);
        if (!(installedCanister.getCapability(CanisterCapabilities.ITEM) instanceof CreativeGasCanisterContainerContents canisterContents)) {
            return;
        }

        if (!applyCanisterState(canisterContents)) {
            return;
        }

        canister = installedCanister;
        notifyUpdate();
    }

    ItemStack getCanister() {
        return canister;
    }

    private boolean applyCanisterState(CreativeGasCanisterContainerContents canisterContents) {
        GasTankLimits limits = new GasTankLimits(canisterContents.getTankVolume(0), canisterContents.getTankMaxPressurePa(0));
        GasTankState state = new GasTankState(limits, canisterContents.getGasInTank(0));
        SmartGasTank gasTank = tankBehaviour.getPrimaryHandler();
        if (!gasTank.canContain(state.limits(), state.contents())) {
            return false;
        }

        gasTank.tryApplyState(state).requireAccepted();
        return true;
    }
}
