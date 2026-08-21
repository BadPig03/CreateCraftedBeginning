package net.ty.createcraftedbeginning.content.photostresses.phohostressbearing;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity.RotationDirection;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.photostresses.network.PhotoStressNetwork;
import net.ty.createcraftedbeginning.content.photostresses.network.PhotoStressNetworkManager;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhotoStressBearingBlockEntity extends GeneratingKineticBlockEntity {
    private static final float GENERATED_SPEED = 32;

    protected final DustColorTransitionOptions particleColor = new DustColorTransitionOptions(Vec3.fromRGB24(16761855).toVector3f(), Vec3.fromRGB24(10185983).toVector3f(), 1);
    protected ScrollOptionBehaviour<RotationDirection> movementDirection;
    protected int receivedStressPoints;
    protected int lightTimer;

    public PhotoStressBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level == null || level.isClientSide) {
            return;
        }

        PhotoStressNetworkManager.registerBearing(level, worldPosition);
        updateGeneratedRotation();
    }

    @Override
    public float calculateAddedStressCapacity() {
        capacity = calculateCapacity();
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (clientPacket) {
            compoundTag.putInt("PhotoStressPoints", receivedStressPoints);
        }
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (clientPacket && compoundTag.contains("PhotoStressPoints")) {
            receivedStressPoints = Mth.clamp(compoundTag.getInt("PhotoStressPoints"), 0, PhotoStressNetwork.MAX_BEARING_STRESS_POINTS);
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (receivedStressPoints <= 0) {
            return 0;
        }

        RotationDirection rotationDirection = movementDirection.get();
        return rotationDirection == RotationDirection.CLOCKWISE ? GENERATED_SPEED : -GENERATED_SPEED;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        movementDirection = new ScrollOptionBehaviour<>(RotationDirection.class, CreateLang.translateDirect("contraptions.windmill.rotation_direction"), this, new PhotoStressBearingValueBox());
        movementDirection.withCallback(this::onDirectionChanged);
        behaviours.add(movementDirection);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || !level.isClientSide || receivedStressPoints <= 0) {
            return;
        }

        int particleInterval = Math.max(1, PhotoStressNetwork.MAX_BEARING_STRESS_POINTS - receivedStressPoints + 1);
        if (++lightTimer >= particleInterval) {
            spawnParticle();
            lightTimer = 0;
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        PhotoStressNetworkManager.ensureBearing(level, worldPosition);
    }

    public int getReceivedStressPoints() {
        return receivedStressPoints;
    }

    protected void onDirectionChanged(int ignored) {
        if (level == null || level.isClientSide) {
            return;
        }

        updateGeneratedRotation();
    }

    public void applyNetworkAllocation(int stressPoints) {
        if (level == null || level.isClientSide) {
            return;
        }

        int clampedStressPoints = Mth.clamp(stressPoints, 0, PhotoStressNetwork.MAX_BEARING_STRESS_POINTS);
        if (clampedStressPoints == receivedStressPoints) {
            return;
        }

        receivedStressPoints = clampedStressPoints;
        updateGeneratedRotation();
    }

    protected float calculateCapacity() {
        float networkFactor = receivedStressPoints / (float) PhotoStressNetwork.MAX_BEARING_STRESS_POINTS;
        return (float) (BlockStressValues.getCapacity(getStressConfigKey()) * networkFactor);
    }

    protected void spawnParticle() {
        if (level == null || !level.isClientSide || speed == 0) {
            return;
        }

        Vec3 centerOf = VecHelper.getCenterOf(worldPosition);
        Vec3 offset = VecHelper.offsetRandomly(centerOf, level.random, 0.85f);
        Vec3 subtracted = centerOf.subtract(offset);
        level.addParticle(particleColor, offset.x, offset.y, offset.z, subtracted.x, subtracted.y, subtracted.z);
    }
}
