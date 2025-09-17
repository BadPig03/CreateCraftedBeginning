package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasConnectivityHandler;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasTank;
import net.ty.createcraftedbeginning.api.gas.SmartGasTank;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasTank;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasTankMultiBlockEntityContainer;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.GasControllerData;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class AirtightTankBlockEntity extends SmartBlockEntity implements IGasTankMultiBlockEntityContainer.Gas, IHaveGoggleInformation {
    private static final int MAX_LENGTH = 4;
    private static final int MAX_WIDTH = 3;
    private static final int SYNC_RATE = 4;
    public GasControllerData gasController;
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

    public AirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tankInventory = createInventory();
        updateConnectivity = false;
        updateCapability = false;
        height = 1;
        width = 1;
        gasController = new GasControllerData();
        refreshCapability();
    }

    public static long getCapacityPerTank() {
        return CCBConfig.server().compressedAir.airtightTankCapacity.get() * 1000L;
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasCapabilities.GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_TANK.get(), (be, context) -> {
            if (be.gasCapability == null) {
                be.refreshCapability();
            }
            return be.gasCapability;
        });
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        sendData();
        if (level != null && level.isClientSide) {
            invalidateRenderBoundingBox();
        }
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

        if (lastKnownPos == null) {
            lastKnownPos = getBlockPos();
        } else if (!lastKnownPos.equals(worldPosition)) {
            onPositionChanged();
            return;
        }

        if (updateCapability) {
            updateCapability = false;
            refreshCapability();
        }
        if (updateConnectivity) {
            updateConnectivity();
        }
        if (isController()) {
            gasController.tick(this);
        }
    }

    @Override
    public void write(@NotNull CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.put("GasController", gasController.write(registries));
        if (updateConnectivity) {
            compound.putBoolean("Uninitialized", true);
        }
        if (lastKnownPos != null) {
            compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        }
        if (!isController()) {
            compound.put("Controller", NbtUtils.writeBlockPos(controllerPos));
        }
        if (isController()) {
            compound.put("TankContent", tankInventory.writeToNBT(registries, new CompoundTag()));
            compound.putInt("Width", width);
            compound.putInt("Height", height);
        }
        super.write(compound, registries, clientPacket);
    }

    @Override
    public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
        if (!isController()) {
            return;
        }
        compound.putInt("Width", width);
        compound.putInt("Height", height);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        BlockPos controllerBefore = controllerPos;
        int prevSize = width;
        int prevHeight = height;

        updateConnectivity = compound.contains("Uninitialized");
        lastKnownPos = null;

        if (compound.contains("LastKnownPos")) {
            lastKnownPos = NBTHelper.readBlockPos(compound, "LastKnownPos");
        }

        controllerPos = null;
        if (compound.contains("Controller")) {
            controllerPos = NBTHelper.readBlockPos(compound, "Controller");
        }

        if (isController()) {
            width = compound.getInt("Width");
            height = compound.getInt("Height");
            tankInventory.setCapacity(getTotalTankSize() * getCapacityPerTank());
            tankInventory.readFromNBT(registries, compound.getCompound("TankContent"));
        }

        gasController.read(compound.getCompound("GasController"), registries);
        updateCapability = true;

        if (!clientPacket) {
            return;
        }

        boolean changed = !Objects.equals(controllerBefore, controllerPos);
        if (!changed && prevSize == width && prevHeight == height) {
            return;
        }
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
        }
        if (isController()) {
            tankInventory.setCapacity(getCapacityPerTank() * getTotalTankSize());
        }
        invalidateRenderBoundingBox();
    }

    protected SmartGasTank createInventory() {
        return new SmartGasTank(getCapacityPerTank(), this::onGasStackChanged);
    }

    public IGasHandler getCapability() {
        return gasCapability;
    }

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level == null || level.isClientSide || !isController()) {
            return;
        }
        GasConnectivityHandler.formMulti(this);
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    protected void onGasStackChanged(GasStack newStack) {
        if (level == null) {
            return;
        }

        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                    AirtightTankBlockEntity tankAt = GasConnectivityHandler.partAt(getType(), level, pos);
                    if (tankAt == null) {
                        continue;
                    }
                    level.updateNeighbourForOutputSignal(pos, tankAt.getBlockState().getBlock());
                }
            }
        }

        if (!level.isClientSide) {
            setChanged();
            sendData();
        }
    }

    public void applyGasTankSize(int blocks) {
        tankInventory.setCapacity((long) blocks * getCapacityPerTank());
        long overflow = tankInventory.getGasAmount() - tankInventory.getCapacity();
        if (overflow <= 0) {
            return;
        }
        tankInventory.drain(overflow, GasAction.EXECUTE);
    }

    public void updateTankState() {
        if (!isController() || level == null) {
            return;
        }

        boolean wasGasController = gasController.isActive();
        boolean changed = gasController.evaluate(this);

        if (wasGasController != gasController.isActive()) {
            for (int yOffset = 0; yOffset < height; yOffset++) {
                for (int xOffset = 0; xOffset < width; xOffset++) {
                    for (int zOffset = 0; zOffset < width; zOffset++) {
                        if (level.getBlockEntity(worldPosition.offset(xOffset, yOffset, zOffset)) instanceof AirtightTankBlockEntity atbe) {
                            atbe.refreshCapability();
                        }
                    }
                }
            }
        }

        if (changed) {
            notifyUpdate();
        }
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

    void refreshCapability() {
        gasCapability = handlerForCapability();
        invalidateCapabilities();
    }

    private IGasHandler handlerForCapability() {
        if (isController()) {
            return gasController.isActive() ? gasController.createHandler() : tankInventory;
        }
        return getControllerBE() != null ? getControllerBE().handlerForCapability() : new GasTank(0);
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controllerPos;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AirtightTankBlockEntity getControllerBE() {
        if (isController() || level == null) {
            return this;
        }
        BlockEntity blockEntity = level.getBlockEntity(controllerPos);
        if (!(blockEntity instanceof AirtightTankBlockEntity)) {
            return null;
        }
        return (AirtightTankBlockEntity) blockEntity;
    }

    @Override
    public boolean isController() {
        return controllerPos == null || (worldPosition.getX() == controllerPos.getX() && worldPosition.getY() == controllerPos.getY() && worldPosition.getZ() == controllerPos.getZ());
    }

    @Override
    public void setController(BlockPos controller) {
        if (level == null || (level.isClientSide && !isVirtual()) || controller.equals(this.controllerPos)) {
            return;
        }

        this.controllerPos = controller;
        refreshCapability();
        setChanged();
        sendData();
    }

    @Override
    public void removeController(boolean keepFluids) {
        if (level == null || level.isClientSide) {
            return;
        }

        updateConnectivity = true;
        applyGasTankSize(1);
        controllerPos = null;
        width = 1;
        height = 1;
        gasController.clear();
        onGasStackChanged(tankInventory.getGas());

        BlockState state = getBlockState();
        if (state.getBlock() instanceof AirtightTankBlock) {
            state = state.setValue(AirtightTankBlock.TOP, true).setValue(AirtightTankBlock.BOTTOM, true);
            level.setBlock(worldPosition, state, 22);
        }

        refreshCapability();
        setChanged();
        sendData();
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
        if (state.getBlock() instanceof AirtightTankBlock) {
            state = state.setValue(AirtightTankBlock.BOTTOM, getController().getY() == getBlockPos().getY());
            state = state.setValue(AirtightTankBlock.TOP, getController().getY() + height - 1 == getBlockPos().getY());
            level.setBlock(worldPosition, state, 6);
        }

        onGasStackChanged(tankInventory.getGas());
        updateTankState();
        setChanged();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return Direction.Axis.Y;
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        return MAX_LENGTH;
    }

    @Override
    public int getMaxWidth() {
        return MAX_WIDTH;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (isController()) {
            return super.createRenderBoundingBox().expandTowards(width - 1, height - 1, width - 1);
        } else {
            return super.createRenderBoundingBox();
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        AirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return false;
        }
        if (controller.gasController.addToGoggleTooltip(tooltip, controller.getTotalTankSize())) {
            return true;
        }
        return tankTooltip(tooltip, gasCapability);
    }

    private boolean tankTooltip(List<Component> tooltip, IGasHandler handler) {
        if (handler == null || handler.getTanks() == 0) {
            return false;
        }

        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        CCBLang.translate("gui.goggles.gas_container").forGoggles(tooltip);

        boolean isEmpty = true;
        for (int i = 0; i < handler.getTanks(); i++) {
            GasStack stack = handler.getGasInTank(i);
            if (stack.isEmpty()) {
                continue;
            }

            CCBLang.gasName(stack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.builder().add(CCBLang.number(stack.getAmount()).add(mb).style(ChatFormatting.GOLD)).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(handler.getTankCapacity(i)).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
            isEmpty = false;
        }

        if (handler.getTanks() > 1) {
            if (isEmpty) {
                tooltip.removeLast();
            }
            return true;
        }

        if (!isEmpty) {
            return true;
        }

        CCBLang.translate("gui.goggles.gas_container.capacity").add(CCBLang.number(handler.getTankCapacity(0)).add(mb).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);

        return true;
    }

    public GasTank getTankInventory() {
        return tankInventory;
    }

    public int getTotalTankSize() {
        return width * width * height;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public long getTankSize(int tank) {
        return getCapacityPerTank();
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyGasTankSize(blocks);
    }

    @Override
    public IGasTank getTank(int tank) {
        return tankInventory;
    }

    @Override
    public GasStack getGas(int tank) {
        return tankInventory.getGas().copy();
    }
}