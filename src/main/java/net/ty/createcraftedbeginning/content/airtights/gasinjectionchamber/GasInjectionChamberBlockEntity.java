package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.MultiFace;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer.MachineFillingStrategy;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.content.particles.ColoredBreezeCloudParticleType.ColoredBreezeCloudParticleOptions;
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

    private static final String COMPOUND_KEY_PROCESSING_TICKS = "ProcessingTicks";
    private static final String COMPOUND_KEY_OPERATION_TYPE = "OperationType";
    private static final String COMPOUND_KEY_OPERATION_GAS = "OperationGas";
    private static final String COMPOUND_KEY_OPERATION_INPUT = "OperationInput";
    private static final String COMPOUND_KEY_OPERATION_RESULT = "OperationResult";
    private static final String COMPOUND_KEY_OPERATION_RESULT_PREPARED = "OperationResultPrepared";
    private static final String COMPOUND_KEY_OPERATION_EXECUTED = "OperationExecuted";
    private static final String COMPOUND_KEY_CLOUD = "Cloud";
    private static final String COMPOUND_KEY_CLOUD_COLOR = "CloudColor";

    private int cloudColor = 0xFFFFFFFF;
    private int processingTicks = -1;
    private boolean sendCloud;
    private boolean operationExecuted;
    private OperationType operationType = OperationType.NONE;
    private GasStack operationGas = GasStack.EMPTY;
    private ItemStack operationInput = ItemStack.EMPTY;
    private ItemStack operationResult = ItemStack.EMPTY;
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

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, getMaxCapacity());
        exposedGasHandler = new OperationLockingGasHandler(tankBehaviour.getCapability());
        BeltProcessingBehaviour beltProcessing = new BeltProcessingBehaviour(this).whenItemEnters(this::onItemEntered).whileItemHeld(this::onItemHeld);
        behaviours.add(tankBehaviour);
        behaviours.add(beltProcessing);
    }

    @Override
    public void tick() {
        super.tick();
        if (processingTicks < 0) {
            return;
        }

        processingTicks--;
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
        if (operationType != OperationType.NONE) {
            compoundTag.putString(COMPOUND_KEY_OPERATION_TYPE, operationType.serializedName);
            compoundTag.put(COMPOUND_KEY_OPERATION_GAS, operationGas.saveOptional(provider));
            compoundTag.put(COMPOUND_KEY_OPERATION_INPUT, operationInput.saveOptional(provider));
            compoundTag.putBoolean(COMPOUND_KEY_OPERATION_RESULT_PREPARED, operationResultPrepared);
            if (operationResultPrepared) {
                compoundTag.put(COMPOUND_KEY_OPERATION_RESULT, operationResult.saveOptional(provider));
            }
            compoundTag.putBoolean(COMPOUND_KEY_OPERATION_EXECUTED, operationExecuted);
        }
        if (!sendCloud || !clientPacket) {
            return;
        }

        compoundTag.putBoolean(COMPOUND_KEY_CLOUD, true);
        compoundTag.putInt(COMPOUND_KEY_CLOUD_COLOR, cloudColor);
        sendCloud = false;
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (compoundTag.contains(COMPOUND_KEY_PROCESSING_TICKS)) {
            processingTicks = compoundTag.getInt(COMPOUND_KEY_PROCESSING_TICKS);
        }

        operationRecipe = null;
        operationType = OperationType.byName(compoundTag.getString(COMPOUND_KEY_OPERATION_TYPE));
        operationGas = compoundTag.contains(COMPOUND_KEY_OPERATION_GAS) ? GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_OPERATION_GAS)) : GasStack.EMPTY;
        operationInput = compoundTag.contains(COMPOUND_KEY_OPERATION_INPUT) ? ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_OPERATION_INPUT)) : ItemStack.EMPTY;
        operationResultPrepared = compoundTag.getBoolean(COMPOUND_KEY_OPERATION_RESULT_PREPARED);
        operationResult = operationResultPrepared && compoundTag.contains(COMPOUND_KEY_OPERATION_RESULT) ? ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_OPERATION_RESULT)) : ItemStack.EMPTY;
        operationExecuted = compoundTag.getBoolean(COMPOUND_KEY_OPERATION_EXECUTED);
        if (operationType == OperationType.NONE || operationGas.isEmpty() || operationInput.isEmpty()) {
            clearOperation();
        }

        if (!clientPacket || !compoundTag.contains(COMPOUND_KEY_CLOUD)) {
            return;
        }

        int color = compoundTag.contains(COMPOUND_KEY_CLOUD_COLOR) ? compoundTag.getInt(COMPOUND_KEY_CLOUD_COLOR) : 0xFFFFFFFF;
        spawnCloud(color);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
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
        if (direction == Direction.UP) {
            return new MultiFace(worldPosition, Set.of(Direction.UP));
        }
        return null;
    }

    public int getProcessingTicks() {
        return processingTicks;
    }

    private void spawnCloud(int color) {
        if (level == null || !level.isClientSide || isVirtual()) {
            return;
        }

        Vec3 cloudPos = VecHelper.getCenterOf(worldPosition).subtract(0, 1.6875f, 0);
        for (int i = 0; i < level.random.nextInt(3, 6); i++) {
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

        if (operationType == OperationType.NONE && !prepareOperation(transported.stack)) {
            return PASS;
        }

        if (!operationExecuted && !ItemStack.isSameItemSameComponents(operationInput, transported.stack)) {
            cancelOperation();
            return PASS;
        }

        if (operationExecuted) {
            return HOLD;
        }

        if (processingTicks < 0) {
            return startProcessing(transported.stack);
        }

        if (processingTicks > PROCESSING_TIME - NOZZLE_TIME - NOZZLE_PART_TIME - NOZZLE_IDLE_TIME) {
            return HOLD;
        }

        return executeInjection(transported, handler);
    }

    private ProcessingResult startProcessing(ItemStack itemStack) {
        GasStack tankGas = getGasInTank();
        if (tankGas.isEmpty()) {
            return HOLD;
        }

        if (!GasStack.isSameGasSameComponents(tankGas, operationGas)) {
            clearOperation();
            if (!prepareOperation(itemStack)) {
                return PASS;
            }

            tankGas = getGasInTank();
        }

        if (tankGas.getAmount() < operationGas.getAmount()) {
            return HOLD;
        }

        if (!prepareRecipeResultIfNeeded(itemStack)) {
            cancelOperation();
            return PASS;
        }

        processingTicks = PROCESSING_TIME + NOZZLE_IDLE_TIME;
        notifyUpdate();
        return HOLD;
    }

    private ProcessingResult executeInjection(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (level == null) {
            return PASS;
        }

        int color = operationGas.getHint();
        if (!executeOperation(transported, handler)) {
            cancelOperation();
            return PASS;
        }

        operationExecuted = true;
        cloudColor = color;
        sendCloud = true;
        notifyUpdate();
        CCBSoundEvents.INJECTING.playOnServer(level, worldPosition, 0.75f, 0.9f + 0.2f * level.random.nextFloat());
        return HOLD;
    }

    private boolean prepareOperation(ItemStack itemStack) {
        if (level == null) {
            return false;
        }

        GasStack tankGas = getGasInTank();
        return !tankGas.isEmpty() && (prepareCanisterOperation(itemStack, tankGas) || prepareRecipeOperation(itemStack, tankGas));
    }

    private boolean prepareCanisterOperation(ItemStack itemStack, GasStack tankGas) {
        IGasCanisterContainer canister = itemStack.getCapability(GasHandler.ITEM);
        if (canister == null) {
            return false;
        }

        long amount = GasCanisterUtils.getInjectableAmount(canister, tankGas, getMaxCapacity());
        if (amount <= 0) {
            return false;
        }

        setOperation(OperationType.CANISTER, itemStack, tankGas, amount, null);
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
        setOperation(OperationType.RECIPE, itemStack, tankGas, match.recipe().getGasIngredient().amount(), match.sequencedAssembly() ? null : match.recipe());
        return true;
    }

    private void setOperation(OperationType type, ItemStack input, GasStack gas, long requiredAmount, @Nullable GasInjectionRecipe recipe) {
        operationType = type;
        operationInput = input.copyWithCount(1);
        operationGas = gas.copyWithAmount(requiredAmount);
        operationRecipe = recipe;
        operationResult = ItemStack.EMPTY;
        operationResultPrepared = false;
        operationExecuted = false;
        setChanged();
    }

    private boolean prepareRecipeResultIfNeeded(ItemStack itemStack) {
        if (operationType != OperationType.RECIPE || operationResultPrepared) {
            return true;
        }

        if (level == null) {
            return false;
        }

        GasInjectionRecipe recipe = operationRecipe;
        if (recipe == null) {
            Optional<RecipeMatch> recipeMatch = GasInjectionRecipe.findRecipeMatch(level, itemStack, operationGas);
            if (recipeMatch.isEmpty()) {
                return false;
            }

            RecipeMatch match = recipeMatch.get();
            if (match.recipe().getGasIngredient().amount() != operationGas.getAmount()) {
                return false;
            }

            recipe = match.recipe();
        }
        operationResult = recipe.rollFirstResult(level);
        operationResultPrepared = true;
        operationRecipe = null;
        setChanged();
        return true;
    }

    private boolean executeOperation(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        return switch (operationType) {
            case CANISTER -> executeCanisterOperation(transported.stack);
            case RECIPE -> executeRecipeOperation(transported, handler);
            case NONE -> false;
        };
    }

    private boolean executeCanisterOperation(ItemStack itemStack) {
        IGasCanisterContainer canisterContents = itemStack.getCapability(GasHandler.ITEM);
        if (canisterContents == null || canisterContents.getMachineFillingStrategy() == MachineFillingStrategy.DENY) {
            return false;
        }

        long accepted = canisterContents.fill(0, operationGas, GasAction.SIMULATE);
        if (accepted != operationGas.getAmount()) {
            return false;
        }

        GasStack drained = drainOperationGas();
        if (drained.isEmpty()) {
            return false;
        }

        long filled = canisterContents.fill(0, drained, GasAction.EXECUTE);
        if (filled <= 0) {
            getTank().fill(drained, GasAction.EXECUTE);
            return false;
        }

        if (filled < drained.getAmount()) {
            getTank().fill(drained.copyWithAmount(drained.getAmount() - filled), GasAction.EXECUTE);
            operationGas = operationGas.copyWithAmount(filled);
        }
        return true;
    }

    private boolean executeRecipeOperation(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (!operationResultPrepared) {
            return false;
        }

        ItemStack resultStack = operationResult.copy();
        GasStack drained = drainOperationGas();
        if (drained.isEmpty()) {
            return false;
        }

        transported.stack.shrink(1);
        transported.clearFanProcessingData();
        TransportedItemStack held = null;
        List<TransportedItemStack> results = new ArrayList<>();
        if (!resultStack.isEmpty()) {
            TransportedItemStack result = transported.copy();
            result.stack = resultStack;
            results.add(result);
        }
        if (!transported.stack.isEmpty()) {
            held = transported.copy();
        }
        handler.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(results, held));
        return true;
    }

    private GasStack drainOperationGas() {
        IGasTank tank = getTank();
        GasStack simulated = tank.drain(operationGas, GasAction.SIMULATE);
        if (!GasStack.matches(simulated, operationGas)) {
            return GasStack.EMPTY;
        }

        GasStack drained = tank.drain(operationGas, GasAction.EXECUTE);
        return GasStack.matches(drained, operationGas) ? drained : GasStack.EMPTY;
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
        operationInput = ItemStack.EMPTY;
        operationResult = ItemStack.EMPTY;
        operationResultPrepared = false;
        operationRecipe = null;
        operationExecuted = false;
    }

    private enum OperationType {
        NONE("none"),
        RECIPE("recipe"),
        CANISTER("canister");

        private final String serializedName;

        OperationType(String serializedName) {
            this.serializedName = serializedName;
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
            return operationType != OperationType.NONE && processingTicks >= 0 && !operationExecuted;
        }
    }
}
