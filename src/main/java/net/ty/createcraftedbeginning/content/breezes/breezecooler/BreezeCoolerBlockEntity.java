package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
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
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.BaseCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.ChilledCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.CreativeCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.InactiveCoolerState;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe.CoolingData;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

import static net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.ATTACHED;
import static net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FROST_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeCoolerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final String COMPOUND_KEY_STATE_TYPE = "StateType";
    private static final String COMPOUND_KEY_STATE_DATA = "StateData";
    private static final String COMPOUND_KEY_GOGGLES = "Goggles";
    private static final String COMPOUND_KEY_TRAIN_HAT = "TrainHat";
    private static final String COMPOUND_KEY_IS_CREATIVE = "isCreative";
    private static final String COMPOUND_KEY_REMAINING_TIME = "RemainingTime";
    private static final int COOLING_RECIPE_CACHE_INTERVAL = 100;
    private static final int COOLING_STATE_SYNC_INTERVAL = 20;
    private static final int COOLING_EFFECT_INTERVAL = 10;
    private static Consumer<BreezeCoolerBlockEntity> clientTicker = cooler -> {};

    private final LerpedFloat headAnimation;

    protected LerpedFloat headAngle;

    private boolean stockKeeper;
    private boolean goggles;
    private boolean trainHat;
    private CCBAdvancementBehaviour advancementBehaviour;
    private SmartFluidTankBehaviour tankBehaviour;
    private BaseCoolerState currentState;
    private FluidStack cachedCoolingFluid;
    private CoolingData cachedFluidCoolingData;
    private long cachedCoolingRecipeExpiry;
    private long lastCoolingEffectTime;

    public BreezeCoolerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        currentState = new InactiveCoolerState();
        goggles = false;
        headAngle = LerpedFloat.angular();
        headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOptionalValue(BreezeCoolerBlock.FACING).orElse(Direction.NORTH)) + 180) % 360);
        headAnimation = LerpedFloat.linear();
        cachedCoolingFluid = FluidStack.EMPTY;
        cachedFluidCoolingData = CoolingData.EMPTY;
        cachedCoolingRecipeExpiry = Long.MIN_VALUE;
        lastCoolingEffectTime = Long.MIN_VALUE;
        stockKeeper = false;
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

    private static BaseCoolerState createState(CoolantType coolantType, int remainingTime, boolean isCreative) {
        if (isCreative && coolantType != CoolantType.NONE) {
            return new CreativeCoolerState(coolantType);
        }
        return switch (coolantType) {
            case NORMAL -> remainingTime > 0 ? new ChilledCoolerState(remainingTime, false) : new InactiveCoolerState();
            case NONE -> new InactiveCoolerState();
        };
    }

    private static BaseCoolerState readState(CompoundTag tag) {
        CompoundTag stateData = tag.getCompound(COMPOUND_KEY_STATE_DATA);
        CoolantType stateType = CoolantType.fromTag(tag, COMPOUND_KEY_STATE_TYPE, CoolantType.NONE);
        boolean isCreative = stateData.contains(COMPOUND_KEY_IS_CREATIVE, Tag.TAG_BYTE) && stateData.getBoolean(COMPOUND_KEY_IS_CREATIVE);
        int remainingTime = stateData.contains(COMPOUND_KEY_REMAINING_TIME, Tag.TAG_ANY_NUMERIC) ? Mth.clamp(stateData.getInt(COMPOUND_KEY_REMAINING_TIME), 0, getMaxCoolantCapacity()) : 0;
        return createState(stateType, remainingTime, isCreative);
    }

    public static int getMaxCoolantCapacity() {
        return Math.max(1, CCBConfig.server().airtights.maxCoolantCapacity.get());
    }

    public static int getOverflowThreshold() {
        return Math.max(1, getMaxCoolantCapacity() / 2);
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
        if (level == null) {
            return;
        }

        if (!currentState.tick(this)) {
            return;
        }

        if (!level.isClientSide) {
            return;
        }

        clientTicker.accept(this);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        stockKeeper = BlazeBurnerBlockEntity.getStockTicker(level, worldPosition) != null;
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        CompoundTag stateTag = new CompoundTag();
        currentState.save(stateTag);
        compoundTag.put(COMPOUND_KEY_STATE_DATA, stateTag);
        compoundTag.putString(COMPOUND_KEY_STATE_TYPE, currentState.getCoolantType().name());
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

        syncFrostLevelBlockState();
    }

    private boolean isLiquidInvalid() {
        SmartFluidTank tank = tankBehaviour.getPrimaryHandler();
        if (tank.isEmpty() || level == null) {
            return false;
        }

        CoolingData data = getFluidCoolingData(tank.getFluid());
        return data.time() <= 0 || data.amount() <= 0;
    }

    public void spawnParticles() {
        if (level == null) {
            return;
        }

        RandomSource random = level.getRandom();
        if (random.nextInt(2) != 0) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(worldPosition);
        Vec3 particlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f).multiply(1, 0, 1));
        boolean hasOpenTop = level.getBlockState(worldPosition.above()).getCollisionShape(level, worldPosition.above()).isEmpty();
        if (hasOpenTop || random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        }
        Vec3 chilledParticlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25, 1).normalize().scale((hasOpenTop ? 0.25 : 0.5) + random.nextDouble() * 0.125)).add(0, 0.5, 0);
        if (!getFrostLevelFromBlock().isAtLeast(FrostLevel.CHILLED)) {
            return;
        }

        level.addParticle(ParticleTypes.SNOWFLAKE, chilledParticlePos.x, chilledParticlePos.y, chilledParticlePos.z, 0, hasOpenTop ? 0.0625 : random.nextDouble() * 0.0125, 0);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null || stockKeeper) {
            return false;
        }

        FrostLevel frostLevel = getFrostLevel();
        CCBLang.translate("gui.breeze_cooler").forGoggles(tooltip);
        CCBLang.translate("gui.breeze_cooler.current_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(frostLevel.getTranslatable()).style(frostLevel.getChatFormatting()).forGoggles(tooltip, 1);

        int time = getCoolRemainingTime();
        if (time > 0) {
            CCBLang.translate("gui.breeze_cooler.remaining_time").style(ChatFormatting.GRAY).forGoggles(tooltip);
            if (isCreative()) {
                CCBLang.translate("gui.fluid_container.infinity").style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.seconds(time, level.tickRateManager().tickrate()).style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            }
        }

        IFluidHandler tank = tankBehaviour.getPrimaryHandler();
        FluidStack fluid = tank.getFluidInTank(0);
        tooltip.add(CommonComponents.EMPTY);
        LangBuilder millibuckets = CCBLang.translate("gui.unit.milli_buckets");
        boolean isEmpty = fluid.isEmpty();
        CCBLang.translate("gui.fluid_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (isEmpty) {
            CCBLang.number(tank.getTankCapacity(0)).add(millibuckets).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.fluidName(fluid).style(ChatFormatting.WHITE).forGoggles(tooltip, 1);
            CCBLang.number(fluid.getAmount()).add(millibuckets).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(tank.getTankCapacity(0)).add(millibuckets).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }

        if (!isLiquidInvalid()) {
            return true;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        CCBLang.addToGoggles(tooltip, "gui.breeze_cooler.invalid_fluid");
        return true;
    }

    public CoolingData getFluidCoolingData(FluidStack fluidStack) {
        if (level == null || fluidStack.isEmpty()) {
            return CoolingData.EMPTY;
        }

        long gameTime = level.getGameTime();
        boolean sameFluid = !cachedCoolingFluid.isEmpty() && FluidStack.isSameFluidSameComponents(cachedCoolingFluid, fluidStack);
        if (sameFluid && gameTime < cachedCoolingRecipeExpiry) {
            return cachedFluidCoolingData;
        }

        cachedCoolingFluid = fluidStack.copyWithAmount(1);
        cachedFluidCoolingData = CoolingRecipe.getCoolingTime(level, null, fluidStack);
        cachedCoolingRecipeExpiry = gameTime + COOLING_RECIPE_CACHE_INTERVAL;
        return cachedFluidCoolingData;
    }

    public void markCoolingChanged() {
        setChanged();
    }

    public void syncCoolingProgress() {
        if (level == null || level.isClientSide) {
            return;
        }

        setChanged();
        long phase = level.getGameTime() + worldPosition.asLong();
        if (Math.floorMod(phase, COOLING_STATE_SYNC_INTERVAL) != 0) {
            return;
        }

        notifyUpdate();
    }

    public void playCoolingEffects() {
        if (level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        if (lastCoolingEffectTime != Long.MIN_VALUE && gameTime - lastCoolingEffectTime < COOLING_EFFECT_INTERVAL) {
            return;
        }

        lastCoolingEffectTime = gameTime;
        playSound();
        spawnParticleBurst();
    }

    public boolean hasGoggles() {
        return goggles;
    }

    public boolean hasTrainHat() {
        return trainHat;
    }

    public boolean isStockKeeper() {
        return stockKeeper;
    }

    public boolean isCreative() {
        return currentState.isCreative();
    }

    public boolean tryUpdateCoolantByItem(ItemStack itemStack, boolean forceOverflow, boolean simulate) {
        if (itemStack.is(CCBItems.CREATIVE_ICE_CREAM)) {
            if (simulate) {
                return true;
            }

            CoolantType coolantType = CreativeCoolerState.getNextCoolantType(currentState.getCoolantType());
            setCoolerState(coolantType == CoolantType.NONE ? new InactiveCoolerState() : new CreativeCoolerState(coolantType));
            spawnParticleBurst();
            playSound();
            return true;
        }

        InteractionResult result = currentState.onItemInsert(this, itemStack, forceOverflow, simulate);
        if (result != InteractionResult.SUCCESS) {
            return false;
        }

        if (simulate) {
            return true;
        }

        if (level != null && !level.isClientSide && itemStack.is(CCBItemTags.ICE_CREAMS.tag)) {
            advancementBehaviour.awardPlayer(CCBAdvancements.FROZEN_AMBROSIA);
        }
        notifyUpdate();
        return true;
    }

    public FrostLevel getFrostLevel() {
        return currentState.getFrostLevel();
    }

    public FrostLevel getFrostLevelForRender() {
        return stockKeeper ? FrostLevel.CHILLED : getFrostLevelFromBlock();
    }

    public FrostLevel getFrostLevelFromBlock() {
        return BreezeCoolerBlock.getFrostLevelOf(getBlockState());
    }

    public int getCoolRemainingTime() {
        return currentState.getRemainingTime();
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
        if (level == null) {
            return;
        }

        level.playSound(null, worldPosition, SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125f + level.random.nextFloat() * 0.125f, 0.75f - level.random.nextFloat() * 0.25f);
    }

    public void setCoolerState(BaseCoolerState newState) {
        currentState = newState;
        setChanged();
        if (level == null || level.isClientSide && !isVirtual()) {
            return;
        }

        syncFrostLevelBlockState();
        notifyUpdate();
    }

    private void syncFrostLevelBlockState() {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        FrostLevel frostLevel = currentState.getFrostLevel();
        if (state.getValue(FROST_LEVEL) == frostLevel) {
            return;
        }

        level.setBlockAndUpdate(worldPosition, state.setValue(FROST_LEVEL, frostLevel));
    }

    public void setGoggles(boolean newGoggles) {
        goggles = newGoggles;
    }

    public void spawnParticleBurst() {
        if (level == null) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(worldPosition);
        RandomSource random = level.random;
        for (int i = 0; i < 20; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25, 1).normalize();
            Vec3 particlePos = center.add(offset.scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.125, 0);
            Vec3 motion = offset.scale(0.03125);

            level.addParticle(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, motion.x, motion.y, motion.z);
        }
    }

    public void switchToChilledState() {
        if (!(level instanceof PonderLevel)) {
            return;
        }

        setCoolerState(new CreativeCoolerState(CoolantType.NORMAL));
        spawnParticleBurst();
    }

    public void tickAnimation(float targetAngle) {
        boolean active = getBlockState().getValue(ATTACHED);
        if (active) {
            float facingAngle = (AngleHelper.horizontalAngle(getBlockState().getOptionalValue(BreezeCoolerBlock.FACING).orElse(Direction.SOUTH)) + 180) % 360;
            headAngle.chase(facingAngle, 0.125f, Chaser.EXP);
        }
        else {
            headAngle.chase(targetAngle, 0.25f, Chaser.exp(5));
        }
        headAngle.tickChaser();
        headAnimation.chase(active ? 1 : 0, 0.25f, Chaser.exp(0.25f));
        headAnimation.tickChaser();
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
