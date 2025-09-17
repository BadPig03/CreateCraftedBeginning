package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasTank;
import net.ty.createcraftedbeginning.api.gas.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import net.ty.createcraftedbeginning.util.Helpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

import static net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WIND_LEVEL;

public class BreezeChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int INSERTION_THRESHOLD = 6000;
    public static final int MAX_WIND_CAPACITY = 72000;
    public static final int MAX_EFFECTIVE_THRESHOLD = 36000;

    public boolean goggles;
    public boolean hat;
    public boolean wind;
    public float windRotationSpeed = 0;
    public LerpedFloat headAnimation;
    public boolean isCreative = false;

    protected ChargerType activeCharger;
    protected int windRemainingTime;
    protected LerpedFloat headAngle;
    protected WeakReference<AirtightTankBlockEntity> source;

    private SmartGasTankBehaviour tankBehaviour;

    public BreezeChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        activeCharger = ChargerType.NONE;
        windRemainingTime = 0;
        headAnimation = LerpedFloat.linear();
        headAngle = LerpedFloat.angular();
        goggles = false;
        wind = false;
        source = new WeakReference<>(null);

        headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.NORTH)) + 180) % 360);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasCapabilities.GasHandler.BLOCK, CCBBlockEntities.BREEZE_CHAMBER.get(), (be, context) -> be.tankBehaviour.getCapability());
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, CCBConfig.server().compressedAir.airtightTankCapacity.get() * 500).forbidInsertion().allowExtraction();
        behaviours.add(tankBehaviour);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) {
            return;
        }

        if (getWindLevel().isAtLeast(WindLevel.BREEZE) && !wind) {
            wind = true;
            windRotationSpeed = 24f;
        } else if (!getWindLevel().isAtLeast(WindLevel.BREEZE) && wind) {
            wind = false;
            windRotationSpeed = 0;
        }

        if (level.isClientSide) {
            if (shouldTickAnimation()) {
                tickAnimation();
            }
            if (!isVirtual()) {
                spawnParticles(getWindLevelFromBlock());
            }
        }

        if (!isCreative) {
            if (windRemainingTime > 0) {
                windRemainingTime--;
            } else if (windRemainingTime < 0) {
                windRemainingTime++;
            }
        }
        if (activeCharger != ChargerType.NONE) {
            updateBlockState();
            if (activeCharger == ChargerType.NORMAL && level.getGameTime() % 20 == 0) {
                TryToVortexing();
            }
        }
        if (windRemainingTime != 0) {
            return;
        }

        activeCharger = ChargerType.NONE;
        updateBlockState();
    }

    @Override
    protected void write(@NotNull CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putInt("chargerLevel", activeCharger.ordinal());
        compound.putInt("windRemainingTime", windRemainingTime);
        compound.putBoolean("Goggles", goggles);
        compound.putBoolean("TrainHat", hat);
        compound.putBoolean("Wind", wind);
        compound.putFloat("WindSpeed", windRotationSpeed);
        compound.putBoolean("isCreative", isCreative);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(@NotNull CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        if (compound.contains("chargerLevel")) {
            activeCharger = ChargerType.values()[compound.getInt("chargerLevel")];
        }
        if (compound.contains("windRemainingTime")) {
            windRemainingTime = compound.getInt("windRemainingTime");
        }
        if (compound.contains("Goggles")) {
            goggles = compound.getBoolean("Goggles");
        }
        if (compound.contains("TrainHat")) {
            hat = compound.getBoolean("TrainHat");
        }
        if (compound.contains("Wind")) {
            wind = compound.getBoolean("Wind");
            windRotationSpeed = compound.getFloat("WindSpeed");
        }
        if (compound.contains("isCreative")) {
            isCreative = compound.getBoolean("isCreative");
        }
        super.read(compound, registries, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        WindLevel windLevel = getWindLevel();
        CCBLang.translate("gui.goggles.breeze_chamber").forGoggles(tooltip);
        CCBLang.builder().add(CCBLang.translate("gui.goggles.breeze_chamber.current_state").style(ChatFormatting.GRAY)).forGoggles(tooltip);
        CCBLang.builder().add(CCBLang.translate(windLevel.getTranslatable()).style(windLevel.getChatFormatting())).forGoggles(tooltip, 1);

        boolean noAirtightTank = !isValidBlockBelow(true);
        boolean invalidAirtightTank = !isValidBlockBelow(false);
        boolean hasAnyTime = windRemainingTime != 0;
        if (hasAnyTime) {
            CCBLang.builder().add(CCBLang.translate("gui.goggles.breeze_chamber.remaining_time")).style(ChatFormatting.GRAY).forGoggles(tooltip);
            ChatFormatting timeColor = windRemainingTime > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
            if (isCreative) {
                CCBLang.builder().add(CCBLang.translateDirect("gui.goggles.infinite")).style(timeColor).forGoggles(tooltip, 1);
            } else {
                CCBLang.builder().add(CCBLang.seconds(windRemainingTime, Helpers.getActualTickRate(level))).style(timeColor).forGoggles(tooltip, 1);
            }

            CCBLang.translate("gui.goggles.breeze_chamber.vortexing_rate").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.translate("gui.goggles.breeze_chamber.milli_buckets_per_second", CCBLang.number(getVortexedAmount() * 20)).style(ChatFormatting.BLUE).forGoggles(tooltip, 1);
        }

        if (windRemainingTime <= 0 || noAirtightTank || invalidAirtightTank) {
            tooltip.add(CommonComponents.EMPTY);
            CCBLang.translate("gui.goggles.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        }

        if (windRemainingTime < 0) {
            CCBLang.addToGoggles(tooltip, "gui.goggles.breeze_chamber.improper_food");
            return true;
        }

        if (noAirtightTank) {
            CCBLang.addToGoggles(tooltip, "gui.goggles.breeze_chamber.no_airtight_tank");
            return true;
        }

        if (invalidAirtightTank) {
            CCBLang.addToGoggles(tooltip, "gui.goggles.breeze_chamber.invalid_airtight_tank");
            return true;
        }

        if (!hasAnyTime) {
            CCBLang.addToGoggles(tooltip, "gui.goggles.breeze_chamber.now_hungry");
            return true;
        }

        return true;
    }

    public WindLevel getWindLevelForRender() {
        return getWindLevelFromBlock();
    }

    public boolean isValidBlockBelow(boolean ignoreWidth) {
        if (level == null || isVirtual()) {
            return false;
        }

        if (ignoreWidth) {
            BlockEntity be = level.getBlockEntity(worldPosition.below());
            return be instanceof AirtightTankBlockEntity;
        }

        return getTank() != null;
    }

    private @Nullable AirtightTankBlockEntity getTank() {
        if (level == null) {
            return null;
        }

        AirtightTankBlockEntity tank = source.get();
        if (tank == null || tank.isRemoved()) {
            source = new WeakReference<>(null);
            BlockEntity be = level.getBlockEntity(worldPosition.below());
            if (be instanceof AirtightTankBlockEntity tankBe) {
                source = new WeakReference<>(tank = tankBe);
            }
        }
        if (tank == null) {
            return null;
        }

        AirtightTankBlockEntity controller = tank.getControllerBE();
        if (controller == null || controller.getWidth() != 1) {
            return null;
        }
        return controller;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean shouldTickAnimation() {
        return !VisualizationManager.supportsVisualization(level);
    }

    @OnlyIn(Dist.CLIENT)
    void tickAnimation() {
        boolean active = isValidBlockBelow(false);

        if (!active) {
            float target = getTarget();
            headAngle.chase(target, 0.25f, LerpedFloat.Chaser.exp(5));
            headAngle.tickChaser();
        } else {
            headAngle.chase((AngleHelper.horizontalAngle(getBlockState().getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.NORTH)) + 180) % 360, 0.125f, LerpedFloat.Chaser.EXP);
            headAngle.tickChaser();
        }

        headAnimation.chase(active ? 1 : 0.75f, 0.25f, LerpedFloat.Chaser.exp(0.25f));
        headAnimation.tickChaser();
    }

    public int getWindRemainingTime() {
        return windRemainingTime;
    }

    protected boolean tryUpdateChargerByItem(ItemStack itemStack, boolean forceOverflow, boolean simulate) {
        if (level == null) {
            return false;
        }

        if (itemStack.getItem() == CCBItems.CREATIVE_ICE_CREAM.asItem()) {
            return handleCreativeIceCream(simulate);
        }

        WindChargingRecipe.WindChargingData data = WindChargingRecipe.getResultingWindChargingTime(level, itemStack);
        int time = data.time();
        if (time == 0 || data.amount() == 0) {
            return false;
        }

        if (data.isMilky()) {
            return handleMilkyItem(simulate);
        }

        return handleChargerType(data.isBadFood() ? ChargerType.BAD : ChargerType.NORMAL, time, forceOverflow, simulate);
    }

    private boolean handleChargerType(ChargerType chargerType, int newWindTime, boolean forceOverflow, boolean simulate) {
        if (level == null) {
            return false;
        }
        if (!forceOverflow && activeCharger == chargerType && Mth.abs(windRemainingTime) > INSERTION_THRESHOLD) {
            return false;
        }

        int newRemainingTime = windRemainingTime;
        newRemainingTime += (chargerType == ChargerType.BAD) ? -newWindTime : newWindTime;

        if (forceOverflow) {
            newRemainingTime = Mth.clamp(newRemainingTime, -MAX_WIND_CAPACITY, MAX_WIND_CAPACITY);
        } else if (Math.abs(newRemainingTime) > MAX_WIND_CAPACITY) {
            return false;
        }

        if (!simulate) {
            if (newRemainingTime > 0) {
                activeCharger = ChargerType.NORMAL;
            } else if (newRemainingTime < 0) {
                activeCharger = ChargerType.BAD;
            } else {
                activeCharger = ChargerType.NONE;
            }

            windRemainingTime = newRemainingTime;
            updateBlockState();

            if (!level.isClientSide) {
                playSound(chargerType);
            } else {
                spawnParticleBurst();
            }
        }
        return true;
    }

    private boolean handleMilkyItem(boolean simulate) {
        if (level == null || activeCharger != ChargerType.BAD) {
            return false;
        }

        if (!simulate) {
            activeCharger = ChargerType.NONE;
            windRemainingTime = 0;
            updateBlockState();

            if (!level.isClientSide) {
                playSound(ChargerType.NORMAL);
            } else {
                spawnParticleBurst();
            }
        }
        return true;
    }

    private boolean handleCreativeIceCream(boolean simulate) {
        if (level == null) {
            return false;
        }

        ChargerType newCharger = switch (activeCharger) {
            case NORMAL -> ChargerType.BAD;
            case BAD -> ChargerType.NONE;
            case NONE -> ChargerType.NORMAL;
        };

        if (!simulate) {
            activeCharger = newCharger;
            windRemainingTime = switch (newCharger) {
                case NORMAL -> MAX_WIND_CAPACITY;
                case NONE -> 0;
                case BAD -> -MAX_WIND_CAPACITY;
            };
            isCreative = activeCharger != ChargerType.NONE;
            updateBlockState();

            if (!level.isClientSide) {
                playSound(newCharger);
            } else if (newCharger == ChargerType.NORMAL) {
                spawnParticleBurst();
            }
        }
        return true;
    }

    private void playSound(ChargerType charger) {
        if (level == null) {
            return;
        }

        if (charger == ChargerType.BAD) {
            level.playSound(null, worldPosition, SoundEvents.BREEZE_HURT, SoundSource.BLOCKS, 0.125f + level.random.nextFloat() * 0.125f, 0.75f - level.random.nextFloat() * 0.25f);
        } else {
            level.playSound(null, worldPosition, SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125f + level.random.nextFloat() * 0.125f, 0.75f - level.random.nextFloat() * 0.25f);
        }
    }

    private int getVortexedAmount() {
        if (windRemainingTime <= 0) {
            return 0;
        }

        int maxAmount = CCBConfig.server().compressedAir.maxVortexingAmount.get();
        float ratio = Mth.clamp((float) windRemainingTime / MAX_EFFECTIVE_THRESHOLD, 0, 1);
        return Mth.clamp((int) (maxAmount * ratio), 1, maxAmount);
    }

    private void TryToVortexing() {
        if (level == null) {
            return;
        }

        AirtightTankBlockEntity tank = getTank();
        if (tank == null) {
            return;
        }

        int maxAmount = getVortexedAmount();
        GasTank inventory = tank.getTankInventory();
        if (inventory.isEmpty() || tankBehaviour.getPrimaryHandler().getSpace() < maxAmount) {
            return;
        }

        GasStack gasStack = inventory.getGas();
        Gas vortexedGas = gasStack.getGas().getVortexedGasName();
        if (vortexedGas == null) {
            return;
        }

        IGasHandler handler = tank.getCapability();
        GasStack drainedStack = handler.drain(Math.min(inventory.getGasAmount(), maxAmount), GasAction.EXECUTE);

        var internalHandler = (SmartGasTankBehaviour.InternalGasHandler) tankBehaviour.getCapability();
        internalHandler.forceFill(new GasStack(vortexedGas.getHolder(), drainedStack.getAmount()), GasAction.EXECUTE);
    }

    public WindLevel getWindLevelFromBlock() {
        return BreezeChamberBlock.getWindLevelOf(getBlockState());
    }

    private float getTarget() {
        float target = 0;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !player.isInvisible()) {
            double x;
            double z;
            if (isVirtual()) {
                x = -4;
                z = -10;
            } else {
                x = player.getX();
                z = player.getZ();
            }
            double dx = x - (getBlockPos().getX() + 0.5);
            double dz = z - (getBlockPos().getZ() + 0.5);
            target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
        }
        target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
        return target;
    }

    private void spawnParticles(BreezeChamberBlock.WindLevel windLevel) {
        if (level == null) {
            return;
        }

        RandomSource r = level.getRandom();
        if (r.nextInt(4) != 0) {
            return;
        }

        Vec3 c = VecHelper.getCenterOf(worldPosition);
        Vec3 v = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, 0.125f).multiply(1, 0, 1));
        boolean empty = level.getBlockState(worldPosition.above()).getCollisionShape(level, worldPosition.above()).isEmpty();
        if (empty || r.nextInt(8) == 0) {
            level.addParticle((ParticleOptions) CCBParticleTypes.BREEZE_CLOUD.get(), v.x, v.y, v.z, 0, 0, 0);
        }

        double yMotion = empty ? 0.0625f : r.nextDouble() * 0.0125f;
        Vec3 v2 = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, 0.5f).multiply(1, 0.25f, 1).normalize().scale((empty ? 0.25f : 0.5) + r.nextDouble() * 0.125f)).add(0, 0.5, 0);
        if (windLevel.isAtLeast(BreezeChamberBlock.WindLevel.BREEZE)) {
            level.addParticle((ParticleOptions) CCBParticleTypes.BREEZE_CLOUD.get(), v2.x, v2.y, v2.z, 0, yMotion, 0);
        }
    }

    public WindLevel getWindLevel() {
        WindLevel level = WindLevel.CALM;
        switch (activeCharger) {
            case BAD:
                level = WindLevel.ILL;
                break;
            case NORMAL:
                boolean lowPercent = (double) windRemainingTime / MAX_WIND_CAPACITY < 0.0125;
                level = lowPercent ? WindLevel.BREEZE : WindLevel.GALE;
                break;
            case NONE:
            default:
                break;
        }
        return level;
    }

    public void spawnParticleBurst() {
        if (level == null) {
            return;
        }

        Vec3 c = VecHelper.getCenterOf(worldPosition);
        RandomSource r = level.random;
        for (int i = 0; i < 20; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, r, 0.5f).multiply(1, 0.25f, 1).normalize();
            Vec3 v = c.add(offset.scale(0.5 + r.nextDouble() * 0.125f)).add(0, 0.125, 0);
            Vec3 m = offset.scale(1 / 32f);

            level.addParticle((ParticleOptions) CCBParticleTypes.BREEZE_CLOUD.get(), v.x, v.y, v.z, m.x, m.y, m.z);
        }
    }

    public void updateBlockState() {
        if (level == null) {
            return;
        }
        WindLevel inBlockState = getWindLevelFromBlock();
        WindLevel wind = getWindLevel();
        if (inBlockState == wind) {
            notifyUpdate();
            return;
        }
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(WIND_LEVEL, wind));
        notifyUpdate();
    }

    public void saveToItem(ItemStack stack) {
        stack.set(CCBDataComponents.BREEZE_TIME, windRemainingTime);
    }

    public void loadFromItem(@NotNull ItemStack stack) {
        int time = stack.getOrDefault(CCBDataComponents.BREEZE_TIME, 0);

        if (time > 0) {
            activeCharger = ChargerType.NORMAL;
        } else if (time < 0) {
            activeCharger = ChargerType.BAD;
        } else {
            activeCharger = ChargerType.NONE;
        }
        windRemainingTime = time;
        updateBlockState();
    }

    public enum ChargerType {
        BAD,
        NONE,
        NORMAL
    }
}
