package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.SmartGasTankBehaviour.InternalGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.IChamberGasTank;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberRecipeIndex.GasConversion;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.BaseChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.CreativeChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.GaleChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.IllChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.InactiveChamberState;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WIND_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IGasInventoryIdentifierProvider {
    private static final int LAZY_TICK_RATE = 20;
    private static final int WIND_STATE_SYNC_INTERVAL = 20;
    private static final int GAS_PROCESSING_INTERVAL = 20;
    private static final String COMPOUND_KEY_STATE_TYPE = "StateType";
    private static final String COMPOUND_KEY_STATE_DATA = "StateData";
    private static final String COMPOUND_KEY_GOGGLES = "Goggles";
    private static final String COMPOUND_KEY_TRAIN_HAT = "TrainHat";
    private static final String COMPOUND_KEY_IS_CREATIVE = "isCreative";
    private static final String COMPOUND_KEY_REMAINING_TIME = "RemainingTime";
    private static Consumer<BreezeChamberBlockEntity> clientTicker = chamber -> {};
    private final LerpedFloat headAnimation;

    protected LerpedFloat headAngle;
    protected WeakReference<IChamberGasTank> source;

    private boolean goggles;
    private boolean trainHat;
    private int lastWindLevel = -1;
    private CCBAdvancementBehaviour advancementBehaviour;
    private SmartGasTankBehaviour tankBehaviour;
    private BaseChamberState currentState;

    public BreezeChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        currentState = new InactiveChamberState();
        goggles = false;
        headAngle = LerpedFloat.angular();
        headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.NORTH)) + 180) % 360);
        headAnimation = LerpedFloat.linear();
        source = new WeakReference<>(null);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.BREEZE_CHAMBER.get(), (chamber, context) -> chamber.isControllerActive() ? null : chamber.tankBehaviour.getCapability());
    }

    public static long getMaxCapacity() {
        return CCBConfig.server().airtights.maxBreezeChamberCapacity.get() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    public static int getMaxWindCapacity() {
        return Math.max(1, CCBConfig.server().airtights.maxWindCapacity.get());
    }

    public static int getMaxEffectiveThreshold() {
        return Math.max(1, getMaxWindCapacity() / 2);
    }

    public static int getOverflowThreshold() {
        return (int) Math.max(1L, (long) getMaxWindCapacity() * 3 / 4);
    }

    public static void setClientTicker(Consumer<BreezeChamberBlockEntity> ticker) {
        clientTicker = ticker;
    }

    private static BaseChamberState createState(ChargerType chargerType, int remainingTime, boolean isCreative) {
        if (isCreative && chargerType != ChargerType.NONE) {
            return new CreativeChamberState(chargerType);
        }

        return switch (chargerType) {
            case NORMAL -> remainingTime > 0 ? new GaleChamberState(remainingTime, false) : new InactiveChamberState();
            case BAD -> remainingTime < 0 ? new IllChamberState(remainingTime, false) : new InactiveChamberState();
            case NONE -> new InactiveChamberState();
        };
    }

    private static BaseChamberState readState(CompoundTag tag) {
        CompoundTag stateData = tag.getCompound(COMPOUND_KEY_STATE_DATA);
        ChargerType stateType = ChargerType.fromTag(tag, COMPOUND_KEY_STATE_TYPE, ChargerType.NONE);
        boolean isCreative = stateData.contains(COMPOUND_KEY_IS_CREATIVE, Tag.TAG_BYTE) && stateData.getBoolean(COMPOUND_KEY_IS_CREATIVE);
        int maxWindCapacity = getMaxWindCapacity();
        int remainingTime = stateData.contains(COMPOUND_KEY_REMAINING_TIME, Tag.TAG_ANY_NUMERIC) ? Mth.clamp(stateData.getInt(COMPOUND_KEY_REMAINING_TIME), -maxWindCapacity, maxWindCapacity) : 0;
        return createState(stateType, remainingTime, isCreative);
    }

    private static ChargerType getChargerTypeForTime(int time) {
        if (time > 0) {
            return ChargerType.NORMAL;
        }
        if (time < 0) {
            return ChargerType.BAD;
        }
        return ChargerType.NONE;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.LUXURY_TREAT, CCBAdvancements.BAD_APPLE, CCBAdvancements.UNIVERSAL_ANTIDOTE);
        behaviours.add(advancementBehaviour);

        tankBehaviour = SmartGasTankBehaviour.single(this, getMaxCapacity()).forbidInsertion().allowExtraction();
        behaviours.add(tankBehaviour);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        currentState.tick(this);
        if (!level.isClientSide) {
            updateAirtightAssemblyDriver();
            return;
        }

        clientTicker.accept(this);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BreezeChamberBlock chamber) || chamber.canSurvive(state, level, getBlockPos())) {
            return;
        }

        level.destroyBlock(worldPosition, true);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        CompoundTag stateTag = new CompoundTag();
        currentState.save(stateTag);
        compoundTag.put(COMPOUND_KEY_STATE_DATA, stateTag);
        compoundTag.putString(COMPOUND_KEY_STATE_TYPE, currentState.getChargerType().name());
        compoundTag.putBoolean(COMPOUND_KEY_GOGGLES, goggles);
        compoundTag.putBoolean(COMPOUND_KEY_TRAIN_HAT, trainHat);

        super.write(compoundTag, provider, clientPacket);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (compoundTag.contains(COMPOUND_KEY_STATE_DATA, Tag.TAG_COMPOUND)) {
            currentState = readState(compoundTag);
        }
        if (compoundTag.contains(COMPOUND_KEY_GOGGLES, Tag.TAG_BYTE)) {
            goggles = compoundTag.getBoolean(COMPOUND_KEY_GOGGLES);
        }
        if (compoundTag.contains(COMPOUND_KEY_TRAIN_HAT, Tag.TAG_BYTE)) {
            trainHat = compoundTag.getBoolean(COMPOUND_KEY_TRAIN_HAT);
        }

        super.read(compoundTag, provider, clientPacket);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) {
            return;
        }

        syncWindLevelBlockState();
        updateAirtightAssemblyDriver();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        WindLevel windLevel = getWindLevel();
        CCBLang.translate("gui.breeze_chamber").forGoggles(tooltip);
        CCBLang.translate("gui.breeze_chamber.current_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(windLevel.getTranslatable()).style(windLevel.getChatFormatting()).forGoggles(tooltip, 1);

        Gas tankGasType = getTankGasType();
        boolean isActive = isControllerActive();
        boolean isInputInvalid = isInputInvalid();
        boolean isOutputFailed = (isOutputFull() || isOutputMismatched()) && !isActive;
        int time = getWindRemainingTime();
        if (windLevel != WindLevel.CALM) {
            CCBLang.translate("gui.breeze_chamber.remaining_time").style(ChatFormatting.GRAY).forGoggles(tooltip);
            ChatFormatting timeColor = time > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
            if (isCreative()) {
                CCBLang.translate("gui.gas_container.infinity").style(timeColor).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.seconds(time, level.tickRateManager().tickrate()).style(timeColor).forGoggles(tooltip, 1);
            }
            if (isActive) {
                CCBLang.translate("gui.breeze_chamber.energization_level").style(ChatFormatting.GRAY).forGoggles(tooltip);
                CCBLang.translate("gui.breeze_chamber.current_level", CCBLang.number(getWindRemainingLevel())).style(ChatFormatting.BLUE).forGoggles(tooltip, 1);
            }
        }

        if (!isActive) {
            tooltip.add(CommonComponents.EMPTY);
            IGasHandler handler = tankBehaviour.getPrimaryHandler();
            GasStack gasStack = handler.getGasInTank(0);
            long capacity = handler.getTankCapacity(0);
            if (gasStack.isEmpty()) {
                CCBLang.translate("gui.gas_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
                GasAmountUtils.precise(capacity).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.translate("gui.gas_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
                CCBLang.gasName(gasStack).style(ChatFormatting.WHITE).forGoggles(tooltip, 1);
                GasAmountUtils.precise(gasStack.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmountUtils.precise(capacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
            }
            if (isInputInvalid || isOutputFailed) {
                tooltip.add(CommonComponents.EMPTY);
                CCBLang.translate("gui.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
            }
            if (isInputInvalid) {
                CCBLang.addToGoggles(tooltip, "gui.breeze_chamber.invalid_gas", Component.translatable(tankGasType.getTranslationKey()));
            }
            if (isOutputFailed) {
                CCBLang.addToGoggles(tooltip, "gui.breeze_chamber.output_failed");
            }
        }
        return true;
    }

    @Override
    public InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        return new Single(worldPosition);
    }

    public boolean hasGoggles() {
        return goggles;
    }

    public boolean hasTrainHat() {
        return trainHat;
    }

    public boolean isControllerActive() {
        IChamberGasTank tank = getTank();
        if (!(tank instanceof AirtightTankBlockEntity controller)) {
            return false;
        }

        AirtightAssemblyDriverCore driverCore = controller.getCore();
        return driverCore.getStructureManager().isActive();
    }

    public boolean isCreative() {
        return currentState.isCreative();
    }

    public boolean tryUpdateChargerByItem(ItemStack stack, boolean forceOverflow, boolean simulate) {
        if (stack.is(CCBItems.CREATIVE_ICE_CREAM)) {
            if (simulate) {
                return true;
            }

            ChargerType chargerType = CreativeChamberState.getNextChargeType(currentState.getChargerType());
            setChamberState(chargerType == ChargerType.NONE ? new InactiveChamberState() : new CreativeChamberState(chargerType));
            boolean isBad = chargerType == ChargerType.BAD;
            spawnParticleBurst(isBad);
            playSound(isBad);
            return true;
        }

        InteractionResult result = currentState.onItemInsert(this, stack, forceOverflow, simulate);
        if (result != InteractionResult.SUCCESS) {
            return false;
        }

        if (simulate) {
            return true;
        }

        setChanged();
        updateAirtightAssemblyDriver();
        notifyUpdate();
        return true;
    }

    public int getWindRemainingLevel() {
        int time = getWindRemainingTime();
        if (time <= 0) {
            return 0;
        }

        return time < getMaxEffectiveThreshold() ? 1 : 2;
    }

    public int getWindRemainingTime() {
        return currentState.getRemainingTime();
    }

    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    public LerpedFloat getHeadAnimation() {
        return headAnimation;
    }

    public LerpedFloat getHeadAngle() {
        return headAngle;
    }

    public void syncWindProgress() {
        if (level == null || level.isClientSide) {
            return;
        }

        setChanged();
        long phase = level.getGameTime() + worldPosition.asLong();
        if (Math.floorMod(phase, WIND_STATE_SYNC_INTERVAL) == 0) {
            notifyUpdate();
        }
    }

    public void tickGasProcessing(ChargerType chargerType) {
        if (level == null || level.isClientSide || chargerType == ChargerType.NONE || isControllerActive()) {
            return;
        }

        long phase = level.getGameTime() + worldPosition.asLong();
        if (Math.floorMod(phase, GAS_PROCESSING_INTERVAL) != 0) {
            return;
        }

        processGas(chargerType);
    }

    private void processGas(ChargerType chargerType) {
        if (level == null || level.isClientSide || chargerType == ChargerType.NONE) {
            return;
        }

        IChamberGasTank tank = getTank();
        if (tank == null || isControllerActive()) {
            return;
        }

        GasTank inventory = tank.getTankInventory();
        GasTank output = tankBehaviour.getPrimaryHandler();
        if (inventory.isEmpty() || output.getSpace() <= 0) {
            return;
        }

        GasStack inputStack = inventory.getGasStack();
        Optional<GasConversion> conversionResult = getConversion(chargerType, inputStack);
        if (conversionResult.isEmpty()) {
            return;
        }

        GasConversion conversion = conversionResult.get();
        GasStack outputPerBatch = conversion.output();
        if (!output.isEmpty() && !GasStack.isSameGasSameComponents(output.getGasStack(), outputPerBatch)) {
            return;
        }

        long inputAmount = conversion.input().amount();
        long outputAmount = outputPerBatch.getAmount();
        long processingBudget = getProcessingAmount();
        long batches = Math.min(inputStack.getAmount() / inputAmount, processingBudget / inputAmount);
        batches = Math.min(batches, output.getSpace() / outputAmount);
        if (batches <= 0) {
            return;
        }

        GasStack inputRequest = inputStack.copyWithAmount(batches * inputAmount);
        GasStack outputRequest = outputPerBatch.copyWithAmount(batches * outputAmount);
        executeGasConversionTransaction(tank.getCapability(), tankBehaviour.getInternalGasHandler(), inputRequest, outputRequest);
    }

    private void executeGasConversionTransaction(IGasHandler sourceHandler, InternalGasHandler outputHandler, GasStack inputRequest, GasStack outputRequest) {
        GasStack simulatedDrain = sourceHandler.drain(inputRequest, GasAction.SIMULATE);
        if (!GasStack.matches(simulatedDrain, inputRequest)) {
            return;
        }

        long simulatedFill = outputHandler.forceFill(outputRequest, GasAction.SIMULATE);
        if (simulatedFill != outputRequest.getAmount()) {
            return;
        }

        long filled = outputHandler.forceFill(outputRequest, GasAction.EXECUTE);
        if (filled != outputRequest.getAmount()) {
            rollbackOutput(outputHandler, outputRequest, filled);
            return;
        }

        GasStack drained = sourceHandler.drain(inputRequest, GasAction.EXECUTE);
        if (GasStack.matches(drained, inputRequest)) {
            return;
        }

        boolean outputRolledBack = rollbackOutput(outputHandler, outputRequest, outputRequest.getAmount());
        long restored = drained.isEmpty() ? 0 : sourceHandler.fill(drained, GasAction.EXECUTE);
        if (!outputRolledBack || !drained.isEmpty() && restored != drained.getAmount()) {
            CreateCraftedBeginning.LOGGER.error("Failed to fully roll back breeze chamber gas conversion at {}: output rollback={}, restored input={}/{}", worldPosition, outputRolledBack, restored, drained.getAmount());
        }
    }

    private boolean rollbackOutput(InternalGasHandler outputHandler, GasStack outputRequest, long amount) {
        if (amount <= 0) {
            return true;
        }

        GasStack rolledBack = outputHandler.forceDrain(outputRequest.copyWithAmount(amount), GasAction.EXECUTE);
        boolean successful = rolledBack.getAmount() == amount && GasStack.isSameGasSameComponents(rolledBack, outputRequest);
        if (!successful) {
            CreateCraftedBeginning.LOGGER.error("Failed to roll back {} units of breeze chamber output gas at {}", amount, worldPosition);
        }
        return successful;
    }

    public void loadFromItem(ItemStack stack) {
        int maxWindCapacity = getMaxWindCapacity();
        int time = Mth.clamp(stack.getOrDefault(CCBDataComponents.BREEZE_TIME, 0), -maxWindCapacity, maxWindCapacity);
        ChargerType chargerType = getChargerTypeForTime(time);
        boolean creative = stack.getOrDefault(CCBDataComponents.BREEZE_CREATIVE, false);
        setChamberState(createState(chargerType, time, creative));
        if (time != 0) {
            playSound(time < 0);
        }
    }

    public void playSound(boolean bad) {
        if (level == null) {
            return;
        }
        if (bad) {
            level.playSound(null, worldPosition, SoundEvents.BREEZE_HURT, SoundSource.BLOCKS, 0.125f + level.random.nextFloat() * 0.125f, 0.75f - level.random.nextFloat() * 0.25f);
            return;
        }

        level.playSound(null, worldPosition, SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125f + level.random.nextFloat() * 0.125f, 0.75f - level.random.nextFloat() * 0.25f);
    }

    public void saveToItem(ItemStack stack) {
        stack.set(CCBDataComponents.BREEZE_TIME, currentState.getRemainingTime());
        stack.set(CCBDataComponents.BREEZE_CREATIVE, currentState.isCreative());
    }

    public void setChamberState(BaseChamberState newState) {
        currentState = newState;
        setChanged();
        if (level == null || level.isClientSide && !isVirtual()) {
            return;
        }

        syncWindLevelBlockState();
        updateAirtightAssemblyDriver();
        notifyUpdate();
    }

    private void syncWindLevelBlockState() {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        WindLevel windLevel = currentState.getWindLevel();
        if (state.getValue(WIND_LEVEL) != windLevel) {
            level.setBlockAndUpdate(worldPosition, state.setValue(WIND_LEVEL, windLevel));
        }
    }

    public void setGoggles(boolean newGoggles) {
        goggles = newGoggles;
    }

    public void spawnParticleBurst(boolean bad) {
        if (level == null) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(worldPosition);
        RandomSource random = level.random;
        int count = bad ? 5 : 20;
        for (int i = 0; i < count; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25f, 1).normalize();
            Vec3 particlePos = center.add(offset.scale(0.5 + random.nextDouble() * 0.125f)).add(0, 0.125, 0);
            Vec3 motion = offset.scale(0.03125f);
            level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), particlePos.x, particlePos.y, particlePos.z, motion.x, motion.y, motion.z);
        }
    }

    public void SwitchToGaleState() {
        if (!(level instanceof PonderLevel)) {
            return;
        }

        setChamberState(new CreativeChamberState(ChargerType.NORMAL));
        spawnParticleBurst(false);
    }

    public void SwitchToIllState() {
        if (!(level instanceof PonderLevel)) {
            return;
        }

        setChamberState(new CreativeChamberState(ChargerType.BAD));
        spawnParticleBurst(true);
    }

    public void tickAnimation(float targetAngle) {
        boolean active = isControllerActive();
        if (active) {
            float facingAngle = (AngleHelper.horizontalAngle(getBlockState().getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.NORTH)) + 180) % 360;
            headAngle.chase(facingAngle, 0.125f, Chaser.EXP);
        }
        else {
            headAngle.chase(targetAngle, 0.25f, Chaser.exp(5));
        }
        headAngle.tickChaser();
        headAnimation.chase(active ? 1 : 0, 0.25f, Chaser.exp(0.25f));
        headAnimation.tickChaser();
    }

    public WindLevel getWindLevel() {
        return currentState.getWindLevel();
    }

    public WindLevel getWindLevelForRender() {
        return getWindLevelFromBlock();
    }

    public WindLevel getWindLevelFromBlock() {
        return BreezeChamberBlock.getWindLevelOf(getBlockState());
    }

    private Optional<GasConversion> getConversion(ChargerType chargerType, GasStack inputStack) {
        if (level == null || inputStack.isEmpty()) {
            return Optional.empty();
        }

        return switch (chargerType) {
            case NORMAL -> BreezeChamberRecipeIndex.findEnergization(level.getRecipeManager(), inputStack);
            case BAD -> BreezeChamberRecipeIndex.findDissipation(level.getRecipeManager(), inputStack);
            case NONE -> Optional.empty();
        };
    }

    private GasStack getTankGasStack() {
        IChamberGasTank tank = getTank();
        if (tank == null) {
            return GasStack.EMPTY;
        }

        GasTank inventory = tank.getTankInventory();
        return inventory.isEmpty() ? GasStack.EMPTY : inventory.getGasStack();
    }

    private Gas getTankGasType() {
        return getTankGasStack().getGasType();
    }

    private @Nullable IChamberGasTank getTank() {
        if (level == null) {
            return null;
        }

        IChamberGasTank tank = source.get();
        if (tank == null || tank.isRemoved()) {
            source = new WeakReference<>(null);
            tank = level.getBlockEntity(worldPosition.below()) instanceof IChamberGasTank tankBe ? tankBe : null;
            source = new WeakReference<>(tank);
        }
        return tank == null ? null : tank.getControllerBE();
    }

    private boolean isOutputFull() {
        return tankBehaviour.getPrimaryHandler().getSpace() == 0;
    }

    private boolean isOutputMismatched() {
        GasStack inputStack = getTankGasStack();
        GasTank output = tankBehaviour.getPrimaryHandler();
        if (inputStack.isEmpty() || output.isEmpty()) {
            return false;
        }

        ChargerType chargerType = switch (getWindLevel()) {
            case GALE -> ChargerType.NORMAL;
            case ILL -> ChargerType.BAD;
            case CALM -> ChargerType.NONE;
        };
        return getConversion(chargerType, inputStack).map(conversion -> !GasStack.isSameGasSameComponents(output.getGasStack(), conversion.output())).orElse(false);
    }

    private boolean isInputInvalid() {
        GasStack inputStack = getTankGasStack();
        if (inputStack.isEmpty()) {
            return false;
        }

        ChargerType chargerType = switch (getWindLevel()) {
            case GALE -> ChargerType.NORMAL;
            case ILL -> ChargerType.BAD;
            case CALM -> ChargerType.NONE;
        };
        return chargerType != ChargerType.NONE && getConversion(chargerType, inputStack).isEmpty();
    }

    private int getProcessingAmount() {
        int time = getWindRemainingTime();
        if (time == 0) {
            return 0;
        }

        IChamberGasTank tank = getTank();
        if (tank == null || isControllerActive()) {
            return 0;
        }

        int maxAmount = CCBConfig.server().airtights.maxProcessingRate.get();
        float ratio = Mth.clamp((float) Mth.abs(time) / getMaxEffectiveThreshold(), 0, 1);
        return Mth.clamp((int) (maxAmount * ratio), 1, maxAmount);
    }

    public void spawnParticles() {
        spawnParticles(getWindLevelFromBlock());
    }

    private void spawnParticles(WindLevel windLevel) {
        if (level == null) {
            return;
        }

        RandomSource random = level.getRandom();
        int possibility = windLevel == WindLevel.ILL ? 4 : 2;
        if (random.nextInt(possibility) != 0) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(worldPosition);
        Vec3 particlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f).multiply(1, 0, 1));
        if (random.nextInt(possibility * 2) == 0) {
            level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        }
        double yMotion = random.nextDouble() * 0.0125f;
        Vec3 galeParticlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25f, 1).normalize().scale(0.5 + random.nextDouble() * 0.125f)).add(0, 0.5, 0);
        if (!windLevel.isAtLeast(WindLevel.GALE)) {
            return;
        }

        level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), galeParticlePos.x, galeParticlePos.y, galeParticlePos.z, 0, yMotion, 0);
    }

    private void updateAirtightAssemblyDriver() {
        if (level == null || level.isClientSide) {
            return;
        }

        int newLevel = getWindRemainingLevel();
        if (newLevel == lastWindLevel) {
            return;
        }

        lastWindLevel = newLevel;
        AirtightTankBlock.updateTankState(level, worldPosition.below());
    }

    public enum ChargerType {
        BAD,
        NONE,
        NORMAL;

        public static ChargerType fromTag(CompoundTag compoundTag, String key, ChargerType fallback) {
            if (compoundTag.contains(key, Tag.TAG_STRING)) {
                try {
                    return valueOf(compoundTag.getString(key));
                } catch (IllegalArgumentException ignored) {
                    return fallback;
                }
            }
            if (!compoundTag.contains(key, Tag.TAG_ANY_NUMERIC)) {
                return fallback;
            }

            int ordinal = compoundTag.getInt(key);
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : fallback;
        }
    }
}
