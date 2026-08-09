package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.IChamberGasTank;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.BaseChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.InactiveChamberState;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IGasInventoryIdentifierProvider {
    private static final int LAZY_TICK_RATE = 20;
    private static Consumer<BreezeChamberBlockEntity> clientTicker = chamber -> {};

    private final LerpedFloat headAnimation;
    private final BreezeChamberSerialization serialization;
    private final BreezeChamberGasProcessor gasProcessor;
    private final BreezeChamberController controller;
    private final BreezeChamberDisplay display;
    protected LerpedFloat headAngle;
    protected WeakReference<IChamberGasTank> source;
    private CCBAdvancementBehaviour advancementBehaviour;
    private SmartGasTankBehaviour tankBehaviour;
    private BaseChamberState currentState;

    public BreezeChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        currentState = new InactiveChamberState();
        headAngle = LerpedFloat.angular();
        headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.NORTH)) + 180) % 360);
        headAnimation = LerpedFloat.linear();
        source = new WeakReference<>(null);
        serialization = new BreezeChamberSerialization();
        gasProcessor = new BreezeChamberGasProcessor(this);
        controller = new BreezeChamberController(this);
        display = new BreezeChamberDisplay(this);
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
        return (int) Math.max(1, (long) getMaxWindCapacity() * 3 / 4);
    }

    public static void setClientTicker(Consumer<BreezeChamberBlockEntity> ticker) {
        clientTicker = ticker;
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
        controller.tick();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        controller.lazyTick();
    }

    @Override
    protected void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        serialization.write(this, tag);
        super.write(tag, provider, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        serialization.read(this, tag);
        super.read(tag, provider, clientPacket);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        controller.onLoad();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToGoggleTooltip(tooltip);
    }

    @Override
    public InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        return new Single(worldPosition);
    }

    public boolean hasGoggles() {
        return display.hasGoggles();
    }

    public boolean hasTrainHat() {
        return display.hasTrainHat();
    }

    public boolean isControllerActive() {
        return gasProcessor.isControllerActive();
    }

    public boolean isCreative() {
        return currentState.isCreative();
    }

    public InteractionResultHolder<ItemStack> tryUpdateChargerByItem(ItemStack stack, boolean forceOverflow, boolean simulate) {
        return controller.tryUpdateChargerByItem(stack, forceOverflow, simulate);
    }

    public int getWindRemainingLevel() {
        int time = getWindRemainingTime();
        return time <= 0 ? 0 : time < getMaxEffectiveThreshold() ? 1 : 2;
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
        controller.syncWindProgress();
    }

    public void tickGasProcessing(ChargerType chargerType) {
        gasProcessor.tickGasProcessing(chargerType);
    }

    public void loadFromItem(ItemStack stack) {
        controller.loadFromItem(stack);
    }

    public void playSound(boolean bad) {
        display.playSound(bad);
    }

    public void saveToItem(ItemStack stack) {
        serialization.saveToItem(this, stack);
    }

    public void setChamberState(BaseChamberState newState) {
        currentState = newState;
        controller.onStateChanged();
    }

    public void setGoggles(boolean newGoggles) {
        display.setGoggles(newGoggles);
    }

    public void spawnParticleBurst(boolean bad) {
        display.spawnParticleBurst(bad);
    }

    public void SwitchToGaleState() {
        controller.switchToGaleState();
    }

    public void SwitchToIllState() {
        controller.switchToIllState();
    }

    public void tickAnimation(float targetAngle) {
        display.tickAnimation(targetAngle);
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

    public void spawnParticles() {
        display.spawnParticles();
    }

    BaseChamberState getChamberStateInternal() {
        return currentState;
    }

    SmartGasTankBehaviour getTankBehaviourInternal() {
        return tankBehaviour;
    }

    BreezeChamberGasProcessor getGasProcessorInternal() {
        return gasProcessor;
    }

    LerpedFloat getHeadAnimationInternal() {
        return headAnimation;
    }

    void runClientTicker() {
        clientTicker.accept(this);
    }

    void setChamberStateFromSerialization(BaseChamberState state) {
        currentState = state;
    }

    void setGogglesFromSerialization(boolean goggles) {
        display.setGoggles(goggles);
    }

    void setTrainHatFromSerialization(boolean trainHat) {
        display.setTrainHat(trainHat);
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
