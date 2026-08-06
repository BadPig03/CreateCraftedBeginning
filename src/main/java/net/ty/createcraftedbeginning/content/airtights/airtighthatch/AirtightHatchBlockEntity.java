package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.createmod.catnip.lang.Lang;
import net.minecraft.ChatFormatting;
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
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.SmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock.CanisterType;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.ICreativeGasContainer;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightHatchBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ICreativeGasContainer, ThresholdSwitchObservable {
    private static final int LAZY_TICK_RATE = 20;
    private static final int TICKS_PER_SECOND = 20;

    private static final String COMPOUND_KEY_CANISTER = "Canister";
    private static final String COMPOUND_KEY_CAPACITY = "Capacity";

    private ItemStack canister = ItemStack.EMPTY;
    private SmartGasTankBehaviour tankBehaviour;
    private ScrollOptionBehaviour<HatchTransferMode> hatchTransferMode;

    private long transferRemainder;

    public AirtightHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_HATCH.get(), (be, context) -> be.tankBehaviour.getCapability());
    }

    private static int getMaxTransferRate() {
        return CCBConfig.server().airtights.maxTransferRate.get();
    }

    private static GasStack executeMatchingDrain(IGasHandler source, GasStack request) {
        if (request.isEmpty()) {
            return GasStack.EMPTY;
        }

        GasStack drained = source.drain(request, GasAction.EXECUTE);
        if (!drained.isEmpty()) {
            return validateDrainedGas(source, request, drained);
        }

        GasStack genericPreview = source.drain(request.getAmount(), GasAction.SIMULATE);
        if (genericPreview.isEmpty() || !GasStack.isSameGasSameComponents(genericPreview, request)) {
            return GasStack.EMPTY;
        }

        drained = source.drain(request.getAmount(), GasAction.EXECUTE);
        return validateDrainedGas(source, request, drained);
    }

    private static GasStack validateDrainedGas(IGasHandler source, GasStack request, GasStack drained) {
        if (drained.isEmpty()) {
            return GasStack.EMPTY;
        }

        if (!GasStack.isSameGasSameComponents(drained, request)) {
            source.fill(drained, GasAction.EXECUTE);
            return GasStack.EMPTY;
        }

        long requestedAmount = request.getAmount();
        if (drained.getAmount() <= requestedAmount) {
            return drained;
        }

        GasStack excess = drained.copyWithAmount(drained.getAmount() - requestedAmount);
        source.fill(excess, GasAction.EXECUTE);
        return drained.copyWithAmount(requestedAmount);
    }

    private static void transferGas(IGasHandler source, IGasHandler target, GasStack offered, boolean isInfiniteSource, boolean isVoidTarget) {
        if (offered.isEmpty()) {
            return;
        }

        long accepted = isVoidTarget ? offered.getAmount() : target.fill(offered, GasAction.SIMULATE);
        accepted = Math.clamp(accepted, 0, offered.getAmount());
        if (accepted == 0) {
            return;
        }

        GasStack request = offered.copyWithAmount(accepted);
        GasStack drained = isInfiniteSource ? request : executeMatchingDrain(source, request);
        if (drained.isEmpty()) {
            return;
        }

        if (isVoidTarget) {
            return;
        }

        long filled = target.fill(drained, GasAction.EXECUTE);
        filled = Math.clamp(filled, 0, drained.getAmount());
        if (isInfiniteSource || filled >= drained.getAmount()) {
            return;
        }

        GasStack remainder = drained.copyWithAmount(drained.getAmount() - filled);
        source.fill(remainder, GasAction.EXECUTE);
    }

    private static void inputOnly(IGasHandler hatch, IGasHandler target, long limit, boolean isCreative) {
        GasStack hatchGas = hatch.getGasInTank(0);
        GasStack available = hatchGas.isEmpty() ? target.drain(limit, GasAction.SIMULATE) : target.drain(hatchGas.copyWithAmount(limit), GasAction.SIMULATE);
        if (available.isEmpty()) {
            return;
        }

        long amount = Math.min(limit, available.getAmount());
        transferGas(target, hatch, available.copyWithAmount(amount), false, isCreative);
    }

    private static void outputOnly(IGasHandler hatch, IGasHandler target, long limit, boolean isCreative) {
        GasStack hatchGas = hatch.getGasInTank(0);
        if (hatchGas.isEmpty()) {
            return;
        }

        long amount = Math.min(limit, hatchGas.getAmount());
        transferGas(hatch, target, hatchGas.copyWithAmount(amount), isCreative, false);
    }

    private static void stayHalf(IGasHandler hatch, IGasHandler target, long limit, boolean isCreative) {
        GasStack hatchGas = hatch.getGasInTank(0);
        long delta = hatchGas.getAmount() - hatch.getTankCapacity(0) / 2;
        if (delta == 0) {
            return;
        }

        long amount = Math.min(limit, Math.abs(delta));
        if (delta > 0) {
            outputOnly(hatch, target, amount, isCreative);
            return;
        }

        inputOnly(hatch, target, amount, isCreative);
    }

    private static void addCreativeTooltip(List<Component> tooltip, GasStack gas) {
        if (gas.isEmpty()) {
            CCBLang.translate("gui.creative_gas_canister.empty").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CCBLang.translate("gui.gas_container.infinity").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
    }

    private static void addStandardTooltip(List<Component> tooltip, GasStack gas, long capacity) {
        if (gas.isEmpty()) {
            CCBLang.translate("gui.gas_container.capacity").add(GasAmountUtils.precise(capacity).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        GasAmountUtils.precise(gas.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmountUtils.precise(capacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }

    private static long getEffectiveCapacity(long configuredCapacity, long currentCapacity, long gasAmount) {
        long normalizedCapacity = Math.max(0, configuredCapacity);
        long normalizedGasAmount = Math.max(0, gasAmount);
        if (normalizedGasAmount <= normalizedCapacity) {
            return normalizedCapacity;
        }

        return Math.max(Math.max(0, currentCapacity), normalizedGasAmount);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, 0).forbidExtraction().forbidInsertion();
        behaviours.add(tankBehaviour);
        hatchTransferMode = new ScrollOptionBehaviour<>(HatchTransferMode.class, CCBLang.translateDirect("gui.airtight_hatch.transfer_mode"), this, new AirtightHatchValueBox());
        behaviours.add(hatchTransferMode);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || isEmpty()) {
            return;
        }

        long transferQuota = getTransferQuota();
        if (transferQuota <= 0) {
            return;
        }

        tryTransferGas(transferQuota);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof AirtightHatchBlock hatch)) {
            return;
        }

        if (!hatch.canSurvive(state, level, getBlockPos())) {
            level.destroyBlock(worldPosition, true);
            return;
        }

        if (isEmpty()) {
            return;
        }

        updateCapacity(true);
    }

    @Override
    protected void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.write(tag, provider, clientPacket);
        if (clientPacket) {
            tag.putLong(COMPOUND_KEY_CAPACITY, getHatchCapacity());
            return;
        }

        if (canister.isEmpty()) {
            return;
        }

        tag.put(COMPOUND_KEY_CANISTER, canister.saveOptional(provider));
        tag.putLong(COMPOUND_KEY_CAPACITY, getHatchCapacity());
    }

    @Override
    protected void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.read(tag, provider, clientPacket);
        if (clientPacket) {
            if (tag.contains(COMPOUND_KEY_CAPACITY)) {
                long capacity = Math.max(0, tag.getLong(COMPOUND_KEY_CAPACITY));
                tankBehaviour.getPrimaryHandler().setCapacity(capacity);
            }
            return;
        }

        canister = tag.contains(COMPOUND_KEY_CANISTER) ? ItemStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_CANISTER)) : ItemStack.EMPTY;
        if (!canister.isEmpty() && tag.contains(COMPOUND_KEY_CAPACITY)) {
            tankBehaviour.getPrimaryHandler().setCapacity(Math.max(0, tag.getLong(COMPOUND_KEY_CAPACITY)));
        }
        updateCapacity(false);
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

        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gas = getHatchGasContent();
        long capacity = getHatchCapacity();
        if (isCreative()) {
            addCreativeTooltip(tooltip, gas);
        }
        else {
            addStandardTooltip(tooltip, gas, capacity);
        }
        return true;
    }

    @Override
    public int getMaxValue() {
        return isEmpty() ? 0 : GasAmountUtils.toWholeBucketsClamped(getHatchCapacity());
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return isEmpty() ? 0 : GasAmountUtils.toWholeBucketsClamped(getHatchGasContent().getAmount());
    }

    @Override
    public MutableComponent format(int value) {
        return GasAmountUtils.formatWholeBuckets(value);
    }

    @Override
    public boolean isCreative(Level level, BlockState blockState, BlockPos blockPos) {
        return blockState.getValue(AirtightHatchBlock.CANISTER_TYPE) == CanisterType.CREATIVE;
    }

    @Nullable
    public IGasHandler getTargetGasHandler(Level level) {
        BlockState state = getBlockState();
        BlockPos pos = getBlockPos();
        if (!AirtightHatchBlock.hasValidAttachment(level, pos, state)) {
            return null;
        }

        Direction facing = state.getValue(AirtightHatchBlock.FACING);
        BlockPos targetPos = pos.relative(facing);
        return level.getCapability(GasHandler.BLOCK, targetPos, facing.getOpposite());
    }

    public ItemStack createCanisterItemStack() {
        ItemStack stack = canister.copyWithCount(1);
        if (stack.isEmpty() || !(stack.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents)) {
            return ItemStack.EMPTY;
        }

        stack.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, List.of(getHatchGasContent()));
        return stack;
    }

    public boolean giveCanisterToPlayer(Player player) {
        ItemStack removedCanister = removeCanister();
        if (removedCanister.isEmpty()) {
            return false;
        }

        ItemHandlerHelper.giveItemToPlayer(player, removedCanister);
        return true;
    }

    public boolean isEmpty() {
        return getBlockState().getValue(AirtightHatchBlock.CANISTER_TYPE) == CanisterType.EMPTY;
    }

    public boolean isCreative() {
        return getBlockState().getValue(AirtightHatchBlock.CANISTER_TYPE) == CanisterType.CREATIVE;
    }

    public GasStack getHatchGasContent() {
        return getInternalHatchGasContent().copy();
    }

    public long getHatchCapacity() {
        return tankBehaviour.getPrimaryHandler().getCapacity();
    }

    public boolean installCanister(ItemStack sourceStack) {
        if (level == null || level.isClientSide || sourceStack.isEmpty() || !isEmpty()) {
            return false;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof AirtightHatchBlock)) {
            return false;
        }

        ItemStack newCanister = sourceStack.copyWithCount(1);
        if (!(newCanister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents contents)) {
            return false;
        }

        CanisterType type = CanisterContainerSuppliers.isValidCreativeGasCanister(newCanister) ? CanisterType.CREATIVE : CanisterType.NORMAL;
        GasStack gas = contents.getGasInTank(0).copy();
        long capacity = getEffectiveCapacity(contents.getTankCapacity(0), 0, gas.getAmount());

        ItemStack oldCanister = canister;
        long oldCapacity = getHatchCapacity();
        GasStack oldGas = getInternalHatchGasContent().copy();
        SmartGasTank tank = tankBehaviour.getPrimaryHandler();
        boolean updated = false;

        tankBehaviour.beginMutation();
        try {
            canister = newCanister;
            tank.setCapacity(capacity);
            tank.setGasStack(gas);
            updated = level.setBlockAndUpdate(worldPosition, state.setValue(AirtightHatchBlock.CANISTER_TYPE, type));
            if (!updated) {
                canister = oldCanister;
                tank.setCapacity(oldCapacity);
                tank.setGasStack(oldGas);
                return false;
            }

            sourceStack.shrink(1);
            transferRemainder = 0;
            setChanged();
            return true;
        } finally {
            boolean hasTankChanged = tankBehaviour.endMutation();
            if (updated && hasTankChanged) {
                tankBehaviour.sendDataImmediately();
            }
        }
    }

    public ItemStack removeCanister() {
        if (level == null || level.isClientSide || isEmpty()) {
            return ItemStack.EMPTY;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof AirtightHatchBlock)) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = createCanisterItemStack();
        if (removed.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack oldCanister = canister;
        long oldCapacity = getHatchCapacity();
        GasStack oldGas = getInternalHatchGasContent().copy();
        SmartGasTank tank = tankBehaviour.getPrimaryHandler();
        boolean updated = false;

        tankBehaviour.beginMutation();
        try {
            canister = ItemStack.EMPTY;
            tank.setGasStack(GasStack.EMPTY);
            tank.setCapacity(0);
            updated = level.setBlockAndUpdate(worldPosition, state.setValue(AirtightHatchBlock.CANISTER_TYPE, CanisterType.EMPTY));
            if (!updated) {
                canister = oldCanister;
                tank.setCapacity(oldCapacity);
                tank.setGasStack(oldGas);
                return ItemStack.EMPTY;
            }

            transferRemainder = 0;
            setChanged();
            return removed;
        } finally {
            boolean hasTankChanged = tankBehaviour.endMutation();
            if (updated && hasTankChanged) {
                tankBehaviour.sendDataImmediately();
            }
        }
    }

    private GasStack getInternalHatchGasContent() {
        return tankBehaviour.getPrimaryHandler().getGasStack();
    }

    private void updateCapacity(boolean syncImmediately) {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return;
        }

        SmartGasTank tank = tankBehaviour.getPrimaryHandler();
        long capacity = getEffectiveCapacity(canisterContents.getTankCapacity(0), tank.getCapacity(), tank.getGasAmount());
        if (tank.getCapacity() == capacity) {
            return;
        }

        tank.setCapacity(capacity);
        if (!syncImmediately || level == null || level.isClientSide) {
            return;
        }

        tankBehaviour.sendDataImmediately();
    }

    private long getTransferQuota() {
        transferRemainder += getMaxTransferRate();
        long quota = transferRemainder / TICKS_PER_SECOND;
        transferRemainder %= TICKS_PER_SECOND;
        return quota;
    }

    private void tryTransferGas(long quota) {
        if (level == null) {
            return;
        }

        HatchTransferMode mode = HatchTransferMode.fromValue(hatchTransferMode.getValue());
        if (mode == HatchTransferMode.NO_TRANSFER) {
            return;
        }

        IGasHandler target = getTargetGasHandler(level);
        if (target == null) {
            return;
        }

        IGasHandler hatch = tankBehaviour.getPrimaryHandler();
        boolean isCreative = isCreative();
        switch (mode) {
            case INPUT_ONLY -> inputOnly(hatch, target, quota, isCreative);
            case OUTPUT_ONLY -> outputOnly(hatch, target, quota, isCreative);
            case STAY_HALF -> stayHalf(hatch, target, quota, isCreative);
        }
    }

    private enum HatchTransferMode implements INamedIconOptions {
        NO_TRANSFER(CCBIcons.I_NO_TRANSFER),
        INPUT_ONLY(CCBIcons.I_INPUT_ONLY),
        OUTPUT_ONLY(CCBIcons.I_OUTPUT_ONLY),
        STAY_HALF(CCBIcons.I_STAY_HALF);

        private static final HatchTransferMode[] VALUES = values();

        private final String translationKey;
        private final CCBIcons icon;

        HatchTransferMode(CCBIcons icon) {
            this.icon = icon;
            translationKey = "createcraftedbeginning.gui.airtight_hatch.transfer_mode." + Lang.asId(name());
        }

        private static HatchTransferMode fromValue(int value) {
            return VALUES[Math.clamp(value, 0, VALUES.length - 1)];
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