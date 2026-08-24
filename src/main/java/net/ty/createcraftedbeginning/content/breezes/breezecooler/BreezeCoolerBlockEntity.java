package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerController.CoolingSyncMode;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.BaseCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.InactiveCoolerState;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe.CoolingData;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeCoolerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static Consumer<BreezeCoolerBlockEntity> clientTicker = cooler -> {};
    protected final LerpedFloat headAnimation;
    protected final BreezeCoolerSerialization serialization;
    protected final BreezeCoolerRecipeCache recipeCache;
    protected final BreezeCoolerController controller;
    protected final BreezeCoolerDisplay display;
    protected LerpedFloat headAngle;
    protected CCBAdvancementBehaviour advancementBehaviour;
    protected SmartFluidTankBehaviour tankBehaviour;
    protected BaseCoolerState currentState;
    private long clientCoolingSyncGameTime = Long.MIN_VALUE;

    public BreezeCoolerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        currentState = new InactiveCoolerState();
        headAngle = LerpedFloat.angular();
        headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOptionalValue(BreezeCoolerBlock.FACING).orElse(Direction.NORTH)) + 180) % 360);
        headAnimation = LerpedFloat.linear();
        serialization = new BreezeCoolerSerialization();
        recipeCache = new BreezeCoolerRecipeCache(this);
        controller = new BreezeCoolerController(this);
        display = new BreezeCoolerDisplay(this);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.BREEZE_COOLER.get(), (cooler, context) -> cooler.tankBehaviour.getCapability());
    }

    public static int getSnowballCoolingTime() {
        return Math.max(0, CCBConfig.server().airtights.snowballCoolingTime.get());
    }

    public static int getDangerousFluidTemperature() {
        return Math.max(1, CCBConfig.server().airtights.dangerousFluidTemperature.get());
    }

    public static int getMaxCoolantCapacity() {
        return Math.max(1, CCBConfig.server().airtights.maxCoolantCapacity.get());
    }

    public static int getOverflowThreshold() {
        return (int) Math.max(1, (long) getMaxCoolantCapacity() * 3 / 4);
    }

    public static int getMaxFluidCapacity() {
        return Math.max(1, CCBConfig.server().airtights.breezeCoolerFluidCapacity.get()) * FluidType.BUCKET_VOLUME;
    }

    public static void setClientTicker(Consumer<BreezeCoolerBlockEntity> ticker) {
        clientTicker = ticker;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartFluidTankBehaviour.single(this, getMaxFluidCapacity());
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.A_MURDER, CCBAdvancements.FROZEN_AMBROSIA);
        behaviours.add(tankBehaviour);
        behaviours.add(advancementBehaviour);
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

    public void spawnParticles() {
        display.spawnParticles();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToGoggleTooltip(tooltip);
    }

    public CoolingData getFluidCoolingData(FluidStack fluidStack) {
        return recipeCache.getFluidCoolingData(fluidStack);
    }

    public void onCoolingTimeChanged(CoolingSyncMode syncMode) {
        controller.onCoolingTimeChanged(syncMode);
    }

    public void playCoolingEffects() {
        controller.playCoolingEffects();
    }

    public boolean hasGoggles() {
        return display.hasGoggles();
    }

    public boolean hasTrainHat() {
        return display.hasTrainHat();
    }

    public boolean isStockKeeper() {
        return controller.isStockKeeper();
    }

    public boolean isCreative() {
        return currentState.isCreative();
    }

    public boolean tryUpdateCoolantByItem(ItemStack itemStack, boolean forceOverflow, boolean simulate) {
        return controller.tryUpdateCoolantByItem(itemStack, forceOverflow, simulate);
    }

    public FrostLevel getFrostLevel() {
        Level level = getLevel();
        if (level != null && level.isClientSide && !isVirtual()) {
            return getFrostLevelFromBlock();
        }

        return currentState.getFrostLevel();
    }

    public FrostLevel getFrostLevelForRender() {
        return isStockKeeper() ? FrostLevel.CHILLED : getFrostLevelFromBlock();
    }

    public FrostLevel getFrostLevelFromBlock() {
        return BreezeCoolerBlock.getFrostLevelOf(getBlockState());
    }

    public int getCoolRemainingTime() {
        int remainingTime = currentState.getRemainingTime();
        Level level = getLevel();
        if (level == null || !level.isClientSide || isVirtual()) {
            return remainingTime;
        }

        if (!getFrostLevelFromBlock().isAtLeast(FrostLevel.CHILLED)) {
            return 0;
        }
        if (remainingTime <= 0 || currentState.isCreative() || clientCoolingSyncGameTime == Long.MIN_VALUE) {
            return remainingTime;
        }

        long elapsedTicks = Math.max(0L, level.getGameTime() - clientCoolingSyncGameTime);
        return (int) Math.max(1L, (long) remainingTime - elapsedTicks);
    }

    public BaseCoolerState getCurrentState() {
        return currentState;
    }

    public LerpedFloat getHeadAnimation() {
        return headAnimation;
    }

    public LerpedFloat getHeadAngle() {
        return headAngle;
    }

    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    public SmartFluidTank getTankInventory() {
        return tankBehaviour.getPrimaryHandler();
    }

    public void playSound() {
        display.playSound();
    }

    public void setCoolerState(BaseCoolerState newState) {
        Level level = getLevel();
        if (level != null && level.isClientSide && !isVirtual()) {
            return;
        }

        currentState = newState;
        controller.onStateChanged();
    }

    public void setGoggles(boolean newGoggles) {
        display.setGoggles(newGoggles);
    }

    public void spawnParticleBurst() {
        display.spawnParticleBurst();
    }

    public void switchToChilledState() {
        controller.switchToChilledState();
    }

    public void tickAnimation(float targetAngle) {
        display.tickAnimation(targetAngle);
    }

    public LerpedFloat getHeadAnimationInternal() {
        return headAnimation;
    }

    public void runClientTicker() {
        clientTicker.accept(this);
    }

    public void setCoolerStateFromSerialization(BaseCoolerState state) {
        currentState = state;
        refreshClientCoolingPredictionBase();
    }

    void refreshClientCoolingPredictionBase() {
        Level level = getLevel();
        if (level == null || !level.isClientSide || isVirtual()) {
            return;
        }

        clientCoolingSyncGameTime = level.getGameTime();
    }

    public void setGogglesFromSerialization(boolean goggles) {
        display.setGoggles(goggles);
    }

    public void setTrainHatFromSerialization(boolean trainHat) {
        display.setTrainHat(trainHat);
    }

    public enum CoolantType {
        NONE,
        NORMAL;

        public static CoolantType fromTag(CompoundTag compoundTag, String key, CoolantType fallback) {
            if (!compoundTag.contains(key, Tag.TAG_STRING)) {
                return fallback;
            }

            try {
                return valueOf(compoundTag.getString(key));
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
    }
}
