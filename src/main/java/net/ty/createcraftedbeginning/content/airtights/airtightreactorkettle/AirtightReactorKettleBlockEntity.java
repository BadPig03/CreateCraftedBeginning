package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.sound.SoundScapes.AmbienceGroup;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
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
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.CombinedGasTankWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class AirtightReactorKettleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation, IGasInventoryIdentifierProvider {
    private static final int LAZY_TICK_RATE = 4;
    private static final int RECIPE_FALLBACK_CHECK_RATE = 40;
    private static final int MAX_ITEM_SLOT = 27;
    private static final int OPERATING_FINISHED = 40;
    private static final int PROCESSING_STARTED = 20;

    private static final String COMPOUND_KEY_CORE = "Core";
    private static final String COMPOUND_KEY_INPUT_ITEMS = "InputItems";
    private static final String COMPOUND_KEY_OPEN_STATE = "OpenState";
    private static final String COMPOUND_KEY_OPERATING = "Operating";
    private static final String COMPOUND_KEY_OPERATING_TICKS = "OperatingTicks";
    private static final String COMPOUND_KEY_OUTPUT_ITEMS = "OutputItems";
    private static final String COMPOUND_KEY_PROCESSING_TICKS = "ProcessingTicks";

    private final AirtightReactorKettleCore core;
    private final AirtightReactorKettleInventory inputInventory;
    private final Couple<SmartInventory> inventories;
    private final IItemHandlerModifiable itemCapability;
    private final LerpedFloat ingredientRotation;
    private final LerpedFloat ingredientRotationSpeed;
    private final LerpedFloat mixerRotation;
    private final LerpedFloat mixerRotationSpeed;
    private final LerpedFloat windowDistance;
    private final SmartInventory outputInventory;

    private boolean contentsChanged;
    private boolean filterChanged;
    private boolean operating;
    private boolean windowsOpenState = true;
    private DeferralBehaviour updateChecker;
    private IFluidHandler fluidCapability;
    private IGasHandler gasCapability;
    private int fallbackRecipeCheckTicks;
    private int operatingTicks;
    private int processingTicks = -1;
    private CraftingRecipe currentCraftingRecipe;
    private ReactorKettleRecipe currentRecipe;
    private SmartFluidTankBehaviour inputFluidTank;
    private SmartFluidTankBehaviour outputFluidTank;
    private SmartGasTankBehaviour inputGasTank;
    private SmartGasTankBehaviour outputGasTank;
    private CCBAdvancementBehaviour advancementBehaviour;

    public AirtightReactorKettleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        core = new AirtightReactorKettleCore(this);

        inputInventory = new AirtightReactorKettleInventory(MAX_ITEM_SLOT, this);
        inputInventory.whenContentsChanged($ -> contentsChanged = true);
        outputInventory = new AirtightReactorKettleInventory(MAX_ITEM_SLOT, this).forbidInsertion();
        outputInventory.whenContentsChanged($ -> contentsChanged = true);
        itemCapability = new CombinedInvWrapper(inputInventory, outputInventory);
        inventories = Couple.create(inputInventory, outputInventory);

        ingredientRotation = LerpedFloat.angular().startWithValue(0);
        ingredientRotationSpeed = LerpedFloat.linear().startWithValue(0);
        mixerRotation = LerpedFloat.angular().startWithValue(0);
        mixerRotationSpeed = LerpedFloat.linear().startWithValue(0);
        windowDistance = LerpedFloat.linear().startWithValue(0.5);
        contentsChanged = true;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (blockEntity, direction) -> blockEntity.itemCapability);
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (blockEntity, direction) -> blockEntity.fluidCapability);
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (blockEntity, direction) -> blockEntity.gasCapability);
    }

    public static int getFluidCapacity() {
        return Math.max(1, CCBConfig.server().airtights.reactorKettleFluidCapacity.get()) * FluidType.BUCKET_VOLUME;
    }

    public static long getGasCapacity() {
        return Math.max(1, CCBConfig.server().airtights.reactorKettleGasCapacity.get()) * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    private static boolean insertFluidOutputs(IFluidHandler target, List<FluidStack> outputs) {
        for (FluidStack stack : outputs) {
            if (stack.isEmpty() || target.fill(stack.copy(), FluidAction.EXECUTE) == stack.getAmount()) {
                continue;
            }

            return false;
        }

        return true;
    }

    private static boolean insertGasOutputs(IGasHandler target, List<GasStack> outputs) {
        for (GasStack stack : outputs) {
            if (stack.isEmpty() || target.fill(stack.copy(), GasAction.EXECUTE) == stack.getAmount()) {
                continue;
            }

            return false;
        }

        return true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.BACK_TO_BASICS);
        behaviours.add(advancementBehaviour);

        addFluidBehaviours(behaviours);
        addGasBehaviours(behaviours);

        updateChecker = new DeferralBehaviour(this, this::updateReactorKettle);
        behaviours.add(updateChecker);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            tickClientAnimations();
        }
        tickOperation();
        if (!contentsChanged) {
            return;
        }

        contentsChanged = false;
        fallbackRecipeCheckTicks = 0;
        updateChecker.scheduleUpdate();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        core.lazyTick();
        fallbackRecipeCheckTicks += LAZY_TICK_RATE;
        if (fallbackRecipeCheckTicks < RECIPE_FALLBACK_CHECK_RATE) {
            return;
        }

        fallbackRecipeCheckTicks = 0;
        updateChecker.scheduleUpdate();
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_CORE, core.write());
        compoundTag.put(COMPOUND_KEY_INPUT_ITEMS, inputInventory.serializeNBT(provider));
        compoundTag.put(COMPOUND_KEY_OUTPUT_ITEMS, outputInventory.serializeNBT(provider));
        compoundTag.putInt(COMPOUND_KEY_OPERATING_TICKS, operatingTicks);
        compoundTag.putInt(COMPOUND_KEY_PROCESSING_TICKS, processingTicks);
        compoundTag.putBoolean(COMPOUND_KEY_OPERATING, operating);
        compoundTag.putBoolean(COMPOUND_KEY_OPEN_STATE, windowsOpenState);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (compoundTag.contains(COMPOUND_KEY_CORE)) {
            core.read(compoundTag.getCompound(COMPOUND_KEY_CORE));
        }
        if (compoundTag.contains(COMPOUND_KEY_INPUT_ITEMS)) {
            inputInventory.deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_INPUT_ITEMS));
        }
        if (compoundTag.contains(COMPOUND_KEY_OUTPUT_ITEMS)) {
            outputInventory.deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_OUTPUT_ITEMS));
        }
        if (compoundTag.contains(COMPOUND_KEY_OPERATING_TICKS)) {
            operatingTicks = compoundTag.getInt(COMPOUND_KEY_OPERATING_TICKS);
        }
        if (compoundTag.contains(COMPOUND_KEY_PROCESSING_TICKS)) {
            processingTicks = compoundTag.getInt(COMPOUND_KEY_PROCESSING_TICKS);
        }
        if (compoundTag.contains(COMPOUND_KEY_OPERATING)) {
            operating = compoundTag.getBoolean(COMPOUND_KEY_OPERATING);
        }
        if (compoundTag.contains(COMPOUND_KEY_OPEN_STATE)) {
            windowsOpenState = compoundTag.getBoolean(COMPOUND_KEY_OPEN_STATE);
        }
        if (clientPacket) {
            return;
        }

        operating = false;
        operatingTicks = 0;
        processingTicks = -1;
        currentRecipe = null;
        currentCraftingRecipe = null;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, inputInventory);
        ItemHelper.dropContents(level, worldPosition, outputInventory);
    }

    private void addFluidBehaviours(List<BlockEntityBehaviour> behaviours) {
        inputFluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 3, getFluidCapacity(), true).whenFluidUpdates(() -> contentsChanged = true);
        outputFluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 2, getFluidCapacity(), true).forbidInsertion().whenFluidUpdates(() -> contentsChanged = true);
        fluidCapability = new CombinedTankWrapper(inputFluidTank.getCapability(), outputFluidTank.getCapability());
        behaviours.add(inputFluidTank);
        behaviours.add(outputFluidTank);
    }

    private void addGasBehaviours(List<BlockEntityBehaviour> behaviours) {
        inputGasTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.INPUT, this, 3, getGasCapacity(), true).whenGasUpdates(() -> contentsChanged = true);
        outputGasTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, this, 2, getGasCapacity(), true).forbidInsertion().whenGasUpdates(() -> contentsChanged = true);
        gasCapability = new CombinedGasTankWrapper(inputGasTank.getCapability(), outputGasTank.getCapability());
        behaviours.add(inputGasTank);
        behaviours.add(outputGasTank);
    }

    private void tickClientAnimations() {
        CatnipServices.PLATFORM.executeOnClientOnly(() -> this::tickAudio);
        ingredientRotationSpeed.tickChaser();
        ingredientRotation.setValue(ingredientRotation.getValue() + ingredientRotationSpeed.getValue());
        mixerRotationSpeed.tickChaser();
        mixerRotation.setValue(mixerRotation.getValue() + mixerRotationSpeed.getValue());
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

    public AirtightReactorKettleCore getCore() {
        return core;
    }

    public boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases, boolean simulate) {
        IItemHandler targetInventory = outputInventory;
        IFluidHandler targetFluidTank = outputFluidTank.getCapability();
        IGasHandler targetGasTank = outputGasTank.getCapability();
        boolean hasItemOutputs = outputItems.stream().anyMatch(stack -> !stack.isEmpty());
        boolean hasFluidOutputs = outputFluids.stream().anyMatch(stack -> !stack.isEmpty());
        boolean hasGasOutputs = outputGases.stream().anyMatch(stack -> !stack.isEmpty());

        if (hasItemOutputs && (targetInventory == null || !canAcceptItemOutputs(outputItems))) {
            return false;
        }
        if (hasFluidOutputs && (targetFluidTank == null || !canAcceptFluidOutputs(targetFluidTank, outputFluids))) {
            return false;
        }
        if (hasGasOutputs && !canAcceptGasOutputs(targetGasTank, outputGases)) {
            return false;
        }
        if (simulate) {
            return true;
        }

        outputInventory.allowInsertion();
        outputFluidTank.allowInsertion();
        outputGasTank.allowInsertion();
        try {
            if (hasItemOutputs && !insertItemOutputs(targetInventory, outputItems)) {
                return false;
            }
            if (hasFluidOutputs && !insertFluidOutputs(targetFluidTank, outputFluids)) {
                return false;
            }
            if (hasGasOutputs && !insertGasOutputs(targetGasTank, outputGases)) {
                return false;
            }

            return true;
        } finally {
            outputInventory.forbidInsertion();
            outputFluidTank.forbidInsertion();
            outputGasTank.forbidInsertion();
        }
    }

    private boolean canAcceptItemOutputs(List<ItemStack> outputs) {
        IItemHandlerModifiable simulation = AirtightReactorKettleInventory.createSimulation(outputInventory.getSlots());
        for (int slot = 0; slot < outputInventory.getSlots(); slot++) {
            simulation.setStackInSlot(slot, outputInventory.getStackInSlot(slot).copy());
        }

        for (ItemStack stack : outputs) {
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(simulation, stack.copy(), false);
            if (!remainder.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private boolean canAcceptFluidOutputs(IFluidHandler target, List<FluidStack> outputs) {
        SmartFluidTankBehaviour simulatedTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, target.getTanks(), getFluidCapacity(), true);
        IFluidHandler simulation = simulatedTank.getCapability();
        for (int tank = 0; tank < target.getTanks(); tank++) {
            FluidStack existing = target.getFluidInTank(tank).copy();
            if (existing.isEmpty() || simulation.fill(existing.copy(), FluidAction.EXECUTE) == existing.getAmount()) {
                continue;
            }

            return false;
        }

        for (FluidStack stack : outputs) {
            if (stack.isEmpty() || simulation.fill(stack.copy(), FluidAction.EXECUTE) == stack.getAmount()) {
                continue;
            }

            return false;
        }

        return true;
    }

    private boolean canAcceptGasOutputs(IGasHandler target, List<GasStack> outputs) {
        SmartGasTankBehaviour simulatedTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, this, target.getTanks(), getGasCapacity(), true);
        IGasHandler simulation = simulatedTank.getCapability();
        for (int tank = 0; tank < target.getTanks(); tank++) {
            GasStack existing = target.getGasInTank(tank).copy();
            if (existing.isEmpty() || simulation.fill(existing.copy(), GasAction.EXECUTE) == existing.getAmount()) {
                continue;
            }

            return false;
        }

        for (GasStack stack : outputs) {
            if (stack.isEmpty() || simulation.fill(stack.copy(), GasAction.EXECUTE) == stack.getAmount()) {
                continue;
            }

            return false;
        }

        return true;
    }

    private boolean insertItemOutputs(IItemHandler target, List<ItemStack> outputs) {
        for (ItemStack stack : outputs) {
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(AllItems.ANDESITE_ALLOY)) {
                advancementBehaviour.awardPlayer(CCBAdvancements.BACK_TO_BASICS);
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
            if (!remainder.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public boolean getWindowsOpenState() {
        return windowsOpenState;
    }

    public boolean isEmpty() {
        return inputInventory.isEmpty() && outputInventory.isEmpty() && inputFluidTank.isEmpty() && outputFluidTank.isEmpty() && inputGasTank.isEmpty() && outputGasTank.isEmpty();
    }

    public boolean isFilterChanged() {
        return filterChanged;
    }

    public SmartFluidTankBehaviour getInputFluidTank() {
        return inputFluidTank;
    }

    public SmartFluidTankBehaviour getOutputFluidTank() {
        return outputFluidTank;
    }

    public SmartGasTankBehaviour getInputGasTank() {
        return inputGasTank;
    }

    public SmartGasTankBehaviour getOutputGasTank() {
        return outputGasTank;
    }

    public Couple<SmartInventory> getInventories() {
        return inventories;
    }

    @Nullable
    public FilteringBehaviour getFilteringBehaviour() {
        BlockPos filterPos = getBlockPos().below().north();
        if (level == null || !(level.getBlockEntity(filterPos) instanceof AirtightReactorKettleStructuralBlockEntity structural)) {
            return null;
        }

        return structural.getFilteringBehaviour();
    }

    public float getDamage() {
        if (!operating) {
            return 0;
        }

        float absSpeed = Mth.abs(core.getStructureManager().getSpeed());
        if (absSpeed == 0) {
            return 0;
        }

        return absSpeed / 32 * Math.max(0, CCBConfig.server().airtights.reactorKettleMixerDamageMultiplier.getF());
    }

    public float getMixerOffset(float partialTicks) {
        if (!operating) {
            return 0;
        }
        if (operatingTicks == PROCESSING_STARTED) {
            return 0.72f;
        }

        boolean starting = operatingTicks < PROCESSING_STARTED;
        int localTick = starting ? operatingTicks : OPERATING_FINISHED - operatingTicks;
        float adjustedTick = starting ? localTick + partialTicks : localTick - partialTicks;
        float progress = adjustedTick / PROCESSING_STARTED;
        progress = (2 - Mth.cos(progress * Mth.PI)) / 2;
        return (progress - 0.5f) * 0.72f;
    }

    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    public IGasHandler getGasCapability() {
        return gasCapability;
    }

    public IItemHandlerModifiable getItemCapability() {
        return itemCapability;
    }

    public LerpedFloat getIngredientRotation() {
        return ingredientRotation;
    }

    public LerpedFloat getMixerRotation() {
        return mixerRotation;
    }

    public LerpedFloat getWindowDistance() {
        return windowDistance;
    }

    public void notifyContentsChanged() {
        contentsChanged = true;
    }

    public void notifyFiltersChanged() {
        filterChanged = true;
    }

    public void scheduleUpdate() {
        updateChecker.scheduleUpdate();
    }

    public void startProcessInPonderLevel() {
        update(false);
        updateReactorKettle();
    }

    private boolean shouldKeepWindowsOpen() {
        boolean hasNoGas = inputGasTank.isEmpty() && outputGasTank.isEmpty();
        if (currentRecipe == null) {
            return hasNoGas;
        }

        return hasNoGas && currentRecipe.getGasIngredients().isEmpty() && currentRecipe.getGasResults().isEmpty();
    }

    private boolean updateReactorKettle() {
        if (level == null) {
            return false;
        }

        float speed = getProcessingSpeed();
        if (level.isClientSide && !isVirtual() || operating || speed < SpeedLevel.FAST.getSpeedValue()) {
            return true;
        }

        Optional<ReactorKettleRecipe> recipe = AirtightReactorKettleUtils.getMatchingRecipe(this);
        if (recipe.isPresent()) {
            currentRecipe = recipe.get();
            currentCraftingRecipe = null;
            operating = true;
            operatingTicks = 0;
            sendData();
            return true;
        }

        if (!CCBConfig.server().airtights.enableAutomaticMixingRecipes.get()) {
            currentRecipe = null;
            currentCraftingRecipe = null;
            return true;
        }

        Optional<RecipeHolder<CraftingRecipe>> craftingRecipe = AirtightReactorKettleUtils.getMatchingCraftingRecipe(this);
        if (craftingRecipe.isEmpty()) {
            currentRecipe = null;
            currentCraftingRecipe = null;
            return true;
        }

        currentRecipe = null;
        currentCraftingRecipe = craftingRecipe.get().value();
        operating = true;
        operatingTicks = 0;
        sendData();
        return true;
    }

    private float getProcessingSpeed() {
        float speed = Mth.abs(core.getStructureManager().getSpeed());
        if (level instanceof PonderLevel) {
            return SpeedLevel.FAST.getSpeedValue();
        }

        return speed;
    }

    private boolean hasRequiredSpeed() {
        float speed = level instanceof PonderLevel ? SpeedLevel.FAST.getSpeedValue() : Mth.abs(core.getStructureManager().getSpeed());
        return speed >= SpeedLevel.FAST.getSpeedValue();
    }

    @OnlyIn(Dist.CLIENT)
    private void tickAudio() {
        if (level == null || !level.isClientSide) {
            return;
        }

        float absSpeed = Mth.abs(core.getStructureManager().getSpeed());
        if (absSpeed == 0) {
            return;
        }

        float pitch = Mth.clamp(absSpeed / 256 + 0.45f, 0.85f, 1);
        SoundScapes.play(AmbienceGroup.KINETIC, worldPosition, pitch);
        if (absSpeed <= 64 && AnimationTickHolder.getTicks() % 2 == 0 || operatingTicks != PROCESSING_STARTED) {
            return;
        }

        CCBSoundEvents.REACTOR_KETTLE_MIXING.playAt(level, worldPosition, 0.75f, 1, true);
    }

    private void tickOperation() {
        if (level == null) {
            return;
        }

        boolean clientSide = level.isClientSide && !isVirtual();
        if (handleFilterChange(clientSide)) {
            return;
        }

        updateWindowsOpenState();
        updateRotationSpeed(operating && operatingTicks <= PROCESSING_STARTED);
        updateWindowDistance();
        if (!operating) {
            return;
        }

        if (operatingTicks >= OPERATING_FINISHED) {
            if (!clientSide) {
                update(true);
            }
            return;
        }

        if (!clientSide && !hasRequiredSpeed()) {
            update(false);
            return;
        }
        if (!clientSide && currentRecipe == null && currentCraftingRecipe != null && !CCBConfig.server().airtights.enableAutomaticMixingRecipes.get()) {
            update(false);
            return;
        }
        if (operatingTicks != PROCESSING_STARTED) {
            operatingTicks++;
            return;
        }
        if (clientSide) {
            return;
        }

        if (processingTicks < 0) {
            startProcessing();
            return;
        }

        processingTicks--;
        if (processingTicks == 0) {
            finishProcessing();
        }
    }

    private boolean handleFilterChange(boolean clientSide) {
        if (!filterChanged) {
            return false;
        }

        filterChanged = false;
        if (!clientSide) {
            update(true);
        }
        return true;
    }

    private void updateWindowsOpenState() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean shouldOpen = shouldKeepWindowsOpen();
        if (shouldOpen == windowsOpenState) {
            return;
        }

        windowsOpenState = shouldOpen;
        sendData();
    }

    private void startProcessing() {
        float recipeSpeed = currentRecipe == null ? 0 : currentRecipe.getProcessingDuration() / 100.0f;
        float speed = getProcessingSpeed();
        int baseProcessingTicks = Mth.clamp(Mth.log2((int) (256 / speed)) * Mth.ceil(recipeSpeed * 15) + 1, 1, 1000);
        processingTicks = Mth.clamp(Mth.ceil(baseProcessingTicks), 1, 1_000_000);
        if (level == null || inputFluidTank.isEmpty() && outputFluidTank.isEmpty()) {
            return;
        }

        level.playSound(null, getBlockPos(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.BLOCKS, 0.75f, speed < 64 ? 0.75f : 1.5f);
    }

    private void finishProcessing() {
        operatingTicks++;
        processingTicks = -1;
        if (level == null || level.isClientSide && !isVirtual()) {
            return;
        }
        if (!applyCurrentRecipe()) {
            update(false);
            return;
        }

        inputFluidTank.sendDataImmediately();
        inputGasTank.sendDataImmediately();
        contentsChanged = true;
        if (canContinueProcessing()) {
            operatingTicks = PROCESSING_STARTED;
        }

        sendData();
    }

    private boolean applyCurrentRecipe() {
        if (currentRecipe != null) {
            return ReactorKettleRecipe.apply(this, currentRecipe);
        }
        return currentCraftingRecipe != null && CCBConfig.server().airtights.enableAutomaticMixingRecipes.get() && AirtightReactorKettleUtils.applyCraftingRecipe(this, currentCraftingRecipe);
    }

    private boolean canContinueProcessing() {
        if (currentRecipe != null) {
            return ReactorKettleRecipe.match(this, currentRecipe);
        }

        return currentCraftingRecipe != null && CCBConfig.server().airtights.enableAutomaticMixingRecipes.get() && AirtightReactorKettleUtils.matchCraftingRecipe(this, currentCraftingRecipe);
    }

    private void update(boolean schedule) {
        operating = false;
        operatingTicks = 0;
        processingTicks = -1;
        currentRecipe = null;
        currentCraftingRecipe = null;
        sendData();
        if (!schedule || level == null || level.isClientSide && !isVirtual()) {
            return;
        }

        updateChecker.scheduleUpdate();
    }

    private void updateRotationSpeed(boolean moving) {
        float speed = Mth.clamp(core.getStructureManager().getSpeed() * 0.5f, -64, 64);
        if (level instanceof PonderLevel) {
            speed = SpeedLevel.FAST.getSpeedValue() * 0.5f;
        }

        boolean processing = operatingTicks > 15 && operatingTicks <= PROCESSING_STARTED;
        double ingredientSpeed = 0;
        double mixerSpeed = 0;
        if (moving) {
            mixerSpeed = processing ? speed * 2 : speed / 2;
            if (processing) {
                ingredientSpeed = speed * 0.5;
            }
        }

        ingredientRotationSpeed.chase(ingredientSpeed, 0.15, Chaser.EXP);
        mixerRotationSpeed.chase(mixerSpeed, 0.1, Chaser.EXP);
    }

    private void updateWindowDistance() {
        double target = windowsOpenState ? 0.5 : 0;
        double chaseSpeed = windowsOpenState ? 0.2 : 0.3;
        windowDistance.chase(target, chaseSpeed, Chaser.EXP);
        windowDistance.tickChaser();
    }
}