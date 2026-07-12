package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

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
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import net.createmod.catnip.data.Couple;
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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation, IGasInventoryIdentifierProvider {
    public static final int MAX_FLUID_CAPACITY = 3000;

    private static final int MAX_INPUT_SLOT = 1;
    private static final int MAX_OUTPUT_SLOT = 8;
    private static final int LAZY_TICK_RATE = 4;
    private static final int OPERATING_FINISHED = 30;
    private static final int PROCESSING_STARTED = 15;
    private static final float PRESS_HEAD_IDLE_OFFSET = -0.625f;
    private static final float PRESS_HEAD_TRAVEL = 0.8125f;

    private static final String COMPOUND_KEY_CORE = "Core";
    private static final String COMPOUND_KEY_INPUT_ITEMS = "InputItems";
    private static final String COMPOUND_KEY_OPERATING = "Operating";
    private static final String COMPOUND_KEY_OPERATING_TICKS = "OperatingTicks";
    private static final String COMPOUND_KEY_OUTPUT_ITEMS = "OutputItems";
    private static final String COMPOUND_KEY_PRESS_HEAD_ITEMS = "PressHeadItems";
    private static final String COMPOUND_KEY_PROCESSING_ITEMS = "ProcessingItems";

    private final AirtightForgingPressCore core;
    private final Couple<SmartInventory> inputOutputInventories;
    private final Couple<SmartInventory> processingInventories;
    private final IItemHandlerModifiable inputOutputCapability;
    private final IItemHandlerModifiable itemCapability;
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

    public AirtightForgingPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        core = new AirtightForgingPressCore(this);

        pressHeadInventory = new SmartInventory(MAX_INPUT_SLOT, this, 1, false, ($, s) -> s.is(CCBItemTags.PRESS_HEAD_TOOLS.tag) || s.getItem() instanceof SmithingTemplateItem).whenContentsChanged($ -> contentsChanged = true);
        processingInventory = new SmartInventory(MAX_INPUT_SLOT, this).whenContentsChanged($ -> contentsChanged = true);
        processingInventories = Couple.create(pressHeadInventory, processingInventory);
        inputInventory = new SmartInventory(MAX_INPUT_SLOT, this).whenContentsChanged($ -> contentsChanged = true);
        outputInventory = new SmartInventory(MAX_OUTPUT_SLOT, this).forbidInsertion().whenContentsChanged($ -> contentsChanged = true);
        inputOutputCapability = new CombinedInvWrapper(inputInventory, outputInventory);
        inputOutputInventories = Couple.create(inputInventory, outputInventory);
        itemCapability = new CombinedInvWrapper(pressHeadInventory, processingInventory, inputInventory, outputInventory);

        contentsChanged = true;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS.get(), (be, direction) -> be.pressHeadInventory);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        fluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 1, MAX_FLUID_CAPACITY, false).whenFluidUpdates(() -> contentsChanged = true);
        fluidCapability = fluidTank.getCapability();
        behaviours.add(fluidTank);

        gasTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.INPUT, this, 1, MAX_FLUID_CAPACITY * 10L, false).whenGasUpdates(() -> contentsChanged = true);
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
        updateChecker.scheduleUpdate();
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_CORE, core.write());
        compoundTag.put(COMPOUND_KEY_PRESS_HEAD_ITEMS, pressHeadInventory.serializeNBT(provider));
        compoundTag.put(COMPOUND_KEY_PROCESSING_ITEMS, processingInventory.serializeNBT(provider));
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

    public void notifyContentsChanged() {
        contentsChanged = true;
    }

    public void notifyFiltersChanged() {
        filterChanged = true;
    }

    public boolean isFilterChanged() {
        return filterChanged;
    }

    public Couple<SmartInventory> getInputOutputInventories() {
        return inputOutputInventories;
    }

    public Couple<SmartInventory> getProcessingInventories() {
        return processingInventories;
    }

    public IItemHandlerModifiable getInputOutputCapability() {
        return inputOutputCapability;
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

    public SmartFluidTankBehaviour getFluidTank() {
        return fluidTank;
    }

    public SmartGasTankBehaviour getGasTank() {
        return gasTank;
    }

    @Nullable
    public FilteringBehaviour getFilteringBehaviour() {
        if (level == null || !(level.getBlockEntity(getBlockPos().below().north()) instanceof AirtightForgingPressStructuralBlockEntity structural)) {
            return null;
        }

        return structural.getFilteringBehaviour();
    }

    public boolean acceptOutputs(List<ItemStack> outputItems, boolean simulate) {
        SmartInventory simulatedOutput = new SmartInventory(outputInventory.getSlots(), this);
        simulatedOutput.allowInsertion();
        for (int slot = 0; slot < outputInventory.getSlots(); slot++) {
            simulatedOutput.setStackInSlot(slot, outputInventory.getStackInSlot(slot).copy());
        }
        for (ItemStack stack : outputItems) {
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(simulatedOutput, stack.copy(), false);
            if (!remainder.isEmpty()) {
                return false;
            }
        }

        if (simulate) {
            return true;
        }

        outputInventory.allowInsertion();
        try {
            for (ItemStack stack : outputItems) {
                if (stack.isEmpty()) {
                    continue;
                }

                ItemStack remainder = ItemHandlerHelper.insertItemStacked(outputInventory, stack.copy(), false);
                if (remainder.isEmpty()) {
                    continue;
                }

                return false;
            }

            return true;
        } finally {
            outputInventory.forbidInsertion();
        }
    }

    public float getPressHeadDistance(float partialTicks) {
        float distance;
        if (operating) {
            float ticks = Mth.clamp(operatingTicks + partialTicks * getOperationSpeed(), 0, OPERATING_FINISHED);
            if (ticks < 20) {
                distance = Mth.clamp((float) Math.pow(ticks / OPERATING_FINISHED * 2.0f, 3), 0, 1);
            }
            else {
                distance = Mth.clamp((OPERATING_FINISHED - ticks) / OPERATING_FINISHED * 3.0f, 0, 1);
            }
        }
        else {
            distance = 0;
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
        if (level == null) {
            return false;
        }

        if (level.isClientSide && !isVirtual() || operating || getOperationSpeed() <= 0) {
            return true;
        }

        List<ForgingPressRecipe> recipes = AirtightForgingPressUtils.getMatchingRecipes(this, core);
        if (!recipes.isEmpty()) {
            currentRecipe = recipes.getFirst();
            currentSmithingRecipe = null;
            operating = true;
            operatingTicks = 0;
            sendData();
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
        operating = true;
        operatingTicks = 0;
        sendData();
        return true;
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

        if (operatingTicks >= OPERATING_FINISHED) {
            update(true);
            return;
        }

        float operationSpeed = getOperationSpeed();
        if (operationSpeed <= 0) {
            update(false);
            return;
        }



        float previousTicks = operatingTicks;
        operatingTicks = Mth.clamp(operatingTicks + operationSpeed, 0, OPERATING_FINISHED);
        if (level == null || level.isClientSide ) {
            return;
        }

        if (previousTicks >= PROCESSING_STARTED || operatingTicks < PROCESSING_STARTED || currentRecipe == null && currentSmithingRecipe == null) {
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
}
