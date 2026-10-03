package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
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
import net.ty.createcraftedbeginning.gas.storage.handler.CombinedGasStorageHandler;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.interfaces.ReactorKettleRecipeContext;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.LongSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation, GasInventoryIdentifierProvider, ReactorKettleRecipeContext {
    private static final int LAZY_TICK_RATE = 4;
    private static final int MAX_ITEM_SLOT = 27;

    private final AirtightReactorKettleCore core;
    private final AirtightReactorKettleAnimationState animationState;
    private final AirtightReactorKettleController controller;
    private final AirtightReactorKettleCrafting crafting;
    private final AirtightReactorKettleSerialization serialization;
    private final AirtightReactorKettleInventory inputInventory;
    private final SmartInventory outputInventory;
    private final Couple<SmartInventory> inventories;
    private final IItemHandlerModifiable recipeItemCapability;
    private final IItemHandler itemPortCapability;

    private DeferralBehaviour updateChecker;
    private IFluidHandler recipeFluidCapability;
    private IFluidHandler fluidPortCapability;
    private GasStorageHandler recipeGasCapability;
    private GasHandler gasPortCapability;
    private SmartFluidTankBehaviour inputFluidTank;
    private SmartFluidTankBehaviour outputFluidTank;
    private SmartGasTankBehaviour inputGasTank;
    private SmartGasTankBehaviour outputGasTank;
    private OverpressureBehaviour overpressureBehaviour;
    private CCBAdvancementBehaviour advancementBehaviour;
    private ItemStack recipeFilter = ItemStack.EMPTY;
    private boolean recipeFilterAuthoritative = true;

    public AirtightReactorKettleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        core = new AirtightReactorKettleCore(this);

        inputInventory = new AirtightReactorKettleInventory(MAX_ITEM_SLOT, this);
        inputInventory.whenContentsChanged(ignored -> notifyContentsChanged());
        outputInventory = new AirtightReactorKettleInventory(MAX_ITEM_SLOT, this).forbidInsertion();
        outputInventory.whenContentsChanged(ignored -> notifyContentsChanged());
        recipeItemCapability = new CombinedInvWrapper(inputInventory, outputInventory);
        itemPortCapability = new AirtightReactorKettlePortHandler(inputInventory, outputInventory);
        inventories = Couple.create(inputInventory, outputInventory);

        animationState = new AirtightReactorKettleAnimationState(this);
        controller = new AirtightReactorKettleController(this, animationState);
        crafting = new AirtightReactorKettleCrafting(this);
        serialization = new AirtightReactorKettleSerialization(this, controller);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (kettle, direction) -> kettle.itemPortCapability);
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (kettle, direction) -> kettle.fluidPortCapability);
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (kettle, direction) -> kettle.gasPortCapability);
    }

    @Internal
    public static int getFluidCapacity() {
        return Math.max(1, CCBConfig.server().machines.airtightReactorKettle.fluidCapacityPerTank.get()) * FluidType.BUCKET_VOLUME;
    }

    static long getGasCapacity() {
        return Math.max(1, CCBConfig.server().machines.airtightReactorKettle.gasVolumePerTank.get()) * GasUnits.LITERS_PER_KILOLITER;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.BUNDLE_OF_JOY);
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

        if (!level.isClientSide && ruptureIfOverstressed()) {
            return;
        }

        if (level.isClientSide) {
            animationState.tickClient();
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
        releaseStoredGases();
        super.destroy();
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
    public InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        return new Single(worldPosition);
    }

    @Override
    public IItemHandlerModifiable getAvailableItems() {
        return recipeItemCapability;
    }

    @Override
    public IFluidHandler getAvailableFluids() {
        return recipeFluidCapability;
    }

    @Override
    public GasStorageHandler getAvailableGases() {
        return recipeGasCapability;
    }

    @Override
    public IItemHandler getOutputItemCapability() {
        return outputInventory;
    }

    @Override
    public IFluidHandler getOutputFluidCapability() {
        return outputFluidTank.getCapability();
    }

    @Override
    public GasHandler getOutputGasCapability() {
        return outputGasTank.getCapability();
    }

    @Override
    public float getRecipeTemperature() {
        return core.getStructureManager().getTemperature();
    }

    @Override
    public boolean matchesRecipeFilter(ReactorKettleRecipe recipe) {
        return AirtightReactorKettleRecipeFilter.matches(this, recipe);
    }

    @Override
    public boolean commitRecipeCraft(int[] itemAmounts, int[] fluidAmounts, GasConsumptionPlan gasPlan, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        return commitCraft(createCraftPlan(itemAmounts, fluidAmounts, gasPlan, outputItems, outputFluids, outputGases));
    }

    public void startProcessInPonderLevel() {
        controller.startProcessInPonderLevel();
    }

    @Internal
    public AirtightReactorKettleCore getCore() {
        return core;
    }

    @Internal
    public CraftPlan createCraftPlan(int[] itemAmounts, int[] fluidAmounts, GasConsumptionPlan gasPlan, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        return crafting.createCraftPlan(itemAmounts, fluidAmounts, gasPlan, outputItems, outputFluids, outputGases);
    }

    @Internal
    public synchronized boolean commitCraft(CraftPlan plan) {
        return crafting.commitCraft(plan);
    }

    @Internal
    public SmartFluidTankBehaviour getInputFluidTank() {
        return inputFluidTank;
    }

    @Internal
    public SmartFluidTankBehaviour getOutputFluidTank() {
        return outputFluidTank;
    }

    @Internal
    public SmartGasTankBehaviour getInputGasTank() {
        return inputGasTank;
    }

    @Internal
    public SmartGasTankBehaviour getOutputGasTank() {
        return outputGasTank;
    }

    @Internal
    public AirtightReactorKettleInventory getInputInventory() {
        return inputInventory;
    }

    @Internal
    public SmartInventory getOutputInventory() {
        return outputInventory;
    }

    @Internal
    public GasHandler getGasPortCapability() {
        return gasPortCapability;
    }

    @Internal
    public void setRecipeFilter(ItemStack stack) {
        ItemStack normalizedFilter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.matches(recipeFilter, normalizedFilter)) {
            return;
        }

        recipeFilter = normalizedFilter;
        recipeFilterAuthoritative = true;
        notifyFiltersChanged();
        syncRecipeFilterReplicas();
        setChanged();
        sendData();
    }

    boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        return crafting.acceptOutputs(outputItems, outputFluids, outputGases);
    }

    boolean getWindowsOpenState() {
        return controller.getWindowsOpenState();
    }

    boolean isEmpty() {
        return inputInventory.isEmpty() && outputInventory.isEmpty() && inputFluidTank.isEmpty() && outputFluidTank.isEmpty() && inputGasTank.isEmpty() && outputGasTank.isEmpty();
    }

    OverpressureBehaviour getOverpressureBehaviour() {
        return overpressureBehaviour;
    }

    Couple<SmartInventory> getInventories() {
        return inventories;
    }

    float getDamage() {
        return controller.getDamage();
    }

    float getMixerOffset(float partialTicks) {
        return controller.getMixerOffset(partialTicks);
    }

    LerpedFloat getIngredientRotation() {
        return animationState.getIngredientRotation();
    }

    LerpedFloat getMixerRotation() {
        return animationState.getMixerRotation();
    }

    LerpedFloat getWindowDistance() {
        return animationState.getWindowDistance();
    }

    void notifyContentsChanged() {
        if (controller == null) {
            return;
        }

        controller.notifyContentsChanged();
    }

    void scheduleUpdate() {
        updateChecker.scheduleUpdate();
    }

    AirtightReactorKettleController getController() {
        return controller;
    }

    void awardBackToBasics() {
        advancementBehaviour.awardPlayer(CCBAdvancements.BUNDLE_OF_JOY);
    }

    IItemHandler getItemPortCapability() {
        return itemPortCapability;
    }

    IFluidHandler getFluidPortCapability() {
        return fluidPortCapability;
    }

    boolean testRecipeFilter(ItemStack stack) {
        return recipeFilter.isEmpty() || level != null && FilterItemStack.of(recipeFilter).test(level, stack);
    }

    boolean testRecipeFilter(FluidStack stack) {
        return recipeFilter.isEmpty() || level != null && FilterItemStack.of(recipeFilter).test(level, stack);
    }

    ItemStack getRecipeFilter() {
        return recipeFilter.copy();
    }

    void loadRecipeFilter(ItemStack stack, boolean authoritative) {
        recipeFilter = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        recipeFilterAuthoritative = authoritative;
    }

    boolean hasAuthoritativeRecipeFilter() {
        return recipeFilterAuthoritative;
    }

    private boolean ruptureIfOverstressed() {
        int failedChannel = overpressureBehaviour.getFailureReadyChannel();
        if (failedChannel < 0 || recipeGasCapability == null || level == null) {
            return false;
        }

        PressureRuptureService.rupture(level, worldPosition, recipeGasCapability.getPressureCompartment(failedChannel));
        return true;
    }

    private void releaseStoredGases() {
        if (level == null || level.isClientSide) {
            return;
        }

        releaseStoredGases(inputGasTank);
        releaseStoredGases(outputGasTank);
    }

    private void releaseStoredGases(@Nullable SmartGasTankBehaviour gasTankBehaviour) {
        if (gasTankBehaviour == null || level == null) {
            return;
        }

        GasStorageHandler gasHandler = gasTankBehaviour.getCapability();
        gasTankBehaviour.beginMutation();
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
            gasTankBehaviour.endMutation();
        }
    }

    private long getCurrentGasPressurePa(int channel) {
        if (recipeGasCapability == null || channel < 0 || channel >= recipeGasCapability.getTanks()) {
            return GasPressure.VACUUM_PA;
        }

        return recipeGasCapability.getTankPressurePa(channel);
    }

    private void notifyFiltersChanged() {
        if (controller == null) {
            return;
        }

        controller.notifyFiltersChanged();
    }

    private void addFluidBehaviours(List<BlockEntityBehaviour> behaviours) {
        inputFluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 3, getFluidCapacity(), true).whenFluidUpdates(this::notifyContentsChanged);
        outputFluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 2, getFluidCapacity(), true).forbidInsertion().whenFluidUpdates(this::notifyContentsChanged);
        IFluidHandler inputCapability = inputFluidTank.getCapability();
        IFluidHandler outputCapability = outputFluidTank.getCapability();
        recipeFluidCapability = new CombinedTankWrapper(inputCapability, outputCapability);
        fluidPortCapability = new AirtightReactorKettleFluidPortHandler(inputCapability, outputCapability);
        behaviours.add(inputFluidTank);
        behaviours.add(outputFluidTank);
    }

    private void addGasBehaviours(List<BlockEntityBehaviour> behaviours) {
        inputGasTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.INPUT, this, 3, getGasCapacity(), GasPressureLimits.HARD_PRESSURE_PA, true).whenTankUpdates(this::notifyContentsChanged);
        outputGasTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, this, 2, getGasCapacity(), GasPressureLimits.HARD_PRESSURE_PA, true).forbidInsertion().whenTankUpdates(this::notifyContentsChanged);
        GasStorageHandler inputCapability = inputGasTank.getCapability();
        GasStorageHandler outputCapability = outputGasTank.getCapability();
        recipeGasCapability = new CombinedGasStorageHandler(inputCapability, outputCapability);
        gasPortCapability = new AirtightReactorKettleGasPortHandler(inputCapability, outputCapability);

        LongSupplier[] pressureChannels = new LongSupplier[recipeGasCapability.getTanks()];
        for (int channel = 0; channel < pressureChannels.length; channel++) {
            int trackedChannel = channel;
            pressureChannels[channel] = () -> getCurrentGasPressurePa(trackedChannel);
        }
        overpressureBehaviour = new OverpressureBehaviour(this, pressureChannels);

        behaviours.add(inputGasTank);
        behaviours.add(outputGasTank);
        behaviours.add(overpressureBehaviour);
    }

    private boolean updateReactorKettle() {
        return controller.updateReactorKettle();
    }

    private void syncRecipeFilterReplicas() {
        if (level == null) {
            return;
        }

        BlockPos filterCenterPos = worldPosition.below();
        for (Direction direction : Iterate.horizontalDirections) {
            BlockPos filterPos = filterCenterPos.relative(direction);
            if (!(level.getBlockEntity(filterPos) instanceof AirtightReactorKettleStructuralBlockEntity filter)) {
                continue;
            }

            filter.syncFilterFromMaster(recipeFilter);
        }
    }

    @Internal
    public record CraftPlan(List<ItemStack> expectedItems, List<FluidStack> expectedFluids, List<GasStack> expectedGases, int[] itemAmounts, int[] fluidAmounts, GasConsumptionPlan gasPlan, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        @Internal
        public CraftPlan(List<ItemStack> expectedItems, List<FluidStack> expectedFluids, List<GasStack> expectedGases, int[] itemAmounts, int[] fluidAmounts, GasConsumptionPlan gasPlan, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
            this.expectedItems = expectedItems.stream().map(ItemStack::copy).toList();
            this.expectedFluids = expectedFluids.stream().map(FluidStack::copy).toList();
            this.expectedGases = expectedGases.stream().map(GasStack::copy).toList();
            this.itemAmounts = itemAmounts.clone();
            this.fluidAmounts = fluidAmounts.clone();
            this.gasPlan = gasPlan;
            this.outputItems = outputItems.stream().map(ItemStack::copy).toList();
            this.outputFluids = outputFluids.stream().map(FluidStack::copy).toList();
            this.outputGases = outputGases.stream().map(GasStack::copy).toList();
        }
    }
}
