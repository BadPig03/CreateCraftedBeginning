package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.Projection;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorControlPose {
    private final Vec3 center;
    private final Matrix3d transform;
    private @Nullable Matrix3d inverse;

    AmethystMirrorControlPose(Vec3 center, Matrix3d transform) {
        this.center = center;
        this.transform = new Matrix3d(transform);
    }

    private AmethystMirrorControlPose(Vec3 center) {
        this.center = center;
        transform = new Matrix3d();
    }

    static AmethystMirrorControlPose create(Level level, BlockPos pos, Quaternionf rotation, float partialTick) {
        Vec3 localCenter = Vec3.atCenterOf(pos);
        CoordinateTransform structure = SubLevelBridge.createRenderTransform(level, localCenter, partialTick);
        if (!structure.inSubLevel()) {
            AmethystMirrorControlPose pose = new AmethystMirrorControlPose(localCenter);
            pose.transform.mul(new Matrix3d().rotation(rotation));
            return pose;
        }

        Vec3 center = structure.transformPosition(localCenter);
        Vec3 x = structure.transformPosition(localCenter.add(1, 0, 0)).subtract(center);
        Vec3 y = structure.transformPosition(localCenter.add(0, 1, 0)).subtract(center);
        Vec3 z = structure.transformPosition(localCenter.add(0, 0, 1)).subtract(center);
        AmethystMirrorControlPose pose = new AmethystMirrorControlPose(center);
        pose.transform.set(x.x, x.y, x.z, y.x, y.y, y.z, z.x, z.y, z.z).mul(new Matrix3d().rotation(rotation));
        return pose;
    }

    static boolean canInteract(Player player, BlockPos pos) {
        Level level = player.level();
        Projection projection = SubLevelBridge.resolve(level, pos);
        if (!projection.inSubLevel()) {
            return player.canInteractWithBlock(pos, 1);
        }

        Vec3 localCenter = Vec3.atCenterOf(pos);
        Vec3 center = projection.worldPosition();
        Vec3 x = SubLevelBridge.projectRay(level, localCenter, localCenter.add(1, 0, 0)).worldEnd().subtract(center);
        Vec3 y = SubLevelBridge.projectRay(level, localCenter, localCenter.add(0, 1, 0)).worldEnd().subtract(center);
        Vec3 z = SubLevelBridge.projectRay(level, localCenter, localCenter.add(0, 0, 1)).worldEnd().subtract(center);
        AmethystMirrorControlPose pose = new AmethystMirrorControlPose(center, new Matrix3d(x.x, x.y, x.z, y.x, y.y, y.z, z.x, z.y, z.z));
        Vec3 eye = player.getEyePosition();
        Vec3 local = pose.toLocal(eye);
        Vec3 nearest = pose.toWorld(new Vec3(Mth.clamp(local.x, -0.5, 0.5), Mth.clamp(local.y, -0.5, 0.5), Mth.clamp(local.z, -0.5, 0.5)));
        double reach = player.blockInteractionRange() + 1;
        return eye.distanceToSqr(nearest) < reach * reach;
    }

    Vec3 center() {
        return center;
    }

    Matrix3f matrix() {
        return new Matrix3f((float) transform.m00(), (float) transform.m01(), (float) transform.m02(), (float) transform.m10(), (float) transform.m11(), (float) transform.m12(), (float) transform.m20(), (float) transform.m21(), (float) transform.m22());
    }

    Vec3 toLocal(Vec3 position) {
        Matrix3d inverse = this.inverse;
        if (inverse == null) {
            inverse = new Matrix3d(transform).invert();
            this.inverse = inverse;
        }
        Vec3 offset = position.subtract(center);
        Vector3d local = inverse.transform(new Vector3d(offset.x, offset.y, offset.z));
        return new Vec3(local.x, local.y, local.z);
    }

    Vec3 toWorld(Vec3 position) {
        Vector3d offset = transform.transform(new Vector3d(position.x, position.y, position.z));
        return center.add(offset.x, offset.y, offset.z);
    }

    AABB bounds(AABB local) {
        Vec3 midpoint = toWorld(local.getCenter());
        double x = local.getXsize() / 2;
        double y = local.getYsize() / 2;
        double z = local.getZsize() / 2;
        double width = Math.abs(transform.m00()) * x + Math.abs(transform.m10()) * y + Math.abs(transform.m20()) * z;
        double height = Math.abs(transform.m01()) * x + Math.abs(transform.m11()) * y + Math.abs(transform.m21()) * z;
        double depth = Math.abs(transform.m02()) * x + Math.abs(transform.m12()) * y + Math.abs(transform.m22()) * z;
        return new AABB(midpoint.x - width, midpoint.y - height, midpoint.z - depth, midpoint.x + width, midpoint.y + height, midpoint.z + depth);
    }

    void apply(Matrix4f destination) {
        destination.identity().m00((float) transform.m00()).m01((float) transform.m01()).m02((float) transform.m02()).m10((float) transform.m10()).m11((float) transform.m11()).m12((float) transform.m12()).m20((float) transform.m20()).m21((float) transform.m21()).m22((float) transform.m22());
    }
}
