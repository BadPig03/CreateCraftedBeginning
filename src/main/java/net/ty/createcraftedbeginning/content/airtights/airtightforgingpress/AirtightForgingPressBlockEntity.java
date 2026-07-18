package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation, IGasInventoryIdentifierProvider {
    private static final int MAX_INPUT_SLOT = 1;
    private static final int MAX_OUTPUT_SLOT = 8;
    private static final int LAZY_TICK_RATE = 4;
    private static final int CYCLE_DURATION = 30;
    private static final float PRESS_HEAD_IDLE_OFFSET = -0.625f;
    private static final float PRESS_HEAD_TRAVEL = 0.8125f;

    private static final String COMPOUND_KEY_CORE = "Core";
    private static final String COMPOUND_KEY_FILTER = "Filter";
    private static final String COMPOUND_KEY_FILTER_INITIALIZED = "FilterInitialized";
    private static final String COMPOUND_KEY_INPUT_ITEMS = "InputItems";
    private static final String COMPOUND_KEY_OPERATING = "Operating";
    private static final String COMPOUND_KEY_OPERATING_TICKS = "OperatingTicks";
    private static final String COMPOUND_KEY_OUTPUT_ITEMS = "OutputItems";
    private static final String COMPOUND_KEY_PRESS_HEAD_ITEMS = "PressHeadItems";
    private static final String COMPOUND_KEY_PROCESSING_ITEMS = "ProcessingItems";

    private final AirtightForgingPressCore core;
    private final IItemHandler inputOutputCapability;
    private final IItemHandlerModifiable recipeInputCapability;
    private final SmartInventory inputInventory;
    private final SmartInventory outputInventory;
    private final SmartInventory pressHeadInventory;
    private final SmartInventory processingInventory;

    private boolean contentsChanged;
    private boolean filterChanged;
    private boolean operating;
    private DeferralBehaviour updateChecker;
    private ForgingPressRecipe currentRecipe;
    private SmithingRecipe currentSmithingRecipe;
    private IFluidHandler fluidCapability;
    private IGasHandler gasCapability;
    private float operatingTicks;
    private SmartFluidTankBehaviour fluidTank;
    private SmartGasTankBehaviour gasTank;
    private ItemStack recipeFilter = ItemStack.EMPTY;
    private boolean filterInitialized = true;
    private long observedRecipeCacheEpoch = AirtightForgingPressUtils.getRecipeCacheEpoch();

    public AirtightForgingPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        core = new AirtightForgingPressCore(this);

        pressHeadInventory = new SmartInventory(MAX_INPUT_SLOT, this, 1, false, (slot, stack) -> stack.is(CCBItemTags.PRESS_HEAD_TOOLS.tag) || stack.getItem() instanceof SmithingTemplateItem).whenContentsChanged(ignored -> contentsChanged = true);
        processingInventory = new AirtightForgingPressInventory(MAX_INPUT_SLOT, this).whenContentsChanged(ignored -> contentsChanged = true);
        inputInventory = new AirtightForgingPressInventory(MAX_INPUT_SLOT, this).whenContentsChanged(ignored -> contentsChanged = true);
        outputInventory = new AirtightForgingPressInventory(MAX_OUTPUT_SLOT, this).forbidInsertion().whenContentsChanged(ignored -> contentsChanged = true);
        inputOutputCapability = new ForgingPressPortHandler(inputInventory, outputInventory);
        recipeInputCapability = new CombinedInvWrapper(pressHeadInventory, processingInventory, inputInventory);

        contentsChanged = true;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS.get(), (press, direction) -> press.pressHeadInventory);
    }

    public static int getFluidCapacity() {
        return Math.max(1, CCBConfig.server().airtights.forgingPressFluidCapacity.get()) * FluidType.BUCKET_VOLUME;
    }

    public static long getGasCapacity() {
        return Math.max(1, CCBConfig.server().airtights.forgingPressGasCapacity.get()) * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    private static boolean insertOutputs(SmartInventory inventory, List<ItemStack> outputItems) {
        for (ItemStack stack : outputItems) {
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, stack.copy(), false);
            if (!remainder.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean canConsumeItem(IItemHandler inventory, ItemStack expectedStack, int amount) {
        if (amount <= 0) {
            return true;
        }

        ItemStack current = inventory.getStackInSlot(0);
        if (current.isEmpty() || expectedStack.isEmpty() || current.getCount() < amount || !ItemStack.isSameItemSameComponents(current, expectedStack)) {
            return false;
        }

        ItemStack simulated = inventory.extractItem(0, amount, true);
        return simulated.getCount() == amount && ItemStack.isSameItemSameComponents(simulated, expectedStack);
    }

    private static boolean consumeItem(IItemHandler inventory, ItemStack expectedStack, int amount) {
        if (amount <= 0) {
            return true;
        }

        ItemStack extracted = inventory.extractItem(0, amount, false);
        return extracted.getCount() == amount && ItemStack.isSameItemSameComponents(extracted, expectedStack);
    }

    private static List<ItemStack> copyInventory(IItemHandler inventory) {
        List<ItemStack> stacks = new ArrayList<>(inventory.getSlots());
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            stacks.add(inventory.getStackInSlot(slot).copy());
        }
        return stacks;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        fluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 1, getFluidCapacity(), false).whenFluidUpdates(() -> contentsChanged = true);
        fluidCapability = fluidTank.getCapability();
        behaviours.add(fluidTank);

        gasTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.INPUT, this, 1, getGasCapacity(), false).whenGasUpdates(() -> contentsChanged = true);
        gasCapability = gasTank.getCapability();
        behaviours.add(gasTank);

        updateChecker = new DeferralBehaviour(this, this::updateForgingPress);
        behaviours.add(updateChecker);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        tickOperation();
        if (!contentsChanged) {
            return;
        }

        contentsChanged = false;
        updateChecker.scheduleUpdate();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        core.lazyTick();
        long recipeCacheEpoch = AirtightForgingPressUtils.getRecipeCacheEpoch();
        if (observedRecipeCacheEpoch == recipeCacheEpoch) {
            return;
        }

        observedRecipeCacheEpoch = recipeCacheEpoch;
        update(true);
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_CORE, core.write());
        compoundTag.put(COMPOUND_KEY_PRESS_HEAD_ITEMS, pressHeadInventory.serializeNBT(provider));
        compoundTag.put(COMPOUND_KEY_PROCESSING_ITEMS, processingInventory.serializeNBT(provider));
        compoundTag.put(COMPOUND_KEY_FILTER, recipeFilter.saveOptional(provider));
        compoundTag.putBoolean(COMPOUND_KEY_FILTER_INITIALIZED, filterInitialized);
        compoundTag.put(COMPOUND_KEY_INPUT_ITEMS, inputInventory.serializeNBT(provider));
        compoundTag.put(COMPOUND_KEY_OUTPUT_ITEMS, outputInventory.serializeNBT(provider));
        compoundTag.putFloat(COMPOUND_KEY_OPERATING_TICKS, operatingTicks);
        compoundTag.putBoolean(COMPOUND_KEY_OPERATING, operating);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (compoundTag.contains(COMPOUND_KEY_CORE)) {
            core.read(compoundTag.getCompound(COMPOUND_KEY_CORE));
        }
        if (compoundTag.contains(COMPOUND_KEY_PRESS_HEAD_ITEMS)) {
            pressHeadInventory.deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_PRESS_HEAD_ITEMS));
        }
        if (compoundTag.contains(COMPOUND_KEY_PROCESSING_ITEMS)) {
            processingInventory.deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_PROCESSING_ITEMS));
        }
        recipeFilter = compoundTag.contains(COMPOUND_KEY_FILTER) ? ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_FILTER)) : ItemStack.EMPTY;
        if (compoundTag.contains(COMPOUND_KEY_FILTER_INITIALIZED)) {
            filterInitialized = compoundTag.getBoolean(COMPOUND_KEY_FILTER_INITIALIZED);
        }
        else {
            filterInitialized = compoundTag.contains(COMPOUND_KEY_FILTER);
        }
        if (compoundTag.contains(COMPOUND_KEY_INPUT_ITEMS)) {
            inputInventory.deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_INPUT_ITEMS));
        }
        if (compoundTag.contains(COMPOUND_KEY_OUTPUT_ITEMS)) {
            outputInventory.deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_OUTPUT_ITEMS));
        }
        if (compoundTag.contains(COMPOUND_KEY_OPERATING_TICKS)) {
            operatingTicks = compoundTag.getFloat(COMPOUND_KEY_OPERATING_TICKS);
        }
        if (compoundTag.contains(COMPOUND_KEY_OPERATING)) {
            operating = compoundTag.getBoolean(COMPOUND_KEY_OPERATING);
        }
        if (clientPacket) {
            return;
        }

        operating = false;
        operatingTicks = 0;
        currentRecipe = null;
        currentSmithingRecipe = null;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, pressHeadInventory);
        ItemHelper.dropContents(level, worldPosition, processingInventory);
        ItemHelper.dropContents(level, worldPosition, inputInventory);
        ItemHelper.dropContents(level, worldPosition, outputInventory);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        core.getTooltipBuilder().addToGoggleTooltip(tooltip);
        return true;
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return core.getTooltipBuilder().addToTooltip(tooltip);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(1, 1, 1);
    }

    @Override
    public InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        return new Single(worldPosition);
    }

    public void scheduleUpdate() {
        updateChecker.scheduleUpdate();
    }

    public void startProcessInPonderLevel() {
        update(false);
        updateForgingPress();
    }

    public boolean isEmpty() {
        return inputInventory.isEmpty() && outputInventory.isEmpty() && processingInventory.isEmpty() && pressHeadInventory.isEmpty() && fluidTank.isEmpty() && gasTank.isEmpty();
    }

    public boolean hasRecipeInputs() {
        return !inputInventory.isEmpty() || !processingInventory.isEmpty() || !fluidTank.isEmpty() || !gasTank.isEmpty();
    }

    public void notifyContentsChanged() {
        contentsChanged = true;
    }

    public SmartInventory getPressHeadInventory() {
        return pressHeadInventory;
    }

    public SmartInventory getAdditionInventory() {
        return processingInventory;
    }

    public SmartInventory getInputInventory() {
        return inputInventory;
    }

    public SmartInventory getOutputInventory() {
        return outputInventory;
    }

    public IItemHandler getInputOutputCapability() {
        return inputOutputCapability;
    }

    public IItemHandlerModifiable getRecipeInputCapability() {
        return recipeInputCapability;
    }

    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    public IGasHandler getGasCapability() {
        return gasCapability;
    }

    public ItemStack getRecipeFilter() {
        return recipeFilter.copy();
    }

    public void setRecipeFilter(ItemStack stack) {
        ItemStack normalized = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (filterInitialized && ItemStack.matches(recipeFilter, normalized)) {
            return;
        }

        recipeFilter = normalized;
        filterInitialized = true;
        filterChanged = true;
        contentsChanged = true;
        syncRecipeFilterReplicas();
        setChanged();
        sendData();
    }

    public boolean testRecipeFilter(ItemStack stack) {
        return recipeFilter.isEmpty() || level != null && FilterItemStack.of(recipeFilter).test(level, stack);
    }

    public void initializeRecipeFilterFromLegacy(ItemStack legacyFilter) {
        if (filterInitialized || legacyFilter.isEmpty()) {
            return;
        }

        recipeFilter = legacyFilter.copyWithCount(1);
        filterInitialized = true;
        filterChanged = true;
        contentsChanged = true;
        syncRecipeFilterReplicas();
        setChanged();
        sendData();
    }

    public boolean isRecipeFilterInitialized() {
        return filterInitialized;
    }

    private void syncRecipeFilterReplicas() {
        if (level == null) {
            return;
        }

        for (AirtightForgingPressStructuralPosition position : AirtightForgingPressStructuralPosition.all()) {
            if (!position.isFilter()) {
                continue;
            }

            BlockPos filterPos = worldPosition.offset(position.getStructureOffset());
            if (level.getBlockEntity(filterPos) instanceof AirtightForgingPressStructuralBlockEntity structural) {
                structural.syncFilterFromMaster(recipeFilter);
            }
        }
    }

    public Optional<OutputPlan> planOutputs(List<ItemStack> outputItems) {
        SmartInventory simulatedOutput = createOutputSimulation();
        if (!insertOutputs(simulatedOutput, outputItems)) {
            return Optional.empty();
        }

        return Optional.of(new OutputPlan(copyInventory(outputInventory), copyInventory(simulatedOutput)));
    }

    public boolean acceptOutputs(List<ItemStack> outputItems, boolean simulate) {
        Optional<OutputPlan> plannedOutput = planOutputs(outputItems);
        if (plannedOutput.isEmpty()) {
            return false;
        }
        if (simulate) {
            return true;
        }

        OutputPlan outputPlan = plannedOutput.get();
        if (!outputPlanMatchesCurrent(outputPlan)) {
            return false;
        }

        applyOutputPlan(outputPlan);
        return true;
    }

    public ConsumptionPlan createConsumptionPlan(ItemStack expectedProcessingStack, int processingAmount, ItemStack expectedInputStack, int inputAmount, int[] fluidAmounts, long[] gasAmounts) {
        if (fluidAmounts.length != fluidCapability.getTanks() || gasAmounts.length != gasCapability.getTanks()) {
            throw new IllegalArgumentException("Consumption plan tank count does not match the forging press");
        }
        if (fluidAmounts.length != 1 || gasAmounts.length != 1) {
            throw new IllegalStateException("The airtight forging press currently requires exactly one fluid tank and one gas tank");
        }

        ItemStack expectedPressHead = pressHeadInventory.getStackInSlot(0).copy();
        FluidStack expectedFluid = fluidTank.getPrimaryHandler().getFluid().copy();
        int fluidAmount = fluidAmounts[0];
        GasStack expectedGas = gasTank.getPrimaryHandler().getGasStack().copy();
        long gasAmount = gasAmounts[0];
        return new ConsumptionPlan(expectedPressHead, expectedProcessingStack, processingAmount, expectedInputStack, inputAmount, expectedFluid, fluidAmount, expectedGas, gasAmount);
    }

    public synchronized boolean commitCraft(ConsumptionPlan consumptionPlan, OutputPlan outputPlan) {
        if (!canCommit(consumptionPlan, outputPlan)) {
            return false;
        }

        TransactionSnapshot snapshot = createTransactionSnapshot();
        boolean committed = false;
        try {
            if (!consumeItem(processingInventory, consumptionPlan.expectedProcessingStack(), consumptionPlan.processingAmount())) {
                return false;
            }
            if (!consumeItem(inputInventory, consumptionPlan.expectedInputStack(), consumptionPlan.inputAmount())) {
                return false;
            }
            if (!consumeFluid(consumptionPlan)) {
                return false;
            }
            if (!consumeGas(consumptionPlan)) {
                return false;
            }

            applyOutputPlan(outputPlan);
            committed = true;
            return true;
        } finally {
            if (!committed) {
                restoreTransactionSnapshot(snapshot);
            }
        }
    }

    private boolean canCommit(ConsumptionPlan plan, OutputPlan outputPlan) {
        return outputPlanMatchesCurrent(outputPlan) && ItemStack.matches(pressHeadInventory.getStackInSlot(0), plan.expectedPressHeadStack()) && canConsumeItem(processingInventory, plan.expectedProcessingStack(), plan.processingAmount()) && canConsumeItem(inputInventory, plan.expectedInputStack(), plan.inputAmount()) && canConsumeFluid(plan) && canConsumeGas(plan);
    }

    private boolean canConsumeFluid(ConsumptionPlan plan) {
        if (plan.fluidAmount() <= 0) {
            return true;
        }

        FluidStack current = fluidTank.getPrimaryHandler().getFluid();
        FluidStack expected = plan.expectedFluid();
        if (current.isEmpty() || expected.isEmpty() || current.getAmount() < plan.fluidAmount() || !FluidStack.isSameFluidSameComponents(current, expected)) {
            return false;
        }

        FluidStack simulated = fluidTank.getPrimaryHandler().drain(expected.copyWithAmount(plan.fluidAmount()), FluidAction.SIMULATE);
        return simulated.getAmount() == plan.fluidAmount();
    }

    private boolean consumeFluid(ConsumptionPlan plan) {
        if (plan.fluidAmount() <= 0) {
            return true;
        }

        FluidStack expected = plan.expectedFluid();
        FluidStack drained = fluidTank.getPrimaryHandler().drain(expected.copyWithAmount(plan.fluidAmount()), FluidAction.EXECUTE);
        return drained.getAmount() == plan.fluidAmount() && FluidStack.isSameFluidSameComponents(drained, expected);
    }

    private boolean canConsumeGas(ConsumptionPlan plan) {
        if (plan.gasAmount() <= 0) {
            return true;
        }

        GasStack current = gasTank.getPrimaryHandler().getGasStack();
        GasStack expected = plan.expectedGas();
        if (current.isEmpty() || expected.isEmpty() || current.getAmount() < plan.gasAmount() || !GasStack.isSameGasSameComponents(current, expected)) {
            return false;
        }

        GasStack simulated = gasTank.getPrimaryHandler().drain(expected.copyWithAmount(plan.gasAmount()), GasAction.SIMULATE);
        return simulated.getAmount() == plan.gasAmount();
    }

    private boolean consumeGas(ConsumptionPlan plan) {
        if (plan.gasAmount() <= 0) {
            return true;
        }

        GasStack expected = plan.expectedGas();
        GasStack drained = gasTank.getPrimaryHandler().drain(expected.copyWithAmount(plan.gasAmount()), GasAction.EXECUTE);
        return drained.getAmount() == plan.gasAmount() && GasStack.isSameGasSameComponents(drained, expected);
    }

    private boolean outputPlanMatchesCurrent(OutputPlan outputPlan) {
        int slots = outputInventory.getSlots();
        List<ItemStack> expectedSlots = outputPlan.expectedSlots();
        if (expectedSlots.size() != slots || outputPlan.finalSlots().size() != slots) {
            return false;
        }

        for (int slot = 0; slot < slots; slot++) {
            if (!ItemStack.matches(outputInventory.getStackInSlot(slot), expectedSlots.get(slot))) {
                return false;
            }
        }
        return true;
    }

    private void applyOutputPlan(OutputPlan outputPlan) {
        List<ItemStack> finalSlots = outputPlan.finalSlots();
        for (int slot = 0; slot < outputInventory.getSlots(); slot++) {
            outputInventory.setStackInSlot(slot, finalSlots.get(slot).copy());
        }
    }

    private TransactionSnapshot createTransactionSnapshot() {
        return new TransactionSnapshot(processingInventory.getStackInSlot(0).copy(), inputInventory.getStackInSlot(0).copy(), copyInventory(outputInventory), fluidTank.getPrimaryHandler().getFluid().copy(), gasTank.getPrimaryHandler().getGasStack().copy());
    }

    private void restoreTransactionSnapshot(TransactionSnapshot snapshot) {
        processingInventory.setStackInSlot(0, snapshot.processingStack().copy());
        inputInventory.setStackInSlot(0, snapshot.inputStack().copy());
        for (int slot = 0; slot < outputInventory.getSlots(); slot++) {
            outputInventory.setStackInSlot(slot, snapshot.outputSlots().get(slot).copy());
        }
        fluidTank.getPrimaryHandler().setFluid(snapshot.fluid().copy());
        gasTank.getPrimaryHandler().setGasStack(snapshot.gas().copy());
    }

    private SmartInventory createOutputSimulation() {
        SmartInventory simulatedOutput = new SmartInventory(outputInventory.getSlots(), this);
        simulatedOutput.allowInsertion();
        for (int slot = 0; slot < outputInventory.getSlots(); slot++) {
            simulatedOutput.setStackInSlot(slot, outputInventory.getStackInSlot(slot).copy());
        }
        return simulatedOutput;
    }

    public float getPressHeadDistance(float partialTicks) {
        if (!operating) {
            return PRESS_HEAD_IDLE_OFFSET;
        }

        float cycleDuration = CYCLE_DURATION;
        float ticks = Mth.clamp(operatingTicks + partialTicks * getOperationSpeed(), 0, cycleDuration);
        float distance;
        if (ticks < cycleDuration * 2.0f / 3.0f) {
            distance = Mth.clamp((float) Math.pow(ticks / cycleDuration * 2.0f, 3), 0, 1);
        }
        else {
            distance = Mth.clamp((cycleDuration - ticks) / cycleDuration * 3.0f, 0, 1);
        }
        return PRESS_HEAD_IDLE_OFFSET + distance * PRESS_HEAD_TRAVEL;
    }

    private float getOperationSpeed() {
        if (level instanceof PonderLevel) {
            return 1;
        }

        float absSpeed = Mth.abs(core.getStructureManager().getSpeed());
        float minSpeed = SpeedLevel.FAST.getSpeedValue();
        if (absSpeed < minSpeed) {
            return 0;
        }

        return Mth.clamp(absSpeed / minSpeed, 1, 16);
    }

    private boolean updateForgingPress() {
        observedRecipeCacheEpoch = AirtightForgingPressUtils.getRecipeCacheEpoch();
        if (level == null) {
            return false;
        }

        boolean inactiveClient = level.isClientSide && !isVirtual();
        if (inactiveClient || operating || getOperationSpeed() <= 0) {
            return true;
        }

        Optional<ForgingPressRecipe> recipe = AirtightForgingPressUtils.getMatchingRecipe(this);
        if (recipe.isPresent()) {
            currentRecipe = recipe.get();
            currentSmithingRecipe = null;
            startOperation();
            return true;
        }

        if (!CCBConfig.server().airtights.enableAutomaticSmithingRecipes.get()) {
            currentRecipe = null;
            currentSmithingRecipe = null;
            return true;
        }

        Optional<RecipeHolder<SmithingRecipe>> smithingRecipe = AirtightForgingPressUtils.getMatchingSmithingRecipe(this);
        if (smithingRecipe.isEmpty()) {
            currentRecipe = null;
            currentSmithingRecipe = null;
            return true;
        }

        currentRecipe = null;
        currentSmithingRecipe = smithingRecipe.get().value();
        startOperation();
        return true;
    }

    private void startOperation() {
        operating = true;
        operatingTicks = 0;
        sendData();
    }

    private void tickOperation() {
        if (filterChanged) {
            filterChanged = false;
            update(true);
            return;
        }

        if (!operating) {
            return;
        }

        if (operatingTicks >= CYCLE_DURATION) {
            update(true);
            return;
        }

        float operationSpeed = getOperationSpeed();
        if (operationSpeed <= 0) {
            update(false);
            return;
        }
        if (currentRecipe == null && currentSmithingRecipe != null && !CCBConfig.server().airtights.enableAutomaticSmithingRecipes.get()) {
            update(false);
            return;
        }

        float previousTicks = operatingTicks;
        operatingTicks = Mth.clamp(operatingTicks + operationSpeed, 0, CYCLE_DURATION);
        if (level == null || level.isClientSide) {
            return;
        }

        float processingStart = CYCLE_DURATION / 2.0f;
        boolean wasAlreadyProcessing = previousTicks >= processingStart;
        boolean hasNotReachedProcessing = operatingTicks < processingStart;
        boolean hasNoRecipe = currentRecipe == null && currentSmithingRecipe == null;
        if (wasAlreadyProcessing || hasNotReachedProcessing || hasNoRecipe) {
            return;
        }

        ItemStack particleStack = inputInventory.getStackInSlot(0).copy();
        boolean success;
        if (currentRecipe != null) {
            if (particleStack.isEmpty()) {
                particleStack = currentRecipe.getResultItem(level.registryAccess()).copy();
            }
            success = ForgingPressRecipe.apply(this, currentRecipe);
        }
        else {
            SmithingRecipeInput input = AirtightForgingPressUtils.createSmithingInput(this);
            ItemStack result = currentSmithingRecipe.assemble(input, level.registryAccess());
            if (!result.isEmpty()) {
                particleStack = result.copy();
            }
            success = AirtightForgingPressUtils.applySmithingRecipe(this, currentSmithingRecipe);
        }
        if (!success) {
            return;
        }

        fluidTank.sendDataImmediately();
        gasTank.sendDataImmediately();
        CCBSoundEvents.FORGING_PRESS_PRESSED.playOnServer(level, getBlockPos());
        spawnParticles(particleStack);
        contentsChanged = true;
        sendData();
    }

    private void update(boolean schedule) {
        operating = false;
        operatingTicks = 0;
        currentRecipe = null;
        currentSmithingRecipe = null;
        sendData();
        if (!schedule || level == null || level.isClientSide && !isVirtual()) {
            return;
        }

        updateChecker.scheduleUpdate();
    }

    private void spawnParticles(ItemStack stack) {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel) || isVirtual() || stack.isEmpty()) {
            return;
        }

        Vec3 pos = VecHelper.getCenterOf(getBlockPos()).add(0, -0.625f, 0);
        serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), pos.x, pos.y, pos.z, 16, 0.15, 0.05, 0.15, 0.08);
    }

    public record OutputPlan(List<ItemStack> expectedSlots, List<ItemStack> finalSlots) {
        public OutputPlan {
            expectedSlots = copyStacks(expectedSlots);
            finalSlots = copyStacks(finalSlots);
        }

        private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
            return stacks.stream().map(ItemStack::copy).toList();
        }
    }

    public record ConsumptionPlan(ItemStack expectedPressHeadStack, ItemStack expectedProcessingStack, int processingAmount, ItemStack expectedInputStack, int inputAmount, FluidStack expectedFluid, int fluidAmount, GasStack expectedGas, long gasAmount) {
        public ConsumptionPlan {
            expectedPressHeadStack = expectedPressHeadStack.copy();
            expectedProcessingStack = expectedProcessingStack.copy();
            expectedInputStack = expectedInputStack.copy();
            expectedFluid = expectedFluid.copy();
            expectedGas = expectedGas.copy();
            if (processingAmount < 0 || inputAmount < 0 || fluidAmount < 0 || gasAmount < 0) {
                throw new IllegalArgumentException("Consumption amounts must not be negative");
            }
        }
    }

    private record TransactionSnapshot(ItemStack processingStack, ItemStack inputStack, List<ItemStack> outputSlots, FluidStack fluid, GasStack gas) {}

    private record ForgingPressPortHandler(IItemHandlerModifiable input, IItemHandlerModifiable output) implements IItemHandler {
        @Override
        public int getSlots() {
            return input.getSlots() + output.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return getHandler(slot).getStackInSlot(getLocalSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            getHandler(slot);
            if (slot >= input.getSlots()) {
                return stack;
            }
            return input.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            getHandler(slot);
            if (slot < input.getSlots()) {
                return ItemStack.EMPTY;
            }
            return output.extractItem(slot - input.getSlots(), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return getHandler(slot).getSlotLimit(getLocalSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            getHandler(slot);
            return slot < input.getSlots() && input.isItemValid(slot, stack);
        }

        private IItemHandlerModifiable getHandler(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                throw new IndexOutOfBoundsException("Slot " + slot + " not in valid range [0," + getSlots() + ')');
            }
            return slot < input.getSlots() ? input : output;
        }

        private int getLocalSlot(int slot) {
            return slot < input.getSlots() ? slot : slot - input.getSlots();
        }
    }
}
