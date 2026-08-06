package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.SmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightTankBlockEntity extends AbstractAirtightTankBlockEntity implements IHaveGoggleInformation, IChamberGasTank, ThresholdSwitchObservable {
    private static final String COMPOUND_KEY_CORE = "Core";

    private final AirtightAssemblyDriverCore driverCore;

    public AirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        driverCore = new AirtightAssemblyDriverCore();
        initializeTank(new SmartGasTank(getCapacityPerTank(), this::onGasStackChanged));
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_TANK.get(), (be, context) -> {
            if (be.gasCapability == null) {
                be.refreshCapability();
            }
            return be.gasCapability;
        });
    }

    public static long getCapacityPerTank() {
        return CCBConfig.server().airtights.maxAirtightTankCapacityPerBlock.get() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    public static int getConfiguredMaxLength() {
        return configuredMaxLength();
    }

    public static int getConfiguredMaxWidth() {
        return configuredMaxWidth();
    }

    public static BlockPos offsetInMulti(BlockPos origin, Axis axis, int lengthOffset, int uOffset, int vOffset) {
        return switch (axis) {
            case X -> origin.offset(lengthOffset, uOffset, vOffset);
            case Y -> origin.offset(uOffset, lengthOffset, vOffset);
            case Z -> origin.offset(uOffset, vOffset, lengthOffset);
        };
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (isController()) {
            compoundTag.put(COMPOUND_KEY_CORE, driverCore.write(provider, clientPacket));
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

        if (compoundTag.contains(COMPOUND_KEY_CORE)) {
            driverCore.read(compoundTag.getCompound(COMPOUND_KEY_CORE), provider, clientPacket);
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

    private void readServerData(CompoundTag compoundTag) {
        updateConnectivity = compoundTag.getBoolean(COMPOUND_KEY_UPDATE_CONNECTIVITY);
        lastKnownPos = readOptionalBlockPos(compoundTag, COMPOUND_KEY_LAST_KNOWN_POS);
    }

    private void readControllerData(CompoundTag compoundTag, Provider provider) {
        width = readDimension(compoundTag, COMPOUND_KEY_WIDTH, getConfiguredMaxWidth());
        height = readDimension(compoundTag, COMPOUND_KEY_HEIGHT, getConfiguredMaxLength());
        tankInventory.setCapacity((long) getTotalTankSize() * getCapacityPerTank());
        if (!compoundTag.contains(COMPOUND_KEY_TANK_CONTENT)) {
            return;
        }

        tankInventory.read(provider, compoundTag.getCompound(COMPOUND_KEY_TANK_CONTENT));
        drainOverflow();
    }

    private void updateClientState() {
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_KNOWN_SHAPE);
        }
        if (isController()) {
            tankInventory.setCapacity(getCapacityPerTank() * getTotalTankSize());
        }
        invalidateRenderBoundingBox();
    }

    public int getTotalTankSize() {
        return width * width * height;
    }

    public void updateTankState() {
        if (level == null || level.isClientSide || !isController()) {
            return;
        }

        driverCore.getStructureManager().requestEvaluation();
    }

    @Override
    protected void tickController() {
        driverCore.tick(this);
    }

    @Override
    protected void updateMultiBlockState() {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof AirtightTankBlock)) {
            return;
        }

        Axis axis = getMainConnectionAxis();
        int controllerCoords = calculateCoords(getController(), axis);
        int posCoords = calculateCoords(getBlockPos(), axis);
        state = state.setValue(AirtightTankBlock.BOTTOM, controllerCoords == posCoords);
        state = state.setValue(AirtightTankBlock.TOP, controllerCoords + height - 1 == posCoords);
        level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
    }

    @Override
    protected void afterMultiUpdated() {
        updateTankState();
    }

    @Override
    protected long capacityPerBlock() {
        return getCapacityPerTank();
    }

    public AirtightAssemblyDriverCore getCore() {
        return driverCore;
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
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        AirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return false;
        }

        AirtightAssemblyDriverCore core = controller.driverCore;
        if (core.getStructureManager().isAssembled() && core.addToGoggleTooltip(tooltip)) {
            tooltip.add(Component.empty());
        }

        IGasHandler handler = controller.tankInventory;
        if (handler == null) {
            return false;
        }

        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gasStack = handler.getGasInTank(0);
        long capacity = handler.getTankCapacity(0);
        if (gasStack.isEmpty()) {
            CCBLang.translate("gui.gas_container.capacity").add(GasAmountUtils.precise(capacity).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.gasName(gasStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        GasAmountUtils.precise(gasStack.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmountUtils.precise(capacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyGasTankSize(blocks);
    }

    public void applyGasTankSize(int blocks) {
        tankInventory.setCapacity((long) blocks * getCapacityPerTank());
        drainOverflow();
    }

    private void drainOverflow() {
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
        return GasAmountUtils.toWholeBucketsClamped(controller.gasCapability.getTankCapacity(0));
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        AirtightTankBlockEntity controller = getControllerBE();
        if (controller == null) {
            return 0;
        }

        IGasHandler handler = controller.gasCapability;
        long amount = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            GasStack stack = handler.getGasInTank(i);
            if (stack.isEmpty()) {
                continue;
            }

            amount += stack.getAmount();
        }
        return GasAmountUtils.toWholeBucketsClamped(amount);
    }

    @Override
    public MutableComponent format(int value) {
        return GasAmountUtils.formatWholeBuckets(value);
    }
}