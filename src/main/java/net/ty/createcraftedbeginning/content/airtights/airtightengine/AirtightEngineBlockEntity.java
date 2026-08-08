package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEngineBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {
    public static final float DELTA_TIME = 0.01f;
    public static final int BASE_ROTATION_SPEED = 8;

    private static final int LAZY_TICK_RATE = 20;
    private static final String COMPOUND_KEY_GENERATED_SPEED = "GeneratedSpeed";
    private static final List<BlockPos> COG_NEIGHBOUR_OFFSETS = List.of(new BlockPos(-1, -1, 0), new BlockPos(-1, 0, -1), new BlockPos(-1, 0, 1), new BlockPos(-1, 1, 0), new BlockPos(0, -1, -1), new BlockPos(0, -1, 1), new BlockPos(0, 1, -1), new BlockPos(0, 1, 1), new BlockPos(1, -1, 0), new BlockPos(1, 0, -1), new BlockPos(1, 0, 1), new BlockPos(1, 1, 0));

    private WeakReference<AirtightTankBlockEntity> source;
    private float pistonPhase;
    private float pistonAnimationSpeed;
    private float lastGeneratedSpeed = Float.NaN;
    private float restoredGeneratedSpeed;
    private float persistedGeneratedSpeed;
    private boolean restoringKineticNetwork;

    private CCBAdvancementBehaviour advancementBehaviour;

    public AirtightEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        source = new WeakReference<>(null);
        pistonPhase = 0;
        setLazyTickRate(LAZY_TICK_RATE);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        boolean isValid = state.getBlock() instanceof AirtightEngineBlock engine && engine.canSurvive(state, level, getBlockPos()) && AirtightEngineBlock.isStateValid(state);
        if (isValid) {
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

        if (level.isClientSide) {
            tickPiston();
            return;
        }

        if (isOverStressed()) {
            lastGeneratedSpeed = Float.NaN;
            return;
        }

        refreshGeneratedRotationIfNeeded();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.airtight_engine").forGoggles(tooltip);
        CCBLang.translate("gui.airtight_engine.rotation_direction").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (getBlockState().getValue(AirtightEngineBlock.CLOCKWISE)) {
            CCBLang.translate("gui.airtight_engine.rotation_direction.clockwise").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.translate("gui.airtight_engine.rotation_direction.counter_clockwise").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        if (!StressImpact.isEnabled()) {
            return true;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.capacity_provided").style(ChatFormatting.GRAY).forGoggles(tooltip);
        double capacity = Mth.abs(getGeneratedSpeed()) * BlockStressValues.getCapacity(CCBBlocks.AIRTIGHT_ENGINE_BLOCK.get());
        CCBLang.number(capacity).translate("gui.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        return true;
    }

    private void tickPiston() {
        pistonAnimationSpeed = Mth.abs(getSpeed());
        if (pistonAnimationSpeed == 0 && !isOverStressed()) {
            pistonAnimationSpeed = Mth.abs(getGeneratedSpeed());
        }

        pistonPhase += pistonAnimationSpeed * DELTA_TIME;
        if (pistonPhase <= Mth.TWO_PI) {
            return;
        }

        pistonPhase %= Mth.TWO_PI;
    }

    @Override
    public void initialize() {
        restoringKineticNetwork = level != null && !level.isClientSide && hasNetwork() && restoredGeneratedSpeed != 0;
        super.initialize();
        restoringKineticNetwork = false;
        restoredGeneratedSpeed = 0;
        lastGeneratedSpeed = Float.NaN;
        refreshGeneratedRotationIfNeeded();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level == null || level.isClientSide || getSpeed() == 0) {
            return;
        }

        AirtightAssemblyDriverCore core = getCore();
        if (core == null || !core.getStructureManager().isActive()) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.EMERGING_POWER);
        if (core.getLevelCalculator().getCurrentLevel() != AirtightAssemblyDriverCore.MAX_LEVEL) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.FLYWHEEL);
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (clientPacket) {
            return;
        }

        compoundTag.putFloat(COMPOUND_KEY_GENERATED_SPEED, persistedGeneratedSpeed);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (clientPacket) {
            return;
        }

        float storedSpeed = compoundTag.contains(COMPOUND_KEY_GENERATED_SPEED) ? compoundTag.getFloat(COMPOUND_KEY_GENERATED_SPEED) : 0;
        restoredGeneratedSpeed = Float.isFinite(storedSpeed) ? storedSpeed : 0;
        persistedGeneratedSpeed = restoredGeneratedSpeed;
    }

    @Override
    public float getGeneratedSpeed() {
        if (restoringKineticNetwork) {
            return restoredGeneratedSpeed;
        }

        float generatedSpeed = BASE_ROTATION_SPEED * getSpeedModifier() * (getRotationDirection() ? 1 : -1);
        persistedGeneratedSpeed = Float.isFinite(generatedSpeed) ? generatedSpeed : 0;
        return persistedGeneratedSpeed;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.EMERGING_POWER, CCBAdvancements.FLYWHEEL);
        behaviours.add(advancementBehaviour);
    }

    @Override
    public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
        for (BlockPos offset : COG_NEIGHBOUR_OFFSETS) {
            neighbours.add(worldPosition.offset(offset));
        }
        return neighbours;
    }

    public void updateRotation() {
        if (level == null || level.isClientSide) {
            return;
        }

        lastGeneratedSpeed = Float.NaN;
        if (hasNetwork()) {
            getOrCreateNetwork().remove(this);
        }
        RotationPropagator.handleRemoved(level, worldPosition, this);
        removeSource();
        attachKinetics();
    }

    private @Nullable AirtightTankBlockEntity getTank() {
        if (level == null) {
            return null;
        }

        AirtightTankBlockEntity tank = source.get();
        if (tank != null && !tank.isRemoved()) {
            return tank;
        }

        tank = findTank(level);
        source = new WeakReference<>(tank);
        return tank;
    }

    private @Nullable AirtightTankBlockEntity findTank(Level level) {
        Direction facing = AirtightEngineBlock.getFacing(getBlockState());
        BlockPos tankPos = worldPosition.relative(facing);
        if (!level.isLoaded(tankPos)) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(tankPos);
        return blockEntity instanceof AirtightTankBlockEntity tank ? tank : null;
    }

    private @Nullable AirtightAssemblyDriverCore getCore() {
        AirtightTankBlockEntity controller = getTankController();
        return controller == null ? null : controller.getCore();
    }

    private @Nullable AirtightTankBlockEntity getTankController() {
        AirtightTankBlockEntity tank = getTank();
        return tank == null ? null : tank.getControllerBE();
    }

    private float getSpeedModifier() {
        AirtightTankBlockEntity controller = getTankController();
        if (controller == null) {
            return 0;
        }

        AirtightAssemblyDriverCore core = controller.getCore();
        int engines = core.getStructureManager().getAttachedEngines();
        return engines == 0 ? 0 : (float) core.getLevelCalculator().getCurrentLevel() / engines;
    }

    private void refreshGeneratedRotationIfNeeded() {
        if (level == null || level.isClientSide) {
            return;
        }

        float generatedSpeed = getGeneratedSpeed();
        if (Float.compare(generatedSpeed, lastGeneratedSpeed) == 0) {
            return;
        }

        lastGeneratedSpeed = generatedSpeed;
        updateGeneratedRotation();
    }

    private boolean getRotationDirection() {
        BlockState state = getBlockState();
        boolean facesPositive = AirtightEngineBlock.getFacing(state).getAxisDirection() == AxisDirection.POSITIVE;
        return facesPositive == state.getValue(AirtightEngineBlock.CLOCKWISE);
    }

    public float getPistonPhase(float partialTicks) {
        return pistonPhase + pistonAnimationSpeed * partialTicks * DELTA_TIME;
    }
}
