package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasConnectivityHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.IGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.IGasTankMultiBlockEntityContainer.iGas;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTank;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver.AirtightAssemblyDriverStructureManager;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class AirtightTankBlockEntity extends SmartBlockEntity implements iGas, IHaveGoggleInformation, IChamberGasTank, ThresholdSwitchObservable {
    private static final int MAX_LENGTH = 4;
    private static final int MAX_WIDTH = 3;
    private static final int SYNC_RATE = 4;

    private static final String COMPOUND_KEY_CORE = "Core";
    private static final String COMPOUND_KEY_UPDATE_CONNECTIVITY = "UpdateConnectivity";
    private static final String COMPOUND_KEY_LAST_KNOWN_POS = "LastKnownPos";
    private static final String COMPOUND_KEY_CONTROLLER_POS = "Controller";
    private static final String COMPOUND_KEY_TANK_CONTENT = "TankContent";
    private static final String COMPOUND_KEY_WIDTH = "Width";
    private static final String COMPOUND_KEY_HEIGHT = "Height";

    private final AirtightAssemblyDriverCore driverCore;

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
        tankInventory = new SmartGasTank(getCapacityPerTank(), this::onGasStackChanged);
        updateConnectivity = false;
        updateCapability = false;
        height = 1;
        width = 1;
        driverCore = new AirtightAssemblyDriverCore();
        refreshCapability();
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_TANK.get(), (be, context) -> {
            if (be.gasCapability == null) {
                be.refreshCapability();
            }
            return be.gasCapability;
        });
    }

    public static long getCapacityPerTank() {
        return CCBConfig.server().airtights.maxCanisterCapacity.get() * 4000L;
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
        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync) {
                sendData();
            }
        }

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
        if (updateConnectivity) {
            updateConnectivity();
        }
        if (!isController()) {
            return;
        }

        driverCore.tick(this);
    }

    @Override
    public void write(@NotNull CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_CORE, driverCore.write(provider, clientPacket));
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
        if (lastKnownPos != null) {
            compoundTag.put(COMPOUND_KEY_LAST_KNOWN_POS, NbtUtils.writeBlockPos(lastKnownPos));
        }
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
        BlockPos controllerBefore = controllerPos;
        int previousSize = width;
        int previousHeight = height;
        if (!clientPacket) {
            updateConnectivity = compoundTag.getBoolean(COMPOUND_KEY_UPDATE_CONNECTIVITY);
            lastKnownPos = compoundTag.contains(COMPOUND_KEY_LAST_KNOWN_POS) ? NBTHelper.readBlockPos(compoundTag, COMPOUND_KEY_LAST_KNOWN_POS) : null;
        }
        controllerPos = compoundTag.contains(COMPOUND_KEY_CONTROLLER_POS) ? NBTHelper.readBlockPos(compoundTag, COMPOUND_KEY_CONTROLLER_POS) : null;
        if (isController()) {
            if (compoundTag.contains(COMPOUND_KEY_WIDTH)) {
                width = compoundTag.getInt(COMPOUND_KEY_WIDTH);
            }
            if (compoundTag.contains(COMPOUND_KEY_HEIGHT)) {
                height = compoundTag.getInt(COMPOUND_KEY_HEIGHT);
            }
            tankInventory.setCapacity(getTotalTankSize() * getCapacityPerTank());
            if (compoundTag.contains(COMPOUND_KEY_TANK_CONTENT)) {
                tankInventory.read(provider, compoundTag.getCompound(COMPOUND_KEY_TANK_CONTENT));
            }
        }

        if (compoundTag.contains(COMPOUND_KEY_CORE)) {
            driverCore.read(compoundTag.getCompound(COMPOUND_KEY_CORE), provider, clientPacket);
        }
        updateCapability = true;
        if (!clientPacket) {
            return;
        }

        boolean changed = !Objects.equals(controllerBefore, controllerPos);
        if (!changed && previousSize == width && previousHeight == height) {
            return;
        }

        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_KNOWN_SHAPE);
        }
        if (isController()) {
            tankInventory.setCapacity(getCapacityPerTank() * getTotalTankSize());
        }
        invalidateRenderBoundingBox();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    public int getTotalTankSize() {
        return width * width * height;
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

        GasConnectivityHandler.formMulti(this);
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    protected void onGasStackChanged(GasStack newStack) {
        if (!isController() || level == null || level.isClientSide) {
            return;
        }

        notifyUpdate();
    }

    public void updateTankState() {
        if (level == null || !isController()) {
            return;
        }

        AirtightAssemblyDriverStructureManager structureManager = driverCore.getStructureManager();
        boolean wasActive = structureManager.isActive();
        boolean changed = structureManager.evaluate(this);
        if (wasActive != structureManager.isActive()) {
            for (int yOffset = 0; yOffset < height; yOffset++) {
                for (int xOffset = 0; xOffset < width; xOffset++) {
                    for (int zOffset = 0; zOffset < width; zOffset++) {
                        if (!(level.getBlockEntity(worldPosition.offset(xOffset, yOffset, zOffset)) instanceof AirtightTankBlockEntity tank)) {
                            continue;
                        }

                        tank.refreshCapability();
                    }
                }
            }
        }
        if (!changed) {
            return;
        }

        notifyUpdate();
    }

    private void refreshCapability() {
        gasCapability = handlerForCapability();
        invalidateCapabilities();
    }

    private IGasHandler handlerForCapability() {
        if (isController()) {
            return driverCore.getStructureManager().isActive() ? driverCore.createGasHandler() : tankInventory;
        }

        return getControllerBE() != null ? getControllerBE().handlerForCapability() : new GasTank(0);
    }

    public AirtightAssemblyDriverCore getCore() {
        return driverCore;
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

        return level.getBlockEntity(controllerPos) instanceof AirtightTankBlockEntity tank ? tank : null;
    }

    @Override
    public boolean isController() {
        return controllerPos == null || worldPosition.getX() == controllerPos.getX() && worldPosition.getY() == controllerPos.getY() && worldPosition.getZ() == controllerPos.getZ();
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
        applyGasTankSize(1);
        controllerPos = null;
        width = 1;
        height = 1;
        driverCore.reset();
        BlockState state = getBlockState();
        if (state.getBlock() instanceof AirtightTankBlock) {
            state = state.setValue(AirtightTankBlock.TOP, true).setValue(AirtightTankBlock.BOTTOM, true);
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
        if (state.getBlock() instanceof AirtightTankBlock) {
            int controllerPosY = getController().getY();
            int posY = getBlockPos().getY();
            state = state.setValue(AirtightTankBlock.BOTTOM, controllerPosY == posY);
            state = state.setValue(AirtightTankBlock.TOP, controllerPosY + height - 1 == posY);
            level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
        }
        onGasStackChanged(tankInventory.getGasStack());
        updateTankState();
        setChanged();
    }

    @Override
    public Axis getMainConnectionAxis() {
        return Axis.Y;
    }

    @Override
    public int getMaxLength(Axis longAxis, int width) {
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
        return isController() ? super.createRenderBoundingBox().expandTowards(width - 1, height - 1, width - 1) : super.createRenderBoundingBox();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        AirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return false;
        }

        AirtightAssemblyDriverCore core = controller.driverCore;
        if (core.getStructureManager().isActive() && core.addToGoggleTooltip(tooltip)) {
            return true;
        }

        IGasHandler handler = controller.gasCapability;
        if (handler == null) {
            return false;
        }

        CCBLang.translate("gui.goggles.gas_container").forGoggles(tooltip);
        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        GasStack gasStack = handler.getGasInTank(0);
        long capacity = handler.getTankCapacity(0);
        if (gasStack.isEmpty()) {
            CCBLang.translate("gui.goggles.gas_container.capacity").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.gasName(gasStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.number(gasStack.getAmount()).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }
        return true;
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
        return tankInventory.getGasStack().copy();
    }

    public void applyGasTankSize(int blocks) {
        tankInventory.setCapacity((long) blocks * getCapacityPerTank());
        long overflow = tankInventory.getGasAmount() - tankInventory.getCapacity();
        if (overflow <= 0) {
            return;
        }

        tankInventory.drain(overflow, GasAction.EXECUTE);
    }

    @Override
    public int getMaxValue() {
        AirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return 0;
        }

        IGasHandler gasHandler = controller.gasCapability;
        return (int) gasHandler.getTankCapacity(0) / 1000;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        AirtightTankBlockEntity controller = getControllerBE();
        if (controller == null || controller.driverCore.getStructureManager().isActive()) {
            return 0;
        }

        IGasHandler gasHandler = controller.gasCapability;
        long amount = 0;
        for (int i = 0; i < gasHandler.getTanks(); i++) {
            GasStack stack = gasHandler.getGasInTank(i);
            if (stack.isEmpty()) {
                continue;
            }

            amount += stack.getAmount();
        }
        return (int) amount / 1000;
    }

    @Override
    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.buckets")).component();
    }
}