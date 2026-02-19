package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.CombinedGasTankWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AirtightReactorKettleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation {
    public static final int MAX_FLUID_CAPACITY = 9000;
    private static final String COMPOUND_KEY_INPUT_ITEMS = "InputItems";
    private static final String COMPOUND_KEY_OUTPUT_ITEMS = "OutputItems";
    private static final String COMPOUND_KEY_CORE = "Core";
    private static final String COMPOUND_KEY_OPERATING = "Operating";
    private static final String COMPOUND_KEY_OPERATING_TICKS = "OperatingTicks";
    private static final String COMPOUND_KEY_PROCESSING_TICKS = "ProcessingTicks";
    private static final String COMPOUND_KEY_OPEN_STATE = "OpenState";

    private static final int PROCESSING_STARTED = 20;
    private static final int OPERATING_FINISHED = 40;
    private static final int MAX_ITEM_SLOT = 27;
    private static final int MAX_GAS_CAPACITY = 90000;
    private static final int LAZY_TICK_RATE = 4;

    private final AirtightReactorKettleInventory inputInventory;
    private final SmartInventory outputInventory;
    private final LerpedFloat ingredientRotationSpeed;
    private final LerpedFloat ingredientRotation;
    private final LerpedFloat mixerRotationSpeed;
    private final LerpedFloat mixerRotation;
    private final LerpedFloat windowDistance;
    private final Couple<SmartInventory> inventories;
    private final Couple<SmartFluidTankBehaviour> fluidTanks;
    private final Couple<SmartGasTankBehaviour> gasTanks;
    private final IItemHandlerModifiable itemCapability;
    private final AirtightReactorKettleCore core;

    private boolean contentsChanged;
    private boolean filterChanged;
    private boolean operating;
    private DeferralBehaviour updateChecker;
    private IFluidHandler fluidCapability;
    private IGasHandler gasCapability;
    private int operatingTicks;
    private int processingTicks;
    private ReactorKettleRecipe currentRecipe;
    private SmartFluidTankBehaviour inputFluidTank;
    private SmartFluidTankBehaviour outputFluidTank;
    private SmartGasTankBehaviour inputGasTank;
    private SmartGasTankBehaviour outputGasTank;
    private boolean windowsOpenState = true;

    public AirtightReactorKettleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        core = new AirtightReactorKettleCore(this);
        inputInventory = new AirtightReactorKettleInventory(MAX_ITEM_SLOT, this);
        inputInventory.whenContentsChanged($ -> contentsChanged = true);
        outputInventory = new AirtightReactorKettleInventory(MAX_ITEM_SLOT, this).forbidInsertion().withMaxStackSize(64);
        outputInventory.whenContentsChanged($ -> contentsChanged = true);
        itemCapability = new CombinedInvWrapper(inputInventory, outputInventory);
        ingredientRotation = LerpedFloat.angular().startWithValue(0);
        ingredientRotationSpeed = LerpedFloat.linear().startWithValue(0);
        mixerRotation = LerpedFloat.angular().startWithValue(0);
        mixerRotationSpeed = LerpedFloat.linear().startWithValue(0);
        windowDistance = LerpedFloat.linear().startWithValue(0.5);
        inventories = Couple.create(inputInventory, outputInventory);
        fluidTanks = Couple.create(inputFluidTank, outputFluidTank);
        gasTanks = Couple.create(inputGasTank, outputGasTank);
        contentsChanged = true;
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (be, context) -> be.itemCapability);
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (be, context) -> be.fluidCapability);
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (be, context) -> be.gasCapability);
    }

    private static boolean acceptItemOutputsIntoKettle(@NotNull List<ItemStack> outputItems, boolean simulate, IItemHandler targetInv) {
        return outputItems.stream().allMatch(itemStack -> ItemHandlerHelper.insertItemStacked(targetInv, itemStack.copy(), simulate).isEmpty());
    }

    private static boolean acceptFluidOutputsIntoKettle(@NotNull List<FluidStack> outputFluids, boolean simulate, IFluidHandler targetTank) {
        return outputFluids.stream().noneMatch(fluidStack -> targetTank.fill(fluidStack.copy(), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE) != fluidStack.getAmount());
    }

    private static boolean acceptGasOutputsIntoKettle(@NotNull List<GasStack> outputGases, boolean simulate, IGasHandler targetTank) {
        return outputGases.stream().noneMatch(gasStack -> targetTank.fill(gasStack.copy(), simulate ? GasAction.SIMULATE : GasAction.EXECUTE) != gasStack.getAmount());
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        inputFluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 3, MAX_FLUID_CAPACITY, true).whenFluidUpdates(() -> contentsChanged = true);
        outputFluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 2, MAX_FLUID_CAPACITY, true).forbidInsertion().whenFluidUpdates(() -> contentsChanged = true);
        fluidCapability = new CombinedTankWrapper(inputFluidTank.getCapability(), outputFluidTank.getCapability());
        behaviours.add(inputFluidTank);
        behaviours.add(outputFluidTank);

        inputGasTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.INPUT, this, 3, MAX_GAS_CAPACITY, true).whenGasUpdates(() -> contentsChanged = true);
        outputGasTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, this, 2, MAX_GAS_CAPACITY, true).forbidInsertion().whenGasUpdates(() -> contentsChanged = true);
        gasCapability = new CombinedGasTankWrapper(inputGasTank.getCapability(), outputGasTank.getCapability());
        behaviours.add(inputGasTank);
        behaviours.add(outputGasTank);

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
            CatnipServices.PLATFORM.executeOnClientOnly(() -> this::tickAudio);
            ingredientRotationSpeed.tickChaser();
            ingredientRotation.setValue(ingredientRotation.getValue() + ingredientRotationSpeed.getValue());
            mixerRotationSpeed.tickChaser();
            mixerRotation.setValue(mixerRotation.getValue() + mixerRotationSpeed.getValue());
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
        if (filterChanged) {
            filterChanged = false;
        }
        if (level == null || level.isClientSide) {
            return;
        }

        core.lazyTick();
        if (level.getGameTime() % 20 != 10) {
            return;
        }

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

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        core.getTooltipBuilder().addToGoggleTooltip(tooltip);
        return true;
    }

    @Override
    public boolean addToTooltip(@NotNull List<Component> tooltip, boolean isPlayerSneaking) {
        return core.getTooltipBuilder().addToTooltip(tooltip);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(1, 1, 1);
    }

    public void scheduleUpdate() {
        updateChecker.scheduleUpdate();
    }

    public boolean isEmpty() {
        return inputInventory.isEmpty() && outputInventory.isEmpty() && inputFluidTank.isEmpty() && outputFluidTank.isEmpty() && inputGasTank.isEmpty() && outputGasTank.isEmpty();
    }

    public boolean getWindowsOpenState() {
        return windowsOpenState;
    }

    public void notifyContentsChanged() {
        contentsChanged = true;
    }

    public void notifyFiltersChanged() {
        filterChanged = true;
    }

    public boolean isFilterChanged() {
        return filterChanged;
    }

    public IItemHandlerModifiable getItemCapability() {
        return itemCapability;
    }

    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    public IGasHandler getGasCapability() {
        return gasCapability;
    }

    public Couple<SmartInventory> getInventories() {
        return inventories;
    }

    public Couple<SmartFluidTankBehaviour> getFluidTanks() {
        return fluidTanks;
    }

    public Couple<SmartGasTankBehaviour> getGasTanks() {
        return gasTanks;
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

    public AirtightReactorKettleCore getCore() {
        return core;
    }

    @Nullable
    public FilteringBehaviour getFilteringBehaviour() {
        if (level == null || !(level.getBlockEntity(getBlockPos().below().north()) instanceof AirtightReactorKettleStructuralBlockEntity structural)) {
            return null;
        }

        return structural.getFilteringBehaviour();
    }

    public boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases, boolean simulate) {
        outputInventory.allowInsertion();
        outputFluidTank.allowInsertion();
        outputGasTank.allowInsertion();
        boolean acceptOutputsInner = acceptOutputsInner(outputItems, outputFluids, outputGases, simulate);
        outputInventory.forbidInsertion();
        outputFluidTank.forbidInsertion();
        outputGasTank.forbidInsertion();
        return acceptOutputsInner;
    }

    public boolean shouldKeepWindowsOpen() {
        boolean empty = inputGasTank.isEmpty() && outputGasTank.isEmpty();
        if (currentRecipe == null) {
            return empty;
        }

        return empty && currentRecipe.getGasIngredients().isEmpty() && currentRecipe.getGasResults().isEmpty();
    }

    public float getMixerOffset(float partialTicks) {
        int localTick;
        float offset = 0;
        if (operating) {
            if (operatingTicks < 20) {
                localTick = operatingTicks;
                float num = (localTick + partialTicks) / PROCESSING_STARTED;
                num = (2 - Mth.cos(num * Mth.PI)) / 2;
                offset = num - 0.5f;
            }
            else if (operatingTicks == PROCESSING_STARTED) {
                offset = 1;
            }
            else {
                localTick = OPERATING_FINISHED - operatingTicks;
                float num = (localTick - partialTicks) / PROCESSING_STARTED;
                num = (2 - Mth.cos(num * Mth.PI)) / 2;
                offset = num - 0.5f;
            }
        }
        return offset * 0.72f;
    }

    public float getDamage() {
        if (!operating) {
            return 0;
        }

        float absSpeed = Mth.abs(core.getStructureManager().getSpeed());
        if (absSpeed == 0) {
            return 0;
        }

        return absSpeed / 32;
    }

    public void startProcessInPonderLevel() {
        operating = true;
        operatingTicks = 0;
        updateReactorKettle();
    }

    private boolean acceptOutputsInner(List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases, boolean simulate) {
        IItemHandler targetInventory = outputInventory;
        IFluidHandler targetFluidTank = outputFluidTank.getCapability();
        IGasHandler targetGasTank = outputGasTank.getCapability();
        if (targetInventory == null && !outputItems.isEmpty() || !acceptItemOutputsIntoKettle(outputItems, simulate, targetInventory)) {
            return false;
        }
        if (!outputFluids.isEmpty() && targetFluidTank == null || !acceptFluidOutputsIntoKettle(outputFluids, simulate, targetFluidTank)) {
            return false;
        }
        if (!outputGases.isEmpty() && targetGasTank == null || !acceptGasOutputsIntoKettle(outputGases, simulate, targetGasTank)) {
            return false;
        }

        return true;
    }

    private void tickOperation() {
        if (filterChanged) {
            operating = false;
            operatingTicks = 0;
            processingTicks = -1;
            updateChecker.scheduleUpdate();
            return;
        }

        if (level != null && !level.isClientSide) {
            windowsOpenState = shouldKeepWindowsOpen();
        }

        updateRotationSpeed(operating && operatingTicks <= PROCESSING_STARTED);
        updateWindowDistance();
        float absSpeed = level instanceof PonderLevel ? SpeedLevel.FAST.getSpeedValue() : Mth.abs(core.getStructureManager().getSpeed());
        if (operatingTicks >= OPERATING_FINISHED || absSpeed < AirtightReactorKettleUtils.getMinSpeedRequired()) {
            operating = false;
            operatingTicks = 0;
            processingTicks = -1;
            updateChecker.scheduleUpdate();
            return;
        }

        if (!operating || level == null) {
            return;
        }

        if (operatingTicks != PROCESSING_STARTED) {
            operatingTicks++;
            return;
        }

        startProcessing();
    }

    private boolean isProcessing() {
        return operatingTicks > 15 && operatingTicks <= PROCESSING_STARTED;
    }

    private void updateRotationSpeed(boolean moving) {
        float speed = Mth.clamp(core.getStructureManager().getSpeed() * 0.5f, -64, 64);
        if (level instanceof PonderLevel) {
            speed = SpeedLevel.FAST.getSpeedValue() * 0.5f;
        }
        boolean processing = isProcessing();
        double ingredientSpeed = processing ? speed * 0.5 : 0;
        ingredientRotationSpeed.chase(moving ? ingredientSpeed : 0, 0.15, Chaser.EXP);
        double mixSpeed = processing ? speed * 2 : speed / 2;
        mixerRotationSpeed.chase(moving ? mixSpeed : 0, 0.1, Chaser.EXP);
    }

    private void updateWindowDistance() {
        if (windowsOpenState) {
            windowDistance.chase(0.5, 0.2, Chaser.EXP);
        }
        else {
            windowDistance.chase(0, 0.3, Chaser.EXP);
        }

        windowDistance.tickChaser();
    }

    private boolean updateReactorKettle() {
        if (level == null) {
            return false;
        }

        float speed = Mth.abs(core.getStructureManager().getSpeed());
        if (level instanceof PonderLevel) {
            speed = SpeedLevel.FAST.getSpeedValue();
        }
        if (level.isClientSide && !isVirtual() || operating || speed < AirtightReactorKettleUtils.getMinSpeedRequired()) {
            return true;
        }

        List<ReactorKettleRecipe> recipes = AirtightReactorKettleUtils.getMatchingRecipes(this, core);
        if (recipes.isEmpty()) {
            currentRecipe = null;
            return true;
        }

        currentRecipe = recipes.getFirst();
        startProcessingKettle();
        sendData();
        return true;
    }

    private void startProcessingKettle() {
        if (operating && operatingTicks <= PROCESSING_STARTED) {
            return;
        }

        operating = true;
        operatingTicks = 0;
    }

    private void startProcessing() {
        if (level == null) {
            return;
        }

        if (processingTicks < 0) {
            float recipeSpeed = 1;
            if (currentRecipe instanceof ReactorKettleRecipe kettleRecipe) {
                recipeSpeed = kettleRecipe.getProcessingDuration() / 100.0f;
            }
            float absSpeed = Mth.abs(core.getStructureManager().getSpeed());
            if (level instanceof PonderLevel) {
                absSpeed = SpeedLevel.FAST.getSpeedValue();
            }
            processingTicks = Mth.clamp(Mth.log2((int) (256 / absSpeed)) * Mth.ceil(recipeSpeed * 15) + 1, 1, 1000);
            if (fluidTanks.both(SmartFluidTankBehaviour::isEmpty)) {
                return;
            }

            level.playSound(null, getBlockPos(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.BLOCKS, 0.75f, absSpeed < 64 ? 0.75f : 1.5f);
            return;
        }

        processingTicks--;
        if (processingTicks != 0) {
            return;
        }

        operatingTicks++;
        processingTicks = -1;
        applyRecipe();
        sendData();
    }

    private void applyRecipe() {
        if (level == null || level.isClientSide && !isVirtual() || currentRecipe == null || !ReactorKettleRecipe.apply(this, currentRecipe)) {
            return;
        }

        inputFluidTank.sendDataImmediately();
        inputGasTank.sendDataImmediately();
        if (ReactorKettleRecipe.match(this, currentRecipe)) {
            operatingTicks = PROCESSING_STARTED;
            sendData();
        }
        contentsChanged = true;
    }

    @OnlyIn(Dist.CLIENT)
    private void tickAudio() {
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
}