package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.simibubi.create.AllEnchantments;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.SmartGasTank;
import net.ty.createcraftedbeginning.api.gas.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.SmartGasTankBehaviour.InternalGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBEnchantments;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AirtightHatchBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ThresholdSwitchObservable {
    private static final int SYNC_RATE = 4;
    private static final int LAZY_TICK_RATE = 20;
    private static final long MILLIBUCKETS_PER_BUCKET = 1000L;
    private static final long MAX_TRANSFER_AMOUNT = 50L;

    private static final String COMPOUND_KEY_CAPACITY_ENCHANT_LEVEL = "CapacityEnchantLevel";
    private static final String COMPOUND_KEY_ECONOMIZE_ENCHANT_LEVEL = "EconomizeEnchantLevel";
    private static final String COMPOUND_KEY_CUSTOM_NAME = "CustomName";

    private int capacityEnchantLevel;
    private int economizeEnchantLevel;
    private Component customName;
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

    private static int getMaxTransferAmount() {
        return 0;
    }

    private static boolean inputOnly(@NotNull InternalGasHandler internalHandler, @NotNull IGasHandler targetHandler, long transferAmount) {
        GasStack drainedGasStack = targetHandler.drain(transferAmount, GasAction.SIMULATE);
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

    private static boolean outputOnly(@NotNull InternalGasHandler internalHandler, @NotNull IGasHandler targetHandler, long transferAmount) {
        GasStack drainedGasStack = internalHandler.forceDrain(transferAmount, GasAction.SIMULATE);
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

    private static boolean stayHalf(@NotNull InternalGasHandler internalHandler, @NotNull IGasHandler targetHandler) {
        long currentAmount = internalHandler.getGasInTank(0).getAmount();
        long halfCapacity = internalHandler.getTankCapacity(0) / 2;
        long delta = halfCapacity - currentAmount;
        long transferAmount = Math.min(Math.abs(delta), MAX_TRANSFER_AMOUNT);
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
        tankBehaviour = SmartGasTankBehaviour.single(this, getMaxCapacity()).forbidInsertion().forbidExtraction();
        hatchTransferMode = new ScrollOptionBehaviour<>(HatchTransferMode.class, CCBLang.translateDirect("gui.airtight_hatch.transfer_mode"), this, new AirtightHatchValueBox());
        behaviours.add(tankBehaviour);
        behaviours.add(hatchTransferMode);
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
    public void tick() {
        super.tick();
        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync) {
                sendData();
            }
        }

        if (!getBlockState().getValue(AirtightHatchBlock.OCCUPIED)) {
            return;
        }

        tryTransferGas();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        super.write(compoundTag, registries, clientPacket);
        compoundTag.putInt(COMPOUND_KEY_CAPACITY_ENCHANT_LEVEL, capacityEnchantLevel);
        compoundTag.putInt(COMPOUND_KEY_ECONOMIZE_ENCHANT_LEVEL, economizeEnchantLevel);
        if (customName == null) {
            return;
        }

        compoundTag.putString(COMPOUND_KEY_CUSTOM_NAME, Serializer.toJson(customName, registries));
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        super.read(compoundTag, registries, clientPacket);
        if (compoundTag.contains(COMPOUND_KEY_CAPACITY_ENCHANT_LEVEL)) {
            capacityEnchantLevel = compoundTag.getInt(COMPOUND_KEY_CAPACITY_ENCHANT_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_ECONOMIZE_ENCHANT_LEVEL)) {
            economizeEnchantLevel = compoundTag.getInt(COMPOUND_KEY_ECONOMIZE_ENCHANT_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_CUSTOM_NAME)) {
            customName = Serializer.fromJson(compoundTag.getString(COMPOUND_KEY_CUSTOM_NAME), registries);
        }
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

        IGasHandler handler = tankBehaviour.getCapability();
        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        CCBLang.translate("gui.goggles.gas_container").forGoggles(tooltip);
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
        if (!inputGasStack.isEmpty() && !internalGasStack.isEmpty() && !GasStack.isSameGas(internalGasStack, inputGasStack)) {
            return;
        }

        int currentMode = hatchTransferMode.getValue();
        if (currentMode == HatchTransferMode.NO_TRANSFER.ordinal()) {
            return;
        }

        boolean result = false;
        if (currentMode == HatchTransferMode.INPUT_ONLY.ordinal()) {
            result = inputOnly(internalHandler, targetHandler, MAX_TRANSFER_AMOUNT);
        }
        else if (currentMode == HatchTransferMode.OUTPUT_ONLY.ordinal()) {
            result = outputOnly(internalHandler, targetHandler, MAX_TRANSFER_AMOUNT);
        }
        else if (currentMode == HatchTransferMode.STAY_HALF.ordinal()) {
            result = stayHalf(internalHandler, targetHandler);
        }
        if (!result) {
            return;
        }

        sendData();
        if (!(getTargetBE(level) instanceof SmartBlockEntity smart)) {
            return;
        }

        smart.sendData();
    }

    @Nullable
    public BlockEntity getTargetBE(@NotNull Level level) {
        return level.getBlockEntity(getBlockPos().relative(getBlockState().getValue(AirtightHatchBlock.FACING)));
    }

    @Nullable
    public IGasHandler getTargetGasHandler(@NotNull Level level) {
        BlockEntity targetBE = getTargetBE(level);
        if (targetBE == null || targetBE instanceof AirtightHatchBlockEntity) {
            return null;
        }

        Direction direction = getBlockState().getValue(AirtightHatchBlock.FACING);
        BlockPos targetPos = getBlockPos().relative(direction);
        return level.getCapability(GasHandler.BLOCK, targetPos, direction.getOpposite());
    }

    private long getMaxCapacity() {
        return CCBConfig.server().gas.canisterCapacity.get() * MILLIBUCKETS_PER_BUCKET * (1 + capacityEnchantLevel);
    }

    private void updateCapacity() {
        long oldCapacity = tankBehaviour.getPrimaryHandler().getCapacity();
        long newCapacity = getMaxCapacity();
        if (oldCapacity == newCapacity) {
            return;
        }

        tankBehaviour.getPrimaryHandler().setCapacity(newCapacity);
        notifyUpdate();
    }

    private GasStack getContent() {
        return tankBehaviour.getPrimaryHandler().getGasStack();
    }

    public void setContent(@NotNull ItemStack canister, @NotNull Level level) {
        customName = canister.has(DataComponents.CUSTOM_NAME) ? canister.getHoverName() : null;
        capacityEnchantLevel = canister.getEnchantmentLevel(level.holderOrThrow(AllEnchantments.CAPACITY));
        economizeEnchantLevel = canister.getEnchantmentLevel(level.holderOrThrow(CCBEnchantments.ECONOMIZE));
        updateCapacity();

        InternalGasHandler internalGasHandler = tankBehaviour.getInternalGasHandler();
        internalGasHandler.forceDrain(getContent().copy(), GasAction.EXECUTE);
        GasStack gasStack = canister.getOrDefault(CCBDataComponents.CANISTER_CONTENT, GasStack.EMPTY);
        internalGasHandler.forceFill(gasStack, GasAction.EXECUTE);
    }

    public ItemStack createCanisterItemStack(@NotNull Level level) {
        ItemStack canister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        if (customName != null) {
            canister.set(DataComponents.CUSTOM_NAME, customName);
        }
        if (capacityEnchantLevel > 0) {
            canister.enchant(level.holderOrThrow(AllEnchantments.CAPACITY), capacityEnchantLevel);
        }
        if (economizeEnchantLevel > 0) {
            canister.enchant(level.holderOrThrow(CCBEnchantments.ECONOMIZE), economizeEnchantLevel);
        }
        canister.set(CCBDataComponents.CANISTER_CONTENT, getContent().copy());
        return canister;
    }

    public void giveCanisterToPlayer(@NotNull Player player, @NotNull Level level) {
        ItemStack canister = createCanisterItemStack(level);
        player.getInventory().placeItemBackInInventory(canister);
        customName = null;
        capacityEnchantLevel = 0;
        economizeEnchantLevel = 0;
        tankBehaviour.getInternalGasHandler().forceDrain(getContent(), GasAction.EXECUTE);
        updateCapacity();
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
        return CreateLang.text(value + " ").add(CreateLang.translate("schedule.condition.threshold.buckets")).component();
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