package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorSerialization {
    private static final String COMPOUND_KEY_MIRROR_ROTATION = "MirrorRotation";
    private static final String COMPOUND_KEY_MIRROR_FACING = "MirrorFacing";
    private static final double UNIT_ROTATION_TOLERANCE = 1.0E-4;

    private AmethystMirrorSerialization() {
    }

    public static void transformMirrorNbt(CompoundTag compoundTag, UnaryOperator<BlockPos> transform) {
        Direction facing = Direction.byName(compoundTag.getString(COMPOUND_KEY_MIRROR_FACING));
        if (facing == null) {
            return;
        }

        BlockPos origin = transform.apply(BlockPos.ZERO);
        Vec3 xAxis = Vec3.atLowerCornerOf(transform.apply(BlockPos.ZERO.east()).subtract(origin));
        Vec3 yAxis = Vec3.atLowerCornerOf(transform.apply(BlockPos.ZERO.above()).subtract(origin));
        Vec3 zAxis = Vec3.atLowerCornerOf(transform.apply(BlockPos.ZERO.south()).subtract(origin));
        Matrix3f basis = new Matrix3f().setColumn(0, xAxis.toVector3f()).setColumn(1, yAxis.toVector3f()).setColumn(2, zAxis.toVector3f());
        Quaternionf transformed = new Quaternionf().setFromNormalized(basis).mul(readRotation(facing, compoundTag)).normalize();
        BlockPos mounting = transform.apply(BlockPos.ZERO.relative(facing)).subtract(origin);
        Direction transformedFacing = Direction.getNearest(mounting.getX(), mounting.getY(), mounting.getZ());
        writeRotation(transformedFacing, transformed, compoundTag);
    }

    @Internal
    public static void writeRotation(Direction facing, float angle, float tilt, CompoundTag compoundTag) {
        writeRotation(facing, AmethystMirrorGeometry.createRotation(facing, angle, tilt), compoundTag);
    }

    @Internal
    public static void writeRotation(Direction facing, Quaternionf rotation, CompoundTag compoundTag) {
        compoundTag.putIntArray(COMPOUND_KEY_MIRROR_ROTATION, new int[]{Float.floatToRawIntBits(rotation.x), Float.floatToRawIntBits(rotation.y), Float.floatToRawIntBits(rotation.z), Float.floatToRawIntBits(rotation.w)});
        compoundTag.putString(COMPOUND_KEY_MIRROR_FACING, facing.getSerializedName());
    }

    static Quaternionf transformRotation(Direction facing, Quaternionf rotation, StructureTransform transform) {
        CompoundTag compoundTag = new CompoundTag();
        Vec3 originalMounting = transform.unapplyWithoutOffset(Vec3.atLowerCornerOf(facing.getNormal()).add(0.5, 0.5, 0.5)).subtract(0.5, 0.5, 0.5);
        Direction originalFacing = Direction.getNearest(originalMounting.x, originalMounting.y, originalMounting.z);
        writeRotation(originalFacing, rotation, compoundTag);
        transformMirrorNbt(compoundTag, transform::applyWithoutOffset);
        return readRotation(facing, compoundTag);
    }

    static Quaternionf readStoredRotation(Direction fallback, CompoundTag compoundTag) {
        Direction facing = Direction.byName(compoundTag.getString(COMPOUND_KEY_MIRROR_FACING));
        if (facing == null) {
            facing = fallback;
        }
        return readRotation(facing, compoundTag);
    }

    static Quaternionf readRotation(Direction facing, CompoundTag compoundTag) {
        int[] components = compoundTag.getIntArray(COMPOUND_KEY_MIRROR_ROTATION);
        if (components.length != 4) {
            return facing.getRotation();
        }

        Quaternionf rotation = new Quaternionf(Float.intBitsToFloat(components[0]), Float.intBitsToFloat(components[1]), Float.intBitsToFloat(components[2]), Float.intBitsToFloat(components[3]));
        if (!rotation.isFinite() || Math.abs(rotation.lengthSquared() - 1) > UNIT_ROTATION_TOLERANCE) {
            return facing.getRotation();
        }

        return rotation;
    }
}
