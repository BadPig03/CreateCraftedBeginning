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
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.CombinedGasTankWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipeContext;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation, IGasInventoryIdentifierProvider, ReactorKettleRecipeContext {
    private static final int LAZY_TICK_RATE = 4;
    private static final int MAX_ITEM_SLOT = 27;

    protected final AirtightReactorKettleCore core;
    protected final AirtightReactorKettleAnimationState animationState;
    protected final AirtightReactorKettleController controller;
    protected final AirtightReactorKettleCrafting crafting;
    protected final AirtightReactorKettleSerialization serialization;
    protected final AirtightReactorKettleInventory inputInventory;
    protected final SmartInventory outputInventory;
    protected final Couple<SmartInventory> inventories;
    protected final IItemHandlerModifiable recipeItemCapability;
    protected final IItemHandler itemPortCapability;

    protected DeferralBehaviour updateChecker;
    protected IFluidHandler recipeFluidCapability;
    protected IFluidHandler fluidPortCapability;
    protected IGasHandler recipeGasCapability;
    protected IGasHandler gasPortCapability;
    protected SmartFluidTankBehaviour inputFluidTank;
    protected SmartFluidTankBehaviour outputFluidTank;
    protected SmartGasTankBehaviour inputGasTank;
    protected SmartGasTankBehaviour outputGasTank;
    protected CCBAdvancementBehaviour advancementBehaviour;
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
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (blockEntity, direction) -> blockEntity.itemPortCapability);
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (blockEntity, direction) -> blockEntity.fluidPortCapability);
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), (blockEntity, direction) -> blockEntity.gasPortCapability);
    }

    public static int getFluidCapacity() {
        return Math.max(1, CCBConfig.server().airtights.reactorKettleFluidCapacity.get()) * FluidType.BUCKET_VOLUME;
    }

    public static long getGasCapacity() {
        return Math.max(1, CCBConfig.server().airtights.reactorKettleGasCapacity.get()) * GasAmounts.MILLIBUCKETS_PER_BUCKET;
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
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
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

    public AirtightReactorKettleCore getCore() {
        return core;
    }

    public CraftPlan createCraftPlan(int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        return crafting.createCraftPlan(itemAmounts, fluidAmounts, gasAmounts, outputItems, outputFluids, outputGases);
    }

    public synchronized boolean commitCraft(CraftPlan plan) {
        return crafting.commitCraft(plan);
    }

    public boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases, boolean simulate) {
        return crafting.acceptOutputs(outputItems, outputFluids, outputGases, simulate);
    }

    public boolean getWindowsOpenState() {
        return controller.getWindowsOpenState();
    }

    public boolean isEmpty() {
        return inputInventory.isEmpty() && outputInventory.isEmpty() && inputFluidTank.isEmpty() && outputFluidTank.isEmpty() && inputGasTank.isEmpty() && outputGasTank.isEmpty();
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

    public float getDamage() {
        return controller.getDamage();
    }

    public float getMixerOffset(float partialTicks) {
        return controller.getMixerOffset(partialTicks);
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
    public IGasHandler getAvailableGases() {
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
    public IGasHandler getOutputGasCapability() {
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
    public boolean commitRecipeCraft(int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        return commitCraft(createCraftPlan(itemAmounts, fluidAmounts, gasAmounts, outputItems, outputFluids, outputGases));
    }

    public LerpedFloat getIngredientRotation() {
        return animationState.getIngredientRotation();
    }

    public LerpedFloat getMixerRotation() {
        return animationState.getMixerRotation();
    }

    public LerpedFloat getWindowDistance() {
        return animationState.getWindowDistance();
    }

    public void notifyContentsChanged() {
        if (controller == null) {
            return;
        }

        controller.notifyContentsChanged();
    }

    public void notifyFiltersChanged() {
        if (controller == null) {
            return;
        }

        controller.notifyFiltersChanged();
    }

    public void scheduleUpdate() {
        updateChecker.scheduleUpdate();
    }

    public void startProcessInPonderLevel() {
        controller.startProcessInPonderLevel();
    }

    public AirtightReactorKettleController getController() {
        return controller;
    }

    public AirtightReactorKettleInventory getInputInventory() {
        return inputInventory;
    }

    public SmartInventory getOutputInventory() {
        return outputInventory;
    }

    public void awardBackToBasics() {
        advancementBehaviour.awardPlayer(CCBAdvancements.BACK_TO_BASICS);
    }

    IItemHandler getItemPortCapability() {
        return itemPortCapability;
    }

    IFluidHandler getFluidPortCapability() {
        return fluidPortCapability;
    }

    IGasHandler getGasPortCapability() {
        return gasPortCapability;
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

    void setRecipeFilter(ItemStack stack) {
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

    void loadRecipeFilter(ItemStack stack, boolean authoritative) {
        recipeFilter = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        recipeFilterAuthoritative = authoritative;
    }

    boolean hasAuthoritativeRecipeFilter() {
        return recipeFilterAuthoritative;
    }

    protected void addFluidBehaviours(List<BlockEntityBehaviour> behaviours) {
        inputFluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 3, getFluidCapacity(), true).whenFluidUpdates(this::notifyContentsChanged);
        outputFluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 2, getFluidCapacity(), true).forbidInsertion().whenFluidUpdates(this::notifyContentsChanged);
        IFluidHandler inputCapability = inputFluidTank.getCapability();
        IFluidHandler outputCapability = outputFluidTank.getCapability();
        recipeFluidCapability = new CombinedTankWrapper(inputCapability, outputCapability);
        fluidPortCapability = new AirtightReactorKettleFluidPortHandler(inputCapability, outputCapability);
        behaviours.add(inputFluidTank);
        behaviours.add(outputFluidTank);
    }

    protected void addGasBehaviours(List<BlockEntityBehaviour> behaviours) {
        inputGasTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.INPUT, this, 3, getGasCapacity(), true).whenGasUpdates(this::notifyContentsChanged);
        outputGasTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, this, 2, getGasCapacity(), true).forbidInsertion().whenGasUpdates(this::notifyContentsChanged);
        IGasHandler inputCapability = inputGasTank.getCapability();
        IGasHandler outputCapability = outputGasTank.getCapability();
        recipeGasCapability = new CombinedGasTankWrapper(inputCapability, outputCapability);
        gasPortCapability = new AirtightReactorKettleGasPortHandler(inputCapability, outputCapability);
        behaviours.add(inputGasTank);
        behaviours.add(outputGasTank);
    }

    protected boolean updateReactorKettle() {
        return controller.updateReactorKettle();
    }

    private void syncRecipeFilterReplicas() {
        if (level == null) {
            return;
        }

        BlockPos filterCenterPos = worldPosition.below();
        for (Direction direction : Iterate.horizontalDirections) {
            BlockPos filterPos = filterCenterPos.relative(direction);
            if (!(level.getBlockEntity(filterPos) instanceof AirtightReactorKettleStructuralBlockEntity filterBlockEntity)) {
                continue;
            }

            filterBlockEntity.syncFilterFromMaster(recipeFilter);
        }
    }

    public record CraftPlan(List<ItemStack> expectedItems, List<FluidStack> expectedFluids, List<GasStack> expectedGases, int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
        public CraftPlan(List<ItemStack> expectedItems, List<FluidStack> expectedFluids, List<GasStack> expectedGases, int[] itemAmounts, int[] fluidAmounts, long[] gasAmounts, List<ItemStack> outputItems, List<FluidStack> outputFluids, List<GasStack> outputGases) {
            this.expectedItems = expectedItems.stream().map(ItemStack::copy).toList();
            this.expectedFluids = expectedFluids.stream().map(FluidStack::copy).toList();
            this.expectedGases = expectedGases.stream().map(GasStack::copy).toList();
            this.itemAmounts = itemAmounts.clone();
            this.fluidAmounts = fluidAmounts.clone();
            this.gasAmounts = gasAmounts.clone();
            this.outputItems = outputItems.stream().map(ItemStack::copy).toList();
            this.outputFluids = outputFluids.stream().map(FluidStack::copy).toList();
            this.outputGases = outputGases.stream().map(GasStack::copy).toList();
        }
    }
}
