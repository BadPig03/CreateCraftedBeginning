package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.amethystlens.AmethystLensBlock;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceGeometry.PreparedBlock;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class OpticalContraptionQuery {
    private OpticalContraptionQuery() {
    }

    @SuppressWarnings("ConstantValue")
    static @Nullable OpticalHit findHit(Level level, Vec3 origin, Vec3 start, Vec3 end, LaserCollisionContext context) {
        float partialTick = context.getPartialTick();
        LaserTraceGeometry geometry = context.getGeometry();
        OpticalHit nearest = null;
        double nearestSignedDistanceSqr = start.distanceToSqr(end);
        for (LaserQuerySpace space : geometry.mounted(level, partialTick)) {
            Vec3 localStart = space.localPosition(start);
            Vec3 localEnd = space.localPosition(end);
            AABB bounds = space.bounds().inflate(LaserBlockClip.NEIGHBOR_REACH);
            if (!bounds.contains(localStart) && bounds.clip(localStart, localEnd).isEmpty()) {
                continue;
            }

            BlockGetter world = space.owner();
            Map<BlockPos, StructureBlockInfo> blocks = space.blocks();
            if (blocks == null) {
                throw new IllegalStateException("Missing mounted optical blocks.");
            }

            Vec3 localError = space.localError(start, end);
            Vec3 localDelta = localEnd.subtract(localStart);
            Vec3 localDirection = localDelta.normalize();
            Function<BlockPos, @Nullable BlockHitResult> query = context.cacheQuery(world, localStart, localEnd, pos -> {
                PreparedBlock block = geometry.getMountedBlock(world, blocks, pos, context);
                if (block == null) {
                    return null;
                }

                if (block.precise()) {
                    return block.clip(localStart, localEnd, pos);
                }

                return LaserOcclusionShape.clip(block.boxes(), localStart, localEnd, pos, localError);
            });
            BlockHitResult blockHit = BlockGetter.traverseBlocks(localStart, localEnd, world, (view, cell) -> {
                int x = cell.getX();
                int y = cell.getY();
                int z = cell.getZ();
                if (x + 1 < bounds.minX || x > bounds.maxX || y + 1 < bounds.minY || y > bounds.maxY || z + 1 < bounds.minZ || z > bounds.maxZ) {
                    return null;
                }

                return LaserBlockClip.clipCell(localStart, localEnd, cell, geometry.getMountedCandidates(world, blocks, cell, context), query);
            }, view -> null);
            if (blockHit == null) {
                continue;
            }

            Vec3 worldPoint = start;
            double lengthSqr = localDelta.lengthSqr();
            if (lengthSqr != 0) {
                double hitFraction = blockHit.getLocation().subtract(localStart).dot(localDelta) / lengthSqr;
                worldPoint = start.lerp(end, hitFraction);
            }

            BlockPos pos = blockHit.getBlockPos();
            PreparedBlock block = geometry.getMountedBlock(world, blocks, pos, context);
            if (block == null) {
                throw new IllegalStateException("Missing mounted optical target at " + pos + '.');
            }

            OpticalGeometry optical = block.optical();
            OpticalSurface surface = OpticalSurface.opaque(blockHit);
            boolean correctedStart = false;
            if (optical != null) {
                surface = optical.getSurface(blockHit, localDirection);
                if (!origin.equals(start) && blockHit.isInside() && block.state().getBlock() instanceof AmethystLensBlock && surface.aperture() == null) {
                    geometry.includeBounds(new AABB(origin, end));
                    Vec3 localOrigin = space.localPosition(origin);
                    BlockHitResult contact = block.shape().clip(localOrigin, localEnd, pos);
                    if (contact != null && !contact.isInside()) {
                        OpticalSurface contactSurface = optical.getSurface(contact, localDirection);
                        Vec3 originalDelta = localEnd.subtract(localOrigin);
                        double fraction = contact.getLocation().subtract(localOrigin).dot(originalDelta) / originalDelta.lengthSqr();
                        Vec3 contactPoint = origin.lerp(end, fraction);
                        if (contactSurface.aperture() == null && fraction > 0 && origin.distanceToSqr(contactPoint) <= origin.distanceToSqr(worldPoint)) {
                            blockHit = contact;
                            surface = contactSurface;
                            worldPoint = contactPoint;
                            correctedStart = true;
                        }
                    }
                }
            }
            double signedDistanceSqr = start.distanceToSqr(worldPoint);
            if (correctedStart) {
                signedDistanceSqr = -signedDistanceSqr;
            }
            if (signedDistanceSqr >= nearestSignedDistanceSqr) {
                continue;
            }

            Vec3 localPlaneNormal = null;
            if (surface.aperture() != null) {
                localPlaneNormal = surface.normal();
            }
            nearestSignedDistanceSqr = signedDistanceSqr;
            OpticalTarget target = new OpticalTarget(level.dimension(), pos, space.id(), false);
            nearest = new OpticalHit(target, blockHit, worldPoint, surface.transform(space), localPlaneNormal);
        }
        return nearest;
    }
}
