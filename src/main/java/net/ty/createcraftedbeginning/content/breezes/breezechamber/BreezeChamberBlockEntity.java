package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.GasTank;
import net.ty.createcraftedbeginning.api.gas.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.IChamberGasTank;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

import static net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WIND_LEVEL;

public class BreezeChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int MAX_WIND_CAPACITY = 72000;
    private static final int LAZY_TICK_RATE = 20;
    private static final int MAX_EFFECTIVE_THRESHOLD = MAX_WIND_CAPACITY / 2;

    private static final String COMPOUND_KEY_STATE_TYPE = "StateType";
    private static final String COMPOUND_KEY_STATE_DATA = "StateData";
    private static final String COMPOUND_KEY_GOGGLES = "Goggles";
    private static final String COMPOUND_KEY_TRAIN_HAT = "TrainHat";
    private static final String COMPOUND_KEY_IS_CREATIVE = "isCreative";
    private static final String COMPOUND_KEY_REMAINING_TIME = "RemainingTime";

    private final LerpedFloat headAnimation;

    protected LerpedFloat headAngle;
    protected WeakReference<IChamberGasTank> source;

    private boolean goggles;
    private boolean trainHat;
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

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.BREEZE_CHAMBER.get(), (be, context) -> be.isControllerActive() ? null : be.tankBehaviour.getCapability());
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, CCBConfig.server().airtights.maxTankCapacity.get() * 500L).forbidInsertion().allowExtraction();
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.A_ROYAL_FEAST, CCBAdvancements.BAD_APPLE, CCBAdvancements.UNIVERSAL_ANTIDOTE);
        behaviours.add(tankBehaviour);
        behaviours.add(advancementBehaviour);
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
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        currentState.tick(this);
        if (!level.isClientSide) {
            return;
        }

        spawnParticles(getWindLevelFromBlock());
        if (!shouldTickAnimation()) {
            return;
        }

        tickAnimation();
    }

    @Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

    @Override
    protected void write(@NotNull CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        CompoundTag stateTag = new CompoundTag();
        currentState.save(stateTag);
        compoundTag.put(COMPOUND_KEY_STATE_DATA, stateTag);
        compoundTag.putInt(COMPOUND_KEY_STATE_TYPE, currentState.getChargerType().ordinal());
        compoundTag.putBoolean(COMPOUND_KEY_GOGGLES, goggles);
        compoundTag.putBoolean(COMPOUND_KEY_TRAIN_HAT, trainHat);
        super.write(compoundTag, registries, clientPacket);
    }

    @Override
    protected void read(@NotNull CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        if (compoundTag.contains(COMPOUND_KEY_STATE_TYPE) && compoundTag.contains(COMPOUND_KEY_STATE_DATA)) {
            ChargerType stateType = ChargerType.values()[compoundTag.getInt(COMPOUND_KEY_STATE_TYPE)];
            CompoundTag stateData = compoundTag.getCompound(COMPOUND_KEY_STATE_DATA);
            boolean isCreative = stateData.contains(COMPOUND_KEY_IS_CREATIVE) && stateData.getBoolean(COMPOUND_KEY_IS_CREATIVE);

            BaseChamberState newState;
            if (isCreative) {
                newState = new CreativeChamberState(stateType);
            }
            else {
                int remainingTime = stateData.contains(COMPOUND_KEY_REMAINING_TIME) ? stateData.getInt(COMPOUND_KEY_REMAINING_TIME) : 0;
                newState = switch (stateType) {
                    case NORMAL -> new GaleChamberState(remainingTime, false);
                    case BAD -> new IllChamberState(remainingTime, false);
                    case NONE -> new InactiveChamberState();
                };
            }

            newState.read(stateData);
            setChamberState(newState);
        }
        if (compoundTag.contains(COMPOUND_KEY_GOGGLES)) {
            goggles = compoundTag.getBoolean(COMPOUND_KEY_GOGGLES);
        }
        if (compoundTag.contains(COMPOUND_KEY_TRAIN_HAT)) {
            trainHat = compoundTag.getBoolean(COMPOUND_KEY_TRAIN_HAT);
        }
        super.read(compoundTag, registries, clientPacket);
    }

    private @NotNull Gas getTankEnergizedGas() {
        return getTankGas().getEnergizedGas();
    }

    private @NotNull Gas getTankGas() {
        IChamberGasTank tank = getTank();
        if (tank == null) {
            return Gas.EMPTY_GAS_HOLDER.value();
        }

        GasTank inventory = tank.getTankInventory();
        if (inventory.isEmpty()) {
            return Gas.EMPTY_GAS_HOLDER.value();
        }

        return inventory.getGasStack().getGas();
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
        Gas tankGas = getTankGas();
        Gas tankEnergizedGas = getTankEnergizedGas();
        return !tankGas.isEmpty() && !tankEnergizedGas.isEmpty() && !tankBehaviour.getPrimaryHandler().isEmpty() && !tankBehaviour.getPrimaryHandler().getGasStack().is(tankEnergizedGas);
    }

    @OnlyIn(Dist.CLIENT)
    private boolean shouldTickAnimation() {
        return !VisualizationManager.supportsVisualization(level);
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
            }
            else {
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

    private int getEnergizationAmount() {
        int time = getWindRemainingTime();
        if (time <= 0) {
            return 0;
        }

        IChamberGasTank tank = getTank();
        if (tank == null || isControllerActive()) {
            return 0;
        }

        int maxAmount = CCBConfig.server().gas.maxVortexingAmount.get();
        float ratio = Mth.clamp((float) time / MAX_EFFECTIVE_THRESHOLD, 0, 1);
        return Mth.clamp((int) (maxAmount * ratio), 1, maxAmount);
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
        Vec3 added = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f).multiply(1, 0, 1));
        if (random.nextInt(possibility * 2) == 0) {
            level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), added.x, added.y, added.z, 0, 0, 0);
        }
        double yMotion = random.nextDouble() * 0.0125f;
        Vec3 galeAdded = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25f, 1).normalize().scale(0.5 + random.nextDouble() * 0.125f)).add(0, 0.5, 0);
        if (!windLevel.isAtLeast(WindLevel.GALE)) {
            return;
        }

        level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), galeAdded.x, galeAdded.y, galeAdded.z, 0, yMotion, 0);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        WindLevel windLevel = getWindLevel();
        CCBLang.translate("gui.goggles.breeze_chamber").forGoggles(tooltip);
        CCBLang.translate("gui.goggles.breeze_chamber.current_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(windLevel.getTranslatable()).style(windLevel.getChatFormatting()).forGoggles(tooltip, 1);

        Gas tankGas = getTankGas();
        boolean isBad = currentState.getWindLevel() == WindLevel.ILL;
        boolean isActive = isControllerActive();
        boolean invalidGas = getTankEnergizedGas().isEmpty() && !tankGas.isEmpty() && !isActive;
        boolean outputFailed = (isOutputFull() || isOutputMismatched()) && !isActive;
        int time = getWindRemainingTime();
        if (currentState.getWindLevel() != WindLevel.CALM) {
            CCBLang.translate("gui.goggles.breeze_chamber.remaining_time").style(ChatFormatting.GRAY).forGoggles(tooltip);
            ChatFormatting timeColor = time > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
            if (isCreative()) {
                CCBLang.translate("gui.goggles.gas_container.infinity").style(timeColor).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.seconds(time, level.tickRateManager().tickrate()).style(timeColor).forGoggles(tooltip, 1);
            }

            if (isActive) {
                CCBLang.translate("gui.goggles.breeze_chamber.energization_level").style(ChatFormatting.GRAY).forGoggles(tooltip);
                CCBLang.translate("gui.goggles.breeze_chamber.current_level", CCBLang.number(getWindRemainingLevel())).style(ChatFormatting.BLUE).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.translate("gui.goggles.breeze_chamber.energization_rate").style(ChatFormatting.GRAY).forGoggles(tooltip);
                CCBLang.translate("gui.goggles.breeze_chamber.milli_buckets_per_second", CCBLang.number(getEnergizationAmount() * 20)).style(ChatFormatting.BLUE).forGoggles(tooltip, 1);
            }
        }

        if (!isActive) {
            tooltip.add(CommonComponents.EMPTY);
            IGasHandler handler = tankBehaviour.getPrimaryHandler();
            GasStack gasStack = handler.getGasInTank(0);
            long capacity = handler.getTankCapacity(0);
            LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
            if (gasStack.isEmpty()) {
                CCBLang.translate("gui.goggles.gas_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
                CCBLang.number(capacity).add(mb).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.translate("gui.goggles.gas_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
                CCBLang.gasName(gasStack).style(ChatFormatting.WHITE).forGoggles(tooltip, 1);
                CCBLang.number(gasStack.getAmount()).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
            }
        }

        if (isBad || invalidGas || outputFailed) {
            tooltip.add(CommonComponents.EMPTY);
            CCBLang.translate("gui.goggles.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        }
        if (isBad) {
            CCBLang.addToGoggles(tooltip, "gui.goggles.breeze_chamber.improper_food");
        }
        if (invalidGas) {
            CCBLang.addToGoggles(tooltip, "gui.goggles.breeze_chamber.invalid_gas", Component.translatable(tankGas.getTranslationKey()));
        }
        if (outputFailed) {
            CCBLang.addToGoggles(tooltip, "gui.goggles.breeze_chamber.output_failed");
        }
        return true;
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

    public boolean tryUpdateChargerByItem(@NotNull ItemStack itemStack, boolean forceOverflow, boolean simulate) {
        if (itemStack.getItem() == CCBItems.CREATIVE_ICE_CREAM.asItem()) {
            if (!simulate) {
                ChargerType chargerType = CreativeChamberState.getNextChargeType(currentState.getChargerType());
                setChamberState(chargerType == ChargerType.NONE ? new InactiveChamberState() : new CreativeChamberState(chargerType));
                spawnParticleBurst(false);
                playSound(chargerType == ChargerType.BAD);
            }
            return true;
        }

        InteractionResult result = currentState.onItemInsert(this, itemStack, forceOverflow, simulate);
        if (result != InteractionResult.SUCCESS) {
            return false;
        }

        notifyUpdate();
        return true;
    }

    public int getWindRemainingLevel() {
        int time = getWindRemainingTime();
        if (time <= 0) {
            return 0;
        }

        return time < MAX_EFFECTIVE_THRESHOLD ? 1 : 2;
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

    public void doEnergization() {
        if (level == null) {
            return;
        }

        IChamberGasTank tank = getTank();
        if (tank == null) {
            return;
        }

        int maxAmount = getEnergizationAmount();
        GasTank inventory = tank.getTankInventory();
        if (inventory.isEmpty() || isOutputFull()) {
            return;
        }

        Gas energizedGas = getTankEnergizedGas();
        if (energizedGas.isEmpty() || isOutputMismatched()) {
            return;
        }

        IGasHandler handler = tank.getCapability();
        GasStack drainedStack = handler.drain(Math.min(inventory.getGasAmount(), Math.min(maxAmount, tankBehaviour.getPrimaryHandler().getSpace())), GasAction.EXECUTE);
        tankBehaviour.getInternalGasHandler().forceFill(new GasStack(energizedGas.getHolder(), drainedStack.getAmount()), GasAction.EXECUTE);
    }

    public void loadFromItem(@NotNull ItemStack stack) {
        int time = stack.getOrDefault(CCBDataComponents.BREEZE_TIME, 0);
        if (time > 0) {
            setChamberState(new GaleChamberState(time, false));
            playSound(false);
        }
        else if (time < 0) {
            setChamberState(new IllChamberState(time, false));
            playSound(true);
        }
        else {
            setChamberState(new InactiveChamberState());
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

    public void saveToItem(@NotNull ItemStack stack) {
        stack.set(CCBDataComponents.BREEZE_TIME, currentState.getRemainingTime());
    }

    public void setChamberState(BaseChamberState newState) {
        currentState = newState;
        if (level == null || level.isClientSide && !isVirtual()) {
            return;
        }

        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(WIND_LEVEL, currentState.getWindLevel()));
        notifyUpdate();
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
            Vec3 added = center.add(offset.scale(0.5 + random.nextDouble() * 0.125f)).add(0, 0.125, 0);
            Vec3 scaled = offset.scale(0.03125f);
            level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), added.x, added.y, added.z, scaled.x, scaled.y, scaled.z);
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

    @OnlyIn(Dist.CLIENT)
    public void tickAnimation() {
        boolean active = isControllerActive();
        if (active) {
            headAngle.chase((AngleHelper.horizontalAngle(getBlockState().getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.NORTH)) + 180) % 360, 0.125f, Chaser.EXP);
            headAngle.tickChaser();
        }
        else {
            headAngle.chase(getTarget(), 0.25f, Chaser.exp(5));
            headAngle.tickChaser();
        }
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

    public enum ChargerType {
        BAD,
        NONE,
        NORMAL
    }
}
