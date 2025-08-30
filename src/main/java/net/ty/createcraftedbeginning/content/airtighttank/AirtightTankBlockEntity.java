package net.ty.createcraftedbeginning.content.airtighttank;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtightengine.GasControllerData;
import net.ty.createcraftedbeginning.content.compressedair.CompressedAirOnlyFluidTank;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.util.Helpers;

import java.util.List;
import java.util.Objects;

public class AirtightTankBlockEntity extends SmartBlockEntity implements IMultiBlockEntityContainer.Fluid, IHaveGoggleInformation {
    private static final int MAX_LENGTH = 4;
    private static final int MAX_WIDTH = 3;
    private static final int SYNC_RATE = 4;
    public GasControllerData gasController;
    protected IFluidHandler fluidCapability;
    protected FluidTank tankInventory;
    protected BlockPos controller;
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

    public static int getCapacityMultiplier() {
        return CCBConfig.server().compressedAir.airtightTankCapacity.get() * 1000;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_TANK.get(), (be, context) -> {
            if (be.fluidCapability == null) {
                be.refreshCapability();
            }
            return be.fluidCapability;
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
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.put("GasController", gasController.write(registries));
        if (updateConnectivity) {
            compound.putBoolean("Uninitialized", true);
        }
        if (lastKnownPos != null) {
            compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
        }
        if (!isController()) {
            compound.put("Controller", NbtUtils.writeBlockPos(controller));
        }
        if (isController()) {
            compound.put("TankContent", tankInventory.writeToNBT(registries, new CompoundTag()));
            compound.putInt("Size", width);
            compound.putInt("Height", height);
        }
        super.write(compound, registries, clientPacket);
    }

    @Override
    public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
        if (!isController()) {
            return;
        }
        compound.putInt("Size", width);
        compound.putInt("Height", height);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        BlockPos controllerBefore = controller;
        int prevSize = width;
        int prevHeight = height;

        updateConnectivity = compound.contains("Uninitialized");
        lastKnownPos = null;

        if (compound.contains("LastKnownPos")) {
            lastKnownPos = NBTHelper.readBlockPos(compound, "LastKnownPos");
        }

        controller = null;
        if (compound.contains("Controller")) {
            controller = NBTHelper.readBlockPos(compound, "Controller");
        }

        if (isController()) {
            width = compound.getInt("Size");
            height = compound.getInt("Height");
            tankInventory.setCapacity(getTotalTankSize() * getCapacityMultiplier());
            tankInventory.readFromNBT(registries, compound.getCompound("TankContent"));
        }

        gasController.read(compound.getCompound("GasController"), registries);
        updateCapability = true;

        if (!clientPacket) {
            return;
        }

        boolean changed = !Objects.equals(controllerBefore, controller);
        if (!changed && prevSize == width && prevHeight == height) {
            return;
        }
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
        }
        if (isController()) {
            tankInventory.setCapacity(getCapacityMultiplier() * getTotalTankSize());
        }
        invalidateRenderBoundingBox();
    }

    protected SmartFluidTank createInventory() {
        return new CompressedAirOnlyFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
    }

    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level == null || level.isClientSide || !isController()) {
            return;
        }
        ConnectivityHandler.formMulti(this);
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    protected void onFluidStackChanged(FluidStack newFluidStack) {
        if (level == null) {
            return;
        }

        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                    AirtightTankBlockEntity tankAt = ConnectivityHandler.partAt(getType(), level, pos);
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

    public void applyFluidTankSize(int blocks) {
        tankInventory.setCapacity(blocks * getCapacityMultiplier());
        int overflow = tankInventory.getFluidAmount() - tankInventory.getCapacity();
        if (overflow <= 0) {
            return;
        }
        tankInventory.drain(overflow, IFluidHandler.FluidAction.EXECUTE);
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
        fluidCapability = handlerForCapability();
        invalidateCapabilities();
    }

    private IFluidHandler handlerForCapability() {
        if (isController()) {
            return gasController.isActive() ? gasController.createHandler() : tankInventory;
        }
        return getControllerBE() != null ? getControllerBE().handlerForCapability() : new FluidTank(0);
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AirtightTankBlockEntity getControllerBE() {
        if (isController() || level == null) {
            return this;
        }
        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (!(blockEntity instanceof AirtightTankBlockEntity)) {
            return null;
        }
        return (AirtightTankBlockEntity) blockEntity;
    }

    @Override
    public boolean isController() {
        return controller == null || Helpers.isBlockPosEqual(worldPosition, controller);
    }

    @Override
    public void setController(BlockPos controller) {
        if (level == null || (level.isClientSide && !isVirtual()) || controller.equals(this.controller)) {
            return;
        }

        this.controller = controller;
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
        if (!keepFluids) {
            applyFluidTankSize(1);
        }
        controller = null;
        width = 1;
        height = 1;
        gasController.clear();
        onFluidStackChanged(tankInventory.getFluid());

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

        onFluidStackChanged(tankInventory.getFluid());
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
        return tankTooltip(tooltip, fluidCapability);
    }

    private boolean tankTooltip(List<Component> tooltip, IFluidHandler handler) {
        if (handler == null || handler.getTanks() == 0) {
            return false;
        }

        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        CCBLang.translate("gui.goggles.gas_container").forGoggles(tooltip);

        boolean isEmpty = true;
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack fluidStack = handler.getFluidInTank(i);
            if (fluidStack.isEmpty()) {
                continue;
            }

            CCBLang.fluidName(fluidStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.builder().add(CCBLang.number(fluidStack.getAmount()).add(mb).style(ChatFormatting.GOLD)).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(handler.getTankCapacity(i)).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
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

    public FluidTank getTankInventory() {
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
    public int getTankSize(int tank) {
        return getCapacityMultiplier();
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyFluidTankSize(blocks);
    }

    @Override
    public IFluidTank getTank(int tank) {
        return tankInventory;
    }

    @Override
    public FluidStack getFluid(int tank) {
        return tankInventory.getFluid().copy();
    }
}