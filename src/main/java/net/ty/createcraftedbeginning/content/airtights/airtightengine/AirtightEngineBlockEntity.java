package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEngineBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {
    public static final float DELTA_TIME = 0.01f;
    public static final int BASE_ROTATION_SPEED = 8;

    private static final int LAZY_TICK_RATE = 20;
    private static final List<BlockPos> COG_NEIGHBOUR_OFFSETS = List.of(new BlockPos(-1, -1, 0), new BlockPos(-1, 0, -1), new BlockPos(-1, 0, 1), new BlockPos(-1, 1, 0), new BlockPos(0, -1, -1), new BlockPos(0, -1, 1), new BlockPos(0, 1, -1), new BlockPos(0, 1, 1), new BlockPos(1, -1, 0), new BlockPos(1, 0, -1), new BlockPos(1, 0, 1), new BlockPos(1, 1, 0));

    private final AirtightEngineAnimationState animationState = new AirtightEngineAnimationState();
    private final AirtightEngineDriveController driveController = new AirtightEngineDriveController(this);

    private CCBAdvancementBehaviour advancementBehaviour;

    public AirtightEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState engineState = getBlockState();
        boolean isEngineValid = engineState.getBlock() instanceof AirtightEngineBlock engine && engine.canSurvive(engineState, level, getBlockPos()) && AirtightEngineBlock.isStateValid(engineState);
        if (isEngineValid) {
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
            animationState.tick(getSpeed(), isOverStressed(), driveController);
            return;
        }

        driveController.tickServer();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        AirtightEngineTooltip.addGoggleInformation(tooltip, getBlockState().getValue(AirtightEngineBlock.CLOCKWISE), getGeneratedSpeed());
        return true;
    }

    @Override
    public void initialize() {
        driveController.beforeInitialize();
        super.initialize();
        driveController.afterInitialize();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level == null || level.isClientSide || getSpeed() == 0 || getGeneratedSpeed() == 0) {
            return;
        }

        AirtightAssemblyDriverCore driverCore = driveController.getDriverCore();
        if (driverCore == null || !driverCore.isActive()) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.EMERGING_POWER);
        if (driverCore.getCurrentLevel() != AirtightAssemblyDriverCore.MAX_LEVEL) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.FLYWHEEL);
    }

    @Override
    protected void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.write(tag, provider, clientPacket);
        if (clientPacket) {
            return;
        }

        driveController.writePersistent(tag);
    }

    @Override
    protected void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.read(tag, provider, clientPacket);
        if (clientPacket) {
            return;
        }

        driveController.readPersistent(tag);
    }

    @Override
    public float getGeneratedSpeed() {
        return driveController.getGeneratedSpeed();
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

    void updateRotation() {
        driveController.rebuildKineticNetwork();
    }

    float getPistonPhase(float partialTicks) {
        return animationState.getPistonPhase(partialTicks);
    }

    boolean isEngineOverStressed() {
        return isOverStressed();
    }

    boolean hasKineticNetwork() {
        return hasNetwork();
    }

    void applyGeneratedRotation() {
        updateGeneratedRotation();
    }

    void rebuildKineticNetwork() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (hasNetwork()) {
            getOrCreateNetwork().remove(this);
        }
        RotationPropagator.handleRemoved(level, worldPosition, this);
        removeSource();
        attachKinetics();
    }
}
