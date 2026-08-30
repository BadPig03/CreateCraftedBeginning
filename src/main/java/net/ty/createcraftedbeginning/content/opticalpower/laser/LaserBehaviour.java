package net.ty.createcraftedbeginning.content.opticalpower.laser;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RayProjection;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBehaviour extends BlockEntityBehaviour {
    public static final int MAX_RANGE = 32;
    public static final BehaviourType<LaserBehaviour> TYPE = new BehaviourType<>();

    private final Supplier<Direction> direction;
    private final BooleanSupplier active;

    private @Nullable BlockHitResult hitResult;
    private float beamLength;

    public LaserBehaviour(SmartBlockEntity blockEntity, Supplier<Direction> direction, BooleanSupplier active) {
        super(blockEntity);
        this.direction = direction;
        this.active = active;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void tick() {
        Level level = blockEntity.getLevel();
        if (level == null || blockEntity.isVirtual()) {
            clearTrace();
            return;
        }

        if (!active.getAsBoolean()) {
            clearTrace();
            return;
        }

        Direction laserDirection = direction.get();
        Vec3 localStart = getStart(laserDirection);
        RayProjection projectedRay = SubLevelBridge.projectRay(level, localStart, localStart.add(Vec3.atLowerCornerOf(laserDirection.getNormal()).scale(MAX_RANGE)));
        Vec3 worldStart = projectedRay.worldStart();
        Vec3 projectedDirection = projectedRay.worldEnd().subtract(worldStart);
        if (projectedDirection.lengthSqr() < 1E-12) {
            clearTrace();
            return;
        }

        Vec3 worldDirection = projectedDirection.normalize();
        double loadedRange = getLoadedRange(worldStart, worldDirection);
        if (loadedRange <= 0) {
            clearTrace();
            return;
        }

        Vec3 traceStart = worldStart.add(worldDirection.scale(1E-4));
        ClipContext clipContext = new ClipContext(traceStart, traceStart.add(worldDirection.scale(Math.max(0, loadedRange - 2E-4))), Block.COLLIDER, Fluid.NONE, CollisionContext.empty());
        BlockHitResult result = level.clip(clipContext);
        hitResult = result.getType() == Type.MISS ? null : result;
        if (hitResult == null) {
            beamLength = (float) loadedRange;
            return;
        }

        Vec3 worldHit = SubLevelBridge.resolve(level, hitResult.getLocation()).worldPosition();
        beamLength = (float) Math.min(loadedRange, worldStart.distanceTo(worldHit));
    }

    public @Nullable BlockHitResult getHitResult() {
        return hitResult;
    }

    public float getBeamLength() {
        return beamLength;
    }

    public Vec3 getStart() {
        return getStart(direction.get());
    }

    private Vec3 getStart(Direction laserDirection) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).add(Vec3.atLowerCornerOf(laserDirection.getNormal()).scale(0.5));
    }

    private double getLoadedRange(Vec3 worldStart, Vec3 worldDirection) {
        BlockPos startPos = BlockPos.containing(worldStart);
        Level level = blockEntity.getLevel();
        int previousChunkX = startPos.getX() >> 4;
        int previousChunkZ = startPos.getZ() >> 4;
        for (int distance = 1; distance <= MAX_RANGE; distance++) {
            BlockPos candidate = BlockPos.containing(worldStart.add(worldDirection.scale(distance)));
            int chunkX = candidate.getX() >> 4;
            int chunkZ = candidate.getZ() >> 4;
            if (chunkX == previousChunkX && chunkZ == previousChunkZ) {
                continue;
            }

            if (level == null || level.isLoaded(candidate)) {
                previousChunkX = chunkX;
                previousChunkZ = chunkZ;
                continue;
            }

            return distance - 1;
        }
        return MAX_RANGE;
    }

    private void clearTrace() {
        hitResult = null;
        beamLength = 0;
    }
}
