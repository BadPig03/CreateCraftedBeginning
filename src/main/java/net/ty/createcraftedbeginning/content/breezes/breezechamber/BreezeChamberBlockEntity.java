package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
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
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.api.gas.logistics.GasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.ChamberGasTank;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.BaseChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.InactiveChamberState;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation, GasInventoryIdentifierProvider {
    private static final int LAZY_TICK_RATE = 20;
    private static Consumer<BreezeChamberBlockEntity> clientTicker = chamber -> {};

    private final LerpedFloat headAnimation;
    private final BreezeChamberSerialization serialization;
    private final BreezeChamberGasProcessor gasProcessor;
    private final BreezeChamberController controller;
    private final BreezeChamberDisplay display;
    private final LerpedFloat headAngle;

    WeakReference<ChamberGasTank> source;
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
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBBlockEntities.BREEZE_CHAMBER.get(), (chamber, context) -> chamber.isControllerActive() ? null : chamber.tankBehaviour.getCapability());
    }

    public static int getMaxWindCapacity() {
        return Math.max(1, CCBConfig.server().machines.breezeChamber.maxWindChargeTicks.get());
    }

    public static int getMaxEffectiveThreshold() {
        return Math.max(1, getMaxWindCapacity() / 2);
    }

    public static int getOverflowThreshold() {
        return Math.max(1, getMaxWindCapacity() * 3 / 4);
    }

    public static void setClientTicker(Consumer<BreezeChamberBlockEntity> ticker) {
        clientTicker = ticker;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.LUXURY_TREAT, CCBAdvancements.BAD_APPLE, CCBAdvancements.IS_THIS_EVEN_SCIENTIFIC);
        behaviours.add(advancementBehaviour);

        tankBehaviour = SmartGasTankBehaviour.single(this, CCBConfig.server().machines.breezeChamber.gasVolume.get() * GasUnits.LITERS_PER_KILOLITER, GasPressureLimits.SAFE_PRESSURE_PA).forbidInsertion().allowExtraction();
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
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        serialization.write(this, compoundTag);
        super.write(compoundTag, provider, clientPacket);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        serialization.read(this, compoundTag);
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
        controller.onLoad();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToGoggleTooltip(tooltip);
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToTooltip(tooltip);
    }

    @Override
    public InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        return new Single(worldPosition);
    }

    public boolean isCreative() {
        return currentState.isCreative();
    }

    public int getWindRemainingLevel() {
        int remainingTime = getWindRemainingTime();
        if (remainingTime <= 0) {
            return 0;
        }

        if (remainingTime < getMaxEffectiveThreshold()) {
            return 1;
        }

        return 2;
    }

    public int getWindRemainingTime() {
        return currentState.getRemainingTime();
    }

    public void syncWindProgress() {
        controller.syncWindProgress();
    }

    public void tickGasProcessing(ChargerType chargerType, int windTime) {
        gasProcessor.tickGasProcessing(chargerType, windTime);
    }

    public void playSound(boolean isIllCharge) {
        display.playSound(isIllCharge);
    }

    public void setChamberState(BaseChamberState newState) {
        if (currentState.getChargerType() != newState.getChargerType()) {
            gasProcessor.flushPendingProcessing();
            gasProcessor.discardProcessingProgress();
        }
        currentState = newState;
        controller.onStateChanged();
    }

    public void spawnParticleBurst(boolean isIllCharge) {
        display.spawnParticleBurst(isIllCharge);
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

    public void spawnParticles() {
        display.spawnParticles();
    }

    public WindLevel getWindLevelFromBlock() {
        return BreezeChamberBlock.getWindLevelOf(getBlockState());
    }

    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    public LerpedFloat getHeadAngle() {
        return headAngle;
    }

    @Internal
    public SmartGasTankBehaviour getTankBehaviourInternal() {
        return tankBehaviour;
    }

    boolean hasGoggles() {
        return display.hasGoggles();
    }

    boolean hasTrainHat() {
        return display.hasTrainHat();
    }

    boolean isControllerActive() {
        return gasProcessor.isControllerActive();
    }

    InteractionResultHolder<ItemStack> tryUpdateChargerByItem(ItemStack stack, boolean forceOverflow, boolean simulate) {
        return controller.tryUpdateChargerByItem(stack, forceOverflow, simulate);
    }

    LerpedFloat getHeadAnimation() {
        return headAnimation;
    }

    void setGoggles(boolean hasGoggles) {
        display.setGoggles(hasGoggles);
    }

    WindLevel getWindLevel() {
        return currentState.getWindLevel();
    }

    WindLevel getWindLevelForRender() {
        return getWindLevelFromBlock();
    }

    BaseChamberState getChamberStateInternal() {
        return currentState;
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

    void setChamberStateFromSerialization(BaseChamberState chamberState) {
        currentState = chamberState;
    }

    void setGogglesFromSerialization(boolean hasGoggles) {
        display.setGoggles(hasGoggles);
    }

    void setTrainHatFromSerialization(boolean hasTrainHat) {
        display.setTrainHat(hasTrainHat);
    }

    public enum ChargerType {
        BAD,
        NONE,
        NORMAL;

        static ChargerType fromTag(CompoundTag compoundTag, String key) {
            if (compoundTag.contains(key, Tag.TAG_STRING)) {
                try {
                    return valueOf(compoundTag.getString(key));
                }
                catch (IllegalArgumentException ignored) {
                    return NONE;
                }
            }
            if (!compoundTag.contains(key, Tag.TAG_ANY_NUMERIC)) {
                return NONE;
            }

            int chargerTypeOrdinal = compoundTag.getInt(key);
            if (chargerTypeOrdinal < 0 || chargerTypeOrdinal >= values().length) {
                return NONE;
            }

            return values()[chargerTypeOrdinal];
        }
    }
}
