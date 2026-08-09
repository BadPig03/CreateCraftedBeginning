package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.MultiFace;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.FAN_PROCESSING;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ThresholdSwitchObservable, IGasInventoryIdentifierProvider {
    public static final int NOZZLE_TIME = 15;
    public static final int NOZZLE_PART_TIME = 15;
    public static final int NOZZLE_IDLE_TIME = 5;
    public static final int PROCESSING_TIME = 60;
    static final int INJECTION_EXECUTION_TICK = PROCESSING_TIME - NOZZLE_TIME - NOZZLE_PART_TIME - NOZZLE_IDLE_TIME;

    private final GasInjectionChamberOperationState operation;
    private final GasInjectionChamberFilterState filter;
    private final GasInjectionChamberDisplay display;
    private final GasInjectionChamberController controller;
    private final GasInjectionChamberSerialization serialization;

    private SmartGasTankBehaviour tankBehaviour;
    private IGasHandler exposedGasHandler;
    private boolean basinCheckScheduled = true;

    public GasInjectionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        GasInjectionChamberVisualState visual = new GasInjectionChamberVisualState();
        operation = new GasInjectionChamberOperationState();
        filter = new GasInjectionChamberFilterState();
        display = new GasInjectionChamberDisplay(this, operation);
        GasInjectionChamberOperationPlanner operationPlanner = new GasInjectionChamberOperationPlanner(this, operation, filter);
        GasInjectionChamberBeltProcessor beltProcessor = new GasInjectionChamberBeltProcessor(this, operation, filter, visual, operationPlanner);
        GasInjectionChamberBasinProcessor basinProcessor = new GasInjectionChamberBasinProcessor(this, operation);
        controller = new GasInjectionChamberController(this, operation, beltProcessor, basinProcessor, visual);
        serialization = new GasInjectionChamberSerialization(this, operation, filter, visual, display);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.GAS_INJECTION_CHAMBER.get(), (blockEntity, direction) -> direction == Direction.UP ? blockEntity.exposedGasHandler : null);
    }

    public static long getMaxCapacity() {
        return CCBConfig.server().airtights.maxGasInjectionChamberCapacity.get() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, getMaxCapacity()).whenGasUpdates(this::scheduleBasinCheck);
        exposedGasHandler = new GasInjectionChamberGasHandler(tankBehaviour.getCapability(), this::isOperationGasLocked, this::getOperationGas);
        BeltProcessingBehaviour beltProcessing = new BeltProcessingBehaviour(this).whenItemEnters(this::onItemEntered).whileItemHeld(this::onItemHeld);
        behaviours.add(tankBehaviour);
        behaviours.add(beltProcessing);
    }

    @Override
    public void tick() {
        super.tick();
        controller.tick();
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
    public void destroy() {
        super.destroy();
        if (level == null || level.isClientSide || !filter.hasInstalledFilter()) {
            return;
        }

        Block.popResource(level, worldPosition, filter.remove());
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToGoggleTooltip(tooltip);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().expandTowards(0, -2, 0);
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
    public @Nullable InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        if (direction != Direction.UP) {
            return null;
        }
        return new MultiFace(worldPosition, Set.of(Direction.UP));
    }

    public float getRenderedProcessingTicks(float partialTicks) {
        return display.getRenderedProcessingTicks(partialTicks);
    }

    public boolean hasInstalledFilter() {
        return filter.hasInstalledFilter();
    }

    public ItemStack getInstalledFilter() {
        return filter.getInstalledFilter();
    }

    public boolean isFilterLocked() {
        return level != null && level.isClientSide ? filter.isClientLocked() : operation.type == FAN_PROCESSING;
    }

    public boolean installFilter(ItemStack stack) {
        if (!filter.install(stack)) {
            return false;
        }

        setChanged();
        notifyUpdate();
        return true;
    }

    public ItemStack removeInstalledFilter() {
        if (!filter.hasInstalledFilter() || isFilterLocked()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = filter.remove();
        setChanged();
        notifyUpdate();
        return removed;
    }

    public void scheduleBasinCheck() {
        if (level != null && level.isClientSide) {
            return;
        }

        basinCheckScheduled = true;
    }

    boolean consumeBasinCheckScheduled() {
        if (!basinCheckScheduled) {
            return false;
        }

        basinCheckScheduled = false;
        return true;
    }

    SmartGasTankBehaviour getGasTankBehaviour() {
        return tankBehaviour;
    }

    IGasTank getGasTank() {
        return tankBehaviour.getPrimaryHandler();
    }

    GasStack getGasInTank() {
        return getGasTank().getGasStack();
    }

    void clearOperationState() {
        operation.clear();
        filter.setClientLocked(false);
    }

    void cancelOperationState() {
        operation.setProcessingTicks(-1);
        clearOperationState();
        notifyUpdate();
    }

    boolean isFanProcessingOperationStillValid(ResourceLocation typeId) {
        return controller.isFanProcessingOperationStillValid(typeId);
    }

    private boolean isOperationGasLocked() {
        return operation.isGasLocked();
    }

    private GasStack getOperationGas() {
        return operation.gas;
    }

    private ProcessingResult onItemEntered(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return controller.onItemEntered(transported, handler);
    }

    private ProcessingResult onItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return controller.onItemHeld(transported, handler);
    }
}
