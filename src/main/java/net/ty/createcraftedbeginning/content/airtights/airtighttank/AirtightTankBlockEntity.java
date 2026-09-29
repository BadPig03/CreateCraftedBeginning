package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.gas.behaviour.OverpressureBehaviour;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockPart;
import net.ty.createcraftedbeginning.gas.multiblock.OverpressureMultiblockPart;
import net.ty.createcraftedbeginning.gas.overpressure.PressureRuptureService;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.SmartGasTank;
import net.ty.createcraftedbeginning.gas.visual.GasPressureDisplayScale;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightTankBlockEntity extends AbstractAirtightTankBlockEntity implements IHaveGoggleInformation, ChamberGasTank, ThresholdSwitchObservable, OverpressureMultiblockPart {
    private static final float TANK_GAUGE_ZERO_PRESSURE_ANGLE = 85;
    private static final float TANK_GAUGE_MAX_PRESSURE_ANGLE = -85;
    private static final float TANK_GAUGE_CHASE_SPEED = 0.2F;
    private final AirtightAssemblyDriverCore driverCore;
    private final AirtightTankStorageController storageController;
    private final AirtightTankDisplay display;
    private final AirtightTankSerialization serialization;
    private final LerpedFloat tankGaugeNeedle;
    private OverpressureBehaviour overpressureBehaviour;
    private final boolean[] tankGaugeOccluded = new boolean[4];
    private boolean tankGaugeInstalled;

    public AirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        driverCore = new AirtightAssemblyDriverCore();
        storageController = new AirtightTankStorageController(this);
        display = new AirtightTankDisplay(this);
        serialization = new AirtightTankSerialization(this, storageController);
        tankGaugeNeedle = LerpedFloat.linear().startWithValue(TANK_GAUGE_ZERO_PRESSURE_ANGLE);
        initializeTank(new SmartGasTank(getVolumePerBlock(), GasPressureLimits.HARD_PRESSURE_PA, this::onTankStateChanged));
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        overpressureBehaviour = new OverpressureBehaviour(this, this::isController, this::getCurrentGasPressurePa);
        behaviours.add(overpressureBehaviour);
    }

    @Override
    public void removeController(boolean keepFluids) {
        super.removeController(keepFluids);
    }

    @Override
    public void tick() {
        super.tick();
        ruptureIfOverstressed();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        AABB bounds = super.createRenderBoundingBox();
        if (!tankGaugeInstalled || !isController()) {
            return bounds;
        }

        return bounds.inflate(0.25, 0, 0.25);
    }

    @Override
    public GasTank getTankInventory() {
        return (GasTank) super.getTankInventory();
    }

    @Override
    protected void updateMultiBlockState() {
        if (level == null) {
            return;
        }

        BlockState tankState = getBlockState();
        if (!(tankState.getBlock() instanceof AirtightTankBlock)) {
            return;
        }

        Axis connectionAxis = getMainConnectionAxis();
        int controllerCoordinate = calculateCoords(getController(), connectionAxis);
        int blockCoordinate = calculateCoords(getBlockPos(), connectionAxis);
        tankState = tankState.setValue(AirtightTankBlock.BOTTOM, controllerCoordinate == blockCoordinate);
        tankState = tankState.setValue(AirtightTankBlock.TOP, controllerCoordinate + getHeight() - 1 == blockCoordinate);
        level.setBlock(worldPosition, tankState, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
    }

    @Override
    protected void resetTankBeforeControllerRemoval(boolean keepFluids) {
        storageController.resizeToBlocks(1);
    }

    @Override
    protected void resetStandaloneBlockState() {
        if (level == null) {
            return;
        }

        BlockState tankState = getBlockState();
        if (!(tankState.getBlock() instanceof AirtightTankBlock)) {
            return;
        }

        tankState = tankState.setValue(AirtightTankBlock.TOP, true).setValue(AirtightTankBlock.BOTTOM, true);
        level.setBlock(worldPosition, tankState, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
    }

    @Override
    protected long volumePerBlock() {
        return getVolumePerBlock();
    }

    @Override
    void tickController() {
        driverCore.tick(this);
        if (level == null || !level.isClientSide || !tankGaugeInstalled) {
            return;
        }

        tankGaugeNeedle.chase(getTankGaugeTargetAngle(), TANK_GAUGE_CHASE_SPEED, Chaser.EXP);
        tankGaugeNeedle.tickChaser();
    }

    @Override
    void afterMultiUpdated() {
        overpressureBehaviour.restartStabilizationGrace();
        synchronizeTankGaugeAttachment();
        updateTankState();
    }

    @Override
    void afterControllerStateCleared(boolean keepFluids) {
        overpressureBehaviour.restartStabilizationGrace();
        driverCore.reset();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        updateTankGaugeOcclusion();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        serialization.write(compoundTag, provider, clientPacket);
    }

    @Override
    public void writeSafe(CompoundTag compoundTag, Provider provider) {
        serialization.writeSafe(compoundTag);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        serialization.read(compoundTag, provider, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return display.addToGoggleTooltip(tooltip);
    }

    @Override
    public void setTankBlockCount(int tank, int blocks) {
        applyGasTankSize(blocks);
    }

    @Override
    public void mergeTankStateFrom(GasTankMultiblockPart source) {
        storageController.mergeTankStateFrom(source);
    }

    @Override
    public GasStack prepareTankStateForSplit(int tank, boolean controllerRemoved) {
        return storageController.prepareTankStateForSplit();
    }

    @Override
    public void applySplitTankState(int tank, GasStack state, int remainingTankCount) {
        storageController.applySplitTankState(state, remainingTankCount);
    }

    @Override
    public int getMaxValue() {
        return display.getMaxValue();
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return display.getCurrentValue();
    }

    @Override
    public MutableComponent format(int value) {
        return display.format(value);
    }

    @Override
    public double getMultiblockOverpressureStress() {
        return overpressureBehaviour.getStress();
    }

    @Override
    public void setMultiblockOverpressureStress(double stress) {
        overpressureBehaviour.setStress(stress);
    }

    @Override
    public void setExtraData(@Nullable Object data) {
        if (!(data instanceof Boolean installed)) {
            return;
        }

        setLocalTankGaugeInstalled(installed);
    }

    @Override
    public Object getExtraData() {
        return tankGaugeInstalled;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBBlockEntities.AIRTIGHT_TANK.get(), (tank, ignoredDirection) -> tank.getCapability());
    }

    public static int getConfiguredMaxLength() {
        return configuredMaxLength();
    }

    public static int getConfiguredMaxWidth() {
        return configuredMaxWidth();
    }

    public static BlockPos offsetInMulti(BlockPos origin, Axis axis, int lengthOffset, int uOffset, int vOffset) {
        return switch (axis) {
            case X -> origin.offset(lengthOffset, uOffset, vOffset);
            case Y -> origin.offset(uOffset, lengthOffset, vOffset);
            case Z -> origin.offset(uOffset, vOffset, lengthOffset);
        };
    }

    public int getTotalTankSize() {
        return getWidth() * getWidth() * getHeight();
    }

    public AirtightAssemblyDriverCore getCore() {
        return driverCore;
    }

    public boolean installTankGauge() {
        if (level == null || level.isClientSide) {
            return false;
        }

        AirtightTankBlockEntity controller = getControllerBE();
        if (controller == null || controller.tankGaugeInstalled) {
            return false;
        }

        controller.setLocalTankGaugeInstalled(true);
        controller.synchronizeTankGaugeAttachment();
        controller.setChanged();
        controller.notifyUpdate();
        return true;
    }

    public boolean removeTankGauge() {
        if (level == null || level.isClientSide) {
            return false;
        }

        AirtightTankBlockEntity controller = getControllerBE();
        if (controller == null || !controller.tankGaugeInstalled) {
            return false;
        }

        controller.clearTankGaugeAttachment();
        controller.notifyUpdate();
        return true;
    }

    public boolean hasTankGauge() {
        AirtightTankBlockEntity controller = getControllerBE();
        return controller != null && controller.tankGaugeInstalled;
    }

    static long getVolumePerBlock() {
        return CCBConfig.server().machines.airtightTank.gasVolumePerBlock.get() * GasUnits.LITERS_PER_KILOLITER;
    }

    OverpressureBehaviour getOverpressureBehaviour() {
        return overpressureBehaviour;
    }

    float getTankGaugeNeedleAngle(float partialTicks) {
        return tankGaugeNeedle.getValue(partialTicks);
    }

    boolean isTankGaugeOccluded(Direction direction) {
        return tankGaugeOccluded[direction.get2DDataValue()];
    }

    boolean isLocalTankGaugeInstalled() {
        return tankGaugeInstalled;
    }

    void setLocalTankGaugeInstalled(boolean installed) {
        if (tankGaugeInstalled == installed) {
            return;
        }

        tankGaugeInstalled = installed;
        invalidateRenderBounds();
        if (!installed) {
            return;
        }

        updateTankGaugeOcclusion();
    }

    void releaseContentsOnRemoval() {
        if (level == null || level.isClientSide) {
            return;
        }

        GasTank tank = getTankInventory();
        long storedAmount = tank.getStoredAmount();
        if (storedAmount <= 0) {
            return;
        }

        long sourcePressurePa = tank.getPressurePa();
        GasStack releasedGas = tank.drain(storedAmount, GasAction.EXECUTE);
        if (releasedGas.isEmpty()) {
            return;
        }

        GasReleaseService.release(level, GasReleaseRequest.radial(releasedGas, worldPosition, GasReleaseCause.TANK_REMOVAL, sourcePressurePa));
    }

    void updateTankState() {
        if (level == null || level.isClientSide || !isController()) {
            return;
        }

        driverCore.requestStructureEvaluation();
    }

    private void ruptureIfOverstressed() {
        if (level == null || level.isClientSide || !isController() || overpressureBehaviour.getFailureReadyChannel() < 0) {
            return;
        }

        PressureRuptureService.rupture(level, worldPosition, getTankInventory());
    }

    private void updateTankGaugeOcclusion() {
        if (level == null || !level.isClientSide || !isController() || !tankGaugeInstalled) {
            return;
        }

        Axis axis = getMainConnectionAxis();
        int xSize = axis == Axis.X ? getHeight() : getWidth();
        int zSize = axis == Axis.Z ? getHeight() : getWidth();
        double minX = worldPosition.getX();
        double maxX = minX + xSize;
        double minZ = worldPosition.getZ();
        double maxZ = minZ + zSize;
        double centerX = (minX + maxX) / 2;
        double centerZ = (minZ + maxZ) / 2;
        double minY = worldPosition.getY() + 0.0625;
        double maxY = worldPosition.getY() + 0.9375;
        for (Direction direction : Iterate.horizontalDirections) {
            AABB gaugeBounds = switch (direction) {
                case EAST -> new AABB(maxX + 9.765625E-4, minY, centerZ - 0.4375, maxX + 0.125, maxY, centerZ + 0.4375);
                case WEST -> new AABB(minX - 0.125, minY, centerZ - 0.4375, minX - 9.765625E-4, maxY, centerZ + 0.4375);
                case SOUTH -> new AABB(centerX - 0.4375, minY, maxZ + 9.765625E-4, centerX + 0.4375, maxY, maxZ + 0.125);
                case NORTH -> new AABB(centerX - 0.4375, minY, minZ - 0.125, centerX + 0.4375, maxY, minZ - 9.765625E-4);
                default -> throw new IllegalArgumentException("Airtight tank face must be horizontal; got " + direction + '.');
            };
            tankGaugeOccluded[direction.get2DDataValue()] = !level.noBlockCollision(null, gaugeBounds);
        }
    }

    private long getCurrentGasPressurePa() {
        return getTankInventory().getPressurePa();
    }

    private void applyGasTankSize(int blocks) {
        storageController.resizeToBlocks(blocks);
    }

    private void clearTankGaugeAttachment() {
        if (level == null || level.isClientSide || !isController()) {
            return;
        }

        Axis axis = getMainConnectionAxis();
        for (int lengthOffset = 0; lengthOffset < getHeight(); lengthOffset++) {
            for (int uOffset = 0; uOffset < getWidth(); uOffset++) {
                for (int vOffset = 0; vOffset < getWidth(); vOffset++) {
                    BlockPos partPos = offsetInMulti(worldPosition, axis, lengthOffset, uOffset, vOffset);
                    if (!(level.getBlockEntity(partPos) instanceof AirtightTankBlockEntity part) || !part.tankGaugeInstalled) {
                        continue;
                    }

                    part.setLocalTankGaugeInstalled(false);
                    part.setChanged();
                }
            }
        }
    }

    private float getTankGaugeTargetAngle() {
        float pressureProgress = GasPressureDisplayScale.fractionForPressure(getTankInventory().getPressurePa());
        return Mth.lerp(pressureProgress, TANK_GAUGE_ZERO_PRESSURE_ANGLE, TANK_GAUGE_MAX_PRESSURE_ANGLE);
    }

    private void synchronizeTankGaugeAttachment() {
        if (level == null || level.isClientSide || !isController()) {
            return;
        }

        Axis axis = getMainConnectionAxis();
        boolean controllerHadGauge = tankGaugeInstalled;
        boolean installed = controllerHadGauge;
        for (int lengthOffset = 0; lengthOffset < getHeight() && !installed; lengthOffset++) {
            for (int uOffset = 0; uOffset < getWidth() && !installed; uOffset++) {
                for (int vOffset = 0; vOffset < getWidth(); vOffset++) {
                    BlockPos partPos = offsetInMulti(worldPosition, axis, lengthOffset, uOffset, vOffset);
                    if (!(level.getBlockEntity(partPos) instanceof AirtightTankBlockEntity part) || !part.tankGaugeInstalled) {
                        continue;
                    }

                    installed = true;
                    break;
                }
            }
        }
        if (!installed) {
            return;
        }

        for (int lengthOffset = 0; lengthOffset < getHeight(); lengthOffset++) {
            for (int uOffset = 0; uOffset < getWidth(); uOffset++) {
                for (int vOffset = 0; vOffset < getWidth(); vOffset++) {
                    BlockPos partPos = offsetInMulti(worldPosition, axis, lengthOffset, uOffset, vOffset);
                    if (!(level.getBlockEntity(partPos) instanceof AirtightTankBlockEntity part) || part.tankGaugeInstalled) {
                        continue;
                    }

                    part.setLocalTankGaugeInstalled(true);
                    part.setChanged();
                }
            }
        }

        if (controllerHadGauge) {
            return;
        }

        notifyUpdate();
    }
}
