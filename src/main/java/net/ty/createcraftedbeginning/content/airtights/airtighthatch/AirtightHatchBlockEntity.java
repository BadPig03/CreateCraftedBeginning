package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.cansiters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTankBehaviour.InternalGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AirtightHatchBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ThresholdSwitchObservable {
    private static final int SYNC_RATE = 4;
    private static final int LAZY_TICK_RATE = 20;

    private static final String COMPOUND_KEY_CANISTER = "Canister";

    private ItemStack canister = ItemStack.EMPTY;
    private SmartGasTankBehaviour tankBehaviour;
    private ScrollOptionBehaviour<HatchTransferMode> hatchTransferMode;

    private int syncCooldown;
    private boolean queuedSync;

    public AirtightHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_HATCH.get(), (be, context) -> be.getBlockState().getValue(AirtightHatchBlock.OCCUPIED) ? be.tankBehaviour.getCapability() : SmartGasTankBehaviour.single(be, 0).forbidInsertion().forbidExtraction().getCapability());
    }

    private static int getMaxTransferRate() {
        return CCBConfig.server().airtights.maxTransferRate.get() / 5;
    }

    private static boolean inputOnly(@NotNull InternalGasHandler internalHandler, @NotNull IGasHandler targetHandler, long maxTransferAmount) {
        GasStack drainedGasStack = targetHandler.drain(maxTransferAmount, GasAction.SIMULATE);
        if (drainedGasStack.isEmpty()) {
            return false;
        }

        long filledAmount = internalHandler.forceFill(drainedGasStack.copy(), GasAction.SIMULATE);
        long realDrainAmount = Math.min(drainedGasStack.getAmount(), filledAmount);
        if (realDrainAmount == 0) {
            return false;
        }

        GasStack realDrainedGasStack = targetHandler.drain(realDrainAmount, GasAction.EXECUTE);
        internalHandler.forceFill(realDrainedGasStack.copy(), GasAction.EXECUTE);
        return true;
    }

    private static boolean outputOnly(@NotNull InternalGasHandler internalHandler, @NotNull IGasHandler targetHandler, long maxTransferAmount) {
        GasStack drainedGasStack = internalHandler.forceDrain(maxTransferAmount, GasAction.SIMULATE);
        if (drainedGasStack.isEmpty()) {
            return false;
        }

        long filledAmount = targetHandler.fill(drainedGasStack.copy(), GasAction.SIMULATE);
        long realDrainAmount = Math.min(drainedGasStack.getAmount(), filledAmount);
        if (realDrainAmount == 0) {
            return false;
        }

        GasStack realDrainedGasStack = internalHandler.forceDrain(realDrainAmount, GasAction.EXECUTE);
        targetHandler.fill(realDrainedGasStack.copy(), GasAction.EXECUTE);
        return true;
    }

    private static boolean stayHalf(@NotNull InternalGasHandler internalHandler, @NotNull IGasHandler targetHandler, long maxTransferAmount) {
        long currentAmount = internalHandler.getGasInTank(0).getAmount();
        long halfCapacity = internalHandler.getTankCapacity(0) / 2;
        long delta = halfCapacity - currentAmount;
        long transferAmount = Math.min(Math.abs(delta), maxTransferAmount);
        if (delta == 0) {
            return false;
        }
        else if (delta > 0) {
            return inputOnly(internalHandler, targetHandler, transferAmount);
        }
        else {
            return outputOnly(internalHandler, targetHandler, transferAmount);
        }
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, 0).forbidInsertion().forbidExtraction();
        hatchTransferMode = new ScrollOptionBehaviour<>(HatchTransferMode.class, CCBLang.translateDirect("gui.airtight_hatch.transfer_mode"), this, new AirtightHatchValueBox());
        behaviours.add(tankBehaviour);
        behaviours.add(hatchTransferMode);
    }

    @Override
    public void tick() {
        super.tick();
        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync) {
                sendData();
            }
            return;
        }
        if (!getBlockState().getValue(AirtightHatchBlock.OCCUPIED)) {
            return;
        }

        tryTransferGas();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof AirtightHatchBlock hatch) || hatch.canSurvive(state, level, getBlockPos())) {
            return;
        }

        level.destroyBlock(worldPosition, true);
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

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!getBlockState().getValue(AirtightHatchBlock.OCCUPIED)) {
            return false;
        }

        CCBLang.translate("gui.goggles.gas_container").forGoggles(tooltip);
        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        IGasHandler handler = tankBehaviour.getCapability();
        GasStack stack = handler.getGasInTank(0);
        if (stack.isEmpty()) {
            CCBLang.translate("gui.goggles.gas_container.capacity").add(CCBLang.number(handler.getTankCapacity(0)).add(mb).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.gasName(stack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.number(stack.getAmount()).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(handler.getTankCapacity(0)).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }
        return true;
    }

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }

        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    private void tryTransferGas() {
        if (level == null || level.isClientSide) {
            return;
        }

        IGasHandler targetHandler = getTargetGasHandler(level);
        if (targetHandler == null) {
            return;
        }

        InternalGasHandler internalHandler = tankBehaviour.getInternalGasHandler();
        GasStack internalGasStack = internalHandler.getGasInTank(0);
        GasStack inputGasStack = targetHandler.getGasInTank(0);
        if (!inputGasStack.isEmpty() && !internalGasStack.isEmpty() && !GasStack.isSameGasSameComponents(internalGasStack, inputGasStack)) {
            return;
        }

        int currentMode = hatchTransferMode.getValue();
        if (currentMode == HatchTransferMode.NO_TRANSFER.ordinal()) {
            return;
        }

        int maxTransferRate = getMaxTransferRate();
        boolean result = false;
        if (currentMode == HatchTransferMode.INPUT_ONLY.ordinal()) {
            result = inputOnly(internalHandler, targetHandler, maxTransferRate);
        }
        else if (currentMode == HatchTransferMode.OUTPUT_ONLY.ordinal()) {
            result = outputOnly(internalHandler, targetHandler, maxTransferRate);
        }
        else if (currentMode == HatchTransferMode.STAY_HALF.ordinal()) {
            result = stayHalf(internalHandler, targetHandler, maxTransferRate);
        }
        if (!result) {
            return;
        }

        sendData();
        if (!(level.getBlockEntity(getBlockPos().relative(getBlockState().getValue(AirtightHatchBlock.FACING))) instanceof SmartBlockEntity smart)) {
            return;
        }

        smart.sendData();
    }

    @Nullable
    public IGasHandler getTargetGasHandler(@NotNull Level level) {
        BlockEntity blockEntity = level.getBlockEntity(getBlockPos().relative(getBlockState().getValue(AirtightHatchBlock.FACING)));
        if (blockEntity == null || blockEntity instanceof AirtightHatchBlockEntity) {
            return null;
        }

        Direction direction = getBlockState().getValue(AirtightHatchBlock.FACING);
        BlockPos targetPos = getBlockPos().relative(direction);
        return level.getCapability(GasHandler.BLOCK, targetPos, direction.getOpposite());
    }

    public ItemStack createCanisterItemStack() {
        ItemStack itemStack = canister.copy();
        IGasCanisterContainer capability = itemStack.getCapability(GasHandler.ITEM);
        if (!(capability instanceof GasCanisterContainerContents canisterContents)) {
            return ItemStack.EMPTY;
        }

        canisterContents.setCapacity(0, GasCanisterContainerContents.getEnchantedCapacity(itemStack));
        canisterContents.drain(0, canisterContents.getGasInTank(0), GasAction.EXECUTE);
        canisterContents.fill(0, getCanisterContent(), GasAction.EXECUTE);
        return itemStack;
    }

    public void giveCanisterToPlayer(@NotNull Player player) {
        player.getInventory().placeItemBackInInventory(createCanisterItemStack());
        tankBehaviour.getInternalGasHandler().forceDrain(getCanisterContent(), GasAction.EXECUTE);
        canister = ItemStack.EMPTY;
        tankBehaviour.getPrimaryHandler().setCapacity(0);
    }

    public void setCanisterContent(@NotNull ItemStack itemStack) {
        canister = itemStack.copy();
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return;
        }

        GasStack gasContent = canisterContents.getGasInTank(0).copy();
        canisterContents.drain(0, gasContent, GasAction.EXECUTE);
        updateCapacity();

        tankBehaviour.getInternalGasHandler().forceDrain(getCanisterContent(), GasAction.EXECUTE);
        tankBehaviour.getInternalGasHandler().forceFill(gasContent, GasAction.EXECUTE);
    }

    @Override
    public int getMaxValue() {
        if (!getBlockState().getValue(AirtightHatchBlock.OCCUPIED)) {
            return 0;
        }

        SmartGasTank gasTank = tankBehaviour.getPrimaryHandler();
        return (int) gasTank.getCapacity() / 1000;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        if (!getBlockState().getValue(AirtightHatchBlock.OCCUPIED)) {
            return 0;
        }

        return (int) tankBehaviour.getPrimaryHandler().getGasStack().getAmount() / 1000;
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.buckets")).component();
    }

    private void updateCapacity() {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return;
        }

        long newCapacity = canisterContents.getTankCapacity(0);
        if (tankBehaviour.getPrimaryHandler().getCapacity() == newCapacity) {
            return;
        }

        tankBehaviour.getPrimaryHandler().setCapacity(newCapacity);
    }

    private GasStack getCanisterContent() {
        return tankBehaviour.getPrimaryHandler().getGasStack().copy();
    }

    public enum HatchTransferMode implements INamedIconOptions {
        NO_TRANSFER(CCBIcons.I_NO_TRANSFER),
        INPUT_ONLY(CCBIcons.I_INPUT_ONLY),
        OUTPUT_ONLY(CCBIcons.I_OUTPUT_ONLY),
        STAY_HALF(CCBIcons.I_STAY_HALF);

        private final String translationKey;
        private final CCBIcons icon;

        HatchTransferMode(CCBIcons icon) {
            this.icon = icon;
            translationKey = "createcraftedbeginning.gui.airtight_hatch.transfer_mode." + Lang.asId(name());
        }

        @Override
        public CCBIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
}