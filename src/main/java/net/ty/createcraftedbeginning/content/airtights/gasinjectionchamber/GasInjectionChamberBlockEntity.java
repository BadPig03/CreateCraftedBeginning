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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
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
    static final int NOZZLE_TIME = 15;
    static final int NOZZLE_PART_TIME = 15;
    static final int NOZZLE_IDLE_TIME = 5;
    static final int PROCESSING_TIME = 60;
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
        GasInjectionChamberVisualState visualState = new GasInjectionChamberVisualState();
        operation = new GasInjectionChamberOperationState();
        filter = new GasInjectionChamberFilterState();
        display = new GasInjectionChamberDisplay(this, operation);
        GasInjectionChamberOperationPlanner operationPlanner = new GasInjectionChamberOperationPlanner(this, filter);
        GasInjectionChamberBeltProcessor beltProcessor = new GasInjectionChamberBeltProcessor(this, operation, filter, visualState, operationPlanner);
        GasInjectionChamberBasinProcessor basinProcessor = new GasInjectionChamberBasinProcessor(this, operation);
        controller = new GasInjectionChamberController(this, operation, beltProcessor, basinProcessor, visualState);
        serialization = new GasInjectionChamberSerialization(this, operation, filter, visualState, display);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.GAS_INJECTION_CHAMBER.get(), (blockEntity, direction) -> direction == Direction.UP ? blockEntity.exposedGasHandler : null);
    }

    private static long getMaxCapacity() {
        return CCBConfig.server().airtights.maxGasInjectionChamberCapacity.get() * GasAmounts.MILLIBUCKETS_PER_BUCKET;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, getMaxCapacity()).whenGasUpdates(this::scheduleBasinCheck);
        exposedGasHandler = tankBehaviour.getCapability();
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
    public void lazyTick() {
        super.lazyTick();
        scheduleBasinCheck();
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

    float getRenderedProcessingTicks(float partialTicks) {
        return display.getRenderedProcessingTicks(partialTicks);
    }

    boolean hasInstalledFilter() {
        return filter.hasInstalledFilter();
    }

    ItemStack getInstalledFilter() {
        return filter.getInstalledFilter();
    }

    boolean isFilterLocked() {
        return level != null && level.isClientSide ? filter.isClientLocked() : operation.type == FAN_PROCESSING;
    }

    boolean installFilter(ItemStack stack) {
        if (!filter.install(stack)) {
            return false;
        }

        setChanged();
        notifyUpdate();
        return true;
    }

    ItemStack removeInstalledFilter() {
        if (!filter.hasInstalledFilter() || isFilterLocked()) {
            return ItemStack.EMPTY;
        }

        ItemStack removedFilter = filter.remove();
        setChanged();
        notifyUpdate();
        return removedFilter;
    }

    void scheduleBasinCheck() {
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
        operation.clearTransientOperation();
        filter.setClientLocked(false);
    }

    private ProcessingResult onItemEntered(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return controller.onItemEntered(transported, handler);
    }

    private ProcessingResult onItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return controller.onItemHeld(transported, handler);
    }
}
