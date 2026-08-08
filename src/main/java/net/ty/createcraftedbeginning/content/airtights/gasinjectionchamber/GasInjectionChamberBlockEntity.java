package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.MultiFace;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer.MachineFillingStrategy;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.content.airtights.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.content.airtights.transaction.MachineResourceSnapshots.FluidTankSnapshot;
import net.ty.createcraftedbeginning.content.airtights.transaction.MachineResourceSnapshots.GasTankSnapshot;
import net.ty.createcraftedbeginning.content.particles.ColoredBreezeCloudParticleType.ColoredBreezeCloudParticleOptions;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction.Participant;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe.RecipeMatch;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.HOLD;
import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.PASS;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ThresholdSwitchObservable, IGasInventoryIdentifierProvider {
    public static final int NOZZLE_TIME = 15;
    public static final int NOZZLE_PART_TIME = 15;
    public static final int NOZZLE_IDLE_TIME = 5;
    public static final int PROCESSING_TIME = 60;
    private static final int INJECTION_EXECUTION_TICK = PROCESSING_TIME - NOZZLE_TIME - NOZZLE_PART_TIME - NOZZLE_IDLE_TIME;

    private static final String COMPOUND_KEY_PROCESSING_TICKS = "ProcessingTicks";
    private static final String COMPOUND_KEY_OPERATION_TYPE = "OperationType";
    private static final String COMPOUND_KEY_OPERATION_GAS = "OperationGas";
    private static final String COMPOUND_KEY_OPERATION_FAN_PROCESSING_TYPE = "OperationFanProcessingType";
    private static final String COMPOUND_KEY_OPERATION_INPUT = "OperationInput";
    private static final String COMPOUND_KEY_OPERATION_RESULTS = "OperationResults";
    private static final String COMPOUND_KEY_OPERATION_FLUID_INPUTS = "OperationFluidInputs";
    private static final String COMPOUND_KEY_OPERATION_FLUID_RESULT = "OperationFluidResult";
    private static final String COMPOUND_KEY_OPERATION_RESULT_PREPARED = "OperationResultPrepared";
    private static final String COMPOUND_KEY_OPERATION_EXECUTED = "OperationExecuted";
    private static final String COMPOUND_KEY_FILTER_LOCKED = "FilterLocked";
    private static final String COMPOUND_KEY_CLOUD = "Cloud";
    private static final String COMPOUND_KEY_CLOUD_COLOR = "CloudColor";
    private static final String COMPOUND_KEY_INSTALLED_FILTER = "InstalledFilter";

    private final List<ItemStack> operationResults = new ArrayList<>();
    private final List<FluidStack> operationFluidInputs = new ArrayList<>();
    private int cloudColor = 0xFFFFFFFF;
    private int processingTicks = -1;
    private int previousProcessingTicks = -1;
    private boolean sendCloud;
    private boolean operationExecuted;
    private boolean clientFilterLocked;
    private boolean basinCheckScheduled = true;
    private OperationType operationType = OperationType.NONE;
    private GasStack operationGas = GasStack.EMPTY;
    private FluidStack operationFluidResult = FluidStack.EMPTY;
    private @Nullable ResourceLocation operationFanProcessingTypeId;
    private ItemStack operationInput = ItemStack.EMPTY;
    private ItemStack installedFilter = ItemStack.EMPTY;
    private boolean operationResultPrepared;
    private @Nullable GasInjectionRecipe operationRecipe;
    private SmartGasTankBehaviour tankBehaviour;
    private IGasHandler exposedGasHandler;

    public GasInjectionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.GAS_INJECTION_CHAMBER.get(), (blockEntity, direction) -> direction == Direction.UP ? blockEntity.exposedGasHandler : null);
    }

    public static long getMaxCapacity() {
        return CCBConfig.server().airtights.maxGasInjectionChamberCapacity.get() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    private static void addResultStack(List<ItemStack> results, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack remaining = stack.copy();
        for (ItemStack existing : results) {
            if (!ItemStack.isSameItemSameComponents(existing, remaining)) {
                continue;
            }

            int space = existing.getMaxStackSize() - existing.getCount();
            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
            if (remaining.isEmpty()) {
                return;
            }
        }

        while (!remaining.isEmpty()) {
            int count = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            results.add(remaining.split(count));
        }
    }

    private static @Nullable List<FluidStack> createFluidDrainPlan(SizedFluidIngredient ingredient, IFluidHandler fluids) {
        int remaining = ingredient.amount();
        if (remaining <= 0) {
            return null;
        }

        List<FluidStack> plan = new ArrayList<>();
        for (int tank = 0; tank < fluids.getTanks() && remaining > 0; tank++) {
            FluidStack stack = fluids.getFluidInTank(tank);
            if (stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }

            int amount = Math.min(remaining, stack.getAmount());
            if (amount <= 0) {
                continue;
            }

            FluidStack request = stack.copyWithAmount(amount);
            boolean merged = false;
            for (FluidStack planned : plan) {
                if (!FluidStack.isSameFluidSameComponents(planned, request)) {
                    continue;
                }
                planned.setAmount(planned.getAmount() + amount);
                merged = true;
                break;
            }
            if (!merged) {
                plan.add(request);
            }
            remaining -= amount;
        }
        return remaining == 0 ? plan : null;
    }

    private static boolean canDrainFluids(IFluidHandler fluids, List<FluidStack> plan) {
        for (FluidStack request : plan) {
            FluidStack drained = fluids.drain(request, FluidAction.SIMULATE);
            if (drained.getAmount() != request.getAmount() || !FluidStack.isSameFluidSameComponents(drained, request)) {
                return false;
            }
        }
        return true;
    }

    private static BasinFluidSnapshot snapshotBasinFluids(BasinBlockEntity basin, SmartFluidTankBehaviour outputTank, BasinTransactionAccess transactionAccess, Provider provider) {
        List<FluidStack> spoutputBuffer = transactionAccess.ccb$getTransactionFluidOverflow().stream().map(FluidStack::copy).toList();
        return new BasinFluidSnapshot(MachineResourceSnapshots.snapshotFluidTanks(provider, basin.inputTank, outputTank), spoutputBuffer);
    }

    private static void restoreBasinFluids(BasinBlockEntity basin, SmartFluidTankBehaviour outputTank, BasinTransactionAccess transactionAccess, Provider provider, BasinFluidSnapshot snapshot) {
        MachineResourceSnapshots.restoreFluidTanks(provider, snapshot.tanks(), basin.inputTank, outputTank);
        List<FluidStack> spoutputBuffer = transactionAccess.ccb$getTransactionFluidOverflow();
        spoutputBuffer.clear();
        snapshot.spoutputBuffer().stream().map(FluidStack::copy).forEach(spoutputBuffer::add);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, getMaxCapacity()).whenGasUpdates(this::scheduleBasinCheck);
        exposedGasHandler = new OperationLockingGasHandler(tankBehaviour.getCapability());
        BeltProcessingBehaviour beltProcessing = new BeltProcessingBehaviour(this).whenItemEnters(this::onItemEntered).whileItemHeld(this::onItemHeld);
        behaviours.add(tankBehaviour);
        behaviours.add(beltProcessing);
    }

    @Override
    public void tick() {
        super.tick();
        previousProcessingTicks = processingTicks;
        if (level == null) {
            return;
        }

        if (!level.isClientSide && processingTicks < 0 && operationType == OperationType.NONE && basinCheckScheduled) {
            basinCheckScheduled = false;
            tryStartBasinOperation();
        }

        if (processingTicks < 0) {
            return;
        }

        processingTicks--;
        if (!level.isClientSide && operationType == OperationType.BASIN_RECIPE && !operationExecuted && processingTicks <= INJECTION_EXECUTION_TICK) {
            executeBasinInjection();
        }
        if (processingTicks >= 0) {
            return;
        }

        clearOperation();
        setChanged();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putInt(COMPOUND_KEY_PROCESSING_TICKS, processingTicks);
        if (!installedFilter.isEmpty()) {
            compoundTag.put(COMPOUND_KEY_INSTALLED_FILTER, installedFilter.saveOptional(provider));
        }

        if (clientPacket) {
            compoundTag.putBoolean(COMPOUND_KEY_FILTER_LOCKED, operationType == OperationType.FAN_PROCESSING);
        }
        else {
            writeOperation(compoundTag, provider);
        }
        writeCloud(compoundTag, clientPacket);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (compoundTag.contains(COMPOUND_KEY_PROCESSING_TICKS)) {
            int synchronizedTicks = compoundTag.getInt(COMPOUND_KEY_PROCESSING_TICKS);

            // The client advances this timer locally for smooth rendering. Mid-animation
            // update packets may contain an older positive value and must not rewind it.
            if (!clientPacket || processingTicks < 0 || synchronizedTicks < 0) {
                processingTicks = synchronizedTicks;
                previousProcessingTicks = synchronizedTicks;
            }
        }

        installedFilter = compoundTag.contains(COMPOUND_KEY_INSTALLED_FILTER) ? ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_INSTALLED_FILTER)) : ItemStack.EMPTY;
        if (!GasInjectionChamberUtils.isFilter(installedFilter)) {
            installedFilter = ItemStack.EMPTY;
        }

        if (clientPacket) {
            clientFilterLocked = compoundTag.getBoolean(COMPOUND_KEY_FILTER_LOCKED);
        }
        else {
            readOperation(compoundTag, provider);
        }
        readCloud(compoundTag, clientPacket);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level == null || level.isClientSide || installedFilter.isEmpty()) {
            return;
        }

        Block.popResource(level, worldPosition, installedFilter);
        installedFilter = ItemStack.EMPTY;
    }

    private void writeOperation(CompoundTag tag, Provider provider) {
        if (operationType == OperationType.NONE) {
            return;
        }

        tag.putString(COMPOUND_KEY_OPERATION_TYPE, operationType.serializedName);
        if (!operationGas.isEmpty()) {
            tag.put(COMPOUND_KEY_OPERATION_GAS, operationGas.saveOptional(provider));
        }
        if (operationFanProcessingTypeId != null) {
            tag.putString(COMPOUND_KEY_OPERATION_FAN_PROCESSING_TYPE, operationFanProcessingTypeId.toString());
        }
        tag.put(COMPOUND_KEY_OPERATION_INPUT, operationInput.saveOptional(provider));
        if (!operationFluidInputs.isEmpty()) {
            ListTag fluidInputs = new ListTag();
            for (FluidStack fluidInput : operationFluidInputs) {
                fluidInputs.add(fluidInput.saveOptional(provider));
            }
            tag.put(COMPOUND_KEY_OPERATION_FLUID_INPUTS, fluidInputs);
        }
        if (!operationFluidResult.isEmpty()) {
            tag.put(COMPOUND_KEY_OPERATION_FLUID_RESULT, operationFluidResult.saveOptional(provider));
        }
        tag.putBoolean(COMPOUND_KEY_OPERATION_RESULT_PREPARED, operationResultPrepared);
        if (operationResultPrepared) {
            ListTag results = new ListTag();
            for (ItemStack result : operationResults) {
                results.add(result.saveOptional(provider));
            }
            tag.put(COMPOUND_KEY_OPERATION_RESULTS, results);
        }
        tag.putBoolean(COMPOUND_KEY_OPERATION_EXECUTED, operationExecuted);
    }

    private void writeCloud(CompoundTag tag, boolean clientPacket) {
        if (!sendCloud || !clientPacket) {
            return;
        }

        tag.putBoolean(COMPOUND_KEY_CLOUD, true);
        tag.putInt(COMPOUND_KEY_CLOUD_COLOR, cloudColor);
        sendCloud = false;
    }

    private void readOperation(CompoundTag tag, Provider provider) {
        operationRecipe = null;
        operationType = OperationType.byName(tag.getString(COMPOUND_KEY_OPERATION_TYPE));
        operationGas = tag.contains(COMPOUND_KEY_OPERATION_GAS) ? GasStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_OPERATION_GAS)) : GasStack.EMPTY;
        operationFanProcessingTypeId = tag.contains(COMPOUND_KEY_OPERATION_FAN_PROCESSING_TYPE) ? ResourceLocation.tryParse(tag.getString(COMPOUND_KEY_OPERATION_FAN_PROCESSING_TYPE)) : null;
        operationInput = tag.contains(COMPOUND_KEY_OPERATION_INPUT) ? ItemStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_OPERATION_INPUT)) : ItemStack.EMPTY;
        operationFluidInputs.clear();
        if (tag.contains(COMPOUND_KEY_OPERATION_FLUID_INPUTS, Tag.TAG_LIST)) {
            ListTag fluidInputs = tag.getList(COMPOUND_KEY_OPERATION_FLUID_INPUTS, Tag.TAG_COMPOUND);
            for (int i = 0; i < fluidInputs.size(); i++) {
                FluidStack fluidInput = FluidStack.parseOptional(provider, fluidInputs.getCompound(i));
                if (!fluidInput.isEmpty()) {
                    operationFluidInputs.add(fluidInput);
                }
            }
        }
        operationFluidResult = tag.contains(COMPOUND_KEY_OPERATION_FLUID_RESULT) ? FluidStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_OPERATION_FLUID_RESULT)) : FluidStack.EMPTY;
        operationResultPrepared = tag.getBoolean(COMPOUND_KEY_OPERATION_RESULT_PREPARED);
        operationResults.clear();
        if (operationResultPrepared && tag.contains(COMPOUND_KEY_OPERATION_RESULTS, Tag.TAG_LIST)) {
            ListTag results = tag.getList(COMPOUND_KEY_OPERATION_RESULTS, Tag.TAG_COMPOUND);
            for (int i = 0; i < results.size(); i++) {
                ItemStack result = ItemStack.parseOptional(provider, results.getCompound(i));
                if (result.isEmpty()) {
                    continue;
                }

                operationResults.add(result);
            }
        }
        operationExecuted = tag.getBoolean(COMPOUND_KEY_OPERATION_EXECUTED);
        if (isLoadedOperationValid()) {
            return;
        }

        clearOperation();
    }

    private void readCloud(CompoundTag tag, boolean clientPacket) {
        if (!clientPacket || !tag.contains(COMPOUND_KEY_CLOUD)) {
            return;
        }

        int color = tag.contains(COMPOUND_KEY_CLOUD_COLOR) ? tag.getInt(COMPOUND_KEY_CLOUD_COLOR) : 0xFFFFFFFF;
        spawnCloud(color);
    }

    private boolean isLoadedOperationValid() {
        if (operationType == OperationType.NONE) {
            return false;
        }

        if (operationType == OperationType.BASIN_RECIPE) {
            return !operationGas.isEmpty() && !operationFluidInputs.isEmpty() && !operationFluidResult.isEmpty();
        }

        if (operationInput.isEmpty()) {
            return false;
        }

        if (operationType != OperationType.FAN_PROCESSING) {
            return !operationType.usesGas || !operationGas.isEmpty();
        }
        return !operationGas.isEmpty() && operationFanProcessingTypeId != null && GasInjectionChamberUtils.getFanProcessingType(operationFanProcessingTypeId).isPresent() && isFanProcessingOperationStillValid();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        IGasHandler gasHandler = tankBehaviour.getPrimaryHandler();
        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gas = gasHandler.getGasInTank(0);
        if (gas.isEmpty()) {
            CCBLang.translate("gui.gas_container.capacity").add(GasAmountUtils.precise(gasHandler.getTankCapacity(0)).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        GasAmountUtils.precise(gas.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmountUtils.precise(gasHandler.getTankCapacity(0)).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        return true;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().expandTowards(0, -2, 0);
    }

    @Override
    public int getMaxValue() {
        return GasAmountUtils.toMillibucketsClamped(tankBehaviour.getPrimaryHandler().getCapacity());
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return GasAmountUtils.toMillibucketsClamped(tankBehaviour.getPrimaryHandler().getGasAmount());
    }

    @Override
    public MutableComponent format(int value) {
        return GasAmountUtils.precise(value).component();
    }

    @Override
    public @Nullable InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        if (direction != Direction.UP) {
            return null;
        }
        return new MultiFace(worldPosition, Set.of(Direction.UP));
    }

    public float getRenderedProcessingTicks(float partialTicks) {
        if (processingTicks < 0) {
            return -1;
        }

        if (previousProcessingTicks < 0) {
            return processingTicks;
        }
        return Mth.lerp(partialTicks, previousProcessingTicks, processingTicks);
    }

    public boolean hasInstalledFilter() {
        return !installedFilter.isEmpty();
    }

    public ItemStack getInstalledFilter() {
        return installedFilter;
    }

    public Optional<ResourceLocation> getInstalledFilterFanProcessingType() {
        return GasInjectionChamberUtils.getFanProcessingTypeId(installedFilter);
    }

    public boolean isFilterLocked() {
        return level != null && level.isClientSide ? clientFilterLocked : operationType == OperationType.FAN_PROCESSING;
    }

    public boolean installFilter(ItemStack stack) {
        if (hasInstalledFilter() || !GasInjectionChamberUtils.isFilter(stack)) {
            return false;
        }

        installedFilter = stack.copyWithCount(1);
        setChanged();
        notifyUpdate();
        return true;
    }

    public ItemStack removeInstalledFilter() {
        if (!hasInstalledFilter() || isFilterLocked()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = installedFilter;
        installedFilter = ItemStack.EMPTY;
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

    private Optional<BasinBlockEntity> getBasin() {
        if (level == null) {
            return Optional.empty();
        }

        if (level.getBlockEntity(worldPosition.below(2)) instanceof BasinBlockEntity basin) {
            return Optional.of(basin);
        }
        return Optional.empty();
    }

    private void spawnCloud(int color) {
        if (level == null || !level.isClientSide || isVirtual()) {
            return;
        }

        Vec3 cloudPos = VecHelper.getCenterOf(worldPosition).subtract(0, 1.6875, 0);
        int count = level.random.nextInt(3, 6);
        for (int i = 0; i < count; i++) {
            Vec3 velocity = VecHelper.offsetRandomly(Vec3.ZERO, level.random, 0.125f);
            velocity = new Vec3(velocity.x, Math.abs(velocity.y), velocity.z);
            level.addAlwaysVisibleParticle(new ColoredBreezeCloudParticleOptions(color), cloudPos.x, cloudPos.y, cloudPos.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private ProcessingResult onItemEntered(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (handler.blockEntity.isVirtual()) {
            return PASS;
        }

        if (processingTicks >= 0) {
            return HOLD;
        }

        if (wasProcessedByInstalledFilter(transported)) {
            return PASS;
        }

        clearOperation();
        return prepareOperation(transported.stack) ? HOLD : PASS;
    }

    private ProcessingResult onItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (handler.blockEntity.isVirtual() || level == null) {
            return PASS;
        }

        if (processingTicks >= 0 && operationType == OperationType.NONE) {
            return HOLD;
        }

        if (operationExecuted) {
            return HOLD;
        }

        if (operationType == OperationType.BASIN_RECIPE) {
            return HOLD;
        }

        if (operationType == OperationType.NONE && wasProcessedByInstalledFilter(transported)) {
            return PASS;
        }

        if (operationType == OperationType.NONE && !prepareOperation(transported.stack)) {
            return PASS;
        }

        if (!matchesOperationInput(transported.stack)) {
            cancelOperation();
            return PASS;
        }

        if (processingTicks < 0) {
            return startProcessing(transported.stack);
        }

        if (processingTicks > INJECTION_EXECUTION_TICK) {
            return HOLD;
        }
        return executeInjection(transported, handler);
    }

    private ProcessingResult startProcessing(ItemStack itemStack) {
        if (operationType == OperationType.FAN_PROCESSING && !isFanProcessingOperationStillValid() && !reprepareOperation(itemStack)) {
            return PASS;
        }

        if (operationType.usesGas) {
            GasStack tankGas = getGasInTank();
            if (tankGas.isEmpty()) {
                return HOLD;
            }

            if (!GasStack.isSameGasSameComponents(tankGas, operationGas)) {
                if (!reprepareOperation(itemStack)) {
                    return PASS;
                }

                tankGas = getGasInTank();
            }

            if (tankGas.getAmount() < operationGas.getAmount()) {
                return HOLD;
            }
        }

        if (!prepareOperationResultsIfNeeded(itemStack)) {
            cancelOperation();
            return PASS;
        }

        processingTicks = PROCESSING_TIME + NOZZLE_IDLE_TIME;
        notifyUpdate();
        return HOLD;
    }

    private boolean reprepareOperation(ItemStack itemStack) {
        clearOperation();
        return prepareOperation(itemStack);
    }

    private ProcessingResult executeInjection(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (level == null) {
            return PASS;
        }

        int color = getOperationCloudColor();
        boolean executed;
        tankBehaviour.beginMutation();
        try {
            executed = executeOperation(transported, handler);
        } finally {
            tankBehaviour.endMutation();
        }

        if (executed) {
            operationExecuted = true;
            cloudColor = color;
            sendCloud = true;
        }
        else {
            processingTicks = -1;
            clearOperation();
        }

        tankBehaviour.sendDataImmediately();
        if (!executed) {
            return PASS;
        }

        CCBSoundEvents.INJECTING.playOnServer(level, worldPosition, 0.75f, 0.9f + 0.2f * level.random.nextFloat());
        return HOLD;
    }

    private void executeBasinInjection() {
        if (level == null) {
            return;
        }

        int color = getOperationCloudColor();
        boolean executed;
        tankBehaviour.beginMutation();
        try {
            executed = executeBasinRecipeOperation();
        } finally {
            tankBehaviour.endMutation();
        }

        if (executed) {
            operationExecuted = true;
            cloudColor = color;
            sendCloud = true;
        }
        else {
            processingTicks = -1;
            clearOperation();
            basinCheckScheduled = true;
        }

        tankBehaviour.sendDataImmediately();
        if (executed) {
            CCBSoundEvents.INJECTING.playOnServer(level, worldPosition, 0.75f, 0.9f + 0.2f * level.random.nextFloat());
        }
    }

    private boolean executeBasinRecipeOperation() {
        if (level == null) {
            return false;
        }

        Optional<BasinBlockEntity> basinOptional = getBasin();
        if (basinOptional.isEmpty()) {
            return false;
        }

        BasinBlockEntity basin = basinOptional.get();
        if (basin.inputTank == null) {
            return false;
        }

        IFluidHandler fluids = basin.inputTank.getCapability();
        FluidStack result = operationFluidResult.copy();
        if (result.isEmpty() || basin.getFilter() == null || !basin.getFilter().test(result)) {
            return false;
        }

        BasinTransactionAccess transactionAccess = (BasinTransactionAccess) basin;
        SmartFluidTankBehaviour outputTank = transactionAccess.ccb$getTransactionOutputTank();
        if (outputTank == null) {
            return false;
        }

        Provider provider = level.registryAccess();
        ResourceTransaction transaction = new ResourceTransaction().add(operationGasParticipant(provider)).add(ResourceTransaction.participant(() -> canDrainFluids(fluids, operationFluidInputs) && basin.acceptOutputs(List.of(), List.of(result), true), () -> snapshotBasinFluids(basin, outputTank, transactionAccess, provider), () -> consumeBasinFluids(fluids) && basin.acceptOutputs(List.of(), List.of(result), false), snapshot -> restoreBasinFluids(basin, outputTank, transactionAccess, provider, snapshot)));
        if (!transaction.commit()) {
            return false;
        }

        basin.notifyChangeOfContents();
        basin.notifyUpdate();
        return true;
    }

    private Participant<GasTankSnapshot> operationGasParticipant(Provider provider) {
        return ResourceTransaction.participant(() -> !operationGas.isEmpty() && GasStack.matches(getTank().drain(operationGas, GasAction.SIMULATE), operationGas), () -> MachineResourceSnapshots.snapshotGasTanks(provider, tankBehaviour), () -> !operationGas.isEmpty() && GasStack.matches(getTank().drain(operationGas, GasAction.EXECUTE), operationGas), snapshot -> MachineResourceSnapshots.restoreGasTanks(provider, snapshot, tankBehaviour));
    }

    private boolean consumeBasinFluids(IFluidHandler fluids) {
        for (FluidStack request : operationFluidInputs) {
            FluidStack drained = fluids.drain(request, FluidAction.EXECUTE);
            if (drained.getAmount() != request.getAmount() || !FluidStack.isSameFluidSameComponents(drained, request)) {
                return false;
            }
        }
        return true;
    }

    private int getOperationCloudColor() {
        if (operationType == OperationType.FAN_PROCESSING) {
            return GasInjectionChamberUtils.getColor(installedFilter);
        }
        return operationGas.getHint();
    }

    private boolean prepareOperation(ItemStack itemStack) {
        if (level == null) {
            return false;
        }

        GasStack tankGas = getGasInTank();
        return !tankGas.isEmpty() && (prepareCanisterOperation(itemStack, tankGas) || prepareRecipeOperation(itemStack, tankGas) || prepareFanProcessingOperation(itemStack, tankGas));
    }

    private void tryStartBasinOperation() {
        Optional<BasinBlockEntity> basin = getBasin();
        if (basin.isEmpty() || !prepareBasinOperation(basin.get())) {
            return;
        }

        processingTicks = PROCESSING_TIME + NOZZLE_IDLE_TIME;
        notifyUpdate();
    }

    private boolean prepareBasinOperation(BasinBlockEntity basin) {
        if (level == null || basin.inputTank == null) {
            return false;
        }

        GasStack tankGas = getGasInTank();
        if (tankGas.isEmpty()) {
            return false;
        }

        IFluidHandler fluids = basin.inputTank.getCapability();
        Optional<RecipeMatch> recipeMatch = GasInjectionRecipe.findFluidRecipeMatch(level, fluids, tankGas);
        if (recipeMatch.isEmpty()) {
            return false;
        }

        GasInjectionRecipe recipe = recipeMatch.get().recipe();
        long requiredGas = recipe.getGasIngredient().amount();
        if (requiredGas <= 0 || tankGas.getAmount() < requiredGas) {
            return false;
        }

        List<FluidStack> fluidDrainPlan = createFluidDrainPlan(recipe.getFluidIngredient(), fluids);
        if (fluidDrainPlan == null || !canDrainFluids(fluids, fluidDrainPlan)) {
            return false;
        }

        FluidStack result = recipe.getFluidResult().copy();
        if (result.isEmpty() || basin.getFilter() == null || !basin.getFilter().test(result) || !basin.acceptOutputs(List.of(), List.of(result), true)) {
            return false;
        }

        setBasinOperation(tankGas, requiredGas, fluidDrainPlan, result);
        return true;
    }

    private void setBasinOperation(GasStack gas, long requiredAmount, List<FluidStack> fluidInputs, FluidStack result) {
        operationType = OperationType.BASIN_RECIPE;
        operationInput = ItemStack.EMPTY;
        operationGas = gas.copyWithAmount(requiredAmount);
        operationFluidInputs.clear();
        fluidInputs.forEach(fluidInput -> operationFluidInputs.add(fluidInput.copy()));
        operationFluidResult = result.copy();
        operationFanProcessingTypeId = null;
        operationRecipe = null;
        operationResults.clear();
        operationResultPrepared = true;
        operationExecuted = false;
        setChanged();
    }

    private boolean prepareCanisterOperation(ItemStack itemStack, GasStack tankGas) {
        IGasCanisterContainer canister = itemStack.getCapability(GasHandler.ITEM);
        if (canister == null) {
            return false;
        }

        long amount = GasCanisterUtils.getInjectableAmount(canister, tankGas, getTank().getCapacity());
        if (amount <= 0) {
            return false;
        }

        setOperation(OperationType.CANISTER, itemStack, 1, tankGas, amount, null, null);
        return true;
    }

    private boolean prepareRecipeOperation(ItemStack itemStack, GasStack tankGas) {
        if (level == null) {
            return false;
        }

        Optional<RecipeMatch> recipeMatch = GasInjectionRecipe.findRecipeMatch(level, itemStack, tankGas);
        if (recipeMatch.isEmpty()) {
            return false;
        }

        RecipeMatch match = recipeMatch.get();
        long gasPerItem = match.recipe().getGasIngredient().amount();
        int batchSize = getRecipeBatchSize(itemStack, gasPerItem);
        if (batchSize <= 0) {
            return false;
        }

        setOperation(OperationType.ITEM_RECIPE, itemStack, batchSize, tankGas, gasPerItem * batchSize, match.sequencedAssembly() ? null : match.recipe(), null);
        return true;
    }

    private boolean wasProcessedByInstalledFilter(TransportedItemStack transported) {
        return transported.processedBy != null && transported.processingTime == -1 && getInstalledFilterFanProcessingType().flatMap(GasInjectionChamberUtils::getFanProcessingType).filter(type -> type == transported.processedBy).isPresent();
    }

    private boolean prepareFanProcessingOperation(ItemStack itemStack, GasStack tankGas) {
        if (level == null || itemStack.isEmpty() || tankGas.isEmpty()) {
            return false;
        }

        Optional<ResourceLocation> typeId = getInstalledFilterFanProcessingType();
        if (typeId.isEmpty()) {
            return false;
        }

        Optional<FanProcessingType> processingType = GasInjectionChamberUtils.getFanProcessingType(typeId.get());
        if (processingType.isEmpty() || !processingType.get().canProcess(itemStack, level)) {
            return false;
        }

        int desiredCount = Math.min(itemStack.getCount(), itemStack.getMaxStackSize());
        int batchSize = GasInjectionChamberUtils.getMaxFanProcessingBatchSize(tankGas, desiredCount);
        if (batchSize <= 0) {
            return false;
        }

        long gasCost = GasInjectionChamberUtils.getFanProcessingGasCost(tankGas, batchSize);
        long gasAmount = gasCost == 0 ? 1 : gasCost;
        setOperation(OperationType.FAN_PROCESSING, itemStack, batchSize, tankGas, gasAmount, null, typeId.get());
        return true;
    }

    private int getRecipeBatchSize(ItemStack input, long gasPerItem) {
        if (gasPerItem <= 0) {
            return 0;
        }

        int desiredCount = Math.min(input.getCount(), input.getMaxStackSize());
        return Math.clamp(getTank().getCapacity() / gasPerItem, 0, desiredCount);
    }

    private void setOperation(OperationType type, ItemStack input, int inputCount, GasStack gas, long requiredAmount, @Nullable GasInjectionRecipe recipe, @Nullable ResourceLocation fanProcessingTypeId) {
        operationType = type;
        operationInput = input.copyWithCount(inputCount);
        operationGas = gas.isEmpty() ? GasStack.EMPTY : gas.copyWithAmount(requiredAmount);
        operationFluidInputs.clear();
        operationFluidResult = FluidStack.EMPTY;
        operationFanProcessingTypeId = fanProcessingTypeId;
        operationRecipe = recipe;
        operationResults.clear();
        operationResultPrepared = false;
        operationExecuted = false;
        setChanged();
    }

    private boolean prepareOperationResultsIfNeeded(ItemStack itemStack) {
        return operationResultPrepared || switch (operationType) {
            case ITEM_RECIPE -> prepareRecipeResults(itemStack);
            case FAN_PROCESSING -> prepareFanProcessingResults();
            case BASIN_RECIPE, CANISTER, NONE -> true;
        };
    }

    private boolean prepareRecipeResults(ItemStack itemStack) {
        if (level == null) {
            return false;
        }

        int inputCount = operationInput.getCount();
        GasInjectionRecipe recipe = operationRecipe;
        if (recipe == null) {
            Optional<RecipeMatch> recipeMatch = GasInjectionRecipe.findRecipeMatch(level, itemStack, operationGas);
            if (recipeMatch.isEmpty()) {
                return false;
            }

            GasInjectionRecipe matchedRecipe = recipeMatch.get().recipe();
            long expectedGas = matchedRecipe.getGasIngredient().amount() * inputCount;
            if (expectedGas != operationGas.getAmount()) {
                return false;
            }

            recipe = matchedRecipe;
        }

        for (int i = 0; i < inputCount; i++) {
            addResultStack(operationResults, recipe.rollFirstResult(level));
        }
        operationResultPrepared = true;
        operationRecipe = null;
        setChanged();
        return true;
    }

    private boolean prepareFanProcessingResults() {
        if (level == null || operationFanProcessingTypeId == null || !isFanProcessingOperationStillValid()) {
            return false;
        }

        Optional<FanProcessingType> processingType = GasInjectionChamberUtils.getFanProcessingType(operationFanProcessingTypeId);
        if (processingType.isEmpty()) {
            return false;
        }

        List<ItemStack> results = processingType.get().process(operationInput.copy(), level);
        if (results == null) {
            return false;
        }

        for (ItemStack result : results) {
            addResultStack(operationResults, result);
        }
        operationResultPrepared = true;
        setChanged();
        return true;
    }

    private boolean isFanProcessingOperationStillValid() {
        return operationFanProcessingTypeId != null && getInstalledFilterFanProcessingType().filter(operationFanProcessingTypeId::equals).isPresent();
    }

    private boolean executeOperation(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return switch (operationType) {
            case CANISTER -> executeCanisterOperation(transported);
            case ITEM_RECIPE -> executeRecipeOperation(transported, handler);
            case FAN_PROCESSING -> executeFanProcessingOperation(transported, handler);
            case BASIN_RECIPE, NONE -> false;
        };
    }

    private boolean executeCanisterOperation(TransportedItemStack transported) {
        if (level == null || operationGas.isEmpty()) {
            return false;
        }

        IGasCanisterContainer canisterContents = transported.stack.getCapability(GasHandler.ITEM);
        if (canisterContents == null || canisterContents.getMachineFillingStrategy() == MachineFillingStrategy.DENY) {
            return false;
        }

        Provider provider = level.registryAccess();
        ResourceTransaction transaction = new ResourceTransaction().add(operationGasParticipant(provider)).add(ResourceTransaction.participant(() -> canisterContents.fill(0, operationGas, GasAction.SIMULATE) == operationGas.getAmount(), () -> transported.stack.copy(), () -> canisterContents.fill(0, operationGas, GasAction.EXECUTE) == operationGas.getAmount(), snapshot -> transported.stack = snapshot.copy()));
        return transaction.commit();
    }

    private boolean executeRecipeOperation(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return operationResultPrepared && drainAndReplace(transported, handler);
    }

    private boolean executeFanProcessingOperation(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (!operationResultPrepared || !isFanProcessingOperationStillValid()) {
            return false;
        }

        if (!GasInjectionChamberUtils.consumesFanProcessingGas(operationGas)) {
            return replaceTransportedStackWithPreparedResults(transported, handler);
        }
        return drainAndReplace(transported, handler);
    }

    private boolean drainAndReplace(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (level == null || !canReplaceTransportedStackWithPreparedResults(transported)) {
            return false;
        }

        Provider provider = level.registryAccess();
        ResourceTransaction transaction = new ResourceTransaction().add(operationGasParticipant(provider)).add(ResourceTransaction.participant(() -> canReplaceTransportedStackWithPreparedResults(transported), () -> transported.stack.copy(), () -> replaceTransportedStackWithPreparedResults(transported, handler), snapshot -> transported.stack = snapshot.copy()));
        return transaction.commit();
    }

    private boolean canReplaceTransportedStackWithPreparedResults(TransportedItemStack transported) {
        int batchSize = operationInput.getCount();
        return batchSize > 0 && transported.stack.getCount() >= batchSize && matchesOperationInput(transported.stack);
    }

    private boolean replaceTransportedStackWithPreparedResults(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (!canReplaceTransportedStackWithPreparedResults(transported)) {
            return false;
        }

        int batchSize = operationInput.getCount();
        transported.stack.shrink(batchSize);
        FanProcessingType completedFanProcessing = operationType == OperationType.FAN_PROCESSING && operationFanProcessingTypeId != null ? GasInjectionChamberUtils.getFanProcessingType(operationFanProcessingTypeId).orElse(null) : null;

        TransportedItemStack held = null;
        List<TransportedItemStack> results = new ArrayList<>(operationResults.size());
        for (ItemStack resultStack : operationResults) {
            TransportedItemStack result = transported.copy();
            result.stack = resultStack.copy();
            result.clearFanProcessingData();
            if (completedFanProcessing != null) {
                result.processedBy = completedFanProcessing;
                result.processingTime = -1;
            }
            results.add(result);
        }
        if (!transported.stack.isEmpty()) {
            held = transported.copy();
            held.clearFanProcessingData();
        }
        handler.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(results, held));
        return true;
    }

    private boolean matchesOperationInput(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(operationInput, stack) && stack.getCount() >= operationInput.getCount();
    }

    private IGasTank getTank() {
        return tankBehaviour.getPrimaryHandler();
    }

    private GasStack getGasInTank() {
        return getTank().getGasStack();
    }

    private void cancelOperation() {
        processingTicks = -1;
        clearOperation();
        notifyUpdate();
    }

    private void clearOperation() {
        operationType = OperationType.NONE;
        operationGas = GasStack.EMPTY;
        operationFluidInputs.clear();
        operationFluidResult = FluidStack.EMPTY;
        operationFanProcessingTypeId = null;
        operationInput = ItemStack.EMPTY;
        operationResults.clear();
        operationResultPrepared = false;
        operationRecipe = null;
        operationExecuted = false;
        clientFilterLocked = false;
    }

    private enum OperationType {
        NONE("none", false),
        ITEM_RECIPE("recipe", true),
        BASIN_RECIPE("basin_recipe", true),
        CANISTER("canister", true),
        FAN_PROCESSING("fan_processing", true);

        private final String serializedName;
        private final boolean usesGas;

        OperationType(String serializedName, boolean usesGas) {
            this.serializedName = serializedName;
            this.usesGas = usesGas;
        }

        private static OperationType byName(String name) {
            for (OperationType type : values()) {
                if (!type.serializedName.equals(name)) {
                    continue;
                }

                return type;
            }
            return NONE;
        }
    }

    private record BasinFluidSnapshot(FluidTankSnapshot tanks, List<FluidStack> spoutputBuffer) {}

    private class OperationLockingGasHandler implements IGasHandler {
        private final IGasHandler delegate;

        private OperationLockingGasHandler(IGasHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return (!isOperationGasLocked() || GasStack.isSameGasSameComponents(stack, operationGas)) && delegate.isGasValid(tank, stack);
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (isOperationGasLocked()) {
                return GasStack.EMPTY;
            }
            return delegate.drain(resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            if (isOperationGasLocked()) {
                return GasStack.EMPTY;
            }
            return delegate.drain(maxDrain, action);
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return delegate.getGasInTank(tank);
        }

        @Override
        public int getTanks() {
            return delegate.getTanks();
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (isOperationGasLocked() && !GasStack.isSameGasSameComponents(resource, operationGas)) {
                return 0;
            }
            return delegate.fill(resource, action);
        }

        @Override
        public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
            if (!isOperationGasLocked()) {
                return delegate.tryFillAtomically(resources, action);
            }

            for (GasStack resource : resources) {
                if (resource == null || resource.isEmpty() || GasStack.isSameGasSameComponents(resource, operationGas)) {
                    continue;
                }

                return AtomicFillResult.REJECTED;
            }
            return delegate.tryFillAtomically(resources, action);
        }

        @Override
        public long getTankCapacity(int tank) {
            return delegate.getTankCapacity(tank);
        }

        private boolean isOperationGasLocked() {
            return operationType.usesGas && processingTicks >= 0 && !operationExecuted;
        }
    }
}
