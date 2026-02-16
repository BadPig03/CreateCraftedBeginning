package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

public class AirtightEngineBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {
    public static final int BASE_ROTATION_SPEED = 8;
    public static final float DELTA_TIME = 0.01f;
    private static final int LAZY_TICK_RATE = 20;
    private WeakReference<AirtightTankBlockEntity> source;
    private float pistonPhase;
    private float previousPhase;

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
        if (!(state.getBlock() instanceof AirtightEngineBlock engine) || !engine.canSurvive(state, level, getBlockPos()) || !AirtightEngineBlock.isStateValid(state)) {
            level.destroyBlock(worldPosition, true);
            return;
        }

        AirtightTankBlock.updateTankState(level, worldPosition.relative(AirtightEngineBlock.getFacing(state)));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            pistonPhase += Mth.abs(getSpeed()) * DELTA_TIME;
            if (pistonPhase > Mth.TWO_PI) {
                pistonPhase -= Mth.TWO_PI;
            }
            return;
        }

        if (isOverStressed()) {
            return;
        }

        updateGeneratedRotation();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.goggles.airtight_engine").forGoggles(tooltip);
        CCBLang.translate("gui.goggles.airtight_engine.rotation_direction").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (getBlockState().getValue(AirtightEngineBlock.CLOCKWISE)) {
            CCBLang.translate("gui.goggles.airtight_engine.rotation_direction.clockwise").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.translate("gui.goggles.airtight_engine.rotation_direction.counter_clockwise").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        if (!StressImpact.isEnabled()) {
            return true;
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.goggles.capacity_provided").style(ChatFormatting.GRAY).forGoggles(tooltip);
        double capacityProvided = Mth.abs(getGeneratedSpeed()) * BlockStressValues.getCapacity(CCBBlocks.AIRTIGHT_ENGINE_BLOCK.get());
        CCBLang.number(capacityProvided).translate("gui.goggles.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public void initialize() {
        super.initialize();
        updateGeneratedRotation();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level == null || level.isClientSide) {
            return;
        }
        if (getSpeed() == 0) {
            return;
        }

        AirtightAssemblyDriverCore driverCore = getCore();
        if (driverCore == null || !driverCore.getStructureManager().isActive()) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.RISING_FORCE);
        if (driverCore.getLevelCalculator().getCurrentLevel() != AirtightAssemblyDriverCore.MAX_LEVEL) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.FLYWHEEL);
    }

    @Override
    public float getGeneratedSpeed() {
        return BASE_ROTATION_SPEED * getSpeedModifier() * (getBlockState().getValue(AirtightEngineBlock.CLOCKWISE) ? 1 : -1);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.RISING_FORCE, CCBAdvancements.FLYWHEEL);
        behaviours.add(advancementBehaviour);
        super.addBehaviours(behaviours);
    }

    @Override
    public List<BlockPos> addPropagationLocations(@NotNull IRotate block, BlockState state, List<BlockPos> neighbours) {
        Axis axis = block.getRotationAxis(state);
        BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1)).forEach(offset -> {
            if (axis.choose(offset.getX(), offset.getY(), offset.getZ()) != 0) {
                return;
            }
            if (offset.distSqr(BlockPos.ZERO) != 2) {
                return;
            }
            neighbours.add(worldPosition.offset(offset));
        });
        return neighbours;
    }

    private @Nullable AirtightTankBlockEntity getTank() {
        if (level == null) {
            return null;
        }

        AirtightTankBlockEntity tank = source.get();
        if (tank == null || tank.isRemoved()) {
            tank = findController(level);
            source = new WeakReference<>(tank);
        }

        return tank == null ? null : tank.getControllerBE();
    }

    private @Nullable AirtightTankBlockEntity findController(@NotNull Level level) {
        Direction facing = AirtightEngineBlock.getFacing(getBlockState());
        BlockEntity be = level.getBlockEntity(worldPosition.relative(facing));
        return be instanceof AirtightTankBlockEntity tank ? tank : null;
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
        AirtightAssemblyDriverCore driverCore = getCore();
        AirtightTankBlockEntity controller = getTankController();
        if (driverCore == null || controller == null) {
            return 0;
        }

        int engines = driverCore.getStructureManager().getAttachedEngines();
        return engines == 0 ? 0 : (float) driverCore.getLevelCalculator().getCurrentLevel() / engines;
    }

    public float getPistonPhase() {
        return pistonPhase;
    }

    public float getPreviousPhase() {
        return previousPhase;
    }

    public void setPreviousPhase(float previousPhase) {
        this.previousPhase = previousPhase;
    }
}
