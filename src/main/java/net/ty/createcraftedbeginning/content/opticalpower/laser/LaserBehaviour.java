package net.ty.createcraftedbeginning.content.opticalpower.laser;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RayProjection;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBehaviour extends BlockEntityBehaviour {
    public static final int MIN_RANGE = 1;
    public static final int MAX_RANGE = 32;
    public static final BehaviourType<LaserBehaviour> TYPE = new BehaviourType<>();
    private static final int CHUNK_SIZE = 16;
    private static final double MIN_PROJECTED_RAY_LENGTH_SQR = 1.0E-12;
    private static final double TRACE_START_OFFSET = 1.0E-4;
    private static final double TRACE_END_OFFSET = 2.0E-4;
    private final Supplier<Direction> direction;
    private final BooleanSupplier active;
    private final IntSupplier range;

    private @Nullable BlockHitResult hitResult;
    private float beamLength;

    public LaserBehaviour(SmartBlockEntity blockEntity, Supplier<Direction> direction, BooleanSupplier active, IntSupplier range) {
        super(blockEntity);
        this.direction = direction;
        this.active = active;
        this.range = range;
    }

    @SuppressWarnings("ConstantValue")
    private static double getLoadedRange(Level level, Vec3 worldStart, Vec3 traceStart, Vec3 worldDirection, int laserRange) {
        BlockPos startPos = BlockPos.containing(traceStart);
        Vec3 worldEnd = worldStart.add(worldDirection.scale(laserRange - TRACE_START_OFFSET));
        Vec3 chunkStart = new Vec3(traceStart.x / CHUNK_SIZE, 0, traceStart.z / CHUNK_SIZE);
        Vec3 chunkEnd = new Vec3(worldEnd.x / CHUNK_SIZE, 0, worldEnd.z / CHUNK_SIZE);
        BlockPos unloadedChunk = BlockGetter.traverseBlocks(chunkStart, chunkEnd, level, (world, chunkPos) -> {
            BlockPos candidate = new BlockPos(chunkPos.getX() * CHUNK_SIZE, startPos.getY(), chunkPos.getZ() * CHUNK_SIZE);
            if (world.isLoaded(candidate)) {
                return null;
            }

            return chunkPos.immutable();
        }, world -> null);
        if (unloadedChunk == null) {
            return laserRange;
        }

        double entryX = 0;
        if (worldDirection.x > 0) {
            entryX = (unloadedChunk.getX() * CHUNK_SIZE - worldStart.x) / worldDirection.x;
        }
        else if (worldDirection.x < 0) {
            entryX = ((unloadedChunk.getX() + 1) * CHUNK_SIZE - worldStart.x) / worldDirection.x;
        }

        double entryZ = 0;
        if (worldDirection.z > 0) {
            entryZ = (unloadedChunk.getZ() * CHUNK_SIZE - worldStart.z) / worldDirection.z;
        }
        else if (worldDirection.z < 0) {
            entryZ = ((unloadedChunk.getZ() + 1) * CHUNK_SIZE - worldStart.z) / worldDirection.z;
        }

        return Math.max(0, Math.max(entryX, entryZ));
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
        int laserRange = Mth.clamp(range.getAsInt(), MIN_RANGE, MAX_RANGE);
        Vec3 localStart = getStart(laserDirection);
        RayProjection projectedRay = SubLevelBridge.projectRay(level, localStart, localStart.add(Vec3.atLowerCornerOf(laserDirection.getNormal()).scale(laserRange)));
        Vec3 worldStart = projectedRay.worldStart();
        Vec3 projectedDirection = projectedRay.worldEnd().subtract(worldStart);
        if (projectedDirection.lengthSqr() < MIN_PROJECTED_RAY_LENGTH_SQR) {
            clearTrace();
            return;
        }

        Vec3 worldDirection = projectedDirection.normalize();
        Vec3 traceStart = worldStart.add(worldDirection.scale(TRACE_START_OFFSET));
        if (!level.isLoaded(BlockPos.containing(traceStart))) {
            clearTrace();
            return;
        }

        double loadedRange = getLoadedRange(level, worldStart, traceStart, worldDirection, laserRange);
        if (loadedRange <= 0) {
            clearTrace();
            return;
        }

        ClipContext clipContext = new LaserClipContext(traceStart, traceStart.add(worldDirection.scale(Math.max(0, loadedRange - TRACE_END_OFFSET))));
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

    private void clearTrace() {
        hitResult = null;
        beamLength = 0;
    }
}
