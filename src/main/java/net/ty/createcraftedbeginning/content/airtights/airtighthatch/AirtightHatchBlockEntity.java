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
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock.CanisterType;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.ICreativeGasContainer;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AirtightHatchBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ICreativeGasContainer, ThresholdSwitchObservable {
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
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_HATCH.get(), (be, context) -> be.getBlockState().getValue(AirtightHatchBlock.CANISTER_TYPE) == CanisterType.EMPTY ? SmartGasTankBehaviour.single(be, 0).forbidExtraction().forbidInsertion().getCapability() : be.tankBehaviour.getCapability());
    }

    private static int getMaxTransferRate() {
        return CCBConfig.server().airtights.maxTransferRate.get();
    }

    private static void inputOnly(@NotNull IGasHandler hatchHandler, @NotNull IGasHandler targetHandler, long maxTransferRate, boolean creative) {
        GasStack hatchGasContent = hatchHandler.getGasInTank(0);
        GasStack toDrain = hatchGasContent.isEmpty() ? targetHandler.drain(maxTransferRate, GasAction.SIMULATE) : targetHandler.drain(hatchGasContent.copyWithAmount(maxTransferRate), GasAction.SIMULATE);
        if (toDrain.isEmpty()) {
            return;
        }

        long filled = hatchHandler.fill(toDrain, GasAction.SIMULATE);
        if (!creative && filled == 0) {
            return;
        }

        GasStack drained = hatchGasContent.isEmpty() ? targetHandler.drain(maxTransferRate, GasAction.EXECUTE) : targetHandler.drain(hatchGasContent.copyWithAmount(maxTransferRate), GasAction.EXECUTE);
        if (creative) {
            return;
        }

        hatchHandler.fill(drained, GasAction.EXECUTE);
    }

    private static void outputOnly(@NotNull IGasHandler hatchHandler, @NotNull IGasHandler targetHandler, long maxTransferRate, boolean creative) {
        GasStack hatchGasContent = hatchHandler.getGasInTank(0);
        if (hatchGasContent.isEmpty()) {
            return;
        }

        long filled = targetHandler.fill(hatchHandler.drain(maxTransferRate, GasAction.SIMULATE), GasAction.SIMULATE);
        if (filled == 0) {
            return;
        }

        GasStack drained = hatchHandler.drain(maxTransferRate, creative ? GasAction.SIMULATE : GasAction.EXECUTE);
        targetHandler.fill(drained, GasAction.EXECUTE);
    }

    private static void stayHalf(@NotNull IGasHandler hatchHandler, @NotNull IGasHandler targetHandler, boolean creative) {
        GasStack hatchGasContent = hatchHandler.getGasInTank(0);
        long delta = hatchGasContent.getAmount() - hatchHandler.getTankCapacity(0) / 2;
        long transferAmount = Math.min(getMaxTransferRate(), Math.abs(delta));
        if (delta == 0) {
            return;
        }

        if (delta > 0) {
            outputOnly(hatchHandler, targetHandler, transferAmount, creative);
        }
        else {
            inputOnly(hatchHandler, targetHandler, transferAmount, creative);
        }
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, 0).forbidExtraction().forbidInsertion();
        behaviours.add(tankBehaviour);
        hatchTransferMode = new ScrollOptionBehaviour<>(HatchTransferMode.class, CCBLang.translateDirect("gui.airtight_hatch.transfer_mode"), this, new AirtightHatchValueBox());
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
        if (isEmpty()) {
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
        if (isEmpty()) {
            return false;
        }

        CCBLang.translate("gui.goggles.gas_container").forGoggles(tooltip);
        GasStack hatchGasContent = getHatchGasContent();
        long capacity = getHatchCapacity();
        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        if (hatchGasContent.isEmpty()) {
            CCBLang.translate("gui.goggles.gas_container.capacity").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.gasName(hatchGasContent).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.number(hatchGasContent.getAmount()).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
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

    @Override
    public int getMaxValue() {
        if (isEmpty()) {
            return 0;
        }

        return Math.clamp(getHatchGasContent().getAmount() / 1000, 0, Integer.MAX_VALUE);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        if (isEmpty()) {
            return 0;
        }

        return Math.clamp(getHatchGasContent().getAmount() / 1000, 0, Integer.MAX_VALUE);
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.buckets")).component();
    }

    @Override
    public boolean isCreative(Level level, @NotNull BlockState blockState, BlockPos blockPos) {
        return blockState.getValue(AirtightHatchBlock.CANISTER_TYPE) == CanisterType.CREATIVE;
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
        if (!(itemStack.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return ItemStack.EMPTY;
        }

        canisterContents.setCapacity(0, GasCanisterContainerContents.getEnchantedCapacity(itemStack));
        canisterContents.drain(0, canisterContents.getGasInTank(0), GasAction.EXECUTE);
        canisterContents.fill(0, getHatchGasContent(), GasAction.EXECUTE);
        return itemStack;
    }

    public void giveCanisterToPlayer(@NotNull Player player) {
        player.getInventory().placeItemBackInInventory(createCanisterItemStack());
        tankBehaviour.getInternalGasHandler().forceDrain(getHatchGasContent(), GasAction.EXECUTE);
        canister = ItemStack.EMPTY;
        tankBehaviour.getPrimaryHandler().setCapacity(0);
    }

    public boolean isEmpty() {
        return getBlockState().getValue(AirtightHatchBlock.CANISTER_TYPE) == CanisterType.EMPTY;
    }

    public boolean isCreative() {
        return getBlockState().getValue(AirtightHatchBlock.CANISTER_TYPE) == CanisterType.CREATIVE;
    }

    public GasStack getHatchGasContent() {
        return tankBehaviour.getPrimaryHandler().getGasStack();
    }

    public long getHatchCapacity() {
        return tankBehaviour.getPrimaryHandler().getCapacity();
    }

    public void setCanisterContent(@NotNull ItemStack itemStack) {
        canister = itemStack.copy();
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return;
        }

        updateCapacity();
        tankBehaviour.getInternalGasHandler().forceDrain(getHatchGasContent(), GasAction.EXECUTE);
        tankBehaviour.getInternalGasHandler().forceFill(canisterContents.getGasInTank(0), GasAction.EXECUTE);
    }

    private void updateCapacity() {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return;
        }

        long newCapacity = canisterContents.getTankCapacity(0);
        if (getHatchCapacity() == newCapacity) {
            return;
        }

        tankBehaviour.getPrimaryHandler().setCapacity(newCapacity);
    }

    private void tryTransferGas() {
        if (level == null || level.isClientSide) {
            return;
        }

        IGasHandler targetHandler = getTargetGasHandler(level);
        if (targetHandler == null) {
            return;
        }

        int currentMode = hatchTransferMode.getValue();
        if (currentMode == HatchTransferMode.NO_TRANSFER.ordinal()) {
            return;
        }

        IGasHandler hatchHandler = tankBehaviour.getPrimaryHandler();
        long transfer = getMaxTransferRate();
        boolean creative = isCreative();
        if (currentMode == HatchTransferMode.INPUT_ONLY.ordinal()) {
            inputOnly(hatchHandler, targetHandler, transfer, creative);
        }
        else if (currentMode == HatchTransferMode.OUTPUT_ONLY.ordinal()) {
            outputOnly(hatchHandler, targetHandler, transfer, creative);
        }
        else if (currentMode == HatchTransferMode.STAY_HALF.ordinal()) {
            stayHalf(hatchHandler, targetHandler, creative);
        }
    }

    private enum HatchTransferMode implements INamedIconOptions {
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