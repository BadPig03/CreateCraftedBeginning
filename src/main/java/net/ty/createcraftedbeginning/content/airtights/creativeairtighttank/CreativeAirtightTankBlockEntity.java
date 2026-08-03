package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasConnectivityHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.CreativeSmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTankMultiBlockEntityContainer;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.IChamberGasTank;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirtightTankBlockEntity extends SmartBlockEntity implements IGasTankMultiBlockEntityContainer, IHaveGoggleInformation, IChamberGasTank, ICreativeGasContainer, ThresholdSwitchObservable {
    private static final int SYNC_RATE = 4;

    private static final String COMPOUND_KEY_UPDATE_CONNECTIVITY = "UpdateConnectivity";
    private static final String COMPOUND_KEY_LAST_KNOWN_POS = "LastKnownPos";
    private static final String COMPOUND_KEY_CONTROLLER_POS = "Controller";
    private static final String COMPOUND_KEY_TANK_CONTENT = "TankContent";
    private static final String COMPOUND_KEY_WIDTH = "Width";
    private static final String COMPOUND_KEY_HEIGHT = "Height";

    protected IGasHandler gasCapability;
    protected GasTank tankInventory;
    protected BlockPos controllerPos;
    protected BlockPos lastKnownPos;
    protected boolean updateConnectivity;
    protected boolean updateCapability;
    protected int width;
    protected int height;
    protected int syncCooldown;
    protected boolean queuedSync;

    public CreativeAirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tankInventory = new CreativeSmartGasTank(getCapacityPerTank(), this::onGasStackChanged);
        updateConnectivity = false;
        updateCapability = false;
        height = 1;
        width = 1;
        refreshCapability();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.CREATIVE_AIRTIGHT_TANK.get(), (be, context) -> {
            if (be.gasCapability == null) {
                be.refreshCapability();
            }
            return be.gasCapability;
        });
    }

    public static long getCapacityPerTank() {
        return Integer.MAX_VALUE * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    @Nullable
    private static BlockPos readOptionalBlockPos(CompoundTag tag, String key) {
        return tag.contains(key) ? NBTHelper.readBlockPos(tag, key) : null;
    }

    @Override
    public GasTank getTankInventory() {
        return tankInventory;
    }

    @Override
    public IGasHandler getCapability() {
        return gasCapability;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        sendData();
        if (level == null || !level.isClientSide) {
            return;
        }

        invalidateRenderBoundingBox();
    }

    @Override
    public void tick() {
        super.tick();
        tickSyncCooldown();

        if (lastKnownPos == null) {
            lastKnownPos = getBlockPos();
        }
        else if (!lastKnownPos.equals(worldPosition)) {
            onPositionChanged();
            return;
        }

        if (updateCapability) {
            updateCapability = false;
            refreshCapability();
        }
        if (!updateConnectivity) {
            return;
        }

        updateConnectivity();
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (isController()) {
            compoundTag.put(COMPOUND_KEY_TANK_CONTENT, tankInventory.write(provider, new CompoundTag()));
            compoundTag.putInt(COMPOUND_KEY_WIDTH, width);
            compoundTag.putInt(COMPOUND_KEY_HEIGHT, height);
        }
        else {
            compoundTag.put(COMPOUND_KEY_CONTROLLER_POS, NbtUtils.writeBlockPos(controllerPos));
        }
        if (clientPacket) {
            return;
        }

        compoundTag.putBoolean(COMPOUND_KEY_UPDATE_CONNECTIVITY, updateConnectivity);
        if (lastKnownPos == null) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_LAST_KNOWN_POS, NbtUtils.writeBlockPos(lastKnownPos));
    }

    @Override
    public void writeSafe(CompoundTag compoundTag, Provider provider) {
        if (!isController()) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_WIDTH, width);
        compoundTag.putInt(COMPOUND_KEY_HEIGHT, height);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        BlockPos previousController = controllerPos;
        int previousWidth = width;
        int previousHeight = height;

        if (!clientPacket) {
            readServerData(compoundTag);
        }

        controllerPos = readOptionalBlockPos(compoundTag, COMPOUND_KEY_CONTROLLER_POS);
        if (isController()) {
            readControllerData(compoundTag, provider);
        }

        updateCapability = true;
        if (!clientPacket) {
            return;
        }

        boolean controllerChanged = !Objects.equals(previousController, controllerPos);
        if (!controllerChanged && previousWidth == width && previousHeight == height) {
            return;
        }

        updateClientState();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    private void tickSyncCooldown() {
        if (syncCooldown <= 0) {
            return;
        }

        syncCooldown--;
        if (syncCooldown != 0 || !queuedSync) {
            return;
        }

        sendData();
    }

    private void readServerData(CompoundTag tag) {
        updateConnectivity = tag.getBoolean(COMPOUND_KEY_UPDATE_CONNECTIVITY);
        lastKnownPos = readOptionalBlockPos(tag, COMPOUND_KEY_LAST_KNOWN_POS);
    }

    private void readControllerData(CompoundTag tag, Provider provider) {
        if (tag.contains(COMPOUND_KEY_WIDTH)) {
            width = Math.clamp(tag.getInt(COMPOUND_KEY_WIDTH), 1, getConfiguredMaxWidth());
        }
        if (tag.contains(COMPOUND_KEY_HEIGHT)) {
            height = Math.clamp(tag.getInt(COMPOUND_KEY_HEIGHT), 1, getConfiguredMaxLength());
        }

        tankInventory.setCapacity(getCapacityPerTank());
        if (!tag.contains(COMPOUND_KEY_TANK_CONTENT)) {
            return;
        }

        tankInventory.read(provider, tag.getCompound(COMPOUND_KEY_TANK_CONTENT));
    }

    private void updateClientState() {
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
        }
        if (isController()) {
            tankInventory.setCapacity(getCapacityPerTank());
        }
        invalidateRenderBoundingBox();
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

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level == null || level.isClientSide || !isController()) {
            return;
        }

        GasConnectivityHandler.formMulti(this, level);
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    void refreshCapability() {
        gasCapability = handlerForCapability();
        invalidateCapabilities();
    }

    private IGasHandler handlerForCapability() {
        if (isController()) {
            return tankInventory;
        }

        CreativeAirtightTankBlockEntity controller = getControllerBE();
        return controller != null ? controller.handlerForCapability() : new GasTank(0);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (!isController()) {
            return super.createRenderBoundingBox();
        }
        return super.createRenderBoundingBox().expandTowards(width - 1, height - 1, width - 1);
    }

    protected void onGasStackChanged(GasStack newStack) {
        if (level == null || level.isClientSide) {
            return;
        }

        notifyUpdate();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        CreativeAirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return false;
        }

        IGasHandler handler = controller.gasCapability;
        if (handler == null) {
            return false;
        }

        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gas = handler.getGasInTank(0);
        if (gas.isEmpty()) {
            CCBLang.translate("gui.gas_container.empty").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CCBLang.translate("gui.gas_container.infinity").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public IGasTank getTank(int tank) {
        return tankInventory;
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        tankInventory.setCapacity(getCapacityPerTank());
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public GasStack getGas(int tank) {
        return tankInventory.getGasStack().copy();
    }

    @Override
    public long getTankSize(int tank) {
        return getCapacityPerTank();
    }

    @Override
    public int getMaxValue() {
        CreativeAirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return 0;
        }
        return GasAmountUtils.toWholeBucketsClamped(getCapacityPerTank());
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        CreativeAirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return 0;
        }

        IGasHandler handler = controller.gasCapability;
        GasStack gas = handler.getGasInTank(0);
        if (gas.isEmpty()) {
            return 0;
        }
        return GasAmountUtils.toWholeBucketsClamped(getCapacityPerTank());
    }

    @Override
    public MutableComponent format(int value) {
        return GasAmountUtils.formatWholeBuckets(value);
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controllerPos;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity & IMultiBlockEntityContainer> @Nullable T getControllerBE() {
        if (isController() || level == null) {
            return (T) this;
        }

        BlockEntity blockEntity = level.getBlockEntity(controllerPos);
        return blockEntity instanceof CreativeAirtightTankBlockEntity tank ? (T) tank : null;
    }

    @Override
    public boolean isController() {
        return controllerPos == null || worldPosition.equals(controllerPos);
    }

    @Override
    public void setController(BlockPos controller) {
        if (level == null || level.isClientSide && !isVirtual() || controller.equals(controllerPos)) {
            return;
        }

        controllerPos = controller;
        refreshCapability();
        notifyUpdate();
    }

    @Override
    public void removeController(boolean keepFluids) {
        if (level == null || level.isClientSide) {
            return;
        }

        updateConnectivity = true;
        tankInventory.setCapacity(getCapacityPerTank());
        controllerPos = null;
        width = 1;
        height = 1;
        onGasStackChanged(tankInventory.getGasStack());
        BlockState state = getBlockState();
        if (state.getBlock() instanceof CreativeAirtightTankBlock) {
            state = state.setValue(CreativeAirtightTankBlock.TOP, true).setValue(CreativeAirtightTankBlock.BOTTOM, true);
            level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
        }
        refreshCapability();
        notifyUpdate();
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        if (state.getBlock() instanceof CreativeAirtightTankBlock) {
            int controllerY = getController().getY();
            int blockY = getBlockPos().getY();
            state = state.setValue(CreativeAirtightTankBlock.BOTTOM, controllerY == blockY);
            state = state.setValue(CreativeAirtightTankBlock.TOP, controllerY + height - 1 == blockY);
            level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
        }
        onGasStackChanged(tankInventory.getGasStack());
        setChanged();
    }

    @Override
    public Axis getMainConnectionAxis() {
        return Axis.Y;
    }

    @Override
    public int getMaxLength(Axis longAxis, int width) {
        return getConfiguredMaxLength();
    }

    @Override
    public int getMaxWidth() {
        return getConfiguredMaxWidth();
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = Math.clamp(height, 1, getConfiguredMaxLength());
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = Math.clamp(width, 1, getConfiguredMaxWidth());
    }

    private static int getConfiguredMaxLength() {
        return Math.max(1, CCBConfig.server().airtights.maxAirtightTankLength.get());
    }

    private static int getConfiguredMaxWidth() {
        return Math.max(1, CCBConfig.server().airtights.maxAirtightTankWidth.get());
    }

    @Override
    public boolean isCreative(Level level, BlockState blockState, BlockPos blockPos) {
        return true;
    }
}