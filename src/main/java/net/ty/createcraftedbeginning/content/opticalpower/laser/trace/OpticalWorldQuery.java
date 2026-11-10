package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.amethystlens.AmethystLensBlock;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceGeometry.PreparedBlock;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class OpticalWorldQuery {
    static final double TRACE_OFFSET = 1.0E-4;
    private static final int CHUNK_SIZE = 16;

    private OpticalWorldQuery() {
    }

    static @Nullable OpticalQueryResult findNext(Level level, BeamFrame frame, double range, LaserCollisionContext collision, boolean rendering, boolean skipStart, boolean trimEnd, boolean closeEnd) {
        float partialTick = collision.getPartialTick();
        if (!Double.isFinite(range) || range <= 0) {
            throw new IllegalArgumentException("Optical query range must be finite and positive; got " + range + " blocks.");
        }

        if (!Float.isFinite(partialTick) || partialTick < 0 || partialTick > 1) {
            throw new IllegalArgumentException("Optical query partial tick must be finite and in [0, 1]; got " + partialTick + '.');
        }

        Vec3 start = frame.origin();
        Vec3 direction = frame.direction();
        double startOffset = Math.min(TRACE_OFFSET, range * 0.25);
        if (!skipStart) {
            startOffset = 0;
        }
        Vec3 traceStart = start.add(direction.scale(startOffset));
        LaserTraceGeometry traceGeometry = collision.getGeometry();
        if (!traceGeometry.isLoaded(level, BlockPos.containing(traceStart))) {
            return null;
        }

        double loadedRange = getLoadedRange(level, start, traceStart, direction, range, collision);
        if (loadedRange <= 0) {
            return null;
        }

        double endOffset = Math.min(TRACE_OFFSET, loadedRange * 0.25);
        if (!trimEnd) {
            endOffset = 0;
        }
        Vec3 traceEnd = start.add(direction.scale(loadedRange - endOffset));
        boolean closed = closeEnd && loadedRange == range;
        if (closed) {
            double extension = Math.min(TRACE_OFFSET, loadedRange * 0.25);
            traceEnd = start.add(direction.scale(loadedRange + extension));
        }
        traceGeometry.includeBounds(new AABB(traceStart, traceEnd));
        ClipContext context = new ClipContext(traceStart, traceEnd, Block.COLLIDER, Fluid.NONE, collision);
        BlockHitResult blockHit;
        if (rendering) {
            blockHit = SubLevelBridge.clipAtRenderPose(level, context, partialTick);
        }
        else {
            blockHit = level.clip(context);
        }

        OpticalHit hit = null;
        Vec3 rangeEnd = start.add(direction.scale(loadedRange));
        Vec3 end = rangeEnd;
        Vec3 contraptionEnd = end;
        if (closed) {
            contraptionEnd = traceEnd;
        }
        double length = loadedRange;
        if (blockHit.getType() != Type.MISS) {
            BlockPos pos = blockHit.getBlockPos();
            Vec3 center = Vec3.atCenterOf(pos);
            CoordinateTransform transform;
            if (rendering) {
                transform = SubLevelBridge.createRenderTransform(level, center, partialTick);
            }
            else {
                transform = SubLevelBridge.createLogicalTransform(level, center);
            }

            end = transform.transformPosition(blockHit.getLocation());
            contraptionEnd = end;
            length = Math.min(loadedRange, start.distanceTo(end));
            OpticalSurface surface = OpticalSurface.opaque(blockHit);
            PreparedBlock block = traceGeometry.getWorldBlock(level, pos, context, partialTick);
            if (block != null) {
                OpticalGeometry geometry = block.optical();
                if (geometry != null) {
                    Vec3 xAxis = transform.transformNormal(new Vec3(1, 0, 0));
                    Vec3 yAxis = transform.transformNormal(new Vec3(0, 1, 0));
                    Vec3 zAxis = transform.transformNormal(new Vec3(0, 0, 1));
                    Vec3 localDirection = new Vec3(direction.dot(xAxis), direction.dot(yAxis), direction.dot(zAxis)).normalize();
                    surface = geometry.getSurface(blockHit, localDirection);
                    if (startOffset > 0 && blockHit.isInside() && block.state().getBlock() instanceof AmethystLensBlock && surface.aperture() == null && traceGeometry.isLoaded(level, BlockPos.containing(start))) {
                        traceGeometry.includeBounds(new AABB(start, traceEnd));
                        ClipContext original = new ClipContext(start, traceEnd, Block.COLLIDER, Fluid.NONE, collision);
                        BlockHitResult contact;
                        if (rendering) {
                            contact = SubLevelBridge.clipAtRenderPose(level, original, partialTick);
                        }
                        else {
                            contact = level.clip(original);
                        }
                        if (contact.getType() != Type.MISS && !contact.isInside() && contact.getBlockPos().equals(pos)) {
                            OpticalSurface contactSurface = geometry.getSurface(contact, localDirection);
                            Vec3 contactEnd = transform.transformPosition(contact.getLocation());
                            double contactLength = start.distanceTo(contactEnd);
                            if (contactSurface.aperture() == null && contactEnd.subtract(start).dot(direction) > 0 && contactLength <= length) {
                                blockHit = contact;
                                surface = contactSurface;
                                end = contactEnd;
                                length = contactLength;
                            }
                        }
                    }
                }
            }

            Vec3 localPlaneNormal = null;
            if (surface.aperture() != null) {
                localPlaneNormal = surface.normal();
            }
            OpticalTarget target = new OpticalTarget(level.dimension(), pos, null, transform.inSubLevel());
            hit = new OpticalHit(target, blockHit, end, surface.transform(transform), localPlaneNormal);
            if (closed && start.distanceTo(end) > loadedRange) {
                if (atClosedEnd(start, rangeEnd, end)) {
                    hit = new OpticalHit(target, blockHit, rangeEnd, hit.surface(), localPlaneNormal);
                    contraptionEnd = rangeEnd;
                }
                else {
                    hit = null;
                    length = loadedRange;
                    contraptionEnd = traceEnd;
                }
            }
        }

        OpticalHit contraptionHit = OpticalContraptionQuery.findHit(level, start, traceStart, contraptionEnd, collision);
        if (contraptionHit != null) {
            double contraptionLength = start.distanceTo(contraptionHit.position());
            if (closed && contraptionLength > loadedRange && atClosedEnd(start, rangeEnd, contraptionHit.position())) {
                contraptionHit = new OpticalHit(contraptionHit.target(), contraptionHit.localHit(), rangeEnd, contraptionHit.surface(), contraptionHit.localPlaneNormal());
                contraptionLength = loadedRange;
            }
            if (contraptionLength < length || closed && hit == null && contraptionLength == length) {
                hit = contraptionHit;
                length = contraptionLength;
            }
        }
        return new OpticalQueryResult(length, hit);
    }

    private static boolean atClosedEnd(Vec3 start, Vec3 end, Vec3 point) {
        double errorX = Math.ulp(start.x) + Math.ulp(end.x) + Math.ulp(point.x);
        double errorY = Math.ulp(start.y) + Math.ulp(end.y) + Math.ulp(point.y);
        double errorZ = Math.ulp(start.z) + Math.ulp(end.z) + Math.ulp(point.z);
        return Math.abs(point.x - end.x) <= errorX && Math.abs(point.y - end.y) <= errorY && Math.abs(point.z - end.z) <= errorZ;
    }

    @SuppressWarnings("ConstantValue")
    private static double getLoadedRange(Level level, Vec3 worldStart, Vec3 traceStart, Vec3 worldDirection, double range, LaserCollisionContext collision) {
        BlockPos startPos = BlockPos.containing(traceStart);
        Vec3 worldEnd = worldStart.add(worldDirection.scale(range - Math.min(TRACE_OFFSET, range * 0.25)));
        Vec3 chunkStart = new Vec3(traceStart.x / CHUNK_SIZE, 0, traceStart.z / CHUNK_SIZE);
        Vec3 chunkEnd = new Vec3(worldEnd.x / CHUNK_SIZE, 0, worldEnd.z / CHUNK_SIZE);
        BlockPos unloadedChunk = BlockGetter.traverseBlocks(chunkStart, chunkEnd, level, (world, chunkPos) -> {
            BlockPos candidate = new BlockPos(chunkPos.getX() * CHUNK_SIZE, startPos.getY(), chunkPos.getZ() * CHUNK_SIZE);
            if (collision.getGeometry().isLoaded(world, candidate)) {
                return null;
            }

            return chunkPos.immutable();
        }, world -> null);
        if (unloadedChunk == null) {
            return range;
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

}
