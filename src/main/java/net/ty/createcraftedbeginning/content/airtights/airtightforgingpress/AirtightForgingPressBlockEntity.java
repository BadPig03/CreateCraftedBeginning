package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.logistics.GasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.gas.behaviour.OverpressureBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.overpressure.PressureRuptureService;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation, GasInventoryIdentifierProvider, ForgingPressRecipeContext {
    private static final int MAX_INPUT_SLOT = 1;
    private static final int MAX_OUTPUT_SLOT = 8;
    private static final int LAZY_TICK_RATE = 4;

    private final AirtightForgingPressCore core;
    private final AirtightForgingPressController controller;
    private final AirtightForgingPressCrafting crafting;
    private final AirtightForgingPressSerialization serialization;
    private final IItemHandler inputOutputCapability;
    private final IItemHandlerModifiable recipeInputCapability;
    private final SmartInventory inputInventory;
    private final SmartInventory outputInventory;
    private final SmartInventory pressHeadInventory;
    private final SmartInventory processingInventory;

    private DeferralBehaviour updateChecker;
    private IFluidHandler fluidCapability;
    private GasStorageHandler gasCapability;
    private SmartFluidTankBehaviour fluidTank;
    private SmartGasTankBehaviour gasTank;
    private OverpressureBehaviour overpressureBehaviour;
    private ItemStack recipeFilter = ItemStack.EMPTY;

    public AirtightForgingPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        core = new AirtightForgingPressCore(this);

        pressHeadInventory = new SmartInventory(MAX_INPUT_SLOT, this, 1, false, (ignoredSlot, stack) -> stack.is(CCBItemTags.PRESS_HEAD_TOOLS.tag) || stack.getItem() instanceof SmithingTemplateItem).whenContentsChanged(ignored -> notifyContentsChanged());
        processingInventory = new AirtightForgingPressInventory(MAX_INPUT_SLOT, this).whenContentsChanged(ignored -> notifyContentsChanged());
        inputInventory = new AirtightForgingPressInventory(MAX_INPUT_SLOT, this).whenContentsChanged(ignored -> notifyContentsChanged());
        outputInventory = new AirtightForgingPressInventory(MAX_OUTPUT_SLOT, this).forbidInsertion().whenContentsChanged(ignored -> notifyContentsChanged());
        inputOutputCapability = new AirtightForgingPressPortHandler(inputInventory, outputInventory);
        recipeInputCapability = new CombinedInvWrapper(pressHeadInventory, processingInventory, inputInventory);

        controller = new AirtightForgingPressController(this);
        crafting = new AirtightForgingPressCrafting(this);
        serialization = new AirtightForgingPressSerialization(this, controller);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new CCBAdvancementBehaviour(this, CCBAdvancements.SUPERMASSIVE));
        fluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 1, Math.max(1, CCBConfig.server().machines.airtightForgingPress.fluidCapacity.get()) * FluidType.BUCKET_VOLUME, false).whenFluidUpdates(this::notifyContentsChanged);
        fluidCapability = fluidTank.getCapability();
        behaviours.add(fluidTank);

        gasTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.INPUT, this, 1, Math.max(1, CCBConfig.server().machines.airtightForgingPress.gasVolume.get()) * GasUnits.LITERS_PER_KILOLITER, GasPressureLimits.HARD_PRESSURE_PA, false).whenTankUpdates(this::notifyContentsChanged);
        gasCapability = gasTank.getCapability();
        overpressureBehaviour = new OverpressureBehaviour(this, this::getCurrentGasPressurePa);
        behaviours.add(gasTank);
        behaviours.add(overpressureBehaviour);

        updateChecker = new DeferralBehaviour(this, this::updateForgingPress);
        behaviours.add(updateChecker);
    }

    @Override
    public void tick() {
        super.tick();
        if (ruptureIfOverstressed()) {
            return;
        }

        controller.tick();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        core.lazyTick();
        controller.lazyTick();
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
        releaseStoredGas();
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, pressHeadInventory);
        ItemHelper.dropContents(level, worldPosition, processingInventory);
        ItemHelper.dropContents(level, worldPosition, inputInventory);
        ItemHelper.dropContents(level, worldPosition, outputInventory);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        core.getTooltipBuilder().addToGoggleTooltip(tooltip, isPlayerSneaking);
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
    public InventoryIdentifier getGasInventoryIdentifier(Direction ignoredDirection) {
        return new Single(worldPosition);
    }

    @Override
    public SmartInventory getPressHeadInventory() {
        return pressHeadInventory;
    }

    @Override
    public SmartInventory getAdditionInventory() {
        return processingInventory;
    }

    @Override
    public SmartInventory getInputInventory() {
        return inputInventory;
    }

    @Override
    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    @Override
    public GasStorageHandler getGasCapability() {
        return gasCapability;
    }

    @Override
    public boolean testRecipeFilter(ItemStack stack) {
        return recipeFilter.isEmpty() || level != null && FilterItemStack.of(recipeFilter).test(level, stack);
    }

    @Override
    public Optional<OutputPlan> planOutputs(List<ItemStack> outputItems) {
        return crafting.planOutputs(outputItems);
    }

    @Override
    public boolean acceptOutputs(List<ItemStack> outputItems, boolean simulate) {
        return crafting.acceptOutputs(outputItems, simulate);
    }

    @Override
    public ConsumptionPlan createConsumptionPlan(ItemStack expectedProcessingStack, int processingAmount, ItemStack expectedInputStack, int inputAmount, int[] fluidAmounts, GasConsumptionPlan gasPlan) {
        return crafting.createConsumptionPlan(expectedProcessingStack, processingAmount, expectedInputStack, inputAmount, fluidAmounts, gasPlan);
    }

    @Override
    public synchronized boolean commitCraft(ConsumptionPlan consumptionPlan, OutputPlan outputPlan) {
        return crafting.commitCraft(consumptionPlan, outputPlan);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FORGING_PRESS.get(), (press, ignoredDirection) -> press.pressHeadInventory);
    }

    public void startProcessInPonderLevel() {
        controller.startProcessInPonderLevel();
    }

    public SmartInventory getOutputInventory() {
        return outputInventory;
    }

    public IItemHandler getInputOutputCapability() {
        return inputOutputCapability;
    }

    @Internal
    public void setRecipeFilter(ItemStack stack) {
        ItemStack normalizedFilter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.matches(recipeFilter, normalizedFilter)) {
            return;
        }

        recipeFilter = normalizedFilter;
        controller.notifyFilterChanged();
        syncRecipeFilterReplicas();
        setChanged();
        sendData();
    }

    void scheduleUpdate() {
        updateChecker.scheduleUpdate();
    }

    boolean hasRecipeInputs() {
        return !inputInventory.isEmpty() || !processingInventory.isEmpty() || !fluidTank.isEmpty() || !gasTank.isEmpty();
    }

    void notifyContentsChanged() {
        if (controller == null) {
            return;
        }

        controller.notifyContentsChanged();
    }

    IItemHandlerModifiable getRecipeInputCapability() {
        return recipeInputCapability;
    }

    ItemStack getRecipeFilter() {
        return recipeFilter.copy();
    }

    float getPressHeadDistance(float partialTicks) {
        return controller.getPressHeadDistance(partialTicks);
    }

    AirtightForgingPressCore getCore() {
        return core;
    }

    SmartFluidTankBehaviour getFluidTankBehaviour() {
        return fluidTank;
    }

    SmartGasTankBehaviour getGasTankBehaviour() {
        return gasTank;
    }

    OverpressureBehaviour getOverpressureBehaviour() {
        return overpressureBehaviour;
    }

    void loadRecipeFilter(ItemStack stack) {
        recipeFilter = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    private boolean ruptureIfOverstressed() {
        if (level == null || level.isClientSide || overpressureBehaviour.getFailureReadyChannel() < 0) {
            return false;
        }

        PressureRuptureService.rupture(level, worldPosition, gasTank.getPrimaryHandler());
        return true;
    }

    private void releaseStoredGas() {
        if (level == null || level.isClientSide || gasTank == null) {
            return;
        }

        GasStorageHandler gasHandler = gasTank.getCapability();
        gasTank.beginMutation();
        try {
            for (int tank = 0; tank < gasHandler.getTanks(); tank++) {
                GasPressureCompartment compartment = gasHandler.getPressureCompartment(tank);
                long storedAmount = compartment.getStoredAmount();
                if (storedAmount <= 0) {
                    continue;
                }

                long sourcePressurePa = compartment.getPressurePa();
                GasStack releasedGas = compartment.drain(storedAmount, GasAction.EXECUTE);
                if (releasedGas.isEmpty()) {
                    continue;
                }

                GasReleaseService.release(level, GasReleaseRequest.radial(releasedGas, worldPosition, GasReleaseCause.TANK_REMOVAL, sourcePressurePa));
            }
        }
        finally {
            gasTank.endMutation();
        }
    }

    private long getCurrentGasPressurePa() {
        if (gasTank == null) {
            return GasPressure.VACUUM_PA;
        }

        return gasTank.getPrimaryHandler().getPressurePa();
    }

    private boolean updateForgingPress() {
        return controller.updateForgingPress();
    }

    private void syncRecipeFilterReplicas() {
        if (level == null) {
            return;
        }

        for (AirtightForgingPressStructuralPosition structuralPosition : AirtightForgingPressStructuralPosition.all()) {
            if (!structuralPosition.isFilter()) {
                continue;
            }

            BlockPos filterPos = worldPosition.offset(structuralPosition.getStructureOffset());
            if (!(level.getBlockEntity(filterPos) instanceof AirtightForgingPressStructuralBlockEntity filterPart)) {
                continue;
            }

            filterPart.syncFilterFromMaster(recipeFilter);
        }
    }
}
