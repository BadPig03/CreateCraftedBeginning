package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplayPacket.Mode;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterSerialization.ClientAllocation;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerConsumerBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkManager;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LaserEmitterBlockEntity extends SmartBlockEntity implements OpticalPowerConsumerBlockEntity, IHaveGoggleInformation {
    private static final int MAX_PONDER_FLUX_LM = 48;

    private final LaserEmitterDisplayState displayState = new LaserEmitterDisplayState();
    private LaserBehaviour laserBehaviour;
    private LaserEmitterRangeBehaviour rangeBehaviour;
    private final LaserEmitterController controller;
    private double ponderFluxLm;

    public LaserEmitterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        controller = new LaserEmitterController(this);
    }

    @Override
    @SuppressWarnings("Convert2MethodRef")
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new CCBAdvancementBehaviour(this, CCBAdvancements.LIGHT_ON_THE_MOTION_OF_PHOTONS, CCBAdvancements.RECURSIVE_GLOW));
        rangeBehaviour = new LaserEmitterRangeBehaviour(this);
        behaviours.add(rangeBehaviour);

        laserBehaviour = new LaserBehaviour(this, () -> Vec3.atCenterOf(worldPosition).add(Vec3.atLowerCornerOf(getLaserDirection().getNormal()).scale(0.5)), () -> Vec3.atLowerCornerOf(getLaserDirection().getNormal()), () -> controller.getAllocatedFluxLm(), this::getLaserRange);
        behaviours.add(laserBehaviour);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level == null || level.isClientSide) {
            return;
        }

        OpticalPowerNetworkManager.registerConsumer(level, worldPosition);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        OpticalPowerNetworkManager.requestConsumerRebuildIfUnassigned(level, worldPosition);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        LaserEmitterSerialization.write(compoundTag, clientPacket, controller, displayState);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (!clientPacket) {
            super.read(compoundTag, provider, false);
            return;
        }

        ClientAllocation allocation = LaserEmitterSerialization.readClient(compoundTag, worldPosition, displayState);
        if (allocation == null) {
            return;
        }

        super.read(compoundTag, provider, true);
        controller.restoreAllocation(allocation.fluxLm());
        displayState.acceptAllocation(allocation.instance());
    }

    @Override
    public void applyNetworkAllocation(double fluxLm, BooleanSupplier allocationValidity) {
        controller.applyAllocation(fluxLm, allocationValidity);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        LaserEmitterDisplay.appendGoggleTooltip(tooltip, controller.getAllocatedFluxLm());
        return true;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(getLaserRange() + 0.0625);
    }

    @Internal
    public void applyOpticalFluxAllocation(double fluxLm) {
        controller.applyAllocation(fluxLm, null);
    }

    @Internal
    public boolean isLaserActive() {
        return getBeamFluxLm() > 0;
    }

    public void setPonderFlux(double fluxLm) {
        if (!isVirtual()) {
            return;
        }

        if (!Double.isFinite(fluxLm)) {
            throw new IllegalArgumentException("Ponder luminous flux must be finite; got " + fluxLm + " lm.");
        }

        ponderFluxLm = Math.clamp(fluxLm, 0, MAX_PONDER_FLUX_LM);
    }

    @Internal
    public @Nullable BeamPropagation getBeamPropagation() {
        if (laserBehaviour == null) {
            return null;
        }

        return laserBehaviour.getPropagation();
    }

    @Internal
    public BeamState createBeamSource() {
        Vec3 direction = Vec3.atLowerCornerOf(getLaserDirection().getNormal());
        Vec3 origin = Vec3.atCenterOf(worldPosition).add(direction.scale(0.5));
        return LaserBehaviour.createSource(origin, direction, getBeamFluxLm());
    }

    @Internal
    public int getLaserRange() {
        return rangeBehaviour.getValue();
    }

    @Internal
    public @Nullable LaserDisplaySnapshot getDisplaySnapshot() {
        if (displayState.isSuspended()) {
            return null;
        }

        return displayState.snapshot();
    }

    @Internal
    public UUID getDisplayInstance() {
        return displayState.instance();
    }

    @Internal
    public boolean isDisplayOff() {
        LaserDisplayPacket packet = getDisplayPacket();
        return packet != null && packet.mode() == Mode.OFF;
    }

    @Internal
    public boolean isDisplaySuspended() {
        return displayState.isSuspended();
    }

    @Internal
    public boolean takeDisplayBaselineRetry(long tick) {
        return displayState.takeBaselineRetry(tick);
    }

    @Nullable LaserDisplayPacket getDisplayPacket() {
        return displayState.packet();
    }

    void acceptDisplay(LaserDisplayPacket packet) {
        if (level == null || !level.isClientSide || isRemoved() || !packet.pos().equals(worldPosition) || !packet.dimension().equals(level.dimension().location())) {
            return;
        }

        displayState.accept(packet);
    }

    private Direction getLaserDirection() {
        return getBlockState().getValue(DirectionalBlock.FACING);
    }

    private double getBeamFluxLm() {
        if (isVirtual()) {
            return ponderFluxLm;
        }

        return controller.getAllocatedFluxLm();
    }
}
