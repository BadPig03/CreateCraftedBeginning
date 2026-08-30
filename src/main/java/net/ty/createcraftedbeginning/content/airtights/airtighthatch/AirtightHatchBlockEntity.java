package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock.CanisterType;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.ICreativeGasContainer;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightHatchBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ICreativeGasContainer, ThresholdSwitchObservable {
    private static final int LAZY_TICK_RATE = 20;
    private final AirtightHatchCanisterManager canisterManager;
    private final AirtightHatchController controller;
    private final AirtightHatchSerialization serialization;
    private final AirtightHatchDisplay display;
    private SmartGasTankBehaviour tankBehaviour;
    private ScrollOptionBehaviour<AirtightHatchTransferMode> hatchTransferMode;

    public AirtightHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        canisterManager = new AirtightHatchCanisterManager(this);
        controller = new AirtightHatchController(this, canisterManager);
        serialization = new AirtightHatchSerialization(this, canisterManager);
        display = new AirtightHatchDisplay(this);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_HATCH.get(), (hatch, context) -> hatch.tankBehaviour.getCapability());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, 0).forbidExtraction().forbidInsertion();
        behaviours.add(tankBehaviour);
        hatchTransferMode = new ScrollOptionBehaviour<>(AirtightHatchTransferMode.class, CCBLang.translateDirect("gui.airtight_hatch.transfer_mode"), this, new AirtightHatchValueBox());
        behaviours.add(hatchTransferMode);
    }

    @Override
    public void tick() {
        super.tick();
        updateTransferModeRange();
        controller.tick();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        controller.lazyTick();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        serialization.write(compoundTag, provider, clientPacket);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        serialization.read(compoundTag, provider, clientPacket);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        canisterManager.reconcileCanisterState();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToGoggleTooltip(tooltip);
    }

    @Override
    public int getMaxValue() {
        return display.getMaxValue();
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return display.getCurrentValue();
    }

    @Override
    public MutableComponent format(int value) {
        return display.format(value);
    }

    @Override
    public boolean isCreative(Level level, BlockState blockState, BlockPos blockPos) {
        if (!level.isClientSide) {
            return canisterManager.isCreative();
        }
        return blockState.getValue(AirtightHatchBlock.CANISTER_TYPE) == CanisterType.CREATIVE;
    }

    @Nullable IGasHandler getTargetGasHandler(Level level) {
        BlockState hatchState = getBlockState();
        BlockPos hatchPos = getBlockPos();
        if (!AirtightHatchBlock.hasValidAttachment(level, hatchPos, hatchState)) {
            return null;
        }

        Direction facing = hatchState.getValue(AirtightHatchBlock.FACING);
        BlockPos targetPos = hatchPos.relative(facing);
        return level.getCapability(GasHandler.BLOCK, targetPos, facing.getOpposite());
    }

    ItemStack createCanisterItemStack() {
        return canisterManager.createCanisterItemStack();
    }

    boolean giveCanisterToPlayer(Player player) {
        return canisterManager.giveCanisterToPlayer(player);
    }

    boolean isEmpty() {
        return getCanisterType() == CanisterType.EMPTY;
    }

    boolean isCreative() {
        return getCanisterType() == CanisterType.CREATIVE;
    }

    GasStack getHatchGasContent() {
        return tankBehaviour.getPrimaryHandler().getGasStack().copy();
    }

    long getHatchCapacity() {
        return tankBehaviour.getPrimaryHandler().getCapacity();
    }

    boolean installCanister(ItemStack sourceStack) {
        return canisterManager.installCanister(sourceStack);
    }

    SmartGasTankBehaviour getGasTankBehaviour() {
        return tankBehaviour;
    }

    int getTransferModeValue() {
        return hatchTransferMode.getValue();
    }

    void resetTransferMode() {
        hatchTransferMode.setValue(0);
    }

    void resetTransferQuota() {
        controller.resetTransferQuota();
    }

    private CanisterType getCanisterType() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            return canisterManager.getStoredCanisterType();
        }
        return getBlockState().getValue(AirtightHatchBlock.CANISTER_TYPE);
    }
    private void updateTransferModeRange() {
        int maxModeValue = isCreative() ? AirtightHatchTransferMode.OUTPUT_ONLY.ordinal() : AirtightHatchTransferMode.STAY_HALF.ordinal();
        hatchTransferMode.between(AirtightHatchTransferMode.NO_TRANSFER.ordinal(), maxModeValue);
    }
}
