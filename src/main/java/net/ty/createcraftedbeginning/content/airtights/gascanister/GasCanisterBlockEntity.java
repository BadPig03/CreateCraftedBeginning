package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import net.ty.createcraftedbeginning.gas.storage.SmartGasTank;
import net.ty.createcraftedbeginning.gas.visual.GasUnitsTooltips;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final String COMPOUND_KEY_CANISTER = "Canister";

    private ItemStack canister = ItemStack.EMPTY;
    private SmartGasTankBehaviour tankBehaviour;
    private boolean overflowPending;

    public GasCanisterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBBlockEntities.GAS_CANISTER.get(), (canister, ignoredDirection) -> canister.tankBehaviour.getCapability());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, GasCanisterContainerContents.getDefaultVolume(), GasCanisterContainerContents.getDefaultMaxPressurePa()).forbidInsertion().forbidExtraction();
        behaviours.add(tankBehaviour);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_CANISTER, canister.saveOptional(provider));
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        ItemStack loadedCanister = ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_CANISTER));
        if (!(loadedCanister.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            super.read(compoundTag, provider, clientPacket);
            return;
        }

        applyCanisterState(canisterContents);
        canister = loadedCanister;
        CompoundTag behaviourData = compoundTag.copy();
        tankBehaviour.write(behaviourData, provider, clientPacket);
        super.read(behaviourData, provider, clientPacket);
    }

    @Override
    public void tick() {
        super.tick();
        if (!overflowPending || level == null || level.isClientSide) {
            return;
        }

        overflowPending = false;
        if (!(canister.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents contents)) {
            return;
        }

        long excessAmount = contents.getGasInTank(0).getAmount() - contents.getTankMaxAmount(0);
        if (excessAmount <= 0) {
            applyCanisterState(contents);
            return;
        }

        long sourcePressurePa = contents.getTankPressurePa(0);
        GasStack excessGas = contents.drain(0, excessAmount, GasAction.EXECUTE);
        applyCanisterState(contents);
        notifyUpdate();
        GasReleaseService.release(level, GasReleaseRequest.radial(excessGas, worldPosition, GasReleaseCause.OVERFLOW, sourcePressurePa));
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
        GasUnitsTooltips.addContainer(tooltip, gasTank);
        return true;
    }

    void setCanisterContent(ItemStack placedCanister) {
        ItemStack installedCanister = placedCanister.copyWithCount(1);
        if (!(installedCanister.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return;
        }

        applyCanisterState(canisterContents);
        canister = installedCanister;
        notifyUpdate();
    }

    ItemStack getCanister() {
        return canister;
    }

    void releaseGas() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (!(canister.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents contents) || contents.isEmpty()) {
            return;
        }

        long sourcePressurePa = contents.getTankPressurePa(0);
        GasStack releasedGas = contents.drain(0, contents.getGasInTank(0).getAmount(), GasAction.EXECUTE);
        setCanisterContent(canister);
        GasReleaseService.release(level, GasReleaseRequest.directional(releasedGas, worldPosition, Direction.UP, GasReleaseCause.MANUAL_VENT, sourcePressurePa));
    }

    private void applyCanisterState(GasCanisterContainerContents canisterContents) {
        GasTankLimits limits = new GasTankLimits(canisterContents.getTankVolume(0), canisterContents.getTankMaxPressurePa(0));
        GasStack storedGas = canisterContents.getGasInTank(0);
        long maxAmount = limits.maxAmount();
        overflowPending = storedGas.getAmount() > maxAmount;
        if (overflowPending) {
            storedGas = storedGas.copyWithAmount(maxAmount);
        }

        SmartGasTank gasTank = tankBehaviour.getPrimaryHandler();
        gasTank.tryApplyState(new GasTankState(limits, storedGas)).requireAccepted();
    }
}
